
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.UpdateReviewRequest;
import chronos.codereviewapi.dtos.requests.CreateCommentRequest;
import chronos.codereviewapi.dtos.requests.ReviewDecisionRequest;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.dtos.responses.ReviewCommentResponse;
import chronos.codereviewapi.dtos.responses.ReviewHistoryResponse;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.services.ReviewRequestService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewRequestController {

    private final ReviewRequestService reviewRequestService;

    // CREATE REVIEW REQUEST
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Review request created")
    public ResponseEntity<ReviewRequestResponse> createReviewRequest(
            @Valid @RequestBody ReviewRequestRequest request
    ) {
        ReviewRequestResponse response =
                reviewRequestService.createReviewRequest(request);

        return ResponseEntity
                .created(URI.create("/api/reviews/" + response.id()))
                .body(response);
    }

    // LIST REVIEW REQUESTS
    @GetMapping
    public ResponseEntity<PageResponse<ReviewRequestResponse>>
    getReviewRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(
                reviewRequestService.listReviewRequests(
                        page,
                        size,
                        status,
                        sort
                )
        );
    }

    // GET A REVIEW REQUEST
    @GetMapping("/{id}")
    public ResponseEntity<ReviewRequestResponse> getReviewRequest(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.getReviewRequest(id)
        );
    }

    // UPDATE A REQUEST OWNED BY THE CURRENT DEVELOPER
    @PatchMapping("/{id}")
    public ResponseEntity<ReviewRequestResponse> updateReviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewRequestService.updateReviewRequest(id, request)
        );
    }

    // CANCEL A REQUEST OWNED BY THE CURRENT DEVELOPER
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReviewRequestResponse> cancelReviewRequest(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.cancelReviewRequest(id)
        );
    }

    // RESUBMIT A REQUEST AFTER THE DEVELOPER ADDRESSES REQUESTED CHANGES
    @PatchMapping("/{id}/resubmit")
    public ResponseEntity<ReviewRequestResponse> resubmitReviewRequest(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.resubmitReviewRequest(id)
        );
    }

    // CLAIM AN OPEN REQUEST FOR REVIEW
    @PatchMapping("/{id}/claim")
    public ResponseEntity<ReviewRequestResponse> claimReviewRequest(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.claimReviewRequest(id)
        );
    }

    // ADD COMMENT TO A REVIEW IN PROGRESS
    @PostMapping("/{id}/comments")
    @ApiResponse(responseCode = "201", description = "Comment created")
    public ResponseEntity<ReviewCommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewRequestService.addComment(id, request));
    }

    // RECORD THE DECISION OF THE ASSIGNED REVIEWER
    @PostMapping("/{id}/decision")
    public ResponseEntity<ReviewRequestResponse> decideReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        return ResponseEntity.ok(
                reviewRequestService.decideReview(id, request)
        );
    }

    // LIST COMMENTS FOR A REQUEST VISIBLE TO THE CURRENT USER
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ReviewCommentResponse>> getReviewComments(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.getReviewComments(id)
        );
    }

    // GET STATUS HISTORY FOR A REQUEST VISIBLE TO THE CURRENT USER
    @GetMapping("/{id}/history")
    public ResponseEntity<ReviewHistoryResponse> getReviewHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                reviewRequestService.getReviewHistory(id)
        );
    }
}
