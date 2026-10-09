package ru.sovmestim.advice.source;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.config.RlsProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Live RLS {@code interact_v2} client. Only active in {@code sovmestim.rls.mode=http}.
 *
 * <p>RLS access was not available in this environment, so this client is intentionally thin: it
 * calls the documented endpoint and parses the same pair shape as the recorded demo responses. The
 * open questions from architecture §9 (auth, rate limits, {@code as_ids} length, licensed method)
 * must be closed before this is production-ready.
 */
@Component
@ConditionalOnProperty(name = "sovmestim.rls.mode", havingValue = "http")
public class RlsHttpInteractionSource implements InteractionSource {

    private static final Logger LOG = LoggerFactory.getLogger(RlsHttpInteractionSource.class);

    private final RestClient client;
    private final ObjectMapper objectMapper;

    /**
     * Builds the client from the configured RLS endpoint and API key.
     *
     * @param properties RLS endpoint properties
     * @param objectMapper JSON mapper for the response body
     */
    public RlsHttpInteractionSource(RlsProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        }
        this.client = builder.build();
    }

    @Override
    public String name() {
        return "rls-http";
    }

    @Override
    public List<SubstanceInteraction> check(Collection<SubstanceRef> substances) {
        String asIds = substances.stream()
                .map(SubstanceRef::name)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(","));
        if (asIds.isBlank()) {
            return List.of();
        }
        String body = client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/interact_v2").queryParam("as_ids", asIds).build())
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(body);
        return RlsResponses.parsePairs(root);
    }

    /**
     * Kept for symmetry with the demo source; a live catalog version is not part of {@code interact_v2}.
     *
     * @return always {@code null} because the live endpoint does not expose a catalog version
     */
    public String catalogVersion() {
        LOG.trace("RLS catalog version is managed by the import job");
        return null;
    }
}
