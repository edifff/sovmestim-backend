package ru.sovmestim.common.error;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tests that client-caused failures keep their 4xx status instead of falling into the 500 catch-all.
 */
class GlobalExceptionHandlerTest {

    private static final String BODY_PATH = "/probe/body";
    private static final String PATH_VARIABLE = "/probe/path/{id}";
    private static final String PARAM_PATH = "/probe/param";
    private static final String MISSING_PATH = "/probe/missing";
    private static final String CONFLICT_PATH = "/probe/conflict";
    private static final String BAD_UUID_PATH = "/probe/path/not-a-uuid";
    private static final String CODE_PATH = "$.code";
    private static final String QUERY_PARAM = "q";

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(BODY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("MALFORMED_REQUEST"));
    }

    @Test
    void badPathVariableReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(BAD_UUID_PATH))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("INVALID_PARAMETER"));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PARAM_PATH))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("MISSING_PARAMETER"));
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(MISSING_PATH))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("NOT_FOUND"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(PARAM_PATH).param(QUERY_PARAM, "x"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void integrityViolationReturns409() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(CONFLICT_PATH))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("CONFLICT"));
    }

    /**
     * Minimal controller whose endpoints trigger each mapped framework exception.
     */
    @RestController
    static class ProbeController {

        /**
         * Consumes a JSON body; invalid JSON fails before the method body runs.
         *
         * @param body the request body
         */
        @PostMapping(BODY_PATH)
        void body(@RequestBody ProbeBody body) {
        }

        /**
         * Binds a UUID path variable; a malformed value fails type conversion.
         *
         * @param id the path variable
         */
        @GetMapping(PATH_VARIABLE)
        void path(@PathVariable("id") UUID id) {
        }

        /**
         * Requires the {@code q} query parameter.
         *
         * @param query the required parameter
         */
        @GetMapping(PARAM_PATH)
        void param(@RequestParam(QUERY_PARAM) String query) {
        }

        /**
         * Throws a missing-resource exception.
         *
         * @throws NoResourceFoundException always, to exercise the 404 mapping
         */
        @GetMapping(MISSING_PATH)
        void missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, MISSING_PATH, "missing");
        }

        /**
         * Throws a data-integrity violation.
         *
         * @throws DataIntegrityViolationException always, to exercise the 409 mapping
         */
        @GetMapping(CONFLICT_PATH)
        void conflict() {
            throw new DataIntegrityViolationException("duplicate key");
        }
    }

    /**
     * Request body of the probe endpoint.
     *
     * @param value an arbitrary value
     */
    record ProbeBody(String value) {
    }
}
