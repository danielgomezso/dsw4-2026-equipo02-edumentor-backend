$ErrorActionPreference = 'Stop'
Set-Location "$PSScriptRoot/edumentor-backend"
$env:SPRING_PROFILES_ACTIVE = 'sqlserver,secret'
./mvnw.cmd spring-boot:run
exit $LASTEXITCODE
