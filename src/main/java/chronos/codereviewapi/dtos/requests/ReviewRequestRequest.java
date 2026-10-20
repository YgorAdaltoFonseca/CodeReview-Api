package chronos.codereviewapi.dtos.requests;

public record ReviewRequestRequest(
        String title ,
        String description ,
        String repositoryUrl ,
        String pullRequestURL
){}
