package chronos.codereviewapi.entities;


import chronos.codereviewapi.enums.InquiryStatus;
import chronos.codereviewapi.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InquiryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , length = 100)
    @Size(max = 100)
    @NotBlank
    private String name;

    @Column(nullable = false , length = 2000)
    @Size(max = 2000)
    @NotBlank
    private String description;

    @Column(nullable = false , length = 2048)
    @Size(max = 2048)
    @NotBlank
    private String url;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus inquiryStatus = InquiryStatus.WAITING;

    @Column(length = 2000)
    @Size(max = 2000)
    private String comment;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
