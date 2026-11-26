package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColaRecordatorios {
    private static final String MOTIVO_CAMBIO_TURNO = "Recordatorio descartado por cambio de turno";

    private final JdbcTemplate jdbc;

    public ColaRecordatorios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Si el turno vuelve a su inicio anterior, reutiliza un registro descartado por ese cambio. */
    public void encolar(Long turnoId, Long pacienteId, String destino, Instant inicio, Instant ahora) {
        jdbc.update("""
                insert into notificacion_enviada
                    (tipo, canal, destinatario, turno_id, paciente_id, inicio_referencia, estado, intentos, programada_para)
                values ('RECORDATORIO', 'EMAIL', ?, ?, ?, ?, 'PENDIENTE', 0, ?)
                on conflict (turno_id, tipo, inicio_referencia) do update
                set programada_para = excluded.programada_para,
                    estado = case when notificacion_enviada.intentos = 0 then 'PENDIENTE' else 'FALLIDA' end
                where notificacion_enviada.estado = 'FALLIDA'
                  and notificacion_enviada.programada_para is null
                  and notificacion_enviada.ultimo_error = ?
                  and exists (select 1 from turno t where t.id = excluded.turno_id
                              and t.estado in ('PROGRAMADO', 'CONFIRMADO') and t.inicio = excluded.inicio_referencia)
                """, destino, turnoId, pacienteId, OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), MOTIVO_CAMBIO_TURNO);
    }

    /** Consulta el turno confirmado para que un evento anterior no descarte el inicio vigente. */
    public void descartarObsoletos(Long turnoId) {
        jdbc.update("""
                update notificacion_enviada n
                set estado = 'FALLIDA', programada_para = null, ultimo_error = ?
                from turno t
                where t.id = ? and n.turno_id = t.id
                  and n.tipo = 'RECORDATORIO' and n.estado in ('PENDIENTE', 'FALLIDA')
                  and n.programada_para is not null
                  and (t.estado = 'CANCELADO' or t.inicio <> n.inicio_referencia)
                """, MOTIVO_CAMBIO_TURNO, turnoId);
    }
}
