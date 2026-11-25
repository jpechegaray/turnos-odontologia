package ar.edu.utn.turnos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = TurnosApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** KB 08: la capa {@code domain} no importa Spring ni JPA. Pasa sin dominio gracias a allowEmptyShould. */
    @ArchTest
    static final ArchRule el_dominio_no_depende_de_frameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .allowEmptyShould(true)
            .as("..domain.. no depende de Spring, JPA ni Hibernate");

    /**
     * Escenario: "El paquete del token no depende de Spring, JPA ni Hibernate" (C-14, D9).
     * Sin allowEmptyShould: si el paquete se vaciara o moviera, la regla falla en lugar de pasar vacía.
     */
    @ArchTest
    static final ArchRule el_servicio_de_token_no_depende_de_frameworks = noClasses()
            .that().resideInAPackage("..shared.token..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .as("..shared.token.. no depende de Spring, JPA ni Hibernate");
}
