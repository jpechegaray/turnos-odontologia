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
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_lista_espera_paciente' AND conrelid = 'lista_espera'::regclass
    ) THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_paciente
            FOREIGN KEY (paciente_id) REFERENCES pacientes (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_lista_espera_prestacion' AND conrelid = 'lista_espera'::regclass
    ) THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_prestacion
            FOREIGN KEY (prestacion_id) REFERENCES prestacion (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_lista_espera_profesional' AND conrelid = 'lista_espera'::regclass
    ) THEN
        ALTER TABLE lista_espera ADD CONSTRAINT fk_lista_espera_profesional
            FOREIGN KEY (profesional_id) REFERENCES profesional (id);
    END IF;
END
$$
^^^
-- RN-21: a lo sumo una entrada ACTIVA por paciente y prestación, también ante altas concurrentes.
CREATE UNIQUE INDEX IF NOT EXISTS ux_lista_espera_activa
    ON lista_espera (paciente_id, prestacion_id) WHERE estado = 'ACTIVA'
^^^
-- C-19: FK de turno hacia los recursos que referencia por id (KB 08).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_turno_paciente' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT fk_turno_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_turno_profesional' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT fk_turno_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_turno_sillon' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT fk_turno_sillon FOREIGN KEY (sillon_id) REFERENCES sillon (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_turno_prestacion' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT fk_turno_prestacion FOREIGN KEY (prestacion_id) REFERENCES prestacion (id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_turno_usuario' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT fk_turno_usuario FOREIGN KEY (creado_por_usuario_id) REFERENCES usuario (id);
    END IF;
END
$$
^^^
-- C-19 (RN-01, RN-02, RN-10, RN-15): garantía final contra sobreturnos. Un profesional y un sillón no
-- pueden tener dos turnos no cancelados que compartan un instante; '[)' hace que los bordes no choquen.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'turno_sin_solape_profesional' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT turno_sin_solape_profesional
            EXCLUDE USING gist (profesional_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
            WHERE (estado <> 'CANCELADO');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'turno_sin_solape_sillon' AND conrelid = 'turno'::regclass
    ) THEN
        ALTER TABLE turno ADD CONSTRAINT turno_sin_solape_sillon
            EXCLUDE USING gist (sillon_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
            WHERE (estado <> 'CANCELADO');
    END IF;
END
$$
^^^
