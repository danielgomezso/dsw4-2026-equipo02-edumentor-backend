# EduMentor Hub - Backend

Requiere Java 21. API: `http://localhost:8080`.

## Servidor universitario

- Servidor: `163.178.173.130:1433`
- Base: `EduMentorHub_Equipo02`
- Usuario SQL: `Lenguajes`

Crear `edumentor-backend/config/application-secret.properties` con:

```properties
DB_USERNAME=Lenguajes
DB_PASSWORD=CONTRASENA_DEL_SERVIDOR
DB_TRUST_SERVER_CERTIFICATE=true
JWT_SECRET=CLAVE_BASE64_DE_AL_MENOS_32_BYTES
```

Solicitar la contraseña al equipo. Este archivo es local y Git lo ignora. Para generar la clave JWT en PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Desde la raíz del repositorio, con `JAVA_HOME` apuntando a Java 21:

```powershell
./iniciar-servidor.ps1
```

## Cuentas de prueba

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Estudiante | estudiante@edumentor.test | Estudiante123! |
| Tutor | tutor@edumentor.test | Tutor123! |
| Coordinador | coordinador@edumentor.test | Coordinador123! |

Scripts en `edumentor-backend/src/main/resources/db/`: `01` crea la base, `02` las tablas, `03` los usuarios y `04` los datos de ejemplo. En el servidor ya están cargados; no repetir `01`, `02` ni `03`.

Para H2 local o pruebas, desde `edumentor-backend/`: `./mvnw.cmd spring-boot:run` o `./mvnw.cmd test`. El frontend se inicia con `npm start` en su repositorio y usa `http://localhost:4200`.
