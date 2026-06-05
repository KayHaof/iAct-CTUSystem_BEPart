# Wait for Keycloak to be ready
function Wait-ForKeycloak {
    $maxAttempts = 15
    $attempt = 0
    Write-Host "Checking if Keycloak is ready..." -ForegroundColor Yellow
    
    while ($attempt -lt $maxAttempts) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8088/realms/master" -Method GET -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Host "Keycloak is ready!" -ForegroundColor Green
                return $true
            }
        } catch {
            # Not ready yet
        }
        $attempt++
        Start-Sleep -Seconds 2
    }
    
    Write-Host "Keycloak is not responding" -ForegroundColor Red
    return $false
}

# Get admin token
function Get-AdminToken {
    try {
        $body = @{
            username = "admin"
            password = "123456"
            grant_type = "password"
            client_id = "admin-cli"
        }
        $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8088/realms/master/protocol/openid-connect/token" `
            -Method POST `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $body `
            -ErrorAction Stop
        return $tokenResponse.access_token
    } catch {
        Write-Host "Failed to authenticate: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Make sure Keycloak is running and admin credentials are correct." -ForegroundColor Yellow
        return $null
    }
}

# Check if Keycloak is ready
if (-not (Wait-ForKeycloak)) {
    Write-Host "Cannot connect to Keycloak. Is it running?" -ForegroundColor Red
    exit 1
}

# Get token
Write-Host ""
Write-Host "Authenticating as admin..." -ForegroundColor Cyan
$accessToken = Get-AdminToken
if (-not $accessToken) {
    Write-Host "Authentication failed" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
}

Write-Host "Connected successfully!" -ForegroundColor Green
Write-Host ""

# Get master realm info
Write-Host "=== MASTER REALM ===" -ForegroundColor Cyan
try {
    $masterRealm = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/master" -Headers $headers
    Write-Host "Realm Name: $($masterRealm.realm)"
    Write-Host "Enabled: $($masterRealm.enabled)"
    Write-Host "Users Count: $($masterRealm.users.Count)"
} catch {
    Write-Host "Error getting master realm: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Get myRealm info
Write-Host "=== MYREALM ===" -ForegroundColor Cyan
try {
    $realm = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm" -Headers $headers
    Write-Host "Realm Name: $($realm.realm)"
    Write-Host "Display Name: $($realm.displayName)"
    Write-Host "Enabled: $($realm.enabled)"
    Write-Host ""
    
    # Get users
    Write-Host "--- Users ---" -ForegroundColor White
    $users = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/users" -Headers $headers
    if ($users.Count -gt 0) {
        foreach ($user in $users) {
            Write-Host "  [$($user.username)] - $($user.email) - Roles: $($user.realmRoles -join ', ')"
        }
    } else {
        Write-Host "  No users found"
    }
    Write-Host ""
    
    # Get clients
    Write-Host "--- Clients ---" -ForegroundColor White
    $clients = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/clients" -Headers $headers
    $clients | ForEach-Object { Write-Host "  - $($_.clientId)" }
    Write-Host ""
    
    # Get roles
    Write-Host "--- Realm Roles ---" -ForegroundColor White
    $roles = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/roles" -Headers $headers
    $roles | ForEach-Object { Write-Host "  - $($_.name)" }
    Write-Host ""
    
    # Get groups
    Write-Host "--- Groups ---" -ForegroundColor White
    $groups = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/groups" -Headers $headers
    $groups | ForEach-Object { Write-Host "  - $($_.name) ($($_.path))" }
    
} catch {
    Write-Host "Realm 'myRealm' not found or error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "To import myRealm, run: .\import-realm.ps1" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== End of Report ===" -ForegroundColor Cyan
