package chronos.codereviewapi.dtos.requests;

import chronos.codereviewapi.validation.HttpsUrl;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequestRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @NotBlank(message = "Repository URL is required")
        @Size(max = 2048, message = "Repository URL cannot exceed 2048 characters")
        @HttpsUrl
        String repositoryUrl,

        @JsonProperty("pullRequestUrl")
        @NotBlank(message = "Pull request URL is required")
        @Size(max = 2048, message = "Pull request URL cannot exceed 2048 characters")
        @HttpsUrl
        String pullRequestURL
) {
}
