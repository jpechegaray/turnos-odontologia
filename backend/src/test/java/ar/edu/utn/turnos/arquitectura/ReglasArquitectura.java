package ar.edu.utn.turnos.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;

/**
 * Reglas de dependencia del backend (KB 08, C-51). Están separadas de {@code ArquitecturaTest} para poder
 * probarlas contra clases de ejemplo que las violan a propósito.
 */
public final class ReglasArquitectura {
    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String REPOSITORIO = "org.springframework.data.repository.Repository";

    private ReglasArquitectura() {
    }

    /** La capa {@code domain} no importa Spring, JPA ni Hibernate. No acepta un conjunto vacío de clases. */
    public static ArchRule dominioSinFrameworks() {
        return noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .as("..domain.. no depende de Spring, JPA ni Hibernate");
    }

    /**
     * Un módulo (paquete de primer nivel bajo {@code raiz}) no usa entidades JPA ni repositorios de otro módulo.
     * El paquete {@code compartido} no cuenta como módulo ajeno.
     */
    public static ArchRule modulosSinPersistenciaAjena(String raiz, String compartido) {
        return classes()
                .that().resideInAPackage(raiz + "..")
                .should(noUsarPersistenciaAjena(raiz, compartido))
                .as("Un módulo no usa entidades ni repositorios de otro módulo");
    }

    private static ArchCondition<JavaClass> noUsarPersistenciaAjena(String raiz, String compartido) {
        return new ArchCondition<>("no usar entidades ni repositorios de otro módulo") {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                Optional<String> moduloOrigen = modulo(origen, raiz);
                if (moduloOrigen.isEmpty()) {
                    return;
                }
                for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                    JavaClass destino = dependencia.getTargetClass();
                    Optional<String> moduloDestino = modulo(destino, raiz);
                    boolean ajeno = moduloDestino.isPresent()
                            && !moduloDestino.get().equals(moduloOrigen.get())
                            && !moduloDestino.get().equals(compartido);
                    if (ajeno && esPersistencia(destino)) {
                        eventos.add(SimpleConditionEvent.violated(dependencia, String.format(
                                "%s (módulo %s) usa %s del módulo %s",
                                origen.getName(), moduloOrigen.get(), destino.getName(), moduloDestino.get())));
                    }
                }
            }
        };
    }

    private static boolean esPersistencia(JavaClass clase) {
        return clase.isAnnotatedWith(ENTITY) || clase.isAssignableTo(REPOSITORIO);
    }

    private static Optional<String> modulo(JavaClass clase, String raiz) {
        String paquete = clase.getPackageName();
        if (!paquete.startsWith(raiz + ".")) {
            return Optional.empty();
        }
        String resto = paquete.substring(raiz.length() + 1);
        int punto = resto.indexOf('.');
        return Optional.of(punto < 0 ? resto : resto.substring(0, punto));
    }
}
