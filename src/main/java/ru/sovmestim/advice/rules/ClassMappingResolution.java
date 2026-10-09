package ru.sovmestim.advice.rules;

import ru.sovmestim.advice.model.AdviceLevel;

/**
 * Resolved danger level together with its explanation.
 *
 * @param level resolved danger level
 * @param explanation expert explanation, may be empty
 */
public record ClassMappingResolution(AdviceLevel level, String explanation) { }
