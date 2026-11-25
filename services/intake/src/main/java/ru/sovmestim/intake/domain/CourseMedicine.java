package ru.sovmestim.intake.domain;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.sovmestim.common.persistence.AbstractSyncedEntity;

/**
 * A scheduled drug course for a patient.
 */
@Entity
@Table(name = "course_medicine")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CourseMedicine extends AbstractSyncedEntity {

    /**
     * Client-assigned id: sync creates rows with the UUIDv7 the client already generated.
     */
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    private String brand;

    private String dosage;

    private String frequency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;
}
