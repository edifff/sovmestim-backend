package ru.sovmestim.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Link between a drug product and its active substances (a drug may have several).
 */
@Entity
@Table(name = "substance_in_medicine", uniqueConstraints = @UniqueConstraint(columnNames = {"medicine_id", "active_substance_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubstanceInMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_substance_id", nullable = false)
    private ActiveSubstance activeSubstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_measurement_id")
    private UnitMeasurement unitMeasurement;

    private String dosage;
}
