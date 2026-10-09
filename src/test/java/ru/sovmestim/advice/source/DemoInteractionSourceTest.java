package ru.sovmestim.advice.source;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests for the demo interaction source's built-in drug pair catalog.
 */
class DemoInteractionSourceTest {

    private static final String WARFARIN = "варфарин";

    private static DemoInteractionSource source;

    @BeforeAll
    static void setUp() {
        source = new DemoInteractionSource(new ObjectMapper());
    }

    @Test
    void findsRecordedInteractionForBothSubstances() {
        List<SubstanceInteraction> interactions =
                source.check(List.of(substance(WARFARIN), substance("ацетилсалициловая кислота")));

        Assertions.assertThat(interactions).hasSize(1);
        SubstanceInteraction interaction = interactions.get(0);
        Assertions.assertThat(interaction.clazz()).isEqualTo("pharm");
        Assertions.assertThat(interaction.subclass()).isEqualTo("synergism");
        Assertions.assertThat(interaction.direction()).isEqualTo("increase_toxicity");
        Assertions.assertThat(interaction.sources()).isNotEmpty();
    }

    @Test
    void ignoresPairsWhenOneSubstanceIsMissing() {
        Assertions.assertThat(source.check(List.of(substance(WARFARIN)))).isEmpty();
        Assertions.assertThat(source.check(List.of(substance(WARFARIN), substance("неизвестное")))).isEmpty();
    }

    @Test
    void exposesCatalogVersion() {
        Assertions.assertThat(source.catalogVersion()).isEqualTo("demo-2026.01");
    }

    private static SubstanceRef substance(String name) {
        return new SubstanceRef(UUID.randomUUID(), name, null, null, null);
    }
}
