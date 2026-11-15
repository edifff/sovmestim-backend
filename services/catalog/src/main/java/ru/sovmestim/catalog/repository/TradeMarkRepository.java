package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.catalog.domain.TradeMark;

/**
 * Repository for medicine trade mark entries.
 */
public interface TradeMarkRepository extends JpaRepository<TradeMark, UUID> {

    /**
     * Finds a trade mark by its brand name, ignoring case.
     *
     * @param nameBrand the brand name
     * @return the trade mark if present
     */
    Optional<TradeMark> findByNameBrandIgnoreCase(String nameBrand);
}
