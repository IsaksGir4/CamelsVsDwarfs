# Asigna 'viewer' al rol default-roles-camelsvsdwarfs via la Admin REST API de Keycloak.
# Necesario porque Keycloak NO aplica correctamente el defaultRole cuando el realm
# se importa con usuarios explícitos (ver README, sección Known Limitations).
# Idempotente: si 'viewer' ya está asignado, no hace nada.

$ErrorActionPreference = "Stop"

$keycloakUrl = "http://localhost:8180"
$realm = "camelsvsdwarfs"
$adminUser = if ($env:KEYCLOAK_ADMIN_USER) { $env:KEYCLOAK_ADMIN_USER } else { "admin" }
$adminPassword = if ($env:KEYCLOAK_ADMIN_PASSWORD) { $env:KEYCLOAK_ADMIN_PASSWORD } else { "admin" }

Write-Host "Obteniendo token de admin..."
$tokenResponse = Invoke-RestMethod -Uri "$keycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body @{
    client_id  = "admin-cli"
    grant_type = "password"
    username   = $adminUser
    password   = $adminPassword
}
$token = $tokenResponse.access_token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "Buscando el rol 'viewer'..."
$viewerRole = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/roles/viewer" -Headers $headers

Write-Host "Revisando composites actuales de default-roles-$realm..."
$currentComposites = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/roles/default-roles-$realm/composites" -Headers $headers

if ($currentComposites | Where-Object { $_.name -eq "viewer" }) {
    Write-Host "'viewer' ya está asignado como rol por defecto. Nada que hacer."
} else {
    Write-Host "Asignando 'viewer' como rol por defecto..."
    $body = @(@{ id = $viewerRole.id; name = $viewerRole.name }) | ConvertTo-Json
    Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/roles/default-roles-$realm/composites" `
        -Method Post -Headers $headers -ContentType "application/json" -Body "[$($viewerRole | ConvertTo-Json -Compress)]"
    Write-Host "Listo 'viewer' asignado correctamente."
}