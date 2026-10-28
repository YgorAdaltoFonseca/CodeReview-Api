package chronos.codereviewapi.services;

import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.exceptions.ReviewRequestConflictException;
import chronos.codereviewapi.repositories.ReviewRequestRepository;
import chronos.codereviewapi.repositories.ReviewStatusHistoryRepository;
import chronos.codereviewapi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "JWT_SECRET=VGhpc0lzQVNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MA=="
})
class ReviewRequestClaimConcurrencyTest {

    @Autowired
    private ReviewRequestService reviewRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewStatusHistoryRepository historyRepository;

    @Test
    void onlyOneOfTwoConcurrentReviewersCanClaimRequest() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString();
        UserEntity requester = saveUser(
                "requester-" + uniqueSuffix + "@example.com",
                Role.DEVELOPER
        );
        UserEntity firstReviewer = saveUser(
                "reviewer-one-" + uniqueSuffix + "@example.com",
                Role.REVIEWER
        );
        UserEntity secondReviewer = saveUser(
                "reviewer-two-" + uniqueSuffix + "@example.com",
                Role.REVIEWER
        );
        ReviewRequestEntity request = reviewRequestRepository.save(
                ReviewRequestEntity.builder()
                        .title("Concurrent claim")
                        .description("Only one reviewer should claim this")
                        .repositoryURL("https://github.com/example/project")
                        .pullRequestURL("https://github.com/example/project/pull/1")
                        .requesterId(requester)
                        .build()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> firstClaim = executor.submit(() -> tryClaim(
                    firstReviewer.getEmail(), request.getId(), ready, start
            ));
            Future<Boolean> secondClaim = executor.submit(() -> tryClaim(
                    secondReviewer.getEmail(), request.getId(), ready, start
            ));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean firstWon = firstClaim.get(15, TimeUnit.SECONDS);
            boolean secondWon = secondClaim.get(15, TimeUnit.SECONDS);
            assertNotEquals(firstWon, secondWon);

            ReviewRequestEntity claimedRequest =
                    reviewRequestRepository.findById(request.getId())
                            .orElseThrow();
            assertEquals(ReviewStatus.IN_REVIEW, claimedRequest.getStatus());
            assertTrue(List.of(firstReviewer.getId(), secondReviewer.getId())
                    .contains(claimedRequest.getReviewerId().getId()));

            List<ReviewStatusHistoryEntity> history =
                    historyRepository
                            .findByReviewRequest_IdOrderByCreatedAtAscIdAsc(
                                    request.getId()
                            );
            assertEquals(1, history.size());
            assertEquals(ReviewStatus.OPEN, history.getFirst().getOldStatus());
            assertEquals(
                    ReviewStatus.IN_REVIEW,
                    history.getFirst().getNewStatus()
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            historyRepository.deleteAll(
                    historyRepository
                            .findByReviewRequest_IdOrderByCreatedAtAscIdAsc(
                                    request.getId()
                            )
            );
            reviewRequestRepository.deleteById(request.getId());
            userRepository.deleteAllById(List.of(
                    requester.getId(), firstReviewer.getId(), secondReviewer.getId()
            ));
        }
    }

    private boolean tryClaim(
            String reviewerEmail,
            Long requestId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        reviewerEmail,
                        "test",
                        List.of(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                )
        );
        SecurityContextHolder.setContext(context);
        ready.countDown();

        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Claim test did not start.");
            }

            try {
                reviewRequestService.claimReviewRequest(requestId);
                return true;
            } catch (ReviewRequestConflictException exception) {
                return false;
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private UserEntity saveUser(String email, Role role) {
        return userRepository.save(
                UserEntity.builder()
                        .name("Concurrency test user")
                        .email(email)
                        .passwordHash("test-hash")
                        .role(role)
                        .active(true)
                        .build()
        );
    }
}
