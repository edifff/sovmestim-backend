package ru.sovmestim.contract;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Guards against drift: the endpoints the application actually registers must be exactly the
 * endpoints documented in {@code openapi.yaml}.
 */
class OpenApiContractIT extends PostgresIntegrationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void implementedEndpointsMatchTheContract() {
        Set<String> implemented = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String path : paths(info)) {
                if (!path.startsWith("/v1/")) {
                    continue;
                }
                for (RequestMethod method : methods) {
                    implemented.add(method.name() + " " + path);
                }
            }
        });

        assertThat(implemented)
                .as("Every controller endpoint must exist in openapi.yaml and vice versa")
                .containsExactlyInAnyOrderElementsOf(OpenApiSpec.operations());
    }

    @Test
    void contractIsServed() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi:")));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }

    private static Set<String> paths(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        return Set.of();
    }
}
