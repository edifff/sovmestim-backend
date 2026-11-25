package ru.sovmestim.advice.model;

import java.util.List;

/**
 * A raw substance-substance interaction returned by an {@link ru.sovmestim.advice.source.InteractionSource}.
 *
 * @param substance1 first substance of the pair
 * @param substance2 second substance of the pair
 * @param clazz RLS pharmacological class, may be {@code null}
 * @param subclass RLS pharmacological subclass, may be {@code null}
 * @param direction RLS direction of use, may be {@code null}
 * @param description description text provided by the source
 * @param explicitLevel when the source already knows the level (e.g. own DB rows); otherwise the
 *                      engine resolves the level from {@code class}/{@code subclass}/{@code direction}
 *                      through the class-mapping table
 * @param sources provenance of the interaction fact
 */
public record SubstanceInteraction(
        SubstanceRef substance1,
        SubstanceRef substance2,
        String clazz,
        String subclass,
        String direction,
        String description,
        AdviceLevel explicitLevel,
        List<AdviceSourceRef> sources) { }
