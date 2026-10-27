
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.UpdateReviewRequest;
import chronos.codereviewapi.dtos.requests.CreateCommentRequest;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.ReviewCommentResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.ReviewCommentEntity;
import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.exceptions.ReviewRequestNotFoundException;
import chronos.codereviewapi.exceptions.ReviewRequestConflictException;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.repositories.ReviewCommentRepository;
import chronos.codereviewapi.repositories.ReviewStatusHistoryRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewStatusHistoryRepository reviewStatusHistoryRepository;
    private final AuthenticatedUserService authenticatedUserService;

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "status",
            "createdAt",
            "updatedAt"
    );

    // CREATE REVIEW REQUEST
    @Transactional
    public ReviewRequestResponse createReviewRequest(
            ReviewRequestRequest reviewRequestRequest
    ) {
        UserEntity user =
                authenticatedUserService.getAuthenticatedUser();

        if (user.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException(
                    "Only developers can create review requests."
            );
        }

        ReviewRequestEntity reviewRequestEntity =
                ReviewRequestEntity.builder()
                        .title(reviewRequestRequest.title())
                        .description(reviewRequestRequest.description())
                        .repositoryURL(reviewRequestRequest.repositoryUrl())
                        .pullRequestURL(reviewRequestRequest.pullRequestURL())
                        .requesterId(user)
                        .reviewerId(null)
                        .build();

        ReviewRequestEntity savedReviewRequest =
                reviewRequestRepository.save(reviewRequestEntity);

        ReviewStatusHistoryEntity initialHistory =
                ReviewStatusHistoryEntity.builder()
                        .reviewRequest(savedReviewRequest)
                        .changedById(user)
                        .oldStatus(null)
                        .newStatus(savedReviewRequest.getStatus())
                        .build();

        reviewStatusHistoryRepository.save(initialHistory);

        return toResponse(savedReviewRequest);
    }

    // LIST REVIEW REQUESTS WITH PAGINATION AND AUTHORIZATION
    @Transactional(readOnly = true)
    public PageResponse<ReviewRequestResponse> listReviewRequests(
            int page,
            int size,
            ReviewStatus status,
            String sort
    ) {
        UserEntity user =
                authenticatedUserService.getAuthenticatedUser();

        validatePagination(page, size);

        Pageable pageable = createPageable(page, size, sort);

        Page<ReviewRequestEntity> reviewRequests;

        if (user.getRole() == Role.DEVELOPER) {

            reviewRequests =
                    reviewRequestRepository.findDeveloperRequests(
                            user.getId(),
                            status,
                            pageable
                    );

        } else if (user.getRole() == Role.REVIEWER) {

            reviewRequests =
                    reviewRequestRepository.findReviewerRequests(
                            user.getId(),
                            status,
                            pageable
                    );

        } else {

            throw new AccessDeniedException(
                    "You do not have permission to list review requests."
            );
        }

        List<ReviewRequestResponse> content =
                reviewRequests.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return new PageResponse<>(
                content,
                reviewRequests.getNumber(),
                reviewRequests.getSize(),
                reviewRequests.getTotalElements(),
                reviewRequests.getTotalPages()
        );
    }

    // GET A REQUEST VISIBLE TO THE CURRENT USER
    @Transactional(readOnly = true)
    public ReviewRequestResponse getReviewRequest(Long reviewRequestId) {
        UserEntity user =
                authenticatedUserService.getAuthenticatedUser();

        ReviewRequestEntity reviewRequest =
                reviewRequestRepository.findById(reviewRequestId)
                        .orElseThrow(() ->
                                new ReviewRequestNotFoundException(
                                        reviewRequestId
                                )
                        );

        if (!canViewReviewRequest(user, reviewRequest)) {
            throw new ReviewRequestNotFoundException(reviewRequestId);
        }

        return toResponse(reviewRequest);
    }

    // UPDATE FIELDS THAT ARE EDITABLE BEFORE REVIEW STARTS
    @Transactional
    public ReviewRequestResponse updateReviewRequest(
            Long reviewRequestId,
            UpdateReviewRequest updateRequest
    ) {
        UserEntity user =
                authenticatedUserService.getAuthenticatedUser();
        ReviewRequestEntity reviewRequest =
                getRequestForDeveloperUpdate(reviewRequestId, user);

        if (reviewRequest.getStatus() != ReviewStatus.OPEN) {
            throw new ReviewRequestConflictException(
                    "Only open review requests can be updated."
            );
        }

        if (updateRequest.title() != null) {
            reviewRequest.setTitle(updateRequest.title());
        }
        if (updateRequest.description() != null) {
            reviewRequest.setDescription(updateRequest.description());
        }

        return toResponse(reviewRequestRepository.save(reviewRequest));
    }

    // CANCEL AN ELIGIBLE REQUEST AND RECORD ITS STATUS CHANGE
    @Transactional
    public ReviewRequestResponse cancelReviewRequest(Long reviewRequestId) {
        UserEntity user =
                authenticatedUserService.getAuthenticatedUser();
        ReviewRequestEntity reviewRequest =
                getRequestForDeveloperUpdate(reviewRequestId, user);

        ReviewStatus oldStatus = reviewRequest.getStatus();
        if (oldStatus != ReviewStatus.OPEN
                && oldStatus != ReviewStatus.CHANGES_REQUESTED) {
            throw new ReviewRequestConflictException(
                    "Only open or changes-requested reviews can be cancelled."
            );
        }

        reviewRequest.setStatus(ReviewStatus.CANCELLED);
        ReviewRequestEntity savedRequest =
                reviewRequestRepository.save(reviewRequest);

        reviewStatusHistoryRepository.save(
                ReviewStatusHistoryEntity.builder()
                        .reviewRequest(savedRequest)
                        .changedById(user)
                        .oldStatus(oldStatus)
                        .newStatus(ReviewStatus.CANCELLED)
                        .build()
        );

        return toResponse(savedRequest);
    }

    // CLAIM AN OPEN REQUEST FOR THE AUTHENTICATED REVIEWER
    @Transactional
    public ReviewRequestResponse claimReviewRequest(Long reviewRequestId) {
        UserEntity reviewer =
                authenticatedUserService.getAuthenticatedUser();

        if (reviewer.getRole() != Role.REVIEWER) {
            throw new AccessDeniedException(
                    "Only reviewers can claim review requests."
            );
        }

        ReviewRequestEntity reviewRequest =
                reviewRequestRepository.findByIdForUpdate(reviewRequestId)
                        .orElseThrow(() ->
                                new ReviewRequestNotFoundException(
                                        reviewRequestId
                                )
                        );

        if (reviewRequest.getRequesterId().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException(
                    "A developer cannot review their own request."
            );
        }

        if (reviewRequest.getStatus() != ReviewStatus.OPEN
                || reviewRequest.getReviewerId() != null) {
            throw new ReviewRequestConflictException(
                    "This review request is no longer available to claim."
            );
        }

        reviewRequest.setReviewerId(reviewer);
        reviewRequest.setStatus(ReviewStatus.IN_REVIEW);
        ReviewRequestEntity savedRequest =
                reviewRequestRepository.save(reviewRequest);

        reviewStatusHistoryRepository.save(
                ReviewStatusHistoryEntity.builder()
                        .reviewRequest(savedRequest)
                        .changedById(reviewer)
                        .oldStatus(ReviewStatus.OPEN)
                        .newStatus(ReviewStatus.IN_REVIEW)
                        .build()
        );

        return toResponse(savedRequest);
    }

    // ADD A COMMENT AS THE REVIEWER ASSIGNED TO AN IN-PROGRESS REQUEST
    @Transactional
    public ReviewCommentResponse addComment(
            Long reviewRequestId,
            CreateCommentRequest commentRequest
    ) {
        UserEntity reviewer =
                authenticatedUserService.getAuthenticatedUser();

        if (reviewer.getRole() != Role.REVIEWER) {
            throw new AccessDeniedException(
                    "Only reviewers can comment on review requests."
            );
        }

        ReviewRequestEntity reviewRequest =
                reviewRequestRepository.findByIdForUpdate(reviewRequestId)
                        .orElseThrow(() ->
                                new ReviewRequestNotFoundException(
                                        reviewRequestId
                                )
                        );

        if (reviewRequest.getReviewerId() == null
                || !reviewRequest.getReviewerId().getId()
                .equals(reviewer.getId())) {
            throw new AccessDeniedException(
                    "Only the assigned reviewer can comment."
            );
        }

        if (reviewRequest.getStatus() != ReviewStatus.IN_REVIEW) {
            throw new ReviewRequestConflictException(
                    "Comments can only be added while a review is in progress."
            );
        }

        ReviewCommentEntity comment = ReviewCommentEntity.builder()
                .reviewRequestEntityId(reviewRequest)
                .requesterId(reviewer)
                .content(commentRequest.content())
                .build();

        ReviewCommentEntity savedComment =
                reviewCommentRepository.save(comment);

        return new ReviewCommentResponse(
                savedComment.getId(),
                reviewRequest.getId(),
                reviewer.getId(),
                reviewer.getName(),
                savedComment.getContent(),
                savedComment.getCreatedAt()
        );
    }

    private ReviewRequestEntity getRequestForDeveloperUpdate(
            Long reviewRequestId,
            UserEntity user
    ) {
        if (user.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException(
                    "Only developers can update or cancel review requests."
            );
        }

        ReviewRequestEntity reviewRequest =
                reviewRequestRepository.findByIdForUpdate(reviewRequestId)
                        .orElseThrow(() ->
                                new ReviewRequestNotFoundException(
                                        reviewRequestId
                                )
                        );

        if (!reviewRequest.getRequesterId().getId().equals(user.getId())) {
            throw new ReviewRequestNotFoundException(reviewRequestId);
        }

        return reviewRequest;
    }

    private boolean canViewReviewRequest(
            UserEntity user,
            ReviewRequestEntity reviewRequest
    ) {
        if (user.getRole() == Role.DEVELOPER) {
            return reviewRequest.getRequesterId().getId()
                    .equals(user.getId());
        }

        if (user.getRole() == Role.REVIEWER) {
            boolean isAvailable =
                    reviewRequest.getStatus() == ReviewStatus.OPEN
                            && reviewRequest.getReviewerId() == null;
            boolean isAssignedToUser =
                    reviewRequest.getReviewerId() != null
                            && reviewRequest.getReviewerId().getId()
                            .equals(user.getId());

            return isAvailable || isAssignedToUser;
        }

        return false;
    }

    // VALIDATE PAGINATION PARAMETERS
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to zero."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE + "."
            );
        }
    }

    // CREATE PAGINATION AND SORTING
    private Pageable createPageable(
            int page,
            int size,
            String sort
    ) {
        String[] sortParameters = sort.split(",", -1);

        if (sortParameters.length > 2
                || sortParameters[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid sort format. Use field,direction."
            );
        }

        String field = sortParameters[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Sorting by this field is not allowed."
            );
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (sortParameters.length == 2) {
            try {
                direction = Sort.Direction.fromString(
                        sortParameters[1].trim()
                );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Sort direction must be asc or desc."
                );
            }
        }

        Sort sorting = Sort.by(direction, field);

        return PageRequest.of(page, size, sorting);
    }

    // MAP ENTITY TO RESPONSE DTO
    private ReviewRequestResponse toResponse(
            ReviewRequestEntity reviewRequest
    ) {
        return new ReviewRequestResponse(
                reviewRequest.getId(),
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
        if (user == null) {
            return null;
        }

        return new UserSummaryResponse(
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
