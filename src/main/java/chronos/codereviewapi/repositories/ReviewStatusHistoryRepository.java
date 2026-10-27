package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewStatusHistoryRepository
        extends JpaRepository<ReviewStatusHistoryEntity, Long> {

    List<ReviewStatusHistoryEntity> findByReviewRequest_IdOrderByCreatedAtAscIdAsc(
            Long reviewRequestId
    );
}
