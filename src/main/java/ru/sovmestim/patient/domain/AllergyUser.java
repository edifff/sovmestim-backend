package ru.sovmestim.patient.domain;

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
 * A patient's allergy. Mutable, synced entity (id, version, updated_at, is_deleted, is_synced).
 */
@Entity
@Table(name = "allergy_user")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AllergyUser extends AbstractSyncedEntity {

    /**
     * Client-assigned id: sync creates rows with the UUIDv7 the client already generated, so the id
     * is not database-generated here.
     */
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergy_id", nullable = false)
    private Allergy allergy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "severity_reaction_id")
    private SeverityReaction severityReaction;

    @Column(columnDefinition = "text")
    private String symptoms;

    @Column(columnDefinition = "text")
    private String reason;
}
