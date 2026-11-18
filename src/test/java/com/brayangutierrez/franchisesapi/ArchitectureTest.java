package com.brayangutierrez.franchisesapi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hace cumplir la organización del proyecto. Si una clase rompe una regla, el build falla.
 * <ul>
 * <li>{@code app}: arranque y composición; nadie depende de él.</li>
 * <li>{@code core}: transversal y técnico, sin negocio.</li>
 * <li>{@code integrations}: tecnología externa, sin negocio.</li>
 * <li>{@code modules}: negocio; su dominio solo conoce Java, Reactor y el dominio de {@code core}.</li>
 * </ul>
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	static final String BASE = "com.brayangutierrez.franchisesapi";

	private static final String APP = BASE + ".app..";
	private static final String CORE = BASE + ".core..";
	private static final String CORE_DOMAIN = BASE + ".core.domain..";
	private static final String INTEGRATIONS = BASE + ".integrations..";
	private static final String MODULES = BASE + ".modules..";
	private static final String MODULE_DOMAIN = BASE + ".modules.*.domain..";
	private static final String MODULE_MODEL = BASE + ".modules.*.domain.model..";
	private static final String MODULE_USECASE = BASE + ".modules.*.domain.usecase..";
	private static final String MODULE_WEB = BASE + ".modules.*.infrastructure.web..";
	private static final String MODULE_PERSISTENCE = BASE + ".modules.*.infrastructure.persistence..";

	private static final String[] PLAIN_JAVA_AND_REACTOR = { "java..", "reactor..", "org.reactivestreams.." };

	@ArchTest
	static final ArchRule moduleDomainOnlyUsesJavaReactorAndCoreDomain = classes()
			.that().resideInAPackage(MODULE_DOMAIN)
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(concat(PLAIN_JAVA_AND_REACTOR, MODULE_DOMAIN, CORE_DOMAIN))
			.as("El dominio de un módulo solo puede depender de Java, Reactor y el dominio de core")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule coreDomainIsPlainJava = classes()
			.that().resideInAPackage(CORE_DOMAIN)
			.should().onlyDependOnClassesThat()
			.resideInAnyPackage(concat(PLAIN_JAVA_AND_REACTOR, CORE_DOMAIN))
			.as("El dominio de core solo puede depender de Java y Reactor");

	@ArchTest
	static final ArchRule modelDoesNotDependOnUseCases = noClasses()
			.that().resideInAPackage(MODULE_MODEL)
			.should().dependOnClassesThat().resideInAPackage(MODULE_USECASE)
			.as("El modelo no puede depender de los casos de uso")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule coreDoesNotDependOnModules = noClasses()
			.that().resideInAPackage(CORE)
			.should().dependOnClassesThat().resideInAPackage(MODULES)
			.as("Core es transversal: no puede conocer ningún módulo de negocio");

	@ArchTest
	static final ArchRule integrationsDoNotDependOnModules = noClasses()
			.that().resideInAPackage(INTEGRATIONS)
			.should().dependOnClassesThat().resideInAPackage(MODULES)
			.as("Las integraciones no pueden conocer ningún módulo de negocio");

	@ArchTest
	static final ArchRule webDoesNotDependOnPersistence = noClasses()
			.that().resideInAPackage(MODULE_WEB)
			.should().dependOnClassesThat().resideInAPackage(MODULE_PERSISTENCE)
			.as("La capa web de un módulo no puede saltarse los casos de uso y usar la persistencia")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule persistenceDoesNotDependOnWeb = noClasses()
			.that().resideInAPackage(MODULE_PERSISTENCE)
			.should().dependOnClassesThat().resideInAPackage(MODULE_WEB)
			.as("La persistencia de un módulo no puede depender de su capa web")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule nobodyDependsOnApp = noClasses()
			.that().resideOutsideOfPackage(APP)
			.should().dependOnClassesThat().resideInAPackage(APP)
			.as("app es la raíz de composición: ninguna otra capa puede depender de él");

	@ArchTest
	static final ArchRule modulesAreIndependent = slices()
			.matching(BASE + ".modules.(*)..")
			.should().notDependOnEachOther()
			.as("Los módulos de negocio no pueden depender entre sí");

	private static String[] concat(String[] base, String... extra) {
		String[] all = new String[base.length + extra.length];
		System.arraycopy(base, 0, all, 0, base.length);
		System.arraycopy(extra, 0, all, base.length, extra.length);
		return all;
	}

}
