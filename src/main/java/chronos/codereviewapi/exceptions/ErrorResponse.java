package chronos.codereviewapi.exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status ,
        String message ,
        LocalDateTime timestamp
) {
}
