package ru.sovmestim.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.catalog.domain.ActiveSubstance;

public interface ActiveSubstanceRepository extends JpaRepository<ActiveSubstance, UUID> {

    Optional<ActiveSubstance> findByNameIgnoreCase(String name);

    @Query("""
            select s from ActiveSubstance s
            where lower(s.name) like lower(concat('%', :q, '%'))
            order by s.name
            """)
    List<ActiveSubstance> search(@Param("q") String query, Pageable pageable);

    @Query("""
            select s from ActiveSubstance s
            where s.id in :ids
            """)
    List<ActiveSubstance> findAllByIdIn(@Param("ids") List<UUID> ids);
}
