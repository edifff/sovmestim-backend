package ru.sovmestim.sync.controller;

import java.time.Instant;
import java.util.UUID;
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

@RestController
@RequestMapping("/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/push")
    public SyncPushResponse push(@AuthenticationPrincipal Jwt jwt, @RequestBody SyncPushRequest request) {
        return syncService.push(CurrentUser.id(jwt), request);
    }

    @GetMapping("/pull")
    public SyncPullResponse pull(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "cursor", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant cursor) {
        return syncService.pull(CurrentUser.id(jwt), cursor);
    }
}
