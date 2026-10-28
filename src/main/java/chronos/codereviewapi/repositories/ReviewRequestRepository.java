
package chronos.codereviewapi.repositories;

import chronos.codereviewapi.entities.ReviewRequestEntity;
import chronos.codereviewapi.enums.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRequestRepository
        extends JpaRepository<ReviewRequestEntity, Long> {

    // Serializes changes to one request while its status is being updated.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReviewRequestEntity r WHERE r.id = :id")
    Optional<ReviewRequestEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    // DEVELOPER: only obligations created by the user themselves
    @Query("""
        SELECT r
        FROM ReviewRequestEntity r
        WHERE r.requesterId.id = :userId
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<ReviewRequestEntity> findDeveloperRequests(
            @Param("userId") Long userId,
            @Param("status") ReviewStatus status,
            Pageable pageable
    );

    // REVIEWER: open requests available
    // or requests assigned to the reviewer themselves
    @Query("""
        SELECT r
        FROM ReviewRequestEntity r
        WHERE (
            (r.status = chronos.codereviewapi.enums.ReviewStatus.OPEN
             AND r.reviewerId IS NULL)
            OR r.reviewerId.id = :userId
        )
        AND (:status IS NULL OR r.status = :status)
        """)
    Page<ReviewRequestEntity> findReviewerRequests(
            @Param("userId") Long userId,
            @Param("status") ReviewStatus status,
            Pageable pageable
    );
}
