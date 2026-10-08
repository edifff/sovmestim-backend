package ru.sovmestim.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sovmestim.identity.domain.AppUser;

/**
 * A patient-entered disease that is not in the directory yet.
 */
@Entity
@Table(name = "user_disease")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "severity_reaction_id")
    private SeverityReaction severityReaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mkb_id")
    private Mkb mkb;

    @Column(name = "disease_name")
    private String diseaseName;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_synced", nullable = false)
    private boolean synced;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
