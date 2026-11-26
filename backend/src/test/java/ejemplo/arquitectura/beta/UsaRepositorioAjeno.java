package ejemplo.arquitectura.beta;

import ejemplo.arquitectura.alfa.RepositorioAlfa;

/** Violación: usa un repositorio de otro módulo. */
public class UsaRepositorioAjeno {
    RepositorioAlfa repositorio;
}
