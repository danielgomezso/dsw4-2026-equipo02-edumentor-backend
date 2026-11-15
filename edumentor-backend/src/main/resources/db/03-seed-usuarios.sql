-- Usuarios iniciales de prueba (uno por rol) para desarrollo local.
-- Contrasenas (BCrypt cost 10):
--   estudiante@edumentor.test  -> Estudiante123!
--   tutor@edumentor.test       -> Tutor123!
--   coordinador@edumentor.test -> Coordinador123!
INSERT INTO dbo.usuario (carne, email, password_hash, nombre, apellido, rol)
VALUES
    ('E00001', 'estudiante@edumentor.test', '$2a$10$ABNw0/w.NTcrGUHVcUFQmOM7BYtLtUg6qoD3l2UeyhPlor51HwIjK', 'Ana', 'Estudiante', 'ESTUDIANTE'),
    ('T00001', 'tutor@edumentor.test', '$2a$10$1lYphkFKnp5EJtMbzfEYT.HBOJTTAMYCXr9G3OrBhY2Cccje1sDcO', 'Tomas', 'Tutor', 'TUTOR'),
    ('C00001', 'coordinador@edumentor.test', '$2a$10$lJn8xUKTpwZANPXyy9DQ7OgQzANjqT45C0qVGHPuvSL7e.SHUExDC', 'Carla', 'Coordinadora', 'COORDINADOR');
