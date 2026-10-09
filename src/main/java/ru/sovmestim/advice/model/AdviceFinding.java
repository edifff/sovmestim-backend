package ru.sovmestim.advice.model;

import java.util.List;

/**
 * One reported advice finding: what was found, how severe it is and where it comes from.
 *
 * @param kind kind of the finding
 * @param level severity of the finding
 * @param title short headline shown to the user
 * @param text detailed explanation shown to the user
 * @param substances substance names the finding refers to
 * @param sources provenance of the finding
 */
public record AdviceFinding(
        AdviceKind kind,
        AdviceLevel level,
        String title,
        String text,
        List<String> substances,
        List<AdviceSourceRef> sources) { }
