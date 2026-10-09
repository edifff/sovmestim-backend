package ru.sovmestim.intake.controller;

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

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import ru.sovmestim.common.security.CurrentUser;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.dto.CourseMedicineView;
import ru.sovmestim.intake.service.MedicationService;

/**
 * REST endpoints for managing the patient's medication courses.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/medications")
public class MedicationController {

    private final MedicationService medicationService;

    /**
     * Lists the authenticated patient's active medication courses.
     *
     * @param jwt authenticated JWT of the patient
     * @return views of the active medication courses
     */
    @GetMapping
    public List<CourseMedicineView> list(@AuthenticationPrincipal Jwt jwt) {
        return medicationService.list(CurrentUser.id(jwt));
    }

    /**
     * Adds a medication course for the authenticated patient.
     *
     * @param jwt authenticated JWT of the patient
     * @param request the course to add
     * @return the stored course as a view
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseMedicineView add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CourseMedicineRequest request) {
        return medicationService.add(CurrentUser.id(jwt), request);
    }

    /**
     * Deletes a medication course of the authenticated patient.
     *
     * @param jwt authenticated JWT of the patient
     * @param id id of the course to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        medicationService.delete(CurrentUser.id(jwt), id);
    }
}
