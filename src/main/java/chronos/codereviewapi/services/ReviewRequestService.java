
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final ReviewRequestRepository reviewRequestRepository;
    private final AuthenticatedUserService authenticatedUserService;

    // CREATE REVIEW REQUEST
    @Transactional
    public ReviewRequestResponse createReviewRequest(
            ReviewRequestRequest reviewRequestRequest
    ) {
        UserEntity user = authenticatedUserService.getAuthenticatedUser();

        ReviewRequestEntity reviewRequestEntity = ReviewRequestEntity.builder()
                .title(reviewRequestRequest.title())
                .description(reviewRequestRequest.description())
                .repositoryURL(reviewRequestRequest.repositoryUrl())
                .pullRequestURL(reviewRequestRequest.pullRequestURL())
                .requesterId(user)
                .reviewerId(user)
                .build();

        ReviewRequestEntity savedReviewRequest =
                reviewRequestRepository.save(reviewRequestEntity);

        return toResponse(savedReviewRequest);
    }

    // LIST REVIEW REQUESTS
    @Transactional(readOnly = true)
    public List<ReviewRequestResponse> listReviewRequests() {

        UserEntity user = authenticatedUserService.getAuthenticatedUser();

        List<ReviewRequestEntity> reviewRequests =
                reviewRequestRepository
                        .findAllByRequesterId_Id(user.getId());

        return reviewRequests.stream()
                .map(this::toResponse)
                .toList();
    }

    // MAP ENTITY TO RESPONSE DTO
    private ReviewRequestResponse toResponse(
            ReviewRequestEntity reviewRequest
    ) {
        return new ReviewRequestResponse(
                reviewRequest.getTitle(),
                reviewRequest.getDescription(),
                reviewRequest.getRepositoryURL(),
                reviewRequest.getPullRequestURL(),
                reviewRequest.getStatus(),
                toUserSummary(reviewRequest.getRequesterId()),
                toUserSummary(reviewRequest.getReviewerId()),
                reviewRequest.getUpdatedAt(),
                reviewRequest.getCreatedAt()
        );
    }

    // MAP USER ENTITY TO SUMMARY DTO
    private UserSummaryResponse toUserSummary(UserEntity user) {
        return new UserSummaryResponse(
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
