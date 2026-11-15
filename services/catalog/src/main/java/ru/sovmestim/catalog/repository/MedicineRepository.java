package ru.sovmestim.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.catalog.domain.Medicine;

/**
 * Repository for medicine entries.
 */
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    /**
     * Searches medicines whose name or trade mark name contains the given text.
     *
     * @param query the text to search for
     * @param pageable pagination and sorting
     * @return matching medicines
     */
    @Query("""
            select distinct m from Medicine m
            left join fetch m.tradeMark
            left join fetch m.formRelease
            where lower(m.name) like lower(concat('%', :q, '%'))
               or lower(m.tradeMark.nameBrand) like lower(concat('%', :q, '%'))
            order by m.name
            """)
    List<Medicine> search(@Param("q") String query, Pageable pageable);

    /**
     * Finds a medicine by id with its trade mark and release form loaded.
     *
     * @param id the medicine identifier
     * @return the medicine if present
     */
    @Query("""
            select distinct m from Medicine m
            left join fetch m.tradeMark
            left join fetch m.formRelease
            where m.id = :id
            """)
    Optional<Medicine> findDetailedById(@Param("id") UUID id);
}
