package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.repositories.ReviewStatusHistoryRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRequestServiceTest {

    @Mock
    private ReviewRequestRepository reviewRequestRepository;

    @Mock
    private ReviewStatusHistoryRepository reviewStatusHistoryRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private ReviewRequestService reviewRequestService;

    @BeforeEach
    void setUp() {
        reviewRequestService = new ReviewRequestService(
                reviewRequestRepository,
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
}
