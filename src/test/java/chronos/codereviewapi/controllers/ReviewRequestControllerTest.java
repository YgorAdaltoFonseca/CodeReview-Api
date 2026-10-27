package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.CreateCommentRequest;
import chronos.codereviewapi.dtos.requests.ReviewDecisionRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.ReviewCommentResponse;
import chronos.codereviewapi.dtos.responses.ReviewHistoryResponse;
import chronos.codereviewapi.enums.ReviewDecision;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.services.ReviewRequestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewRequestControllerTest {

    @Test
    void createReviewRequestReturnsCreatedLocation() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewRequestRequest request = new ReviewRequestRequest(
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12"
        );
        ReviewRequestResponse response = new ReviewRequestResponse(
                42L,
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12",
                ReviewStatus.OPEN,
                null,
                null,
                null,
                null
        );
        when(service.createReviewRequest(request)).thenReturn(response);

        var result = controller.createReviewRequest(request);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(
                "/api/reviews/42",
                result.getHeaders().getLocation().toString()
        );
        assertEquals(42L, result.getBody().id());
    }

    @Test
    void getReviewRequestReturnsRequestDetails() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewRequestResponse response = new ReviewRequestResponse(
                42L,
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12",
                ReviewStatus.OPEN,
                null,
                null,
                null,
                null
        );
        when(service.getReviewRequest(42L)).thenReturn(response);

        var result = controller.getReviewRequest(42L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        verify(service).getReviewRequest(42L);
    }

    @Test
    void claimReviewRequestReturnsUpdatedRequest() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewRequestResponse response = new ReviewRequestResponse(
                42L,
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12",
                ReviewStatus.IN_REVIEW,
                null,
                null,
                null,
                null
        );
        when(service.claimReviewRequest(42L)).thenReturn(response);

        var result = controller.claimReviewRequest(42L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(ReviewStatus.IN_REVIEW, result.getBody().status());
        verify(service).claimReviewRequest(42L);
    }

    @Test
    void addCommentReturnsCreatedResponse() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        CreateCommentRequest request = new CreateCommentRequest("Review note");
        ReviewCommentResponse response = new ReviewCommentResponse(
                101L,
                42L,
                8L,
                "Maria",
                "Review note",
                null
        );
        when(service.addComment(42L, request)).thenReturn(response);

        var result = controller.addComment(42L, request);

        assertEquals(201, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        verify(service).addComment(42L, request);
    }

    @Test
    void decideReviewReturnsUpdatedRequest() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewDecisionRequest request = new ReviewDecisionRequest(
                ReviewDecision.CHANGES_REQUESTED,
                null
        );
        ReviewRequestResponse response = new ReviewRequestResponse(
                42L,
                "Review authentication",
                "Check token handling",
                "https://github.com/ygor/project",
                "https://github.com/ygor/project/pull/12",
                ReviewStatus.CHANGES_REQUESTED,
                null,
                null,
                null,
                null
        );
        when(service.decideReview(42L, request)).thenReturn(response);

        var result = controller.decideReview(42L, request);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(ReviewStatus.CHANGES_REQUESTED, result.getBody().status());
        verify(service).decideReview(42L, request);
    }

    @Test
    void getReviewCommentsReturnsComments() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewCommentResponse comment = new ReviewCommentResponse(
                101L, 42L, 8L, "Maria", "Review note", null
        );
        when(service.getReviewComments(42L)).thenReturn(java.util.List.of(comment));

        var result = controller.getReviewComments(42L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(java.util.List.of(comment), result.getBody());
        verify(service).getReviewComments(42L);
    }

    @Test
    void getReviewHistoryReturnsHistory() {
        ReviewRequestService service = mock(ReviewRequestService.class);
        ReviewRequestController controller =
                new ReviewRequestController(service);
        ReviewHistoryResponse response = new ReviewHistoryResponse(42L, java.util.List.of());
        when(service.getReviewHistory(42L)).thenReturn(response);

        var result = controller.getReviewHistory(42L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        verify(service).getReviewHistory(42L);
    }
}
