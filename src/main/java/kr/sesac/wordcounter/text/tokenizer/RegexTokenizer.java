package kr.sesac.wordcounter.text.tokenizer;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정규식을 사용하여 영어, 한글, 숫자로 구성된 토큰을 추출합니다.
 */
public class RegexTokenizer implements TextTokenizer {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[0-9]+");

    @Override
    public void tokenize(String text, Consumer<String> consumer) {
        if (text == null) {
            throw new IllegalArgumentException("text는 null일 수 없습니다.");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("consumer는 null일 수 없습니다.");
        }

        // 영어, 한글, 숫자로만 이루어진 토큰
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(text);
        while (tokenMatcher.find()) {
            String token = tokenMatcher.group();

            // 숫자만으로 이루어진 토큰은 배제
            if (!NUMERIC_PATTERN.matcher(token).matches()) {
                consumer.accept(token.toLowerCase(Locale.ROOT));
            }
        }
    }
}
