package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final ReviewRequestRepository reviewRequestRepository;

    private final AuthenticatedUserService authenticatedUserService;

    public ReviewRequestResponse createReviewRequest(
            ReviewRequestRequest reviewRequestRequest) {

        UserEntity user = authenticatedUserService.getAuthenticatedUser();

        ReviewRequestEntity reviewRequestEntity = ReviewRequestEntity.builder()
                .title(reviewRequestRequest.title())
                .description(reviewRequestRequest.description())
                .repositoryURL(reviewRequestRequest.repositoryUrl())
                .pullRequestURL(reviewRequestRequest.pullRequestURL())
                .requesterId(user)
                .reviewerId(user)
                .build();

        ReviewRequestEntity reviewRequestEntitySaved =
                reviewRequestRepository.save(reviewRequestEntity);

        return new ReviewRequestResponse(
                reviewRequestEntitySaved.getTitle() ,
                reviewRequestEntitySaved.getDescription() ,
                reviewRequestEntitySaved.getRepositoryURL() ,
                reviewRequestEntitySaved.getPullRequestURL() ,
                reviewRequestEntitySaved.getStatus() ,
                reviewRequestEntitySaved.getRequesterId() ,
                reviewRequestEntitySaved.getReviewerId() ,
                reviewRequestEntitySaved.getUpdatedAt() ,
                reviewRequestEntitySaved.getCreatedAt()
        );
    }


}
