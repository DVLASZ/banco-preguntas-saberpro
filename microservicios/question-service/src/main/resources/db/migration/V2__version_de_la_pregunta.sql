-- Número de versión del contenido: sube cada vez que el autor reabre una pregunta rechazada.
ALTER TABLE question ADD COLUMN question_version INTEGER NOT NULL DEFAULT 1 CHECK (question_version >= 1);
