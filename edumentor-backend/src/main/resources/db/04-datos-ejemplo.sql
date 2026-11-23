SET XACT_ABORT ON;
BEGIN TRANSACTION;

INSERT INTO dbo.materia (codigo_materia, nombre_materia, carrera, creditos)
SELECT codigo, nombre, carrera, creditos
FROM (VALUES
    ('DEMO-PROG', 'Programacion', 'Informatica Empresarial', 4),
    ('DEMO-BD', 'Bases de Datos', 'Informatica Empresarial', 4),
    ('DEMO-MAT', 'Matematica', 'Informatica Empresarial', 3)
) AS datos(codigo, nombre, carrera, creditos)
WHERE NOT EXISTS (SELECT 1 FROM dbo.materia WHERE codigo_materia = datos.codigo);

DECLARE @tutor BIGINT = (SELECT id FROM dbo.usuario WHERE email = 'tutor@edumentor.test');
DECLARE @estudiante BIGINT = (SELECT id FROM dbo.usuario WHERE email = 'estudiante@edumentor.test');
DECLARE @materia BIGINT = (SELECT id FROM dbo.materia WHERE codigo_materia = 'DEMO-PROG');

INSERT INTO dbo.disponibilidad_tutor (tutor_id, dia_semana, hora_inicio, hora_fin, esta_activo)
SELECT @tutor, dia, '08:00', '12:00', 1
FROM (VALUES (1), (2), (3), (4), (5)) AS dias(dia)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.disponibilidad_tutor
    WHERE tutor_id = @tutor AND dia_semana = dias.dia
    AND hora_inicio < '12:00' AND hora_fin > '08:00'
);

IF NOT EXISTS (
    SELECT 1 FROM dbo.sesion_tutoria
    WHERE estudiante_id = @estudiante AND tutor_id = @tutor
    AND materia_id = @materia AND fecha_hora = '2026-11-20T09:00:00'
)
BEGIN
    INSERT INTO dbo.sesion_tutoria
        (estudiante_id, tutor_id, materia_id, fecha_hora, duracion_minutos, estado, modalidad, resumen)
    VALUES (@estudiante, @tutor, @materia, '2026-11-20T09:00:00', 60, 'REVIEWED', 'VIRTUAL', 'Repaso de variables y ciclos.');

    DECLARE @sesion BIGINT = SCOPE_IDENTITY();

    INSERT INTO dbo.recurso_estudio (sesion_id, titulo, url_recurso, tipo_archivo)
    VALUES (@sesion, 'Documentacion de Java', 'https://docs.oracle.com/en/java/', 'ENLACE');

    INSERT INTO dbo.evaluacion_tutoria (sesion_id, calificacion_estrellas, comentario, fecha_evaluacion)
    VALUES (@sesion, 5, 'Explicacion clara y ejercicios utiles.', '2026-11-20T10:05:00');
END;

COMMIT TRANSACTION;
