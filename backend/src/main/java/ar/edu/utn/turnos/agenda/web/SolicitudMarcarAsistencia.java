package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cuerpo de {@code POST /api/turnos/{id}/asistencia} (C-29): el estado destino es obligatorio y solo puede ser
 * ATENDIDO o AUSENTE. La restricción va sobre el componente para que el 400 reporte {@code campo = estado}.
 */
record SolicitudMarcarAsistencia(@NotNull @MarcaDeAsistencia EstadoTurno estado) {

    /** Solo ATENDIDO o AUSENTE; el nulo lo reporta {@link NotNull}, así no hay dos errores para el mismo campo. */
    @Documented
    @Constraint(validatedBy = MarcaDeAsistencia.Validador.class)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MarcaDeAsistencia {

        String message() default "debe ser ATENDIDO o AUSENTE";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        class Validador implements ConstraintValidator<MarcaDeAsistencia, EstadoTurno> {

            @Override
            public boolean isValid(EstadoTurno estado, ConstraintValidatorContext contexto) {
                return estado == null || estado == EstadoTurno.ATENDIDO || estado == EstadoTurno.AUSENTE;
            }
        }
    }
}
