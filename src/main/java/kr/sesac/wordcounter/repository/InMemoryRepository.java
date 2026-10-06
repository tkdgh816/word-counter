package kr.sesac.wordcounter.repository;

import kr.sesac.wordcounter.model.TokenCount;

import java.util.*;

public class InMemoryRepository implements TokenCountRepository {
    private final Map<String, Long> countsByToken = new HashMap<>();

    @Override
    public void clear() {
        countsByToken.clear();
    }

    @Override
    public long getUniqueCount() {
        return countsByToken.size();
    }

    @Override
    public void addAll(Collection<TokenCount> tokenCounts) {
        for (TokenCount tokenCount : tokenCounts) {
            String token = tokenCount.token();
            countsByToken.put(token, countsByToken.getOrDefault(token, 0L) + tokenCount.count());
        }
    }

    // 토큰 정렬 방법
    private static final Comparator<Map.Entry<String, Long>> TOKEN_COUNT_COMPARATOR =
            (e1, e2) -> {
                // 출현 횟수 내림차순
                int countComparison = Long.compare(e2.getValue(), e1.getValue());

                // 출현 횟수가 같으면 String.compareTo 오름차순
                if (countComparison == 0) {
                    return e1.getKey().compareTo(e2.getKey());
                }

                return countComparison;
            };

    @Override
    public List<TokenCount> findTop(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit은 0 이상이어야 합니다.");
        }

        return countsByToken.entrySet().stream()
                .sorted(TOKEN_COUNT_COMPARATOR)
                .limit(limit)
                .map(entry -> new TokenCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public TokenCount findByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
        }

        return new TokenCount(token, countsByToken.getOrDefault(token, 0L));
    }

    @Override
    public List<TokenCount> findAllSorted() {
        return countsByToken.entrySet().stream()
                .sorted(TOKEN_COUNT_COMPARATOR)
                .map(entry -> new TokenCount(entry.getKey(), entry.getValue()))
                .toList();
    }
}
