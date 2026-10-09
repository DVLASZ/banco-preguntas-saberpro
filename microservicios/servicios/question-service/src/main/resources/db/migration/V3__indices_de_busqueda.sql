-- Índices para las listas paginadas (más recientes primero): sin ellos, cada consulta ordena todas las preguntas del
-- autor, o de toda la tabla en la lista del administrador.
CREATE INDEX idx_question_author_creada ON question (author_id, creada_en DESC, id);
CREATE INDEX idx_question_creada ON question (creada_en DESC, id);
CREATE INDEX idx_question_estado_creada ON question (estado, creada_en DESC, id);
