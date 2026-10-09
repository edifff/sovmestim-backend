package ru.sovmestim.contract;

import java.util.Set;

import io.swagger.v3.parser.core.models.SwaggerParseResult;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Validates the contract itself: it must parse without errors and document exactly the v1 surface.
 */
class OpenApiSpecTest {

    private static final Set<String> EXPECTED_OPERATIONS = Set.of(
            "POST /v1/auth/request-code",
            "POST /v1/auth/verify",
            "POST /v1/auth/refresh",
            "GET /v1/catalog/substances",
            "GET /v1/catalog/medicines",
            "GET /v1/catalog/medicines/{id}",
            "GET /v1/catalog/resolve",
            "GET /v1/profile",
            "GET /v1/profile/allergies",
            "POST /v1/profile/allergies",
            "PUT /v1/profile/allergies/{id}",
            "DELETE /v1/profile/allergies/{id}",
            "GET /v1/profile/conditions",
            "POST /v1/profile/conditions",
            "DELETE /v1/profile/conditions/{id}",
            "GET /v1/medications",
            "POST /v1/medications",
            "DELETE /v1/medications/{id}",
            "POST /v1/advice/check",
            "POST /v1/advice/recheck",
            "POST /v1/sync/push",
            "GET /v1/sync/pull");

    @Test
    void parsesWithoutErrors() {
        SwaggerParseResult result = OpenApiSpec.parse();
        Assertions.assertThat(result.getOpenAPI()).isNotNull();
        Assertions.assertThat(result.getMessages()).isEmpty();
    }

    @Test
    void documentsTheExpectedOperations() {
        Assertions.assertThat(OpenApiSpec.operations()).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);
    }

    @Test
    void declaresBearerSecurityScheme() {
        Assertions.assertThat(OpenApiSpec.openApi().getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void declaresCoreSchemas() {
        Assertions.assertThat(OpenApiSpec.openApi().getComponents().getSchemas())
                .containsKeys(
                        "ApiError",
                        "AdviceCheckRequest",
                        "AdviceResult",
                        "AdviceFinding",
                        "ResolvedDrug",
                        "MedicationChange",
                        "AdviceDelivery",
                        "SyncPushRequest",
                        "SyncPullResponse",
                        "CourseMedicineRequest",
                        "AllergyRequest",
                        "ConditionRequest");
    }
}
