package chronos.codereviewapi.exceptions;

public class ReviewRequestConflictException extends RuntimeException {

    public ReviewRequestConflictException(String message) {
        super(message);
    }
}
