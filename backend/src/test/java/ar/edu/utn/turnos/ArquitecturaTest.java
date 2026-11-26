package ar.edu.utn.turnos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import ar.edu.utn.turnos.arquitectura.ReglasArquitectura;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = TurnosApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Escenario: "El código actual cumple las reglas de dependencia" (C-51). Sin conjunto vacío aceptado. */
    @ArchTest
    static final ArchRule el_dominio_no_depende_de_frameworks = ReglasArquitectura.dominioSinFrameworks();

    /** KB 08: un módulo no usa entidades JPA ni repositorios de otro; {@code shared} es común (C-51). */
    @ArchTest
    static final ArchRule los_modulos_no_usan_persistencia_ajena =
            ReglasArquitectura.modulosSinPersistenciaAjena("ar.edu.utn.turnos", "shared");

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
