package ru.sovmestim.advice.source;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.domain.InteractionSubstances;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.advice.repository.InteractionSubstancesRepository;

/**
 * Own interaction cache backed by the {@code interaction_substances} table. The table stores ids
 * only; names and ATC data come from the substances already loaded for the check, so the advice
 * service never needs a catalog call for cached rows.
 */
@Component
@RequiredArgsConstructor
public class DatabaseInteractionSource implements InteractionSource {

    private final InteractionSubstancesRepository repository;

    @Override
    public String name() {
        return "db-cache";
    }

    @Override
    public List<SubstanceInteraction> check(Collection<SubstanceRef> substances) {
        Map<UUID, SubstanceRef> byId = new LinkedHashMap<>();
        for (SubstanceRef substance : substances) {
            if (substance.id() != null) {
                byId.putIfAbsent(substance.id(), substance);
            }
        }
        if (byId.size() < 2) {
            return List.of();
        }
        return repository.findWithin(byId.keySet()).stream()
                .map(row -> toInteraction(row, byId))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static SubstanceInteraction toInteraction(InteractionSubstances row, Map<UUID, SubstanceRef> byId) {
        SubstanceRef first = byId.get(row.getSubstance1Id());
        SubstanceRef second = byId.get(row.getSubstance2Id());
        if (first == null || second == null) {
            return null;
        }
        return new SubstanceInteraction(
                first,
                second,
                null,
                null,
                null,
                row.getDescription(),
                parseLevel(row.getDangerLevel().getName()),
                List.of(new AdviceSourceRef("own-rule", "interaction_cache", null, null)));
    }

    private static AdviceLevel parseLevel(String name) {
        if (name == null) {
            return AdviceLevel.INFO;
        }
        try {
            return AdviceLevel.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AdviceLevel.INFO;
        }
    }
}
