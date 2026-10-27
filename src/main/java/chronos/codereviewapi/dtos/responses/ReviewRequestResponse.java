package chronos.codereviewapi.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import chronos.codereviewapi.enums.ReviewStatus;

import java.time.Instant;

public record ReviewRequestResponse(

        Long id,
        String title ,
        String description ,
        String repositoryUrl ,
        @JsonProperty("pullRequestUrl")
        String pullRequestURL ,
        ReviewStatus status ,
        UserSummaryResponse requesterId ,
        UserSummaryResponse reviewerId ,
        Instant updatedAt ,
        Instant createdAt

){}
