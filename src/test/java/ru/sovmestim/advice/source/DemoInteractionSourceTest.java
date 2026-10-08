package ru.sovmestim.advice.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import tools.jackson.databind.ObjectMapper;

class DemoInteractionSourceTest {

    private static DemoInteractionSource source;

    @BeforeAll
    static void setUp() {
        source = new DemoInteractionSource(new ObjectMapper());
    }

    @Test
    void findsRecordedInteractionForBothSubstances() {
        List<SubstanceInteraction> interactions =
                source.check(List.of(substance("варфарин"), substance("ацетилсалициловая кислота")));

        assertThat(interactions).hasSize(1);
        SubstanceInteraction interaction = interactions.get(0);
        assertThat(interaction.clazz()).isEqualTo("pharm");
        assertThat(interaction.subclass()).isEqualTo("synergism");
        assertThat(interaction.direction()).isEqualTo("increase_toxicity");
        assertThat(interaction.sources()).isNotEmpty();
    }

    @Test
    void ignoresPairsWhenOneSubstanceIsMissing() {
        assertThat(source.check(List.of(substance("варфарин")))).isEmpty();
        assertThat(source.check(List.of(substance("варфарин"), substance("неизвестное")))).isEmpty();
    }

    @Test
    void exposesCatalogVersion() {
        assertThat(source.catalogVersion()).isEqualTo("demo-2026.01");
    }

    private static SubstanceRef substance(String name) {
        return new SubstanceRef(UUID.randomUUID(), name, null, null, null);
    }
}
