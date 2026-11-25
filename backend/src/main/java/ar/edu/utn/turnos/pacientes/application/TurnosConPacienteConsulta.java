package ar.edu.utn.turnos.pacientes.application;

/**
 * Pregunta que pacientes necesita hacerle a la agenda para autorizar la ficha del odontólogo (C-28, RN-33).
 * La define pacientes y la implementa agenda, así pacientes no depende de agenda (KB 08).
 */
public interface TurnosConPacienteConsulta {

    /** El paciente tiene algún turno, en cualquier estado, con ese profesional. */
    boolean existeTurno(Long pacienteId, Long profesionalId);
}
