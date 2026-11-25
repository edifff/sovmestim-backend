package ru.sovmestim.catalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.catalog.domain.SubstanceInMedicine;

/**
 * Repository for links between medicines and their active substances.
 */
public interface SubstanceInMedicineRepository extends JpaRepository<SubstanceInMedicine, UUID> {

    /**
     * Finds all substance links of one medicine, with substances and ATC data loaded.
     *
     * @param medicineId the medicine identifier
     * @return the substance links of the medicine
     */
    @Query("""
            select sim from SubstanceInMedicine sim
            join fetch sim.activeSubstance s
            left join fetch s.atc
            where sim.medicine.id = :medicineId
            """)
    List<SubstanceInMedicine> findByMedicineId(@Param("medicineId") UUID medicineId);

    /**
     * Finds all substance links of several medicines, with substances and ATC data loaded.
     *
     * @param medicineIds the medicine identifiers
     * @return the substance links of the given medicines
     */
    @Query("""
            select sim from SubstanceInMedicine sim
            join fetch sim.activeSubstance s
            left join fetch s.atc
            where sim.medicine.id in :medicineIds
            """)
    List<SubstanceInMedicine> findByMedicineIdIn(@Param("medicineIds") Collection<UUID> medicineIds);
}
