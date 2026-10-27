package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.UpdateReviewRequest;
import chronos.codereviewapi.dtos.requests.CreateCommentRequest;
import chronos.codereviewapi.dtos.requests.ReviewDecisionRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.ReviewCommentResponse;
import chronos.codereviewapi.dtos.responses.ReviewHistoryResponse;
import chronos.codereviewapi.entities.ReviewCommentEntity;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.enums.ReviewDecision;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.exceptions.ReviewRequestConflictException;
import chronos.codereviewapi.exceptions.ReviewRequestNotFoundException;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.repositories.ReviewCommentRepository;
import chronos.codereviewapi.repositories.ReviewStatusHistoryRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRequestServiceTest {

    @Mock
    private ReviewRequestRepository reviewRequestRepository;

    @Mock
    private ReviewCommentRepository reviewCommentRepository;

    @Mock
    private ReviewStatusHistoryRepository reviewStatusHistoryRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private ReviewRequestService reviewRequestService;

    @BeforeEach
    void setUp() {
        reviewRequestService = new ReviewRequestService(
                reviewRequestRepository,
                reviewCommentRepository,
                reviewStatusHistoryRepository,
                authenticatedUserService
        );
    }

    @Test
    void createReviewRequestSavesInitialOpenHistory() {
        UserEntity requester = UserEntity.builder()
                .id(7L)
                .name("Ygor")
                .email("ygor@example.com")
                .role(Role.DEVELOPER)
                .build();
        ReviewRequestRequest request = new ReviewRequestRequest(
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12"
        );

        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.save(any(ReviewRequestEntity.class)))
                .thenAnswer(invocation -> {
                    ReviewRequestEntity savedRequest = invocation.getArgument(0);
                    savedRequest.setId(42L);
                    return savedRequest;
                });

        ReviewRequestResponse response =
                reviewRequestService.createReviewRequest(request);

        ArgumentCaptor<ReviewStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(ReviewStatusHistoryEntity.class);
        InOrder saveOrder = inOrder(
                reviewRequestRepository,
                reviewStatusHistoryRepository
        );
        saveOrder.verify(reviewRequestRepository)
                .save(any(ReviewRequestEntity.class));
        saveOrder.verify(reviewStatusHistoryRepository)
                .save(historyCaptor.capture());

        ReviewStatusHistoryEntity history = historyCaptor.getValue();
        assertEquals(42L, response.id());
        assertEquals(ReviewStatus.OPEN, response.status());
        assertNull(history.getOldStatus());
        assertEquals(ReviewStatus.OPEN, history.getNewStatus());
        assertSame(requester, history.getChangedById());
        assertEquals(42L, history.getReviewRequest().getId());
    }

    @Test
    void createReviewRequestRejectsNonDeveloper() {
        UserEntity reviewer = UserEntity.builder()
                .id(8L)
                .name("Maria")
                .email("maria@example.com")
                .role(Role.REVIEWER)
                .build();
        ReviewRequestRequest request = new ReviewRequestRequest(
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12"
        );

        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);

        assertThrows(
                AccessDeniedException.class,
                () -> reviewRequestService.createReviewRequest(request)
        );

        verifyNoInteractions(
                reviewRequestRepository,
                reviewStatusHistoryRepository
        );
    }

    @Test
    void developerCanViewOwnReviewRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));

        ReviewRequestResponse response =
                reviewRequestService.getReviewRequest(42L);

        assertEquals(42L, response.id());
        assertEquals(ReviewStatus.OPEN, response.status());
    }

    @Test
    void developerCannotViewAnotherDevelopersReviewRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        UserEntity anotherDeveloper = createUser(9L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                anotherDeveloper,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                ReviewRequestNotFoundException.class,
                () -> reviewRequestService.getReviewRequest(42L)
        );
    }

    @Test
    void reviewerCanViewOpenUnassignedRequest() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));

        ReviewRequestResponse response =
                reviewRequestService.getReviewRequest(42L);

        assertEquals(42L, response.id());
        verify(reviewRequestRepository).findById(42L);
    }

    @Test
    void developerCanUpdateOwnOpenRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewRequestRepository.save(reviewRequest))
                .thenReturn(reviewRequest);

        ReviewRequestResponse response =
                reviewRequestService.updateReviewRequest(
                        42L,
                        new UpdateReviewRequest(null, "Updated description")
                );

        assertEquals("Review authentication", response.title());
        assertEquals("Updated description", response.description());
        verify(reviewRequestRepository).save(reviewRequest);
    }

    @Test
    void developerCannotUpdateAnotherDevelopersRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity anotherDevelopersRequest = createReviewRequest(
                createUser(9L, Role.DEVELOPER),
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(anotherDevelopersRequest));

        assertThrows(
                ReviewRequestNotFoundException.class,
                () -> reviewRequestService.updateReviewRequest(
                        42L,
                        new UpdateReviewRequest("Changed", null)
                )
        );

        verify(reviewRequestRepository, never()).save(anotherDevelopersRequest);
    }

    @Test
    void cancellationChangesStatusAndSavesHistory() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewRequestRepository.save(reviewRequest))
                .thenReturn(reviewRequest);

        ReviewRequestResponse response =
                reviewRequestService.cancelReviewRequest(42L);

        ArgumentCaptor<ReviewStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(ReviewStatusHistoryEntity.class);
        verify(reviewStatusHistoryRepository).save(historyCaptor.capture());
        ReviewStatusHistoryEntity history = historyCaptor.getValue();

        assertEquals(ReviewStatus.CANCELLED, response.status());
        assertEquals(ReviewStatus.OPEN, history.getOldStatus());
        assertEquals(ReviewStatus.CANCELLED, history.getNewStatus());
        assertSame(requester, history.getChangedById());
        assertEquals(42L, history.getReviewRequest().getId());
    }

    @Test
    void requestInReviewCannotBeCancelled() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                createUser(8L, Role.REVIEWER),
                ReviewStatus.IN_REVIEW
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                ReviewRequestConflictException.class,
                () -> reviewRequestService.cancelReviewRequest(42L)
        );

        verifyNoInteractions(reviewStatusHistoryRepository);
        verify(reviewRequestRepository, never()).save(reviewRequest);
    }

    @Test
    void reviewerClaimsOpenRequestAndHistoryIsRecorded() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewRequestRepository.save(reviewRequest))
                .thenReturn(reviewRequest);

        ReviewRequestResponse response =
                reviewRequestService.claimReviewRequest(42L);

        ArgumentCaptor<ReviewStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(ReviewStatusHistoryEntity.class);
        verify(reviewStatusHistoryRepository).save(historyCaptor.capture());
        ReviewStatusHistoryEntity history = historyCaptor.getValue();

        assertEquals(ReviewStatus.IN_REVIEW, response.status());
        assertEquals(reviewer, reviewRequest.getReviewerId());
        assertEquals(ReviewStatus.OPEN, history.getOldStatus());
        assertEquals(ReviewStatus.IN_REVIEW, history.getNewStatus());
        assertSame(reviewer, history.getChangedById());
    }

    @Test
    void reviewerCannotClaimOwnRequest() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                reviewer,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                AccessDeniedException.class,
                () -> reviewRequestService.claimReviewRequest(42L)
        );

        verify(reviewRequestRepository, never()).save(reviewRequest);
        verifyNoInteractions(reviewStatusHistoryRepository);
    }

    @Test
    void reviewerCannotClaimRequestThatIsNoLongerOpen() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                null,
                ReviewStatus.CANCELLED
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                ReviewRequestConflictException.class,
                () -> reviewRequestService.claimReviewRequest(42L)
        );

        verify(reviewRequestRepository, never()).save(reviewRequest);
        verifyNoInteractions(reviewStatusHistoryRepository);
    }

    @Test
    void assignedReviewerCanAddCommentDuringReview() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                reviewer,
                ReviewStatus.IN_REVIEW
        );
        CreateCommentRequest request =
                new CreateCommentRequest("Please add a test for expired tokens.");
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewCommentRepository.save(any(ReviewCommentEntity.class)))
                .thenAnswer(invocation -> {
                    ReviewCommentEntity comment = invocation.getArgument(0);
                    comment.setId(101L);
                    return comment;
                });

        ReviewCommentResponse response =
                reviewRequestService.addComment(42L, request);

        assertEquals(101L, response.id());
        assertEquals(42L, response.reviewRequestId());
        assertEquals(8L, response.authorId());
        assertEquals(reviewer.getName(), response.authorName());
        assertEquals(request.content(), response.content());
    }

    @Test
    void otherReviewerCannotAddComment() {
        UserEntity assignedReviewer = createUser(8L, Role.REVIEWER);
        UserEntity otherReviewer = createUser(10L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                assignedReviewer,
                ReviewStatus.IN_REVIEW
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(otherReviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                AccessDeniedException.class,
                () -> reviewRequestService.addComment(
                        42L,
                        new CreateCommentRequest("Unauthorized comment")
                )
        );

        verifyNoInteractions(reviewCommentRepository);
    }

    @Test
    void assignedReviewerDecisionUpdatesStatusAndHistory() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                reviewer,
                ReviewStatus.IN_REVIEW
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewRequestRepository.save(reviewRequest))
                .thenReturn(reviewRequest);

        ReviewRequestResponse response = reviewRequestService.decideReview(
                42L,
                new ReviewDecisionRequest(
                        ReviewDecision.APPROVED,
                        "The changes look good."
                )
        );

        ArgumentCaptor<ReviewStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(ReviewStatusHistoryEntity.class);
        ArgumentCaptor<ReviewCommentEntity> commentCaptor =
                ArgumentCaptor.forClass(ReviewCommentEntity.class);
        verify(reviewStatusHistoryRepository).save(historyCaptor.capture());
        verify(reviewCommentRepository).save(commentCaptor.capture());

        assertEquals(ReviewStatus.APPROVED, response.status());
        assertEquals(ReviewStatus.IN_REVIEW, historyCaptor.getValue().getOldStatus());
        assertEquals(ReviewStatus.APPROVED, historyCaptor.getValue().getNewStatus());
        assertEquals("The changes look good.", commentCaptor.getValue().getContent());
        assertSame(reviewer, commentCaptor.getValue().getRequesterId());
    }

    @Test
    void reviewDecisionIsRejectedWhenRequestIsNotInReview() {
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                createUser(7L, Role.DEVELOPER),
                reviewer,
                ReviewStatus.APPROVED
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(reviewer);
        when(reviewRequestRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                ReviewRequestConflictException.class,
                () -> reviewRequestService.decideReview(
                        42L,
                        new ReviewDecisionRequest(ReviewDecision.REJECTED, null)
                )
        );

        verify(reviewRequestRepository, never()).save(reviewRequest);
        verifyNoInteractions(reviewStatusHistoryRepository, reviewCommentRepository);
    }

    @Test
    void developerCanReadCommentsOnOwnRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        UserEntity reviewer = createUser(8L, Role.REVIEWER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                reviewer,
                ReviewStatus.CHANGES_REQUESTED
        );
        ReviewCommentEntity comment = ReviewCommentEntity.builder()
                .id(101L)
                .reviewRequestEntityId(reviewRequest)
                .requesterId(reviewer)
                .content("Please add an expiry test.")
                .build();
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewCommentRepository
                .findByReviewRequestEntityId_IdOrderByCreatedAtAscIdAsc(42L))
                .thenReturn(List.of(comment));

        List<ReviewCommentResponse> comments =
                reviewRequestService.getReviewComments(42L);

        assertEquals(1, comments.size());
        assertEquals(101L, comments.getFirst().id());
        assertEquals(8L, comments.getFirst().authorId());
        assertEquals("Please add an expiry test.", comments.getFirst().content());
    }

    @Test
    void developerCanReadChronologicalStatusHistoryOnOwnRequest() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                requester,
                null,
                ReviewStatus.OPEN
        );
        ReviewStatusHistoryEntity historyItem =
                ReviewStatusHistoryEntity.builder()
                        .id(5L)
                        .reviewRequest(reviewRequest)
                        .changedById(requester)
                        .oldStatus(null)
                        .newStatus(ReviewStatus.OPEN)
                        .build();
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));
        when(reviewStatusHistoryRepository
                .findByReviewRequest_IdOrderByCreatedAtAscIdAsc(42L))
                .thenReturn(List.of(historyItem));

        ReviewHistoryResponse response =
                reviewRequestService.getReviewHistory(42L);

        assertEquals(42L, response.reviewRequestId());
        assertEquals(1, response.history().size());
        assertNull(response.history().getFirst().oldStatus());
        assertEquals(ReviewStatus.OPEN, response.history().getFirst().newStatus());
        assertEquals(7L, response.history().getFirst().changedBy().id());
    }

    @Test
    void anotherDeveloperCannotReadCommentsOrHistory() {
        UserEntity requester = createUser(7L, Role.DEVELOPER);
        UserEntity anotherDeveloper = createUser(9L, Role.DEVELOPER);
        ReviewRequestEntity reviewRequest = createReviewRequest(
                anotherDeveloper,
                null,
                ReviewStatus.OPEN
        );
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(requester);
        when(reviewRequestRepository.findById(42L))
                .thenReturn(Optional.of(reviewRequest));

        assertThrows(
                ReviewRequestNotFoundException.class,
                () -> reviewRequestService.getReviewComments(42L)
        );
        assertThrows(
                ReviewRequestNotFoundException.class,
                () -> reviewRequestService.getReviewHistory(42L)
        );

        verifyNoInteractions(reviewCommentRepository, reviewStatusHistoryRepository);
    }

    private UserEntity createUser(Long id, Role role) {
        return UserEntity.builder()
                .id(id)
                .name("User " + id)
                .email("user" + id + "@example.com")
                .role(role)
                .build();
    }

    private ReviewRequestEntity createReviewRequest(
            UserEntity requester,
            UserEntity reviewer,
            ReviewStatus status
    ) {
        return ReviewRequestEntity.builder()
                .id(42L)
                .title("Review authentication")
                .description("Check token handling")
                .repositoryURL("https://github.com/ygor/project")
                .pullRequestURL("https://github.com/ygor/project/pull/12")
                .requesterId(requester)
                .reviewerId(reviewer)
                .status(status)
                .build();
    }
}
