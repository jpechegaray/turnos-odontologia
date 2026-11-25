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
