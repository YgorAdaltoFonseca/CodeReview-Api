package chronos.codereviewapi.entities;


import chronos.codereviewapi.enums.ReviewStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reviewrequest_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , length = 100)
    @Size(max = 100)
    @NotBlank
    private String title;

    @Column(nullable = false , length = 2000)
    @Size(max = 2000)
    @NotBlank
    private String description;

    @Column(nullable = false , length = 2048)
    @Size(max = 2048)
    @NotBlank
    private String repositoryURL;

    @Column(nullable = false , length = 2048)
    @Size(max = 2048)
    @NotBlank
    private String pullRequestURL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.OPEN;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserEntity requesterId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private UserEntity reviewerId;


    @UpdateTimestamp
    @Column(nullable = false, updatable = false)
    private Instant updatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
