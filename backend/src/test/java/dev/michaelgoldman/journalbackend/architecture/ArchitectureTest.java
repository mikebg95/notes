package dev.michaelgoldman.journalbackend.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import dev.michaelgoldman.journalbackend.JournalBackendApplication;

@AnalyzeClasses(
        packagesOf = JournalBackendApplication.class,
        importOptions = {ImportOption.DoNotIncludeTests.class, ArchitectureTest.DoNotIncludeGeneratedApi.class})
class ArchitectureTest {

    private static final String ROOT = "dev.michaelgoldman.journalbackend";

    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String PORTS = ROOT + ".application.port..";
    private static final String OUTBOUND_PORTS = ROOT + ".application.port.out..";
    private static final String WEB_ADAPTER = ROOT + ".adapter.in.web..";
    private static final String AI_ADAPTER = ROOT + ".adapter.out.ai..";
    private static final String PERSISTENCE_ADAPTER = ROOT + ".adapter.out.persistence..";
    private static final String OUTBOUND_ADAPTERS = ROOT + ".adapter.out..";
    private static final String GENERATED_API = ROOT + ".api..";
    private static final String JDK = "java..";
    private static final String NULLNESS = "org.jspecify.annotations..";

    private static final String SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";

    /**
     * The OpenAPI generator owns everything under the api package, so a violation there
     * cannot be fixed by hand — it would have to be fixed in the spec or the generator config.
     * Rules still see dependencies *onto* these classes; they just aren't checked themselves.
     */
    static class DoNotIncludeGeneratedApi implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/journalbackend/api/");
        }
    }

    /**
     * Optional layers stay on permanently: onionArchitecture() always defines a domain-service
     * layer, and this design deliberately has none — orchestration lives in application/service.
     * Emptiness of the layers that must exist is caught by the per-layer rules below instead.
     */
    @ArchTest
    static final ArchRule onion_architecture_should_be_respected = onionArchitecture()
            .domainModels(DOMAIN)
            .applicationServices(APPLICATION)
            .adapter("web", WEB_ADAPTER)
            .adapter("persistence", PERSISTENCE_ADAPTER)
            .adapter("ai", AI_ADAPTER)
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule domain_should_only_depend_on_itself_and_the_jdk = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(DOMAIN, JDK, NULLNESS);

    /**
     * Deliberately narrower than the domain rule, and widened one package at a time:
     * application services are Spring beans, so they will need the stereotype annotations
     * and nothing else.
     */
    @ArchTest
    static final ArchRule application_should_only_depend_on_domain_and_the_jdk = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(DOMAIN, APPLICATION, JDK, NULLNESS);

    /**
     * Sliced at the first level below the root, so domain / application / adapter are the units.
     * Slicing deeper reports a cycle for something like domain.model referencing
     * domain.exception, which is not a design problem.
     */
    @ArchTest
    static final ArchRule packages_should_be_free_of_cycles =
            slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule jpa_should_only_be_used_in_the_persistence_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule spring_data_should_only_be_used_in_the_persistence_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.data..");

    @ArchTest
    static final ArchRule spring_ai_should_only_be_used_in_the_ai_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(AI_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.ai..");

    @ArchTest
    static final ArchRule spring_web_and_generated_api_types_should_only_be_used_in_the_web_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(WEB_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", GENERATED_API);

    /**
     * Covers both annotations: the wrong import compiles and does nothing useful here.
     * The transaction wraps the millisecond database write only, never the AI call.
     */
    @ArchTest
    static final ArchRule transactional_should_only_be_used_in_the_persistence_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(SPRING_TRANSACTIONAL)
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(JAKARTA_TRANSACTIONAL);

    /**
     * Spring's proxy only advises public methods, so @Transactional on anything else is
     * silently ignored — no error, no transaction.
     */
    // TODO(phase-5): drop allowEmptyShould once the persistence adapter exists
    @ArchTest
    static final ArchRule transactional_methods_should_be_public = methods()
            .that()
            .areAnnotatedWith(SPRING_TRANSACTIONAL)
            .or()
            .areAnnotatedWith(JAKARTA_TRANSACTIONAL)
            .should()
            .bePublic()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule ports_should_only_contain_interfaces_and_records_and_enums = classes()
            .that()
            .resideInAPackage(PORTS)
            .should()
            .beInterfaces()
            .orShould()
            .beRecords()
            .orShould()
            .beEnums();

    // TODO(phase-5): drop allowEmptyShould once the persistence adapter exists
    @ArchTest
    static final ArchRule outbound_ports_should_only_be_implemented_by_outbound_adapters = classes()
            .that()
            .implement(resideInAPackage(OUTBOUND_PORTS))
            .should()
            .resideInAPackage(OUTBOUND_ADAPTERS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dependencies_should_not_be_field_injected = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule java_util_logging_should_not_be_used = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule standard_streams_should_not_be_accessed = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule generic_exceptions_should_not_be_thrown = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule legacy_date_and_time_classes_should_not_be_used = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Calendar")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.sql.Timestamp")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.text.SimpleDateFormat");

    /**
     * Throwable rather than RuntimeException, so a checked exception declared in an adapter
     * is caught too. Adapters translate infrastructure failures into the core's exception
     * types; they never invent their own.
     */
    // TODO(phase-5): drop allowEmptyShould once domain/exception is populated
    @ArchTest
    static final ArchRule custom_exceptions_should_live_in_the_domain = classes()
            .that()
            .areAssignableTo(Throwable.class)
            .and()
            .resideInAPackage(ROOT + "..")
            .should()
            .resideInAPackage(ROOT + ".domain.exception..")
            .allowEmptyShould(true);
}
