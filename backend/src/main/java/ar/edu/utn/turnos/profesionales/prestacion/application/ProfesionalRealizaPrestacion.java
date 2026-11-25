package ar.edu.utn.turnos.profesionales.prestacion.application;

import java.util.Collection;
import java.util.Set;

/**
 * Puerto de lectura de las asociaciones profesional-prestación (RN-11) para agenda y reserva pública.
 * No filtra por estado: quien lo consume decide qué hacer con profesionales o prestaciones inactivos.
 */
public interface ProfesionalRealizaPrestacion {

    boolean realiza(Long profesionalId, Long prestacionId);

    /** @return los ids de los profesionales asociados a la prestación (vacío si no hay o no existe) */
    Set<Long> profesionalesQueRealizan(Long prestacionId);

    /** @return los ids de las prestaciones que realiza al menos uno de los profesionales dados */
    Set<Long> prestacionesConProfesional(Collection<Long> profesionalIds);
}
