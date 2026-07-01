package com.pecunia.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architectural fitness tests (ADR-0016) enforcing the hexagonal boundaries (ADR-0003), the port
 * placement convention (ADR-0026) and the shared-kernel discipline (Session 19).
 *
 * <p>The rules are convention-driven: they key off the package suffixes every bounded context
 * follows ({@code ..domain..}, {@code ..application..}, {@code ..web..}, {@code ..infrastructure..})
 * and off {@code com.pecunia.shared..}, so a new context is policed the moment it is created without
 * touching this file.
 *
 * <p>The shared-kernel rules, the slices cycle rule and the {@code ..domain..} rules are strict:
 * {@code com.pecunia.shared}, the top-level packages and {@code account.domain} already hold classes.
 * The remaining layer rules ({@code ..application..}, {@code ..web..}) still match no classes until
 * the {@code account} application and web layers land, so they carry {@code allowEmptyShould(true)} to
 * keep the baseline green now; drop it on each rule as its layer gains classes.
 */
@AnalyzeClasses(packages = "com.pecunia", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ---------------------------------------------------------------------------
    // Domain purity and isolation (ADR-0003, ADR-0016, ADR-0026)
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule domain_is_free_of_frameworks = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..", "lombok..")
            .as("the domain layer must stay pure Java (no Spring, JPA, Hibernate, Lombok)");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..web..", "..infrastructure..")
            .as("the domain must not depend on the application, web, or infrastructure layers");

    // ---------------------------------------------------------------------------
    // Port placement: the application owns the ports and must not see the adapters
    // (ADR-0026). Driven ports (port.out) are implemented from infrastructure;
    // driving ports (port.in) are called from web. Neither adapter is visible to
    // the application layer itself.
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..web..", "..infrastructure..")
            .allowEmptyShould(true)
            .as("the application layer (use cases + ports) must not depend on web or infrastructure");

    @ArchTest
    static final ArchRule web_does_not_access_infrastructure = noClasses()
            .that()
            .resideInAPackage("..web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)
            .as("the web layer must go through the application layer, never straight to infrastructure");

    // ---------------------------------------------------------------------------
    // Shared kernel (Session 19, ADR-0026): a pure, framework-free sink that every
    // bounded context may depend on, and which depends on no context in return.
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule shared_kernel_is_free_of_frameworks = noClasses()
            .that()
            .resideInAPackage("com.pecunia.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..", "lombok..")
            .as("the shared kernel must be pure Java — it is depended on by every domain");

    @ArchTest
    static final ArchRule shared_kernel_does_not_depend_on_contexts = noClasses()
            .that()
            .resideInAPackage("com.pecunia.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..account..", "..transaction..", "..category..", "..budget..")
            .as("the shared kernel is a sink: contexts depend on it, never the reverse");

    // ---------------------------------------------------------------------------
    // Modular monolith (ADR-0004, ADR-0016): no cyclic dependencies between the
    // top-level packages. Per-context independence (a context reaching another
    // context only via its public application API) is tightened at Block 3, when
    // `transaction` introduces the first deliberate cross-context arc through the
    // Open Host Service / Anti-Corruption Layer (Session 19).
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule top_level_packages_are_free_of_cycles =
            slices().matching("com.pecunia.(*)..").should().beFreeOfCycles();
}
