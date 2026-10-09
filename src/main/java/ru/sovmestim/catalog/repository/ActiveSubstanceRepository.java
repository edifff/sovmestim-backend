package ru.sovmestim.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.catalog.domain.ActiveSubstance;

/**
 * Repository for active substance entries.
 */
public interface ActiveSubstanceRepository extends JpaRepository<ActiveSubstance, UUID> {

    /**
     * Finds a substance by its name, ignoring case.
     *
     * @param name the substance name
     * @return the active substance if present
     */
    Optional<ActiveSubstance> findByNameIgnoreCase(String name);

    /**
     * Searches substances whose name contains the given text.
     *
     * @param query the text to search for
     * @param pageable pagination and sorting
     * @return matching substances
     */
    @Query("""
            select s from ActiveSubstance s
            where lower(s.name) like lower(concat('%', :q, '%'))
            order by s.name
            """)
    List<ActiveSubstance> search(@Param("q") String query, Pageable pageable);

    /**
     * Finds all substances with the given identifiers.
     *
     * @param ids the substance identifiers
     * @return matching substances
     */
    @Query("""
            select s from ActiveSubstance s
            where s.id in :ids
            """)
    List<ActiveSubstance> findAllByIdIn(@Param("ids") List<UUID> ids);
}
