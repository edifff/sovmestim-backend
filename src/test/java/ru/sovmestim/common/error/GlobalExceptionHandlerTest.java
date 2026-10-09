package ru.sovmestim.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests that client-caused failures keep their 4xx status instead of falling into the 500 catch-all.
 */
class GlobalExceptionHandlerTest {

    private static final String CODE_PATH = "$.code";

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(ProbeController.BODY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("MALFORMED_REQUEST"));
    }

    @Test
    void badPathVariableReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ProbeController.BAD_UUID_PATH))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("INVALID_PARAMETER"));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ProbeController.PARAM_PATH))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("MISSING_PARAMETER"));
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ProbeController.MISSING_PATH))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("NOT_FOUND"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(ProbeController.PARAM_PATH)
                        .param(ProbeController.QUERY_PARAM, "x"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void integrityViolationReturns409() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ProbeController.CONFLICT_PATH))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value("CONFLICT"));
    }
}
