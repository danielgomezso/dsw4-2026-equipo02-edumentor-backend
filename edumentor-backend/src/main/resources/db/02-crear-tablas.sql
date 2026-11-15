CREATE TABLE dbo.usuario (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    carne NVARCHAR(20) NOT NULL UNIQUE,
    email NVARCHAR(254) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    apellido NVARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CHECK (rol IN ('ESTUDIANTE', 'TUTOR', 'COORDINADOR'))
);

CREATE TABLE dbo.materia (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    codigo_materia NVARCHAR(20) NOT NULL UNIQUE,
    nombre_materia NVARCHAR(150) NOT NULL,
    carrera NVARCHAR(150) NOT NULL,
    creditos INT NOT NULL,
    CHECK (creditos > 0)
);

CREATE TABLE dbo.disponibilidad_tutor (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    tutor_id BIGINT NOT NULL,
    dia_semana INT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    esta_activo BIT DEFAULT 1 NOT NULL,
    FOREIGN KEY (tutor_id) REFERENCES dbo.usuario(id),
    CHECK (dia_semana BETWEEN 1 AND 7),
    CHECK (hora_fin > hora_inicio),
    UNIQUE (tutor_id, dia_semana, hora_inicio, hora_fin)
);

CREATE TABLE dbo.sesion_tutoria (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    materia_id BIGINT NOT NULL,
    fecha_hora DATETIME2 NOT NULL,
    duracion_minutos INT NOT NULL,
    estado VARCHAR(20) DEFAULT 'REQUESTED' NOT NULL,
    modalidad VARCHAR(10) NOT NULL,
    resumen NVARCHAR(2000) NULL,
    FOREIGN KEY (estudiante_id) REFERENCES dbo.usuario(id),
    FOREIGN KEY (tutor_id) REFERENCES dbo.usuario(id),
    FOREIGN KEY (materia_id) REFERENCES dbo.materia(id),
    CHECK (estudiante_id <> tutor_id),
    CHECK (duracion_minutos > 0),
    CHECK (estado IN ('REQUESTED', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'IN_SESSION', 'COMPLETED', 'REVIEWED')),
    CHECK (modalidad IN ('VIRTUAL', 'PRESENCIAL'))
);

CREATE TABLE dbo.recurso_estudio (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sesion_id BIGINT NOT NULL,
    titulo NVARCHAR(200) NOT NULL,
    url_recurso NVARCHAR(2048) NOT NULL,
    tipo_archivo NVARCHAR(50) NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES dbo.sesion_tutoria(id)
);

CREATE TABLE dbo.evaluacion_tutoria (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sesion_id BIGINT NOT NULL UNIQUE,
    calificacion_estrellas INT NOT NULL,
    comentario NVARCHAR(2000) NULL,
    fecha_evaluacion DATETIME2 NOT NULL,
    FOREIGN KEY (sesion_id) REFERENCES dbo.sesion_tutoria(id),
    CHECK (calificacion_estrellas BETWEEN 1 AND 5)
);

CREATE INDEX ix_sesion_tutor_agenda ON dbo.sesion_tutoria(tutor_id, estado, fecha_hora);
CREATE INDEX ix_sesion_estudiante_agenda ON dbo.sesion_tutoria(estudiante_id, estado, fecha_hora);
CREATE INDEX ix_sesion_materia ON dbo.sesion_tutoria(materia_id);
CREATE INDEX ix_recurso_sesion ON dbo.recurso_estudio(sesion_id);
