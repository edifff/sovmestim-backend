package ru.sovmestim.advice.model;

/**
 * Kind of an advice finding, i.e. which rule produced it.
 */
public enum AdviceKind {
    DRUG_DRUG,
    DRUG_ALLERGY,
    DUPLICATE_SUBSTANCE,
    DRUG_DISEASE,
    DRUG_FOOD
}
