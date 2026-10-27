package chronos.codereviewapi.dtos.requests;

import chronos.codereviewapi.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
        @NotNull(message = "Decision is required")
        ReviewDecision decision,

        @Size(max = 5000, message = "Comment cannot exceed 5000 characters")
        String comment
) {
}
