
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.enums.ReviewStatus;
import chronos.codereviewapi.services.ReviewRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
        return ResponseEntity.ok(
                reviewRequestService.createReviewRequest(request)
        );
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
}
