package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.ReviewRequestRequest;
import chronos.codereviewapi.dtos.requests.auth.RegisterRequest;
import chronos.codereviewapi.dtos.responses.ReviewRequestResponse;
import chronos.codereviewapi.services.ReviewRequestService;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
public class ReviewRequestController {

    private final ReviewRequestService reviewRequestService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/reviews")
    public ResponseEntity<ReviewRequestResponse> reviews
    (
            @Valid @RequestBody ReviewRequestRequest reviewRequestRequest) {

        return ResponseEntity.ok(
                reviewRequestService.createReviewRequest(reviewRequestRequest)
        );

    }



}
