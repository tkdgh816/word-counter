package kr.sesac.wordcounter.model;

/**
 * 토큰과 해당 토큰의 출현 횟수를 나타냅니다.
 */
public record TokenCount(String token, long count) {}
