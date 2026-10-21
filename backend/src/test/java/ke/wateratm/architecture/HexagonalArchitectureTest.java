package ke.wateratm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fails the build if anyone breaks the hexagon:
 * domain ← application ← adapters, and only config sees everything.
 */
@AnalyzeClasses(packages = "ke.wateratm", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domainIsPureJava = classes().that()
		.resideInAPackage("..domain..")
		.should()
		.onlyDependOnClassesThat()
		.resideInAnyPackage("..domain..", "java..");

	@ArchTest
	static final ArchRule applicationOnlyUsesDomain = classes().that()
		.resideInAPackage("..application..")
		.should()
		.onlyDependOnClassesThat()
		.resideInAnyPackage("..application..", "..domain..", "java..");

	@ArchTest
	static final ArchRule nobodyDependsOnAdapters = noClasses().that()
		.resideInAnyPackage("..domain..", "..application..", "..config..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..adapter..");

	@ArchTest
	static final ArchRule adaptersOnlyTalkThroughPorts = noClasses().that()
		.resideInAPackage("..adapter..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..application.service..");

	@ArchTest
	static final ArchRule waysInAndWaysOutAreIndependent = noClasses().that()
		.resideInAPackage("..adapter.in..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..adapter.out..");

}
