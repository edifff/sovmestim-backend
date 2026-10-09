package ru.sovmestim.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Architecture rules enforced by ArchUnit: module boundaries, layering and injection style.
 *
 * <p>{@link #FEATURE_SLICES_ARE_CYCLE_FREE} is frozen. The current {@code advice}/{@code catalog} and
 * {@code advice}/{@code patient} cycles (shared vocabulary in {@code advice.model}) are recorded as
 * a baseline under {@code src/test/resources/archunit_store}, so only new cycles fail the build.
 */
@AnalyzeClasses(packages = "ru.sovmestim", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ADVICE = "..advice..";
    private static final String CATALOG = "..catalog..";
    private static final String IDENTITY = "..identity..";
    private static final String INTAKE = "..intake..";
    private static final String PATIENT = "..patient..";
    private static final String SYNC = "..sync..";
    private static final String COMMON = "..common..";
    private static final String CONFIG = "..config..";
    private static final String DOMAIN = "..domain..";
    private static final String SERVICE = "..service..";
    private static final String REPOSITORY = "..repository..";
    private static final String CONTROLLER = "..controller..";
    private static final String BASE_PACKAGE = "ru.sovmestim";
    private static final String DOT = ".";
    private static final String[] FEATURE_NAMES = {"advice", "catalog", "identity", "intake", "patient", "sync"};

    /** Groups the feature modules into slices and ignores wiring ({@code config}) and shared code. */
    private static final SliceAssignment FEATURE_SLICES = new SliceAssignment() {
        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            for (String feature : FEATURE_NAMES) {
                String prefix = BASE_PACKAGE + DOT + feature;
                if (javaClass.getPackageName().equals(prefix)
                        || javaClass.getPackageName().startsWith(prefix + DOT)) {
                    return SliceIdentifier.of(feature);
                }
            }
            return SliceIdentifier.ignore();
        }

        @Override
        public String getDescription() {
            return "feature slices";
        }
    };

    @ArchTest
    private static final ArchRule COMMON_IS_INDEPENDENT_OF_FEATURES =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(COMMON)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(ADVICE, CATALOG, IDENTITY, INTAKE, PATIENT, SYNC, CONFIG)
                    .because("common is shared code and must not depend on feature modules");

    @ArchTest
    private static final ArchRule DOMAIN_IS_INDEPENDENT_OF_SERVICES =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(SERVICE, CONTROLLER)
                    .because("entities must not depend on service or web layers");

    @ArchTest
    private static final ArchRule FEATURES_DO_NOT_DEPEND_ON_SYNC =
            ArchRuleDefinition.noClasses()
                    .that().resideInAnyPackage(ADVICE, CATALOG, IDENTITY, INTAKE, PATIENT)
                    .should().dependOnClassesThat()
                    .resideInAPackage(SYNC)
                    .because("sync is the top orchestrator and depends on them, not the other way round");

    @ArchTest
    private static final ArchRule CONTROLLERS_USE_SERVICES_ONLY =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(CONTROLLER)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(REPOSITORY, DOMAIN)
                    .because("controllers must not contain persistence or domain logic");

    @ArchTest
    private static final ArchRule NO_FIELD_INJECTION =
            ArchRuleDefinition.fields()
                    .should().notBeAnnotatedWith(Autowired.class)
                    .because("constructor injection is used throughout the project");

    @ArchTest
    private static final ArchRule FEATURE_SLICES_ARE_CYCLE_FREE = FreezingArchRule.freeze(
            SlicesRuleDefinition.slices()
                    .assignedFrom(FEATURE_SLICES)
                    .should().beFreeOfCycles());
}
