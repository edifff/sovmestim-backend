package ru.sovmestim.advice.source;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ru.sovmestim.advice.domain.InteractionSubstances;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.advice.repository.InteractionSubstancesRepository;
import ru.sovmestim.catalog.domain.ActiveSubstance;

/**
 * Own interaction cache backed by the {@code interaction_substances} table. Complements the live or
 * demo source with locally stored facts and own rules.
 */
@Component
public class DatabaseInteractionSource implements InteractionSource {

    private final InteractionSubstancesRepository repository;

    /**
     * Creates the cache source over the stored interaction rows.
     *
     * @param repository repository reading the {@code interaction_substances} table
     */
    public DatabaseInteractionSource(InteractionSubstancesRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "db-cache";
    }

    @Override
    public List<SubstanceInteraction> check(Collection<SubstanceRef> substances) {
        List<UUID> ids = substances.stream()
                .map(SubstanceRef::id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.size() < 2) {
            return List.of();
        }
        return repository.findWithin(ids).stream().map(DatabaseInteractionSource::toInteraction).toList();
    }

    private static SubstanceInteraction toInteraction(InteractionSubstances row) {
        return new SubstanceInteraction(
                toRef(row.getSubstance1()),
                toRef(row.getSubstance2()),
                null,
                null,
                null,
                row.getDescription(),
                parseLevel(row.getDangerLevel().getName()),
                List.of(new AdviceSourceRef("own-rule", "interaction_cache", null, null)));
    }

    private static SubstanceRef toRef(ActiveSubstance substance) {
        String atcCode = substance.getAtc() != null ? substance.getAtc().getName() : null;
        String atcName = substance.getAtc() != null ? substance.getAtc().getDescription() : null;
        return new SubstanceRef(substance.getId(), substance.getName(), atcCode, atcName, null);
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
