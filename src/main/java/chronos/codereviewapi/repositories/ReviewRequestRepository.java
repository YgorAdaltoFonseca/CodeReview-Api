package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRequestRepository
        extends JpaRepository<ReviewRequestEntity, Long> {

    List<ReviewRequestEntity> findAllByRequesterId_Id(Long userId);

}
