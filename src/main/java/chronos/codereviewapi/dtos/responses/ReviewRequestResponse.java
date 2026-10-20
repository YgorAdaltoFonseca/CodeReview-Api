package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;

import java.time.Instant;

public record ReviewRequestResponse(

        String title ,
        String description ,
        String repositoryUrl ,
        String pullRequestURL ,
        ReviewStatus status ,
        UserEntity requester ,
        UserEntity reviewer ,
        Instant updatedAt ,
        Instant createdAt

){}
