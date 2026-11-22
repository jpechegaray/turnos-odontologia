package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.domain.ClaveUsuario;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Contraseña nueva de 8 caracteres a 72 bytes UTF-8 (ClaveUsuario). El mensaje nunca repite el valor. */
@Documented
@Constraint(validatedBy = ClaveValida.Validador.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClaveValida {

    String message() default "debe tener al menos 8 caracteres y no más de 72 bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<ClaveValida, String> {

        @Override
        public boolean isValid(String clave, ConstraintValidatorContext contexto) {
            return ClaveUsuario.esValida(clave);
        }
    }
}
