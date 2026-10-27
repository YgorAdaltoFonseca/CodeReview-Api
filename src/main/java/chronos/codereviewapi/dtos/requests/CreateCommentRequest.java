package chronos.codereviewapi.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 5000, message = "Comment cannot exceed 5000 characters")
        String content
) {
}
