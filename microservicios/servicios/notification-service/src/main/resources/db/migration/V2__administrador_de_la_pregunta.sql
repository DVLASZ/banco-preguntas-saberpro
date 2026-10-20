-- Quién asignó los revisores de la pregunta (aprendido de review.reviewers-assigned), para avisarle de las
-- evaluaciones y del resultado de la revisión.
ALTER TABLE question_info ADD COLUMN admin_id VARCHAR(100);
