package ru.sovmestim.common.error;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Probe endpoints that trigger each framework exception mapped by {@link GlobalExceptionHandler}.
 */
@RestController
class ProbeController {

    static final String BODY_PATH = "/probe/body";
    static final String PATH_VARIABLE = "/probe/path/{id}";
    static final String PARAM_PATH = "/probe/param";
    static final String MISSING_PATH = "/probe/missing";
    static final String CONFLICT_PATH = "/probe/conflict";
    static final String BAD_UUID_PATH = "/probe/path/not-a-uuid";
    static final String QUERY_PARAM = "q";

    @PostMapping(BODY_PATH)
    void body(@RequestBody ProbeBody body) {
    }

    @GetMapping(PATH_VARIABLE)
    void path(@PathVariable("id") UUID id) {
    }

    @GetMapping(PARAM_PATH)
    void param(@RequestParam(QUERY_PARAM) String query) {
    }

    @GetMapping(MISSING_PATH)
    void missing() throws NoResourceFoundException {
        throw new NoResourceFoundException(HttpMethod.GET, MISSING_PATH, "missing");
    }

    @GetMapping(CONFLICT_PATH)
    void conflict() {
        throw new DataIntegrityViolationException("duplicate key");
    }
}
