package ru.sovmestim.contract;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
     * Resolves the OpenAPI document to a file system path.
     *
     * @return the path of the OpenAPI document on the classpath
     * @throws UncheckedIOException if the document is missing from the classpath
     */
    public static Path file() {
        try {
            return new ClassPathResource(RESOURCE).getFile().toPath();
        } catch (IOException ex) {
            throw new UncheckedIOException("OpenAPI spec not found on the classpath: " + RESOURCE, ex);
        }
    }

    /**
     * Parses the OpenAPI document.
     *
     * @return the raw parse result, never {@code null}
     */
    public static SwaggerParseResult parse() {
        return new OpenAPIV3Parser().readLocation(file().toAbsolutePath().toString(), null, new ParseOptions());
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
