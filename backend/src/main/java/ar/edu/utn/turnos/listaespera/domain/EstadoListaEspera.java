package ar.edu.utn.turnos.listaespera.domain;

/** Ciclo de una entrada (RN-21, RN-24): nace ACTIVA; ASIGNADA, CANCELADA y VENCIDA son finales. */
public enum EstadoListaEspera {
    ACTIVA,
    ASIGNADA,
    CANCELADA,
    VENCIDA
}
