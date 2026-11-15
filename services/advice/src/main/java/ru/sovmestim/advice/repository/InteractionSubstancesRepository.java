package ru.sovmestim.advice.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.advice.domain.InteractionSubstances;

/**
 * Repository for stored substance-substance interactions.
 */
public interface InteractionSubstancesRepository extends JpaRepository<InteractionSubstances, UUID> {

    /**
     * Loads interactions whose both substances are among the given ids, with levels fetched.
     *
     * @param ids substance ids to look up
     * @return matching stored interactions
     */
    @Query("""
            select i from InteractionSubstances i
            join fetch i.dangerLevel
            where i.substance1Id in :ids and i.substance2Id in :ids
            """)
    List<InteractionSubstances> findWithin(@Param("ids") Collection<UUID> ids);
}
