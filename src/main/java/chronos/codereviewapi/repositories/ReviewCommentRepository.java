package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewCommentRepository
        extends JpaRepository<ReviewCommentEntity, Long> {
}
