package chronos.codereviewapi.exceptions;

public class ReviewRequestNotFoundException extends RuntimeException {

    public ReviewRequestNotFoundException(Long reviewRequestId) {
        super("Review request " + reviewRequestId + " was not found.");
    }
}
