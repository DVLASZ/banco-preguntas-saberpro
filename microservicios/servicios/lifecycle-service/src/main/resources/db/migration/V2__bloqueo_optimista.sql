-- Bloqueo optimista: dos cambios simultaneos sobre la misma pregunta ya no se pisan.
ALTER TABLE question_lifecycle ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
