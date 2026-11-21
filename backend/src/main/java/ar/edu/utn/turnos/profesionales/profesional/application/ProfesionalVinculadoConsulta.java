package ar.edu.utn.turnos.profesionales.profesional.application;

import java.util.Optional;

/** Resuelve el profesional vinculado a la identidad autenticada por el servidor. */
public interface ProfesionalVinculadoConsulta {

    Optional<Long> buscarIdPorIdentidad(String identidad);
}
