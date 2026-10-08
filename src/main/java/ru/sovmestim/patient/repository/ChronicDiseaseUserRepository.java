package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;

public interface ChronicDiseaseUserRepository extends JpaRepository<ChronicDiseaseUser, UUID> {

    @Query("""
            select cu from ChronicDiseaseUser cu
            join fetch cu.chronicDisease
            left join fetch cu.status
            where cu.user.id = :userId and cu.deleted = false
            order by cu.updatedAt desc
            """)
    List<ChronicDiseaseUser> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select cu from ChronicDiseaseUser cu
            join fetch cu.chronicDisease d
            left join fetch d.mkb
            left join fetch cu.status
            where cu.user.id = :userId and cu.updatedAt > :since
            order by cu.updatedAt asc
            """)
    List<ChronicDiseaseUser> findChangedSince(@Param("userId") UUID userId, @Param("since") java.time.Instant since);

    Optional<ChronicDiseaseUser> findByIdAndUserId(UUID id, UUID userId);
}
