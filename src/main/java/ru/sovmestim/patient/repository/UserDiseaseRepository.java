package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.UserDisease;

public interface UserDiseaseRepository extends JpaRepository<UserDisease, UUID> {

    List<UserDisease> findByUserIdAndDeletedFalse(UUID userId);
}
