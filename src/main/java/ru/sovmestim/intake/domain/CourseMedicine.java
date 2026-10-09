package ru.sovmestim.intake.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.patient.domain.Status;

/**
 * A scheduled drug course for a patient.
 */
@Entity
@Table(name = "course_medicine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMedicine {

    /**
     * Client-assigned id: sync creates rows with the UUIDv7 the client already generated.
     */
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    private String dosage;

    private String frequency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

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
