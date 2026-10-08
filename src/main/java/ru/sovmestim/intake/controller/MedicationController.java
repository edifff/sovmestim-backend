package ru.sovmestim.intake.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.sovmestim.common.security.CurrentUser;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.dto.CourseMedicineView;
import ru.sovmestim.intake.service.MedicationService;

@RestController
@RequestMapping("/v1/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<CourseMedicineView> list(@AuthenticationPrincipal Jwt jwt) {
        return medicationService.list(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseMedicineView add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseMedicineRequest request) {
        return medicationService.add(CurrentUser.id(jwt), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        medicationService.delete(CurrentUser.id(jwt), id);
    }
}
