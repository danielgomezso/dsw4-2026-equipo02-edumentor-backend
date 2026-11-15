# EduMentor Hub - Backend

Java 21, Spring Boot 3.5 y Maven.

## Base de datos

Servidor: `163.178.173.130:1433`. Base: `EduMentorHub_Equipo02`.

Los scripts estan en `edumentor-backend/src/main/resources/db/`:

1. `01-crear-base.sql`: crea la base.
2. `02-crear-tablas.sql`: crea las seis tablas. Ejecutarlo con `EduMentorHub_Equipo02`.
3. `03-seed-usuarios.sql`: inserta un usuario de prueba por rol (ejecutado automaticamente en perfil `local`).

## Ejecutar

Desde `edumentor-backend/`, con JDK 21 configurado en `JAVA_HOME`:

```powershell
./mvnw.cmd spring-boot:run
```

Por defecto utiliza el perfil `local` con H2 en memoria y carga los usuarios de `03-seed-usuarios.sql`:

| Rol | Correo | Contrasena | Carne |
| :--- | :--- | :--- | :--- |
| `ESTUDIANTE` | `estudiante@edumentor.test` | `Estudiante123!` | `E00001` |
| `TUTOR` | `tutor@edumentor.test` | `Tutor123!` | `T00001` |
| `COORDINADOR` | `coordinador@edumentor.test` | `Coordinador123!` | `C00001` |

Para usar el servidor SQL Server, configurar antes las variables de entorno (incluido un secreto JWT en Base64 de al menos 32 bytes):

```powershell
$env:SPRING_PROFILES_ACTIVE = 'sqlserver'
$env:DB_USERNAME = 'Lenguajes'
$credencial = Get-Credential -UserName 'Lenguajes' -Message 'Acceso a SQL Server'
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
$env:DB_TRUST_SERVER_CERTIFICATE = 'true'
$env:JWT_SECRET = '<clave-base64-minimo-32-bytes>'
```

No guardar contrasenas ni secretos en Git.

## Pruebas

```powershell
./mvnw.cmd test
```

## Endpoints de autenticacion y usuarios

- `POST /api/v1/auth/register` (publico, permite roles `ESTUDIANTE` y `TUTOR`)
- `POST /api/v1/auth/login` (publico, devuelve token JWT y datos del usuario)
- `GET /api/v1/usuarios/me` (autenticado)
- `GET /api/v1/usuarios` (`COORDINADOR`)
- `GET /api/v1/usuarios/{id}` (`COORDINADOR` o propio usuario en `{id}`)
