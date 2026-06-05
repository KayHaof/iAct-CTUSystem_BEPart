# Wait for Keycloak to be ready
function Wait-ForKeycloak {
    $maxAttempts = 30
    $attempt = 0
    Write-Host "Waiting for Keycloak to be ready..." -ForegroundColor Yellow
    
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
        Write-Host "  Attempt $attempt/$maxAttempts..." -ForegroundColor Gray
    }
    
    Write-Host "Keycloak is not responding after $maxAttempts attempts" -ForegroundColor Red
    return $false
}

# Get admin token
function Get-AdminToken {
    try {
        $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8088/realms/master/protocol/openid-connect/token" `
            -Method POST `
            -ContentType "application/x-www-form-urlencoded" `
            -Body "username=admin&password=123456&grant_type=password&client_id=admin-cli" `
            -ErrorAction Stop
        return $tokenResponse.access_token
    } catch {
        Write-Host "Failed to get admin token: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Wait for Keycloak
if (-not (Wait-ForKeycloak)) {
    Write-Host "Cannot proceed - Keycloak is not ready" -ForegroundColor Red
    exit 1
}

# Get token
$accessToken = Get-AdminToken
if (-not $accessToken) {
    Write-Host "Cannot proceed - Failed to authenticate" -ForegroundColor Red
    exit 1
}

# Read realm JSON
$realmJson = Get-Content "D:\Technology_Directory\CT550_LVTN\initialProject\keycloak\myRealm_export.json" -Raw

# Import realm
$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

try {
    # Check if realm exists first
    try {
        $existingRealm = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm" -Headers $headers -ErrorAction SilentlyContinue
        if ($existingRealm) {
            Write-Host "Realm 'myRealm' already exists. Updating..." -ForegroundColor Yellow
            Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm" `
                -Method PUT `
                -Headers $headers `
                -Body $realmJson | Out-Null
            Write-Host "Realm 'myRealm' updated successfully!" -ForegroundColor Green
        }
    } catch {
        # Realm doesn't exist, create new
        $response = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms" `
            -Method POST `
            -Headers $headers `
            -Body $realmJson `
            -ErrorAction Stop
        Write-Host "Realm 'myRealm' imported successfully!" -ForegroundColor Green
    }
    
    # Import users from myRealm-users-0.json
    $usersJson = Get-Content "D:\Technology_Directory\CT550_LVTN\initialProject\keycloak\myRealm-users-0.json" -Raw
    $users = $usersJson | ConvertFrom-Json
    
    Write-Host ""
    Write-Host "Importing users..." -ForegroundColor Cyan
    foreach ($user in $users.users) {
        $userBody = @{
            username = $user.username
            firstName = $user.firstName
            lastName = $user.lastName
            email = $user.email
            emailVerified = $user.emailVerified
            enabled = $user.enabled
            credentials = $user.credentials
            realmRoles = $user.realmRoles
            groups = $user.groups
        } | ConvertTo-Json -Depth 10
        
        try {
            # Check if user exists
            $existingUser = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/users?username=$($user.username)" `
                -Headers $headers -ErrorAction SilentlyContinue
            if ($existingUser -and $existingUser.Count -gt 0) {
                Write-Host "  - User '$($user.username)' already exists, skipping..." -ForegroundColor Yellow
            } else {
                Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm/users" `
                    -Method POST `
                    -Headers $headers `
                    -Body $userBody | Out-Null
                Write-Host "  + User '$($user.username)' imported" -ForegroundColor Green
            }
        } catch {
            Write-Host "  ! Failed to import user '$($user.username)': $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "=== Import completed! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access Keycloak Admin Console:" -ForegroundColor Cyan
    Write-Host "  URL: http://localhost:8088/admin/master/console/" -ForegroundColor White
    Write-Host "  Username: admin" -ForegroundColor White
    Write-Host "  Password: 123456" -ForegroundColor White
    
} catch {
    $errorDetail = $_.Exception.Message
    Write-Host "Error: $errorDetail" -ForegroundColor Red
}
