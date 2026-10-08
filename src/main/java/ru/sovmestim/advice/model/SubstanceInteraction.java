package ru.sovmestim.advice.model;

import java.util.List;

/**
 * A raw substance-substance interaction returned by an {@link ru.sovmestim.advice.source.InteractionSource}.
 *
 * @param explicitLevel when the source already knows the level (e.g. own DB rows); otherwise the
 *                      engine resolves the level from {@code class}/{@code subclass}/{@code direction}
 *                      through the class-mapping table
 */
public record SubstanceInteraction(
        SubstanceRef substance1,
        SubstanceRef substance2,
        String clazz,
        String subclass,
        String direction,
        String description,
        AdviceLevel explicitLevel,
        List<AdviceSourceRef> sources) {}
