package kr.sesac.wordcounter.checkpoint;

import kr.sesac.wordcounter.analysis.FileTokenCountResult;
import kr.sesac.wordcounter.model.TokenCount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CheckpointFileTokenCounter implements FileTokenCountResult {
    private final Map<String, Long> countsByToken = new HashMap<>();
    private long totalTokenCount;

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

    public void add(String token, Long count){
        countsByToken.put(token, countsByToken.getOrDefault(token, 0L) + count);
        totalTokenCount += count;
    }
}
