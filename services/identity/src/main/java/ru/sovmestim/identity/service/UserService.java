package ru.sovmestim.identity.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.dto.UserResponse;
import ru.sovmestim.identity.repository.AppUserRepository;

/**
 * Read access to the public profile of a user. The gateway composes {@code GET /v1/profile} from
 * this data and the patient service.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    /**
     * Loads the public profile of a user.
     *
     * @param userId the user identifier
     * @return the public profile
     * @throws NotFoundException when the user does not exist
     */
    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        AppUser user = userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return new UserResponse(
                user.getId().toString(), user.getEmail(), user.getSurname(), user.getName(), user.getBirthDate());
    }
}
