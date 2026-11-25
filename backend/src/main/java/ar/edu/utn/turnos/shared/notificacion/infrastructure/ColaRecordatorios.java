package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColaRecordatorios {
    private final JdbcTemplate jdbc;

    public ColaRecordatorios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** La clave de C-15 evita duplicados incluso con ciclos concurrentes. */
    public void encolar(Long turnoId, Long pacienteId, String destino, Instant inicio, Instant ahora) {
        jdbc.update("""
                insert into notificacion_enviada
                    (tipo, canal, destinatario, turno_id, paciente_id, inicio_referencia, estado, intentos, programada_para)
                values ('RECORDATORIO', 'EMAIL', ?, ?, ?, ?, 'PENDIENTE', 0, ?)
                on conflict (turno_id, tipo, inicio_referencia) do nothing
                """, destino, turnoId, pacienteId, OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
    }
}
