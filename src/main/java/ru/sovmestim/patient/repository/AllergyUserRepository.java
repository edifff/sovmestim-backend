package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.patient.domain.AllergyUser;

public interface AllergyUserRepository extends JpaRepository<AllergyUser, UUID> {

    @Query("""
            select au from AllergyUser au
            join fetch au.allergy
            left join fetch au.severityReaction
            where au.user.id = :userId and au.deleted = false
            order by au.updatedAt desc
            """)
    List<AllergyUser> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select au from AllergyUser au
            join fetch au.allergy
            left join fetch au.severityReaction
            where au.user.id = :userId and au.updatedAt > :since
            order by au.updatedAt asc
            """)
    List<AllergyUser> findChangedSince(@Param("userId") UUID userId, @Param("since") java.time.Instant since);

    Optional<AllergyUser> findByIdAndUserId(UUID id, UUID userId);
}
