
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
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
