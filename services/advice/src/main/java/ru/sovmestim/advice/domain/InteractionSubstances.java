package ru.sovmestim.advice.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Column(name = "substance1_id", nullable = false)
    private UUID substance1Id;

    @Column(name = "substance2_id", nullable = false)
    private UUID substance2Id;

    @Column(columnDefinition = "text")
    private String description;
}
