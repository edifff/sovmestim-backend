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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.sovmestim.common.persistence.AbstractSyncedEntity;
import ru.sovmestim.identity.domain.AppUser;

/**
 * A patient-entered disease that is not in the directory yet.
 */
@Entity
@Table(name = "user_disease")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserDisease extends AbstractSyncedEntity {

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
}
