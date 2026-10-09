package ru.sovmestim.advice.source;

import java.util.Collection;
import java.util.List;

import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Source of drug-drug interaction facts. The licensed RLS source, the recorded demo responses and
 * the own database cache all implement this interface, so the source can be swapped without a
 * rewrite (architecture §8).
 */
public interface InteractionSource {

    /**
     * Human-readable source name for logs and advice provenance.
     *
     * @return source name
     */
    String name();

    /**
     * Checks the given substances as one list (this mirrors RLS {@code interact_v2}, which is called
     * for the patient's whole list at once).
     *
     * @param substances substances of the patient and the checked drug
     * @return interactions known for any pair of the given substances
     */
    List<SubstanceInteraction> check(Collection<SubstanceRef> substances);
}
