# Check if realm exists
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8088/realms/master/protocol/openid-connect/token" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=admin&password=admin&grant_type=password&client_id=admin-cli"

$accessToken = $tokenResponse.access_token

# Get realm info
$headers = @{
    "Authorization" = "Bearer $accessToken"
}

try {
    $realm = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm" -Headers $headers
    Write-Host "=== Realm Info ===" -ForegroundColor Cyan
    Write-Host "Realm Name: $($realm.realm)"
    Write-Host "Enabled: $($realm.enabled)"
    Write-Host "Display Name: $($realm.displayName)"
    Write-Host ""
    
    # Get clients
    $clients = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/clients" -Headers $headers
    Write-Host "=== Clients ===" -ForegroundColor Cyan
    $clients | ForEach-Object { Write-Host "  - $($_.clientId)" }
    Write-Host ""
    
    # Get roles
    $roles = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/roles" -Headers $headers
    Write-Host "=== Realm Roles ===" -ForegroundColor Cyan
    $roles | ForEach-Object { Write-Host "  - $($_.name)" }
    Write-Host ""
    
    # Get groups
    $groups = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/groups" -Headers $headers
    Write-Host "=== Groups ===" -ForegroundColor Cyan
    $groups | ForEach-Object { Write-Host "  - $($_.name) ($($_.path))" }
    
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}
