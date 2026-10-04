package kr.sesac.wordcounter.repository;

import kr.sesac.wordcounter.model.TokenCount;

import java.util.Collection;
import java.util.List;

/**
 * 토큰별 출현 횟수를 저장하고 조회하기 위한 저장소입니다.
 */
public interface TokenCountRepository {
    /**
     * 새 분석을 위해 저장소에 존재하는 기존 집계 결과를 모두 제거합니다.
     * @throws RepositoryException 저장소를 초기화하지 못한 경우
     */
    void clear();

    /**
     * 서로 다른 토큰 개수를 반환합니다.
     * @throws RepositoryException 저장소에서 토큰 개수를 조회하지 못한 경우
     */
    long getUniqueCount();

    /**
     * 주어진 토큰 집계 결과를 저장소의 기존 결과에 합산합니다.
     * @throws RepositoryException 집계 결과를 저장하지 못한 경우
     */
    void addAll(Collection<TokenCount> tokenCounts);

    /**
     * 출현 횟수 내림차순, 토큰 오름차순으로 상위 결과를 조회합니다.
     * @throws IllegalArgumentException {@code limit}이 음수인 경우
     * @throws RepositoryException 집계 결과를 조회하지 못한 경우
     */
    List<TokenCount> findTop(int limit);

    /**
     * 지정한 토큰의 집계 결과를 조회합니다.
     * @throws IllegalArgumentException {@code token}이 {@code null}이거나 유효하지 않은 경우
     * @throws RepositoryException 집계 결과를 조회하지 못한 경우
     */
    TokenCount findByToken(String token);

    /**
     * 저장소에 집계된 모든 토큰을 정렬하여 반환합니다.
     * @throws RepositoryException 집계 결과를 조회하지 못한 경우
     */
    List<TokenCount> findAllSorted();
}
