package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCommentRepository
        extends JpaRepository<ReviewCommentEntity, Long> {

    List<ReviewCommentEntity>
    findByReviewRequestEntityId_IdOrderByCreatedAtAscIdAsc(Long reviewRequestId);
}
