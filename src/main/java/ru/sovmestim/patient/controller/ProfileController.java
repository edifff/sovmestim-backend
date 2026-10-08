package ru.sovmestim.patient.controller;

import jakarta.validation.Valid;
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
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.common.security.CurrentUser;
import ru.sovmestim.identity.dto.UserResponse;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.patient.dto.AllergyRequest;
import ru.sovmestim.patient.dto.AllergyView;
import ru.sovmestim.patient.dto.ConditionRequest;
import ru.sovmestim.patient.dto.ConditionView;
import ru.sovmestim.patient.service.PatientService;

@RestController
@RequestMapping("/v1/profile")
public class ProfileController {

    private final PatientService patientService;
    private final AppUserRepository userRepository;

    public ProfileController(PatientService patientService, AppUserRepository userRepository) {
        this.patientService = patientService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        return userRepository
                .findById(userId)
                .map(user -> new UserResponse(
                        user.getId().toString(), user.getEmail(), user.getSurname(), user.getName(), user.getBirthDate()))
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    @GetMapping("/allergies")
    public List<AllergyView> allergies(@AuthenticationPrincipal Jwt jwt) {
        return patientService.listAllergies(CurrentUser.id(jwt));
    }

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public AllergyView addAllergy(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AllergyRequest request) {
        return patientService.addAllergy(CurrentUser.id(jwt), request);
    }

    @PutMapping("/allergies/{id}")
    public AllergyView updateAllergy(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AllergyRequest request) {
        return patientService.updateAllergy(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/allergies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllergy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        patientService.deleteAllergy(CurrentUser.id(jwt), id);
    }

    @GetMapping("/conditions")
    public List<ConditionView> conditions(@AuthenticationPrincipal Jwt jwt) {
        return patientService.listConditions(CurrentUser.id(jwt));
    }

    @PostMapping("/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionView addCondition(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConditionRequest request) {
        return patientService.addCondition(CurrentUser.id(jwt), request);
    }

    @DeleteMapping("/conditions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCondition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        patientService.deleteCondition(CurrentUser.id(jwt), id);
    }
}
