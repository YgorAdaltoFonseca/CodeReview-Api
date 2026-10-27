package chronos.codereviewapi.dtos.requests;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @NotBlank(message = "Description cannot be blank")
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description
) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return title != null || description != null;
    }
}
