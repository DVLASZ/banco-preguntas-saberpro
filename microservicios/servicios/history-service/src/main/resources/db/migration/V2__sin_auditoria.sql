-- La auditoría (y con ella los inicios de sesión) pasó al audit-service: aquí quedan solo los hechos de las preguntas.
-- Lo que se quita se copia antes a history_entry_auditoria, para no perder datos si el audit-service aún no los tiene.
CREATE TABLE history_entry_auditoria AS
SELECT * FROM history_entry WHERE type IN ('INICIO_DE_SESION', 'INICIO_DE_SESION_FALLIDO') OR question_id IS NULL;

DELETE FROM history_entry WHERE type IN ('INICIO_DE_SESION', 'INICIO_DE_SESION_FALLIDO') OR question_id IS NULL;

ALTER TABLE history_entry ALTER COLUMN question_id SET NOT NULL;

DROP INDEX IF EXISTS idx_history_type;
DROP INDEX IF EXISTS idx_history_actor;
DROP INDEX IF EXISTS idx_history_occurred;
