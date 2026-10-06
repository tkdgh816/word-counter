package kr.sesac.wordcounter.analysis;

import kr.sesac.wordcounter.analysis.summary.AnalysisSummary;
import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.repository.RepositoryException;

import java.util.List;

/**
 * 텍스트 분석 결과를 조회합니다.
 * <p>분석에 성공한 파일의 존재 여부, 단어별 출현 횟수,
 * 정렬된 집계 결과 및 전체 분석 요약을 제공합니다.</p>
 */
public interface Analysis {
    /**
     * 단어 집계 결과를 사용할 수 있는지 확인합니다.
     * 하나 이상의 입력 대상이 성공적으로 분석되었으면 {@code true}
     */
    boolean hasUsableResult();

    /**
     * 출현 횟수가 많은 순서대로 최대 {@code limit}개의 단어를 반환합니다.
     * <p>출현 횟수가 같으면 토큰의 문자열 오름차순으로 정렬합니다.
     * {@code limit}이 전체 단어 개수보다 크면 전체 결과를 반환하고,
     * {@code 0}이면 빈 목록을 반환합니다.</p>
     * @throws IllegalArgumentException {@code limit}이 음수인 경우
     * @throws RepositoryException 저장소에서 집계 결과를 조회하지 못한 경우
     */
    List<TokenCount> getTopWords(int limit);

    /**
     * 집계된 모든 단어를 정렬하여 반환합니다.
     * <p>출현 횟수 내림차순으로 정렬하며, 출현 횟수가 같으면
     * 토큰의 문자열 오름차순으로 정렬합니다.</p>
     * @throws RepositoryException 저장소에서 집계 결과를 조회하지 못한 경우
     */
    List<TokenCount> getAllWordsSorted();

    /**
     * 입력값을 분석에 사용한 토큰화 규칙으로 처리한 후, 해당 단어의 출현 횟수를 조회합니다.
     * <p>입력값은 토큰화 결과가 정확히 하나의 단어여야 합니다.</p>
     * @throws IllegalArgumentException 입력값의 토큰화 결과가 정확히 하나가 아닌 경우
     * @throws RepositoryException 저장소에서 집계 결과를 조회하지 못한 경우
     */
    TokenCount findByWord(String word);

    /**
     * 입력 경로, 파일 처리 결과, 단어 집계 결과, 처리 시간 및
     * 파일별 실패 내역을 포함하는 전체 분석 요약을 반환합니다.
     */
    AnalysisSummary getSummary();
}
