package kr.sesac.wordcounter.analysis;

import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.text.reader.TextReader;
import kr.sesac.wordcounter.text.reader.TextReaderException;
import kr.sesac.wordcounter.text.tokenizer.TextTokenizer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SourceFileTokenCounter implements FileTokenCountResult {
    private final Path filePath;
    private final TextReader textReader;
    private final TextTokenizer tokenizer;

    private final Map<String, Long> countsByToken = new HashMap<>();
    private long totalTokenCount;

    private SourceFileTokenCounter(Path filePath, TextReader textReader, TextTokenizer tokenizer) throws TextReaderException {
        this.filePath = filePath;
        this.textReader = textReader;
        this.tokenizer = tokenizer;
    }

    public static SourceFileTokenCounter countTokens(Path filePath, TextReader textReader, TextTokenizer tokenizer) throws TextReaderException {
        SourceFileTokenCounter tokenCounter = new SourceFileTokenCounter(filePath, textReader, tokenizer);

        // 파일 분석 시작
        tokenCounter.analyze();

        return tokenCounter;
    }

    private void analyze() {
        textReader.parseText(filePath, text ->
                tokenizer.tokenize(text, token -> {
                    // 토큰이 Map에 없으면 1을 저장하고, 있으면 기존 값과 1을 합산
                    countsByToken.merge(token, 1L, Long::sum);
                    totalTokenCount++;
                }));
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }

    @Override
    public List<TokenCount> getTokenCounts() {
        return countsByToken.entrySet().stream()
                .map(entry -> new TokenCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public long getTotalTokenCount() {
        return totalTokenCount;
    }
}
