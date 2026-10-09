package ru.sovmestim.advice.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.util.NameNormalizer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Demo {@link InteractionSource} that serves recorded real RLS responses with zero paid calls. This
 * is the fallback that keeps a presentation alive when the RLS license is not available yet
 * (architecture §13.1).
 */
@Component
@ConditionalOnProperty(name = "sovmestim.rls.mode", havingValue = "demo", matchIfMissing = true)
public class DemoInteractionSource implements InteractionSource {

    public static final String DEFAULT_RESOURCE = "demo/rls_interact_v2.json";

    private static final Logger LOG = LoggerFactory.getLogger(DemoInteractionSource.class);

    private final List<SubstanceInteraction> recorded;
    private final String catalogVersion;

    /**
     * Loads the recorded RLS responses from the classpath.
     *
     * @param objectMapper JSON mapper used to read the recorded file
     * @throws UncheckedIOException when the recorded file cannot be read
     */
    public DemoInteractionSource(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(DEFAULT_RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            this.catalogVersion = RlsResponses.catalogVersion(root);
            this.recorded = List.copyOf(RlsResponses.parsePairs(root));
            LOG.info("Demo interaction source loaded {} recorded pairs (catalog {})", recorded.size(), catalogVersion);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read recorded RLS responses " + DEFAULT_RESOURCE, ex);
        }
    }

    @Override
    public String name() {
        return "rls-demo";
    }

    /**
     * Returns the catalog version of the loaded recording.
     *
     * @return catalog version recorded in the demo file
     */
    public String catalogVersion() {
        return catalogVersion;
    }

    @Override
    public List<SubstanceInteraction> check(Collection<SubstanceRef> substances) {
        Set<String> names = substances.stream()
                .map(substance -> NameNormalizer.normalize(substance.name()))
                .collect(Collectors.toSet());
        return recorded.stream()
                .filter(interaction -> names.contains(NameNormalizer.normalize(interaction.substance1().name()))
                        && names.contains(NameNormalizer.normalize(interaction.substance2().name())))
                .toList();
    }
}
