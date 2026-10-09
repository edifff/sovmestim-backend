package ru.sovmestim.advice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import ru.sovmestim.advice.dto.AdviceCheckRequest;
import ru.sovmestim.advice.dto.AdviceCheckResponse;
import ru.sovmestim.advice.service.AdviceService;
import ru.sovmestim.common.security.CurrentUser;

/**
 * Online compatibility check. Reminders and offline screens never depend on this endpoint.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/advice")
public class AdviceController {

    private final AdviceService adviceService;

    /**
     * Checks one drug against the current patient state.
     *
     * @param jwt authenticated caller token
     * @param request drug identification for the check
     * @return stored advice response
     */
    @PostMapping("/check")
    public AdviceCheckResponse check(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AdviceCheckRequest request) {
        UUID userId = CurrentUser.id(jwt);
        return adviceService.check(userId, request);
    }

    /**
     * Recompute advice for all active medications, e.g. after a rules or catalog update.
     *
     * @param jwt authenticated caller token
     * @return one stored response per active course
     */
    @PostMapping("/recheck")
    public List<AdviceCheckResponse> recheck(@AuthenticationPrincipal Jwt jwt) {
        return adviceService.recheckAll(CurrentUser.id(jwt));
    }
}
