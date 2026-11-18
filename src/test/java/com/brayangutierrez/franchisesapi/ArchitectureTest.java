package com.brayangutierrez.franchisesapi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hace cumplir la regla de dependencia de la arquitectura limpia: las dependencias
 * solo apuntan hacia el dominio. Si una clase rompe la regla, el build falla.
 */
@AnalyzeClasses(packages = "com.brayangutierrez.franchisesapi", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String DOMAIN = "..domain..";
	private static final String MODEL = "..domain.model..";
	private static final String USECASE = "..domain.usecase..";
	private static final String INFRASTRUCTURE = "..infrastructure..";
	private static final String DRIVEN_ADAPTERS = "..infrastructure.drivenadapters..";
	private static final String ENTRY_POINTS = "..infrastructure.entrypoints..";

	@ArchTest
	static final ArchRule domainOnlyUsesJavaAndReactor = classes()
			.that().resideInAPackage(DOMAIN)
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(DOMAIN, "java..", "reactor..", "org.reactivestreams..")
			.as("El dominio solo puede depender de Java, Reactor y del propio dominio")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule modelDoesNotDependOnUseCases = noClasses()
			.that().resideInAPackage(MODEL)
			.should().dependOnClassesThat().resideInAPackage(USECASE)
			.as("El modelo no puede depender de los casos de uso")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule domainDoesNotDependOnInfrastructure = noClasses()
			.that().resideInAPackage(DOMAIN)
			.should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
			.as("El dominio no puede depender de la infraestructura")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule drivenAdaptersDoNotDependOnEntryPoints = noClasses()
			.that().resideInAPackage(DRIVEN_ADAPTERS)
			.should().dependOnClassesThat().resideInAPackage(ENTRY_POINTS)
			.as("Los driven adapters no pueden depender de los entry points")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule entryPointsDoNotDependOnDrivenAdapters = noClasses()
			.that().resideInAPackage(ENTRY_POINTS)
			.should().dependOnClassesThat().resideInAPackage(DRIVEN_ADAPTERS)
			.as("Los entry points no pueden depender de los driven adapters")
			.allowEmptyShould(true);

}
