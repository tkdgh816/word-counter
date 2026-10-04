package kr.sesac.wordcounter.text.tokenizer;

import java.util.function.Consumer;

/**
 * 입력 텍스트를 규칙에 따라 토큰으로 분리합니다.
 */
@FunctionalInterface
public interface TextTokenizer {
    /**
     * 입력 텍스트에서 토큰을 추출하여 발견된 순서대로 전달합니다.
     *
     * <p>토큰은 별도의 목록으로 반환하지 않고 consumer를 통해 하나씩 전달하므로,
     * 호출자는 필요한 방식으로 즉시 처리하거나 저장할 수 있습니다.</p>
     */
    void tokenize(String text, Consumer<String> consumer);
}
