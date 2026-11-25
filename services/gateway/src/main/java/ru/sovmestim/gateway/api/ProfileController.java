package ru.sovmestim.gateway.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.ConflictException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.client.IdentityClient;
import ru.sovmestim.gateway.client.PatientClient;
import ru.sovmestim.gateway.dto.AllergyRequest;
import ru.sovmestim.gateway.dto.AllergyView;
import ru.sovmestim.gateway.dto.ConditionRequest;
import ru.sovmestim.gateway.dto.ConditionView;
import ru.sovmestim.gateway.dto.UserResponse;
import ru.sovmestim.gateway.security.CurrentUser;

/**
 * REST endpoints for the authenticated patient's profile: personal data, allergies and conditions.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/profile")
public class ProfileController {

    private final IdentityClient identityClient;
    private final PatientClient patientClient;

    /**
     * Returns the personal data of the authenticated user.
     *
     * @param jwt the token of the authenticated user
     * @return the current user's profile
     */
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        var user = identityClient.user(CurrentUser.id(jwt));
        return new UserResponse(user.id(), user.email(), user.surname(), user.name(), user.birthDate());
    }

    /**
     * Lists the active allergies of the authenticated user.
     *
     * @param jwt the token of the authenticated user
     * @return the user's active allergies
     */
    @GetMapping("/allergies")
    public List<AllergyView> allergies(@AuthenticationPrincipal Jwt jwt) {
        return patientClient.allergies(CurrentUser.id(jwt));
    }

    /**
     * Adds an allergy to the authenticated user's profile.
     *
     * @param jwt the token of the authenticated user
     * @param request the allergy data to store
     * @return the created allergy
     */
    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public AllergyView addAllergy(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AllergyRequest request) {
        UUID userId = CurrentUser.id(jwt);
        ChangeResult result = patientClient.upsertAllergy(userId, null, null, request);
        return requireAllergy(userId, requireApplied(result));
    }

    /**
     * Updates an existing allergy of the authenticated user.
     *
     * @param jwt the token of the authenticated user
     * @param id the id of the allergy record to update
     * @param request the new allergy data
     * @return the updated allergy
     */
    @PutMapping("/allergies/{id}")
    public AllergyView updateAllergy(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AllergyRequest request) {
        UUID userId = CurrentUser.id(jwt);
        ChangeResult result = patientClient.upsertAllergy(userId, id, null, request);
        return requireAllergy(userId, requireApplied(result));
    }

    /**
     * Deletes an allergy from the authenticated user's profile.
     *
     * @param jwt the token of the authenticated user
     * @param id the id of the allergy record to delete
     */
    @DeleteMapping("/allergies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllergy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        requireApplied(patientClient.deleteAllergy(CurrentUser.id(jwt), id, null));
    }

    /**
     * Lists the active chronic conditions of the authenticated user.
     *
     * @param jwt the token of the authenticated user
     * @return the user's active chronic conditions
     */
    @GetMapping("/conditions")
    public List<ConditionView> conditions(@AuthenticationPrincipal Jwt jwt) {
        return patientClient.conditions(CurrentUser.id(jwt));
    }

    /**
     * Adds a chronic condition to the authenticated user's profile.
     *
     * @param jwt the token of the authenticated user
     * @param request the condition data to store
     * @return the created chronic condition
     */
    @PostMapping("/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionView addCondition(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConditionRequest request) {
        UUID userId = CurrentUser.id(jwt);
        ChangeResult result = patientClient.upsertCondition(userId, null, null, request);
        return requireCondition(userId, requireApplied(result));
    }

    /**
     * Deletes a chronic condition from the authenticated user's profile.
     *
     * @param jwt the token of the authenticated user
     * @param id the id of the condition record to delete
     */
    @DeleteMapping("/conditions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCondition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        requireApplied(patientClient.deleteCondition(CurrentUser.id(jwt), id, null));
    }

    private static UUID requireApplied(ChangeResult result) {
        if (ChangeResult.APPLIED.equals(result.status())) {
            return result.id();
        }
        if (ChangeResult.CONFLICT.equals(result.status())) {
            throw new ConflictException(result.message() != null ? result.message() : "Server row is newer");
        }
        throw new BadRequestException(result.message() != null ? result.message() : "Change rejected");
    }

    private AllergyView requireAllergy(UUID userId, UUID id) {
        return patientClient.allergies(userId).stream()
                .filter(allergy -> allergy.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Allergy not found: " + id));
    }

    private ConditionView requireCondition(UUID userId, UUID id) {
        return patientClient.conditions(userId).stream()
                .filter(condition -> condition.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Condition not found: " + id));
    }
}
