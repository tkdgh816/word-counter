package kr.sesac.wordcounter.analysis.test;

import kr.sesac.wordcounter.analysis.Analysis;
import kr.sesac.wordcounter.analysis.AnalysisException;
import kr.sesac.wordcounter.analysis.ProcessingMode;
import kr.sesac.wordcounter.analysis.summary.AnalysisSummary;
import kr.sesac.wordcounter.analysis.summary.FileAnalysisFailure;
import kr.sesac.wordcounter.analysis.summary.FileAnalysisSummary;
import kr.sesac.wordcounter.analysis.summary.WordAnalysisSummary;
import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.repository.RepositoryException;
import kr.sesac.wordcounter.repository.test.DbAggregationMode;
import kr.sesac.wordcounter.repository.test.JdbcTestRepository;
import kr.sesac.wordcounter.text.reader.TextReader;
import kr.sesac.wordcounter.text.reader.TextReaderException;
import kr.sesac.wordcounter.text.reader.TextReaderKind;
import kr.sesac.wordcounter.text.tokenizer.TextTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class JdbcTestAnalyzer implements Analysis {
    private final TextTokenizer tokenizer;
    private static final JdbcTestRepository REPOSITORY = new JdbcTestRepository();
    private static final int MAX_THREAD_COUNT = 4;
    private final DbAggregationMode dbAggregationMode;
    private long totalTokenCount;    // 전체 분석 단어 횟수
    private int successfulFileCount;   // 지원 파일 분석 성공 횟수
    private final AnalysisSummary analysisSummary;
    private final List<FileAnalysisFailure> failures = new ArrayList<>();

    public static JdbcTestAnalyzer analyze(String pathString, TextTokenizer tokenizer, DbAggregationMode dbAggregationMode, ProcessingMode processingMode) throws IOException, AnalysisException, RepositoryException {
        return new JdbcTestAnalyzer(pathString, tokenizer, dbAggregationMode, processingMode);
    }

    private JdbcTestAnalyzer(String pathString, TextTokenizer tokenizer, DbAggregationMode dbAggregationMode, ProcessingMode processingMode) throws IOException, AnalysisException, RepositoryException {
        if (tokenizer == null) {
            throw new IllegalArgumentException("tokenizer는 null일 수 없습니다.");
        }

        if (dbAggregationMode == null) {
            throw new IllegalArgumentException("dbAggregationMode는 null일 수 없습니다.");
        }

        if (processingMode == null) {
            throw new IllegalArgumentException("analysisMode는 null일 수 없습니다.");
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
        this.dbAggregationMode = dbAggregationMode;
        REPOSITORY.clear();

        long elapsedNanoTime = System.nanoTime();
        // 분석 모드에 따라 분석 시작 (순차/병렬 처리)
        switch (processingMode) {
            case ProcessingMode.SEQUENTIAL -> analyzeSequential(readerKindsByPath);
            case ProcessingMode.FIXED_THREAD_POOL -> analyzeFixedThreadPool(readerKindsByPath);
        }
        // 분석 처리 시간 기록
        elapsedNanoTime = System.nanoTime() - elapsedNanoTime;

        FileAnalysisSummary fileSummary = new FileAnalysisSummary(readerKindsByPath.size(), successfulFileCount, skippedFileCount,  List.copyOf(failures));
        WordAnalysisSummary wordSummary = new WordAnalysisSummary(totalTokenCount, REPOSITORY.getUniqueCount());
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

    // 개별 토큰이 생성되었을 때 바로 DB 저장소에서 처리하기 위함
    // 이는 파일별로 토큰 집계 결과를 모으는 방식이 아닐 때 해당될 수 있음
    // inner consumer에는 저장소에서 토큰 정보를 처리(저장)하는 동작이 들어갈 수 있음
    private static class TokenConsumer implements Consumer<String> {
        private final Consumer<String> inner;
        private long count;

        public TokenConsumer(Consumer<String> inner) {
            this.inner = inner;
        }

        @Override
        public void accept(String s) {
            inner.accept(s);
            count++;
        }

        public long getCount() {
            return count;
        }
    }

    private void analyzeSequential(Map<Path, TextReaderKind> readerKindsByPath) {
        for (var entry : readerKindsByPath.entrySet()) {
            Path filePath = entry.getKey();
            // 파일 유형에 해당하는 TextReader 가져옴
            TextReader textReader = entry.getValue().createReader();

            // 파일을 분석하고 결과를 집계, 실패 시 실패 목록에 추가
            try (var session = REPOSITORY.createSession(dbAggregationMode)) {
                TokenConsumer tokenConsumer = new TokenConsumer(session::add);
                textReader.parseText(filePath, text ->
                        tokenizer.tokenize(text, tokenConsumer));
                session.complete();
                totalTokenCount += tokenConsumer.getCount();
                successfulFileCount++;
            } catch (RepositoryException | TextReaderException e) {
                failures.add(new FileAnalysisFailure(filePath, e.getMessage()));
            }
        }
    }

    private void analyzeFixedThreadPool(Map<Path, TextReaderKind> readerKindsByPath) throws AnalysisException {
        // 파일 경로와 파일 분석 결과
        Map<Path, Future<Long>> futuresByFilePath = new HashMap<>();

        // 적절한 스레드 개수를 계산하여 스레드 풀 생성
        int threadCount = Math.min(readerKindsByPath.size(), MAX_THREAD_COUNT);
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (var entry : readerKindsByPath.entrySet()) {
                Path filePath = entry.getKey();

                // 개별 파일 분석
                TextReader textReader = entry.getValue().createReader();
                Future<Long> future = executorService.submit(() -> {
                    try (var session = REPOSITORY.createSession(dbAggregationMode)) {
                        TokenConsumer tokenConsumer = new TokenConsumer(session::add);
                        textReader.parseText(filePath, text ->
                                tokenizer.tokenize(text, tokenConsumer));
                        session.complete();
                        return tokenConsumer.getCount();
                    }
                });

                futuresByFilePath.put(filePath, future);
            }
        }

        for (var entry : futuresByFilePath.entrySet()) {
            Path filePath = entry.getKey();
            Future<Long> future = entry.getValue();

            try {
                // 개별 파일 분석 작업 완료를 기다리고 결과 통합
                Long fileTokenCount = future.get();
                totalTokenCount += fileTokenCount;
                successfulFileCount++;
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

    @Override
    public List<TokenCount> getTopWords(int limit) {
        return REPOSITORY.findTop(limit);
    }

    @Override
    public List<TokenCount> getAllWordsSorted() {
        return REPOSITORY.findAllSorted();
    }

    @Override
    public TokenCount findByWord(String word) {
        List<String> filteredTokens = new ArrayList<>();
        tokenizer.tokenize(word, filteredTokens::add);

        if (filteredTokens.size() != 1) {
            throw new IllegalArgumentException("한 개의 단어여야 합니다.");
        }

        String targetToken = filteredTokens.getFirst();
        return REPOSITORY.findByToken(targetToken);
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
}