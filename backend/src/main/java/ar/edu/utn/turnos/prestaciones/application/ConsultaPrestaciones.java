package ar.edu.utn.turnos.prestaciones.application;

import java.util.Optional;

/**
 * Puerto de lectura del catálogo para otros módulos (profesionales, agenda, reserva pública).
 * No exige sesión: los flujos públicos también lo usan. Quien lo consume decide qué hacer con una
 * prestación inactiva (RN-11, RN-37).
 */
public interface ConsultaPrestaciones {

    /** @return la prestación, activa o no, o vacío si el id no existe */
    Optional<PrestacionResumen> buscarPorId(Long id);
}
