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
