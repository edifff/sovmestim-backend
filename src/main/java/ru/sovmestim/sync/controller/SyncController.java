package ru.sovmestim.sync.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.sovmestim.common.security.CurrentUser;
import ru.sovmestim.sync.dto.SyncPullResponse;
import ru.sovmestim.sync.dto.SyncPushRequest;
import ru.sovmestim.sync.dto.SyncPushResponse;
import ru.sovmestim.sync.service.SyncService;

/**
 * REST endpoints for pushing patient changes and pulling server-side changes.
 */
@RestController
@RequestMapping("/v1/sync")
public class SyncController {

    private final SyncService syncService;

    /**
     * Creates the controller around the sync service.
     *
     * @param syncService service performing push and pull
     */
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Applies a push batch for the authenticated patient.
     *
     * @param jwt authenticated JWT of the patient
     * @param request the batch of changes to apply
     * @return per-record results of the push
     */
    @PostMapping("/push")
    public SyncPushResponse push(@AuthenticationPrincipal Jwt jwt, @RequestBody SyncPushRequest request) {
        return syncService.push(CurrentUser.id(jwt), request);
    }

    /**
     * Returns the changes made after the given cursor.
     *
     * @param jwt authenticated JWT of the patient
     * @param cursor exclusive change cursor parsed from the {@code cursor} query parameter
     * @return changed records together with the next cursor
     */
    @GetMapping("/pull")
    public SyncPullResponse pull(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "cursor", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant cursor) {
        return syncService.pull(CurrentUser.id(jwt), cursor);
    }
}
