package com.iwfc.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Executable version of the Clean Architecture rule: dependencies point inward,
 * and the domain depends on nothing outside itself.
 */
@AnalyzeClasses(packages = "com.iwfc", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("com.iwfc.domain..")
            .layer("Application").definedBy("com.iwfc.application..")
            .layer("Infrastructure").definedBy("com.iwfc.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("com.iwfc.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("com.iwfc.application..", "com.iwfc.infrastructure..");

    @ArchTest
    static final ArchRule domain_is_free_of_frameworks = noClasses()
            .that().resideInAPackage("com.iwfc.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "com.fasterxml..", "java.sql..", "java.io..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("com.iwfc.application..")
            .should().dependOnClassesThat().resideInAPackage("com.iwfc.infrastructure..");

    @ArchTest
    static final ArchRule exceptions_live_in_the_domain_exception_package = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .and().resideInAPackage("com.iwfc..")
            .should().resideInAPackage("com.iwfc.domain.exception..");

    @ArchTest
    static final ArchRule use_cases_are_named_and_placed_consistently = classes()
            .that().resideInAPackage("com.iwfc.application.usecase..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule repositories_are_generic_ports_implemented_in_infrastructure = classes()
            .that().implement(com.iwfc.domain.repository.Repository.class)
            .should().resideInAPackage("com.iwfc.infrastructure..");
}
