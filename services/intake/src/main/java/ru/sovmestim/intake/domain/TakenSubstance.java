package ru.sovmestim.intake.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.sovmestim.common.persistence.AbstractSyncedEntity;

/**
 * A substance-level intake rule: drug, dosage, frequency and time of day. This is what the reminder
 * planner works from.
 */
@Entity
@Table(name = "taken_substance")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TakenSubstance extends AbstractSyncedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intake_id", nullable = false)
    private Intake intake;

    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "active_substance_id")
    private UUID activeSubstanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    private String dosage;

    private String frequency;

    @Column(name = "intake_time")
    private LocalTime intakeTime;

    @Column(name = "end_date")
    private LocalDate endDate;
}
