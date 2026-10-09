package ru.sovmestim.patient.domain;

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
import ru.sovmestim.identity.domain.AppUser;

/**
 * A patient's chronic disease from the directory.
 */
@Entity
@Table(name = "chronic_disease_user")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChronicDiseaseUser extends AbstractSyncedEntity {

    /**
     * Client-assigned id: sync creates rows with the UUIDv7 the client already generated.
     */
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chronic_disease_id", nullable = false)
    private ChronicDisease chronicDisease;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "diagnosis_date")
    private LocalDate diagnosisDate;

    @Column(columnDefinition = "text")
    private String note;
}
