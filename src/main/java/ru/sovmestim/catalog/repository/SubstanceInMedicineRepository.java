package ru.sovmestim.catalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.catalog.domain.SubstanceInMedicine;

public interface SubstanceInMedicineRepository extends JpaRepository<SubstanceInMedicine, UUID> {

    @Query("""
            select sim from SubstanceInMedicine sim
            join fetch sim.activeSubstance s
            left join fetch s.atc
            where sim.medicine.id = :medicineId
            """)
    List<SubstanceInMedicine> findByMedicineId(@Param("medicineId") UUID medicineId);

    @Query("""
            select sim from SubstanceInMedicine sim
            join fetch sim.activeSubstance s
            left join fetch s.atc
            where sim.medicine.id in :medicineIds
            """)
    List<SubstanceInMedicine> findByMedicineIdIn(@Param("medicineIds") Collection<UUID> medicineIds);
}
