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
