package kr.sesac.wordcounter.analysis;

import kr.sesac.wordcounter.analysis.summary.AnalysisSummary;
import kr.sesac.wordcounter.analysis.summary.FileAnalysisFailure;
import kr.sesac.wordcounter.analysis.summary.FileAnalysisSummary;
import kr.sesac.wordcounter.analysis.summary.WordAnalysisSummary;
import kr.sesac.wordcounter.checkpoint.CheckpointException;
import kr.sesac.wordcounter.checkpoint.CheckpointSession;
import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.repository.RepositoryException;
import kr.sesac.wordcounter.repository.TokenCountRepository;
import kr.sesac.wordcounter.text.reader.TextReader;
import kr.sesac.wordcounter.text.reader.TextReaderException;
import kr.sesac.wordcounter.text.reader.TextReaderKind;
import kr.sesac.wordcounter.text.tokenizer.TextTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Analyzer implements Analysis {
    private final TextTokenizer tokenizer;
    private final TokenCountRepository repository;
    private static final int MAX_THREAD_COUNT = 4;
    private long totalTokenCount;    // 전체 분석 단어 횟수
    private int successfulFileCount;   // 지원 파일 분석 성공 횟수
    private final AnalysisSummary analysisSummary;
    private final List<FileAnalysisFailure> fileFailures = Collections.synchronizedList(new ArrayList<>());
    private final AtomicReference<Throwable> analysisFailure = new AtomicReference<>(null);

    public static Analyzer analyze(String pathString, TextTokenizer tokenizer, TokenCountRepository repository, ProcessingMode processingMode, boolean useCheckpoint) throws IOException, AnalysisException, RepositoryException {
        return new Analyzer(pathString, tokenizer, repository, processingMode, useCheckpoint);
    }

    private Analyzer(String pathString, TextTokenizer tokenizer, TokenCountRepository repository, ProcessingMode processingMode, boolean useCheckpoint) throws IOException, AnalysisException, RepositoryException {
        if (tokenizer == null) {
            throw new IllegalArgumentException("tokenizer는 null일 수 없습니다.");
        }

        if (repository == null) {
            throw new IllegalArgumentException("repository는 null일 수 없습니다.");
        }

        if (processingMode == null) {
            throw new IllegalArgumentException("processingMode는 null일 수 없습니다.");
        }

        Path inputPath;
        Map<Path, TextReaderKind> readerKindsByPath = new HashMap<>();
        int skippedFileCount = 0;

        try {
            inputPath = Path.of(pathString);
            List<Path> filePaths;

            if (Files.isDirectory(inputPath)) {
                // 경로가 디렉터리인 경우 하위 파일(디렉터리 제외) 경로들을 목록에 넣음
                try (var fileListStream = Files.list(inputPath)) {
                    filePaths = fileListStream.filter(Files::isRegularFile).toList();
                }
            } else if (Files.isRegularFile(inputPath)) {
                // 경로가 파일인 경우 그 경로를 목록에 넣음
                filePaths = List.of(inputPath);
            } else {
                throw new IllegalArgumentException("잘못된 경로입니다: " + inputPath);
            }


            for (Path filePath : filePaths) {
                TextReaderKind kind = getTextReaderKind(filePath);

                if (kind == null) {
                    skippedFileCount++;
                    continue;
                }

                readerKindsByPath.put(filePath, kind);
            }
        } catch (InvalidPathException e) {
            // 경로 형식 자체가 잘못되었을 때
            throw new IllegalArgumentException("잘못된 경로 형식입니다: " + pathString);
        }

        if (readerKindsByPath.isEmpty()) {
            throw new IllegalArgumentException("지원하는 파일이 없습니다. 지원 확장자: txt, csv, tsv, html, htm");
        }

        this.tokenizer = tokenizer;

        // 새로 입력한 경로가 잘못되거나 지원 파일이 없을 때 저장소를 초기화하지 않음
        this.repository = repository;
        repository.clear();


        long elapsedNanoTime = System.nanoTime();
        // 분석 모드에 따라 분석 시작 (순차/병렬 처리, 체크포인트 사용 여부)
        if (useCheckpoint) {
            switch (processingMode) {
                case SEQUENTIAL -> analyzeSequentialCheckpoint(readerKindsByPath);
                case FIXED_THREAD_POOL -> analyzeFixedThreadPoolCheckpoint(readerKindsByPath);
            }
        } else {
            switch (processingMode) {
                case SEQUENTIAL -> analyzeSequential(readerKindsByPath);
                case FIXED_THREAD_POOL -> analyzeFixedThreadPool(readerKindsByPath);
            }
        }
        // 분석 처리 시간 기록
        elapsedNanoTime = System.nanoTime() - elapsedNanoTime;

        FileAnalysisSummary fileSummary = new FileAnalysisSummary(readerKindsByPath.size(), successfulFileCount, skippedFileCount, List.copyOf(fileFailures));
        WordAnalysisSummary wordSummary = new WordAnalysisSummary(totalTokenCount, repository.getUniqueCount());
        analysisSummary = new AnalysisSummary(
                inputPath,
                fileSummary,
                wordSummary,
                elapsedNanoTime / 1_000_000L);
    }

    @Override
    public boolean hasUsableResult() {
        return successfulFileCount > 0;
    }

    private void analyzeSequential(Map<Path, TextReaderKind> readerKindsByPath) {
        try {
            readerKindsByPath.forEach((filePath, readerKind) ->
                    analyzeForEachFile(filePath, readerKind).ifPresent(this::integrate));
        } catch (RuntimeException e) {
            throw new AnalysisException("분석 작업이 실패했습니다.", e);
        }
    }

    private void analyzeFixedThreadPool(Map<Path, TextReaderKind> readerKindsByPath) throws AnalysisException {
        // 적절한 스레드 개수를 계산하여 스레드 풀 생성
        int threadCount = Math.min(readerKindsByPath.size(), MAX_THREAD_COUNT);
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            var cfs = readerKindsByPath.entrySet().stream()
                    .map(entry -> CompletableFuture
                            .supplyAsync(() -> {
                                if (analysisFailure.get() != null) {
                                    throw new CancellationException("전체 분석 실패로 작업 중단됨");
                                }
                                return analyzeForEachFile(entry.getKey(), entry.getValue());
                            }, executorService)
                            // 파일 분석 결과를 얻으면
                            .thenAccept(result -> result.ifPresent(this::integrate))
                            // 예외가 발생하면(통합 집계 중 예외 혹은 그로 인해 다른 작업 취소 시)
                            .whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    analysisFailure.compareAndSet(null, throwable);
                                }
                            }))
                    .toArray(CompletableFuture[]::new);

            try {
                CompletableFuture.allOf(cfs).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AnalysisException("분석 작업이 중단되었습니다.", e);
            } catch (ExecutionException e) {
                throw new AnalysisException("분석 작업이 실패했습니다.", e);
            }
        }
    }

    // 파일을 분석하고 결과를 집계, 실패 시 실패 목록에 추가
    private Optional<FileTokenCountResult> analyzeForEachFile(Path filePath, TextReaderKind textReaderKind) {
        try {
            FileTokenCountResult result = SourceFileTokenCounter.countTokens(filePath, textReaderKind.createReader(), tokenizer);
            return Optional.of(result);
        } catch (TextReaderException e) {
            fileFailures.add(new FileAnalysisFailure(filePath, e.getMessage()));
            return Optional.empty();
        }
    }

    // 각 파일 분석 결과를 통합하여 집계
    private synchronized void integrate(FileTokenCountResult fileTokenCountResult) {
        if (fileTokenCountResult != null) {
            repository.addAll(fileTokenCountResult.getTokenCounts());
            totalTokenCount += fileTokenCountResult.getTotalTokenCount();
            successfulFileCount++;
        }
    }

    @Override
    public List<TokenCount> getTopWords(int limit) {
        return repository.findTop(limit);
    }

    @Override
    public List<TokenCount> getAllWordsSorted() {
        return repository.findAllSorted();
    }

    @Override
    public TokenCount findByWord(String word) {
        List<String> filteredTokens = new ArrayList<>();
        tokenizer.tokenize(word, filteredTokens::add);

        if (filteredTokens.size() != 1) {
            throw new IllegalArgumentException("한 개의 단어여야 합니다.");
        }

        String targetToken = filteredTokens.getFirst();
        return repository.findByToken(targetToken);
    }

    @Override
    public AnalysisSummary getSummary() {
        return analysisSummary;
    }

    // 경로 검사하여 확장자 기반으로 TextReaderKind 반환
    // 실패하면 null 반환
    private static TextReaderKind getTextReaderKind(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            try {
                return TextReaderKind.fromExtension(fileName.substring(dotIndex + 1));
            } catch (IllegalArgumentException e) {
                // 아래에서 null 반환
            }
        }

        return null;
    }

    // 체크포인트 사용 시 분석
    private void analyzeSequentialCheckpoint(Map<Path, TextReaderKind> readerKindsByPath) {
        try (CheckpointSession checkpointSession = CheckpointSession.open()) {
            try {
                readerKindsByPath.forEach((filePath, readerKind) -> {
                    checkpointSession.restore(filePath).ifPresentOrElse(
                            // 체크포인트 있는 경우 바로 집계
                            this::integrate,
                            // 체크포인트 없는 경우
                            () -> {
                                System.out.println("새 분석 시작: " + filePath);

                                // 원본 파일 분석 시작
                                analyzeForEachFile(filePath, readerKind).ifPresent(result -> integrateAndSaveCheckpoint(checkpointSession, result));
                            }
                    );
                });
            } catch (RuntimeException e) {
                throw new AnalysisException("분석 작업이 실패했습니다.", e);
            }
        } catch (CheckpointException e) {
            // 종료 시 체크포인트 메타데이터 저장 실패는 분석 결과에 영향을 주지 않음
            System.out.println("체크포인트 메타데이터 파일 저장에 실패했습니다. 분석 결과는 사용할 수 있습니다.");
        }
    }

    private void analyzeFixedThreadPoolCheckpoint(Map<Path, TextReaderKind> readerKindsByPath) throws AnalysisException {
        record AnalyzedFileTokenCountResult(Optional<FileTokenCountResult> result, boolean fromCheckpoint) {
        }

        try (CheckpointSession checkpointSession = CheckpointSession.open()) {
            // 적절한 스레드 개수를 계산하여 스레드 풀 생성
            int threadCount = Math.min(readerKindsByPath.size(), MAX_THREAD_COUNT);
            try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
                var cfs = readerKindsByPath.entrySet().stream()
                        .map(entry -> CompletableFuture
                                .supplyAsync(() -> {
                                    if (analysisFailure.get() != null) {
                                        throw new CancellationException("전체 분석 실패로 작업 중단됨");
                                    }

                                    Path filePath = entry.getKey();

                                    var restored = checkpointSession.restore(filePath);
                                    if (restored.isPresent()) {
                                        return new AnalyzedFileTokenCountResult(restored, true);
                                    }

                                    System.out.println("새 분석 시작: " + filePath);
                                    return new AnalyzedFileTokenCountResult(analyzeForEachFile(filePath, entry.getValue()), false);
                                }, executorService)
                                // 파일 분석 결과를 얻으면
                                .thenAccept(analyzed ->
                                        analyzed.result().ifPresent(result -> {
                                            if (analyzed.fromCheckpoint()) {
                                                // 복원된 결과는 집계만 수행
                                                integrate(result);
                                            } else {
                                                // 새 분석 결과는 집계 후 체크포인트 저장
                                                integrateAndSaveCheckpoint(checkpointSession, result);
                                            }
                                        })
                                )
                                // 예외가 발생하면(통합 집계 중 예외 혹은 그로 인해 다른 작업 취소 시)
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        analysisFailure.compareAndSet(null, throwable);
                                    }
                                }))
                        .toArray(CompletableFuture[]::new);
                try {
                    CompletableFuture.allOf(cfs).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AnalysisException("분석 작업이 중단되었습니다.", e);
                } catch (ExecutionException e) {
                    throw new AnalysisException("분석 작업이 실패했습니다.", e);
                }
            }
        } catch (CheckpointException e) {
            // 종료 시 체크포인트 메타데이터 저장 실패는 분석 결과에 영향을 주지 않음
            System.out.println("체크포인트 메타데이터 파일 저장에 실패했습니다. 분석 결과는 사용할 수 있습니다.");
        }
    }

    private void integrateAndSaveCheckpoint(CheckpointSession checkpointSession, FileTokenCountResult fileTokenCountResult) {
        // 분석 결과를 집계
        integrate(fileTokenCountResult);

        // 분석 및 집계가 성공하면 체크포인트에 기록, 기록 실패하더라도 전체 분석 실패는 아님
        try {
            checkpointSession.save(fileTokenCountResult);
        } catch (CheckpointException e) {
            // 분석 성공했지만 체크포인트 저장에 실패한 경우
            System.out.println("체크포인트 저장 실패: " + fileTokenCountResult.getFilePath());
            System.out.println("분석 결과는 집계되었습니다.");
        }
    }
}