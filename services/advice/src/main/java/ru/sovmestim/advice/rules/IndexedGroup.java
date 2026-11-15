package ru.sovmestim.advice.rules;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import ru.sovmestim.common.util.NameNormalizer;

/**
 * A cross-reactivity group with pre-normalized lookups so that matching never re-normalizes.
 *
 * @param group the original rule group
 * @param substanceNames normalized substance names of the group
 * @param upperAtcPrefixes upper-cased ATC prefixes of the group
 */
record IndexedGroup(CrossReactivity group, Set<String> substanceNames, List<String> upperAtcPrefixes) {

    IndexedGroup(CrossReactivity group) {
        this(
                group,
                group.substances().stream()
                        .map(NameNormalizer::normalize)
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toSet()),
                group.atcPrefixes().stream().map(prefix -> prefix.toUpperCase(Locale.ROOT)).toList());
    }

    String name() {
        return group.name();
    }

    boolean containsSubstance(IndexedSubstance substance) {
        if (substance.hasName() && substanceNames.contains(substance.normalizedName())) {
            return true;
        }
        if (substance.upperAtc() == null) {
            return false;
        }
        for (String prefix : upperAtcPrefixes) {
            if (substance.upperAtc().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
