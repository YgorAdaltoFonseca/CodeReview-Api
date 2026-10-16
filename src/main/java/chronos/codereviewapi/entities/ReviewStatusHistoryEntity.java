package chronos.codereviewapi.entities;

import chronos.codereviewapi.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reviewstatushistory_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_request_id", nullable = false)
    private ReviewRequestEntity reviewRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private UserEntity changedById;

    @Enumerated(EnumType.STRING)
    @Column
    private ReviewStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column
    private ReviewStatus newStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
