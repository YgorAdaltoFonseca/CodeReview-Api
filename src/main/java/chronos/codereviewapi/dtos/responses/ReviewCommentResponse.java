package chronos.codereviewapi.dtos.responses;

import java.time.Instant;

public record ReviewCommentResponse(
        Long id,
        Long reviewRequestId,
        Long authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}
