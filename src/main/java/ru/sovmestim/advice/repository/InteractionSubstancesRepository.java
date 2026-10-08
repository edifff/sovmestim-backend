package ru.sovmestim.advice.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.advice.domain.InteractionSubstances;

public interface InteractionSubstancesRepository extends JpaRepository<InteractionSubstances, UUID> {

    @Query("""
            select i from InteractionSubstances i
            join fetch i.dangerLevel
            join fetch i.substance1 s1
            left join fetch s1.atc
            join fetch i.substance2 s2
            left join fetch s2.atc
            where i.substance1.id in :ids and i.substance2.id in :ids
            """)
    List<InteractionSubstances> findWithin(@Param("ids") Collection<UUID> ids);
}
