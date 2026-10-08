package ru.sovmestim.catalog.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.sovmestim.catalog.domain.Medicine;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    @Query("""
            select distinct m from Medicine m
            left join m.tradeMark t
            where lower(m.name) like lower(concat('%', :q, '%'))
               or lower(t.nameBrand) like lower(concat('%', :q, '%'))
            order by m.name
            """)
    List<Medicine> search(@Param("q") String query, Pageable pageable);

    @Query("""
            select distinct m from Medicine m
            left join fetch m.tradeMark
            left join fetch m.formRelease
            where m.id = :id
            """)
    java.util.Optional<Medicine> findDetailedById(@Param("id") UUID id);
}
