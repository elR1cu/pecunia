package com.pecunia.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architectural fitness tests (ADR-0016) enforcing the hexagonal boundaries (ADR-0003), the port
 * placement convention (ADR-0026) and the shared-kernel discipline (Session 19).
 *
 * <p>The rules are convention-driven: they key off the package suffixes every bounded context
 * follows ({@code ..domain..}, {@code ..application..}, {@code ..web..}, {@code ..infrastructure..})
 * and off {@code com.pecunia.sharedkernel..}, so a new context is policed the moment it is created
 * without touching this file.
 *
 * <p>The shared-kernel rules, the slices cycle rule, the {@code ..domain..} rules and now the
 * {@code ..web..} rule are strict: {@code com.pecunia.sharedkernel}, the top-level packages,
 * {@code account.domain} and {@code identity.web} all hold classes. The
 * {@code application_does_not_depend_on_adapters} rule still carries {@code allowEmptyShould(true)}
 * even though {@code account.application} now holds classes — a lingering allowance to drop when that
 * rule is next revisited. The convention stands: drop {@code allowEmptyShould(true)} on each rule as
 * its layer gains classes.
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
            .as("the web layer must go through the application layer, never straight to infrastructure");

    // ---------------------------------------------------------------------------
    // Read-model discipline (Session 25 debt, closed by the account/category read
    // models): driving-port interfaces return read models or ids, never domain
    // types — otherwise the mutable aggregate leaks to the adapters (archive()
    // callable from a controller). Scoped to interfaces only: the command/query
    // records living in port.in legitimately carry domain value objects as
    // inputs. Generic type arguments are inspected too, so a List<Account> is
    // caught, not just a bare Account.
    // ---------------------------------------------------------------------------

    private static final DescribedPredicate<JavaClass> A_DOMAIN_TYPE = resideInAPackage("..domain..");

    @ArchTest
    static final ArchRule driving_ports_do_not_return_domain_types = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..application.port.in..")
            .and()
            .areDeclaredInClassesThat()
            .areInterfaces()
            .should(notReturnDomainTypes())
            .as("driving ports must return read models or ids, never domain types");

    private static ArchCondition<JavaMethod> notReturnDomainTypes() {
        return new ArchCondition<>("not return a domain type") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getReturnType().getAllInvolvedRawTypes().stream()
                        .filter(A_DOMAIN_TYPE)
                        .forEach(domainType -> events.add(SimpleConditionEvent.violated(
                                method,
                                "%s returns %s, which resides in the domain layer"
                                        .formatted(method.getFullName(), domainType.getName()))));
            }
        };
    }

    // ---------------------------------------------------------------------------
    // Shared kernel (Session 19, ADR-0026, ADR-0032): a pure, framework-free sink
    // that every bounded context may depend on, and which depends on no context in
    // return. Framework-bearing cross-cutting concerns live in the sibling
    // `com.pecunia.sharedinfra`, deliberately outside `com.pecunia.sharedkernel..`
    // so the framework-free rule below needs no carve-out.
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule shared_kernel_is_free_of_frameworks = noClasses()
            .that()
            .resideInAPackage("com.pecunia.sharedkernel..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..", "lombok..")
            .as("the shared kernel must be pure Java — it is depended on by every domain");

    @ArchTest
    static final ArchRule shared_kernel_does_not_depend_on_contexts = noClasses()
            .that()
            .resideInAPackage("com.pecunia.sharedkernel..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..account..", "..transaction..", "..category..", "..budget..")
            .as("the shared kernel is a sink: contexts depend on it, never the reverse");

    // The inner layers see only the pure kernel (its ports and value objects),
    // never the framework-bearing shared adapters: an `IdGenerator` is injected as
    // the `com.pecunia.sharedkernel` port, not as `sharedinfra.id.Uuidv7IdGenerator`
    // (ADR-0032).
    @ArchTest
    static final ArchRule domain_and_application_do_not_depend_on_shared_infra = noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.pecunia.sharedinfra..")
            .as(
                    "domain and application must depend on the shared kernel ports only, never on shared infrastructure adapters");

    // ---------------------------------------------------------------------------
    // Modular monolith (ADR-0004, ADR-0016): no cyclic dependencies between the
    // top-level packages. The slices are the first segment under com.pecunia
    // (account, identity, sharedkernel, sharedinfra), so `sharedinfra` collapses
    // its id/security/observability concerns into one slice (ADR-0032) — a cycle
    // between those three would be intra-slice and invisible here, an accepted
    // trade-off for leaf technical concerns. Per-context independence (a context
    // reaching another context only via its public application API) is tightened
    // at Block 3, when `transaction` introduces the first deliberate cross-context
    // arc through the Open Host Service / Anti-Corruption Layer (Session 19).
    // ---------------------------------------------------------------------------

    @ArchTest
    static final ArchRule top_level_packages_are_free_of_cycles =
            slices().matching("com.pecunia.(*)..").should().beFreeOfCycles();
}
