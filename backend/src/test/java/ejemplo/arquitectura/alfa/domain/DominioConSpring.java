package ejemplo.arquitectura.alfa.domain;

import org.springframework.util.Assert;

/** Violación: el dominio usa Spring. */
public class DominioConSpring {
    void validar(Object valor) {
        Assert.notNull(valor, "valor");
    }
}
