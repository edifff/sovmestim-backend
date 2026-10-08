package ru.sovmestim.intake.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.intake.domain.TakenSubstance;

public interface TakenSubstanceRepository extends JpaRepository<TakenSubstance, UUID> {

    @Query("""
            select ts from TakenSubstance ts
            left join fetch ts.activeSubstance s
            left join fetch s.atc
            left join fetch ts.medicine
            where ts.intake.user.id = :userId and ts.deleted = false
            """)
    List<TakenSubstance> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select ts from TakenSubstance ts
            left join fetch ts.activeSubstance s
            left join fetch s.atc
            left join fetch ts.medicine
            where ts.intake.id in :intakeIds and ts.deleted = false
            """)
    List<TakenSubstance> findActiveByIntakeIds(@Param("intakeIds") Collection<UUID> intakeIds);

    Optional<TakenSubstance> findByIdAndIntakeUserId(UUID id, UUID userId);
}
