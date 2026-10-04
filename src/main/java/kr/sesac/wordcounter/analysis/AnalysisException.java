package kr.sesac.wordcounter.analysis;

public class AnalysisException extends RuntimeException {
    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable e) {
        super(message, e);
    }
}
