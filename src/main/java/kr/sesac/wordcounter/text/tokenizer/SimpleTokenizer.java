package kr.sesac.wordcounter.text.tokenizer;

import java.util.Locale;
import java.util.function.Consumer;

public class SimpleTokenizer implements TextTokenizer {
    @Override
    public void tokenize(String text, Consumer<String> consumer) {
        if (text == null) {
            throw new IllegalArgumentException("text는 null일 수 없습니다.");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("consumer는 null일 수 없습니다.");
        }

        int startIndex = 0;
        boolean containsLetter = false;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (isNumber(character)) {
                continue;
            }

            if (isAlphabet(character) || isHangeul(character)) {
                containsLetter = true;
                continue;
            }

            if (containsLetter) {
                consumer.accept(text.substring(startIndex, i).toLowerCase(Locale.ROOT));
            }

            startIndex = i + 1;
            containsLetter = false;
        }


        if (containsLetter) {
            consumer.accept(text.substring(startIndex).toLowerCase(Locale.ROOT));
        }
    }

    private boolean isNumber(char character) {
        return character >= '0' && character <= '9';
    }

    private boolean isAlphabet(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    private boolean isHangeul(char character) {
        return (character >= '가' && character <= '힣') || (character >= 'ㄱ' && character <= 'ㅎ') || (character >= 'ㅏ' && character <= 'ㅣ');
    }
}
