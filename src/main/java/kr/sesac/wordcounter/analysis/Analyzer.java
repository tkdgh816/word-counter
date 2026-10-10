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

public class Analyzer implements Analysis {
    private final TextTokenizer tokenizer;
    private final TokenCountRepository repository;
    private static final int MAX_THREAD_COUNT = 4;
    private long totalTokenCount;    // 전체 분석 단어 횟수
    private int successfulFileCount;   // 지원 파일 분석 성공 횟수
    private final AnalysisSummary analysisSummary;
    private final List<FileAnalysisFailure> failures = new ArrayList<>();

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

        FileAnalysisSummary fileSummary = new FileAnalysisSummary(readerKindsByPath.size(), successfulFileCount, skippedFileCount, List.copyOf(failures));
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
        for (var entry : readerKindsByPath.entrySet()) {
            Path filePath = entry.getKey();
            // 파일 유형에 해당하는 TextReader 가져옴
            TextReader textReader = entry.getValue().createReader();

            // 파일을 분석하고 결과를 집계, 실패 시 실패 목록에 추가
            try {
                FileTokenCountResult fileTokenCountResult = SourceFileTokenCounter.countTokens(filePath, textReader, tokenizer);
                integrate(fileTokenCountResult);
            } catch (TextReaderException e) {
                failures.add(new FileAnalysisFailure(filePath, e.getMessage()));
            }
        }
    }

    private void analyzeFixedThreadPool(Map<Path, TextReaderKind> readerKindsByPath) throws AnalysisException {
        // 파일 경로와 파일 분석 결과
        Map<Path, Future<FileTokenCountResult>> futuresByFilePath = new HashMap<>();

        // 적절한 스레드 개수를 계산하여 스레드 풀 생성
        int threadCount = Math.min(readerKindsByPath.size(), MAX_THREAD_COUNT);
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (var entry : readerKindsByPath.entrySet()) {
                Path filePath = entry.getKey();

                // 개별 파일 분석
                TextReader textReader = entry.getValue().createReader();
                Future<FileTokenCountResult> future = executorService.submit(() -> SourceFileTokenCounter.countTokens(filePath, textReader, tokenizer));

                futuresByFilePath.put(filePath, future);
            }
        }

        for (var entry : futuresByFilePath.entrySet()) {
            Path filePath = entry.getKey();
            Future<FileTokenCountResult> future = entry.getValue();

            try {
                // 개별 파일 분석 작업 완료를 기다리고 결과 통합
                FileTokenCountResult fileTokenCountResult = future.get();
                integrate(fileTokenCountResult);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AnalysisException("분석 작업이 중단되었습니다.", e);
            } catch (ExecutionException e) {
                // 개별 파일 분석 실패 시 실패 목록에 추가
                if (e.getCause() instanceof TextReaderException textReaderException) {
                    failures.add(new FileAnalysisFailure(filePath, textReaderException.getMessage()));
                    continue;
                }
                throw new AnalysisException("분석 작업이 실패했습니다.", e.getCause());
            } catch (CancellationException e) {
                throw new AnalysisException("분석 작업이 취소되었습니다.", e);
            }
        }
    }

    // 각 파일 분석 결과를 통합하여 집계
    private void integrate(FileTokenCountResult fileTokenCountResult) {
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
            for (var entry : readerKindsByPath.entrySet()) {
                Path filePath = entry.getKey();

                checkpointSession.restore(filePath).ifPresentOrElse(
                        this::integrate,
                        () -> {
                            System.out.println("새 분석 시작: " + filePath);
                            // 파일 유형에 해당하는 TextReader 가져옴
                            TextReader textReader = entry.getValue().createReader();

                            // 파일을 분석하고 결과를 집계, 실패 시 실패 목록에 추가
                            try {
                                FileTokenCountResult fileTokenCountResult = SourceFileTokenCounter.countTokens(filePath, textReader, tokenizer);
                                integrate(fileTokenCountResult);
                                checkpointSession.save(filePath, fileTokenCountResult);
                            } catch (TextReaderException e) {
                                failures.add(new FileAnalysisFailure(filePath, e.getMessage()));
                            } catch (CheckpointException e) {
                                // 분석 성공했지만 체크포인트 저장에 실패한 경우
                                System.out.println("체크포인트 저장 실패: " + filePath);
                                System.out.println("분석 결과는 집계되었습니다.");
                            }
                        }
                );
            }
        } catch (CheckpointException e) {
            // 세션 종료 시 체크포인트 메타데이터 저장 실패는 분석 결과에 영향을 주지 않음
            System.out.println("체크포인트 메타데이터 파일 저장에 실패했습니다. 분석 결과는 사용할 수 있습니다.");
        }
    }

    private void analyzeFixedThreadPoolCheckpoint(Map<Path, TextReaderKind> readerKindsByPath) throws AnalysisException {
        record FileTokenCounterResult(FileTokenCountResult fileTokenCounter, boolean fromCheckpoint) {
        }

        // 파일 경로와 파일 분석 결과
        Map<Path, Future<FileTokenCounterResult>> futuresByFilePath = new HashMap<>();

        try (CheckpointSession checkpointSession = CheckpointSession.open()) {
            // 적절한 스레드 개수를 계산하여 스레드 풀 생성
            int threadCount = Math.min(readerKindsByPath.size(), MAX_THREAD_COUNT);
            try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
                for (var entry : readerKindsByPath.entrySet()) {
                    Path filePath = entry.getKey();

                    // 개별 파일 분석
                    Future<FileTokenCounterResult> future = executorService.submit(() ->
                    {
                        Optional<FileTokenCountResult> restored = checkpointSession.restore(filePath);
                        if (restored.isPresent()) {
                            return new FileTokenCounterResult(restored.get(), true);
                        }

                        System.out.println("새 분석 시작: " + filePath);
                        // 파일 유형에 해당하는 TextReader 가져옴
                        TextReader textReader = entry.getValue().createReader();

                        // 파일을 분석하고 결과를 집계, 실패 시 실패 목록에 추가
                        FileTokenCountResult fileTokenCountResult = SourceFileTokenCounter.countTokens(filePath, textReader, tokenizer);
                        return new FileTokenCounterResult(fileTokenCountResult, false);
                    });

                    futuresByFilePath.put(filePath, future);
                }
            }


            for (var entry : futuresByFilePath.entrySet()) {
                Path filePath = entry.getKey();
                Future<FileTokenCounterResult> future = entry.getValue();

                try {
                    // 개별 파일 분석 작업 완료를 기다리고 결과 통합
                    FileTokenCounterResult result = future.get();
                    integrate(result.fileTokenCounter());
                    if (!result.fromCheckpoint()) {
                        try {
                            checkpointSession.save(filePath, result.fileTokenCounter());
                        } catch (CheckpointException e) {
                            // 체크포인트 저장 실패하더라도 분석 결과는 이미 집계됨
                            // 다음 파일 결과 처리 계속
                            System.out.println("체크포인트 저장 실패: " + filePath);
                            System.out.println("분석 결과는 집계되었습니다.");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AnalysisException("분석 작업이 중단되었습니다.", e);
                } catch (ExecutionException e) {
                    // 개별 파일 분석 실패 시 실패 목록에 추가
                    Throwable cause = e.getCause();
                    if (cause instanceof TextReaderException textReaderException) {
                        failures.add(new FileAnalysisFailure(filePath, textReaderException.getMessage()));
                        continue;
                    }
                    throw new AnalysisException("분석 작업이 실패했습니다.", e.getCause());
                } catch (CancellationException e) {
                    throw new AnalysisException("분석 작업이 취소되었습니다.", e);
                }
            }
        } catch (CheckpointException e) {
            // 세션 종료 시 체크포인트 메타데이터 저장 실패는 분석 결과에 영향을 주지 않음
            System.out.println("체크포인트 메타데이터 파일 저장에 실패했습니다. 분석 결과는 사용할 수 있습니다.");
        }
    }
}