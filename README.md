# EduMentor Hub - Backend

Java 21, Spring Boot 3.5 y Maven.

## Base de datos

Servidor: `163.178.173.130:1433`. Base: `EduMentorHub_Equipo02`.

Los scripts estan en `edumentor-backend/src/main/resources/db/`:

1. `01-crear-base.sql`: crea la base.
2. `02-crear-tablas.sql`: crea las seis tablas. Ejecutarlo con `EduMentorHub_Equipo02`

## Ejecutar

Desde `edumentor-backend/`, con JDK 21 configurado en `JAVA_HOME`:

```powershell
./mvnw.cmd spring-boot:run
```

Por defecto utiliza H2 temporal. Para usar el servidor, configurar antes:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'sqlserver'
$env:DB_USERNAME = 'Lenguajes'
$credencial = Get-Credential -UserName 'Lenguajes' -Message 'Acceso a SQL Server'
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
$env:DB_TRUST_SERVER_CERTIFICATE = 'true'
```

La ultima variable permite el certificado del servidor universitario. No guardar contrasenas en Git.

Pruebas: `./mvnw.cmd test`.
