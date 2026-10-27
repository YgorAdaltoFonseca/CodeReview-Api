package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.enums.ReviewStatus;

import java.time.Instant;

public record ReviewRequestResponse(

        Long id,
        String title ,
        String description ,
        String repositoryUrl ,
        String pullRequestURL ,
        ReviewStatus status ,
        UserSummaryResponse requesterId ,
        UserSummaryResponse reviewerId ,
        Instant updatedAt ,
        Instant createdAt

){}
