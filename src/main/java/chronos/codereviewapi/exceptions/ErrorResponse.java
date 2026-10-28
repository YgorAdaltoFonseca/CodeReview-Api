package chronos.codereviewapi.exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this(status, standardErrorCode(status), message, timestamp);
    }

    private static String standardErrorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> status >= 500 ? "INTERNAL_SERVER_ERROR" : "HTTP_ERROR";
        };
    }
}
