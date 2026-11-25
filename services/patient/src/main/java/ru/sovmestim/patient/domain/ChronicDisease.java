package ru.sovmestim.patient.domain;

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
 * Directory entry for a chronic disease, optionally linked to an ICD-10 code.
 */
@Entity
@Table(name = "chronic_disease")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChronicDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mkb_id")
    private Mkb mkb;

    @Column(nullable = false, unique = true)
    private String name;
}
