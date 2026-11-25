package ru.sovmestim.gateway.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import org.springframework.core.io.ClassPathResource;

/**
 * Access to the OpenAPI contract that lives at {@code src/main/resources/static/openapi.yaml} and is
 * served at {@code /openapi.yaml}. The contract is the source of truth for the v1 API.
 */
public final class OpenApiSpec {

    public static final String RESOURCE = "static/openapi.yaml";

    private OpenApiSpec() {
    }

    /**
     * Parses the OpenAPI document from the classpath. The content is read as a stream so the test
     * also works when the application runs from the repackaged jar.
     *
     * @return the raw parse result, never {@code null}
     * @throws UncheckedIOException when the contract is missing from the classpath
     */
    public static SwaggerParseResult parse() {
        try (InputStream input = new ClassPathResource(RESOURCE).getInputStream()) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return new OpenAPIV3Parser().readContents(content, null, new ParseOptions());
        } catch (IOException ex) {
            throw new UncheckedIOException("OpenAPI spec not found on the classpath: " + RESOURCE, ex);
        }
    }

    /**
     * Parses the OpenAPI document into the model representation.
     *
     * @return the parsed {@code OpenAPI} model
     * @throws IllegalStateException if the document does not parse
     */
    public static OpenAPI openApi() {
        SwaggerParseResult result = parse();
        if (result.getOpenAPI() == null) {
            throw new IllegalStateException("OpenAPI spec did not parse: " + result.getMessages());
        }
        return result.getOpenAPI();
    }

    /**
     * All documented operations as {@code "METHOD /path"} strings.
     *
     * @return the set of documented operations
     */
    public static Set<String> operations() {
        Set<String> operations = new TreeSet<>();
        openApi().getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, operation) -> operations.add(method.name() + " " + path)));
        return operations;
    }
}
