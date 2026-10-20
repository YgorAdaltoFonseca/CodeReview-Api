package chronos.codereviewapi.services;

import chronos.codereviewapi.entities.ReviewRequestEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewRequestService {

    private final ReviewRequestEntity reviewRequestEntity;

    public ResponseEntity<?> createReviewRequest(ReviewRequestEntity reviewRequestEntity) {

    }


}
