package kr.sesac.wordcounter.text.reader;

public class TextReaderException extends RuntimeException {
    public TextReaderException(String message) {
        super(message);
    }

    public TextReaderException(String message, Throwable e) {
        super(message, e);
    }
}
