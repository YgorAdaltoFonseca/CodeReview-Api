package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.enums.ReviewStatus;

import java.time.Instant;

public record ReviewRequestResponse(
        Long id,
        String title,
        String description,
        String repositoryUrl,
        String pullRequestUrl,
        ReviewStatus status,
        UserReferenceResponse requester,
        UserReferenceResponse reviewer,
        Instant createdAt,
        Instant updatedAt
) {
}
