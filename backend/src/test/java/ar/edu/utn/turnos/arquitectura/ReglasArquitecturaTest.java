package ar.edu.utn.turnos.arquitectura;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las reglas de arquitectura detectan violaciones en clases de ejemplo hechas a propósito (C-51). */
class ReglasArquitecturaTest {
    private static final String RAIZ = "ejemplo.arquitectura";
    private static final JavaClasses EJEMPLO = new ClassFileImporter().importPackages(RAIZ);

    @Test
    @DisplayName("Usar el repositorio de otro módulo rompe la build")
    void usarElRepositorioDeOtroModuloRompeLaBuild() {
        EvaluationResult resultado = ReglasArquitectura.modulosSinPersistenciaAjena(RAIZ, "comun").evaluate(EJEMPLO);

        assertThat(resultado.hasViolation()).isTrue();
        String informe = String.join("\n", resultado.getFailureReport().getDetails());
        assertThat(informe)
                .contains("UsaEntidadAjena", "UsaRepositorioAjeno", "módulo alfa")
                .doesNotContain("UsaSuPropiaEntidad", "UsaLoCompartido");
    }

    @Test
    @DisplayName("Una dependencia del dominio hacia el framework rompe la build")
    void unaDependenciaDelDominioHaciaElFrameworkRompeLaBuild() {
        EvaluationResult resultado = ReglasArquitectura.dominioSinFrameworks().evaluate(EJEMPLO);

        assertThat(resultado.hasViolation()).isTrue();
        String informe = String.join("\n", resultado.getFailureReport().getDetails());
        assertThat(informe).contains("DominioConSpring").doesNotContain("DominioPuro");
    }
}
