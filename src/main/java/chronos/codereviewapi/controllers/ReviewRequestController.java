
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.UpdateReviewRequest;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.services.ReviewRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewRequestController {

    private final ReviewRequestService reviewRequestService;

    // CREATE REVIEW REQUEST
    @PostMapping
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
}
