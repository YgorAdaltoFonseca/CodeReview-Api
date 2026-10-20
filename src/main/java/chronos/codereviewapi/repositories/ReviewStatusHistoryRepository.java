package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewStatusHistoryRepository
        extends JpaRepository<ReviewStatusHistoryEntity, Long> {
}
