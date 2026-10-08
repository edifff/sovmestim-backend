package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.catalog.domain.TradeMark;

public interface TradeMarkRepository extends JpaRepository<TradeMark, UUID> {

    Optional<TradeMark> findByNameBrandIgnoreCase(String nameBrand);
}
