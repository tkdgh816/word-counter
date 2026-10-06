package kr.sesac.wordcounter.checkpoint;

public class CheckpointException extends RuntimeException {
    public CheckpointException(String message) {
        super(message);
    }

    public CheckpointException(String message, Throwable e) {
        super(message, e);
    }
}
