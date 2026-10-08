package ru.sovmestim.intake.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.intake.domain.CourseMedicine;

public interface CourseMedicineRepository extends JpaRepository<CourseMedicine, UUID> {

    @Query("""
            select cm from CourseMedicine cm
            join fetch cm.medicine m
            left join fetch m.tradeMark
            where cm.user.id = :userId and cm.deleted = false
            order by cm.updatedAt desc
            """)
    List<CourseMedicine> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select cm from CourseMedicine cm
            join fetch cm.medicine m
            left join fetch m.tradeMark
            where cm.user.id = :userId and cm.updatedAt > :since
            order by cm.updatedAt asc
            """)
    List<CourseMedicine> findChangedSince(@Param("userId") UUID userId, @Param("since") java.time.Instant since);
}
