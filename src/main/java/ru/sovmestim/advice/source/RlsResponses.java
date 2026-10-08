package ru.sovmestim.advice.source;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Parses recorded or live {@code interact_v2} responses. The RLS response is a list of pairs with
 * class/subclass/direction and official/alternative sources (architecture §8). No danger level is
 * read from here: the mapping table assigns it.
 */
final class RlsResponses {

    private RlsResponses() {
    }

    static List<SubstanceInteraction> parsePairs(JsonNode root) {
        List<SubstanceInteraction> interactions = new ArrayList<>();
        JsonNode pairs = root.path("pairs");
        if (pairs == null || !pairs.isArray()) {
            return interactions;
        }
        for (JsonNode pair : pairs) {
            interactions.add(toInteraction(pair));
        }
        return interactions;
    }

    static String catalogVersion(JsonNode root) {
        return text(root.get("catalog_version"));
    }

    private static SubstanceInteraction toInteraction(JsonNode pair) {
        SubstanceRef first = new SubstanceRef(
                null, text(pair.get("as1_name")), text(pair.get("as1_atc")), null, text(pair.get("as1_phg_name")));
        SubstanceRef second = new SubstanceRef(
                null, text(pair.get("as2_name")), text(pair.get("as2_atc")), null, text(pair.get("as2_phg_name")));

        List<AdviceSourceRef> sources = new ArrayList<>();
        addSource(sources, "official", pair, "official_src_name", "official_src_date", "official_src_note");
        addSource(sources, "alternative", pair, "alternative_src_name", "alternative_src_date", "alternative_src_note");

        return new SubstanceInteraction(
                first,
                second,
                text(pair.get("class")),
                text(pair.get("subclass")),
                text(pair.get("direction")),
                text(pair.get("description")),
                null,
                sources);
    }

    private static void addSource(
            List<AdviceSourceRef> sources, String kind, JsonNode pair, String nameKey, String dateKey, String noteKey) {
        String name = text(pair.get(nameKey));
        if (name == null || name.isBlank()) {
            return;
        }
        sources.add(new AdviceSourceRef(kind, name, date(text(pair.get(dateKey))), text(pair.get(noteKey))));
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asText();
    }
}
