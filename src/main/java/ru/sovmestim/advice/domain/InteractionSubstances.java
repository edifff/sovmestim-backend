package ru.sovmestim.advice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.sovmestim.catalog.domain.ActiveSubstance;

/**
 * Stored substance-substance interaction. Rows come from cached RLS responses or from own rules;
 * the database source is consulted in addition to the live RLS source.
 */
@Entity
@Table(name = "interaction_substances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionSubstances {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "danger_level_id", nullable = false)
    private DangerLevel dangerLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substance1_id", nullable = false)
    private ActiveSubstance substance1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substance2_id", nullable = false)
    private ActiveSubstance substance2;

    @Column(columnDefinition = "text")
    private String description;
}
