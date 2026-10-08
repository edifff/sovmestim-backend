package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.identity.domain.OtpCode;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);
}
