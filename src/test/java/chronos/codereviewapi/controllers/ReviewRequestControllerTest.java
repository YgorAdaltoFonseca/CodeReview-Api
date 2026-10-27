package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.services.ReviewRequestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
}
