package ejemplo.arquitectura.beta;

import ejemplo.arquitectura.alfa.domain.DominioPuro;
import ejemplo.arquitectura.comun.EntidadComun;

/** Permitido: usa el paquete compartido y una clase de otro módulo que no es entidad ni repositorio. */
public class UsaLoCompartido {
    EntidadComun comun;
    DominioPuro dominio;
}
