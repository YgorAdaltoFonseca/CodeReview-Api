package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.enums.ReviewStatus;

import java.time.Instant;

public record ReviewStatusHistoryItemResponse(
        ReviewStatus oldStatus,
        ReviewStatus newStatus,
        UserReferenceResponse changedBy,
        Instant changedAt
) {
}
