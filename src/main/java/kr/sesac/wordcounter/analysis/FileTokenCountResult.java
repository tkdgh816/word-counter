package kr.sesac.wordcounter.analysis;

import kr.sesac.wordcounter.model.TokenCount;

import java.nio.file.Path;
import java.util.List;

/**
 * 파일 하나에 대한 토큰 집계 결과를 제공합니다.
 */
public interface FileTokenCountResult {
    Path getFilePath();

    /**
     * 집계된 토큰별 출현 횟수를 반환합니다.
     */
    List<TokenCount> getTokenCounts();

    /**
     * 중복 출현을 포함하여 집계된 전체 토큰 수를 반환합니다.
     */
    long getTotalTokenCount();
}