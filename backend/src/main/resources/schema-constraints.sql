CREATE EXTENSION IF NOT EXISTS btree_gist
^^^
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_bloqueo_profesional' AND conrelid = 'bloqueo'::regclass
    ) THEN
        ALTER TABLE bloqueo ADD CONSTRAINT fk_bloqueo_profesional
            FOREIGN KEY (profesional_id) REFERENCES profesional (id);
    END IF;
END
$$
^^^
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_profesional_prestacion_profesional'
          AND conrelid = 'profesional_prestacion'::regclass
    ) THEN
        ALTER TABLE profesional_prestacion ADD CONSTRAINT fk_profesional_prestacion_profesional
            FOREIGN KEY (profesional_id) REFERENCES profesional (id);
    END IF;
END
$$
^^^
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_profesional_prestacion_prestacion'
          AND conrelid = 'profesional_prestacion'::regclass
    ) THEN
        ALTER TABLE profesional_prestacion ADD CONSTRAINT fk_profesional_prestacion_prestacion
            FOREIGN KEY (prestacion_id) REFERENCES prestacion (id);
    END IF;
END
$$
^^^
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_horario_atencion_profesional' AND conrelid = 'horario_atencion'::regclass
    ) THEN
        ALTER TABLE horario_atencion ADD CONSTRAINT fk_horario_atencion_profesional
            FOREIGN KEY (profesional_id) REFERENCES profesional (id);
    END IF;
END
$$
^^^
-- C-07: el DNI es único solo entre fichas ACTIVAS y solo cuando se informa (decisión del líder, 2026-11-23).
-- Hibernate no genera índices parciales; se descartan las uniques completas de versiones previas.
ALTER TABLE IF EXISTS pacientes DROP CONSTRAINT IF EXISTS uk_paciente_dni
^^^
ALTER TABLE IF EXISTS pacientes DROP CONSTRAINT IF EXISTS pacientes_dni_key
^^^
CREATE UNIQUE INDEX IF NOT EXISTS uk_paciente_dni_activo ON pacientes (dni) WHERE activo = true AND dni IS NOT NULL
^^^
-- C-15: idempotencia de notificaciones. Hibernate crea estas restricciones en una base nueva
-- (create-drop de tests); en dev con ddl-auto=update no las agrega a una tabla ya existente.
-- Los avisos de oferta (turno_id NULL) y los de turno (oferta_hueco_id NULL) no colisionan porque
-- PostgreSQL considera distintos los NULL. Sin FKs: turno y oferta_hueco aún no existen (C-19/C-36).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_notificacion_turno_tipo_inicio'
          AND conrelid = 'notificacion_enviada'::regclass
    ) THEN
        ALTER TABLE notificacion_enviada ADD CONSTRAINT uk_notificacion_turno_tipo_inicio
            UNIQUE (turno_id, tipo, inicio_referencia);
    END IF;
END
$$
^^^
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_notificacion_oferta_tipo'
          AND conrelid = 'notificacion_enviada'::regclass
    ) THEN
        ALTER TABLE notificacion_enviada ADD CONSTRAINT uk_notificacion_oferta_tipo
            UNIQUE (oferta_hueco_id, tipo);
    END IF;
END
$$
^^^
CREATE INDEX IF NOT EXISTS ix_notificacion_estado_programada ON notificacion_enviada (estado, programada_para)
^^^
