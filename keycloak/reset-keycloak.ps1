# ============================================================================
# Keycloak Reset Script
# Resets Keycloak completely and re-imports realm
# ============================================================================
#Requires -RunAsAdministrator

$ErrorActionPreference = "Continue"
$projectRoot = "D:\Technology_Directory\CT550_LVTN\initialProject"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  KEYCLOAK COMPLETE RESET SCRIPT" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop and remove all containers
Write-Host "[1/6] Stopping containers..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose down -v 2>$null
Pop-Location
Write-Host "  Containers stopped and volumes removed" -ForegroundColor Green

# Step 2: Clean up Keycloak data folder
Write-Host "[2/6] Cleaning up Keycloak data..." -ForegroundColor Yellow
$kcDataPath = Join-Path $projectRoot "keycloak\data"
if (Test-Path $kcDataPath) {
    Remove-Item -Path $kcDataPath -Recurse -Force
    Write-Host "  Keycloak data folder cleaned" -ForegroundColor Green
} else {
    Write-Host "  No Keycloak data folder found, skipping" -ForegroundColor Gray
}

# Step 3: Start MySQL first
Write-Host "[3/6] Starting MySQL..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose up -d my-db
Pop-Location
Write-Host "  Waiting for MySQL to be ready (90 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 90

# Check MySQL status
$mysqlLogs = docker-compose -f (Join-Path $projectRoot "docker-compose.yml") logs my-db 2>$null
if ($mysqlLogs -match "ready for connections") {
    Write-Host "  MySQL is ready!" -ForegroundColor Green
} else {
    Write-Host "  MySQL may not be ready, continuing anyway..." -ForegroundColor Yellow
}

# Step 4: Start Keycloak
Write-Host "[4/6] Starting Keycloak..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose up -d keycloak
Pop-Location

# Wait for Keycloak to start
Write-Host "  Waiting for Keycloak to start (90 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 90

# Check Keycloak status
Write-Host "  Checking Keycloak logs..." -ForegroundColor Gray
$kcLogs = docker-compose -f (Join-Path $projectRoot "docker-compose.yml") logs keycloak 2>$null
if ($kcLogs -match "started in") {
    Write-Host "  Keycloak started successfully!" -ForegroundColor Green
} else {
    Write-Host "  Checking for startup messages..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[5/6] Checking Keycloak status..." -ForegroundColor Yellow

# Wait for Keycloak to be ready for connections
$maxAttempts = 30
$attempt = 0
$kcReady = $false

while ($attempt -lt $maxAttempts -and -not $kcReady) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8088/realms/master" -Method GET -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $kcReady = $true
            Write-Host "  Keycloak is responding!" -ForegroundColor Green
        }
    } catch {
        $attempt++
        if ($attempt % 10 -eq 0) {
            Write-Host "  Still waiting... ($attempt/$maxAttempts)" -ForegroundColor Gray
        }
        Start-Sleep -Seconds 2
    }
}

if (-not $kcReady) {
    Write-Host "  Warning: Keycloak may not be fully ready" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[6/6] Testing admin login..." -ForegroundColor Yellow

# Test admin login
try {
    $tokenBody = @{
        username = "admin"
        password = "123456"
        grant_type = "password"
        client_id = "admin-cli"
    }
    $tokenResponse = Invoke-RestMethod -Uri "http://localhost:8088/realms/master/protocol/openid-connect/token" `
        -Method POST `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody `
        -ErrorAction Stop
    
    Write-Host "  Admin login successful!" -ForegroundColor Green
    
    # Get realms
    $headers = @{
        "Authorization" = "Bearer $($tokenResponse.access_token)"
    }
    
    $realms = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms" -Headers $headers
    Write-Host ""
    Write-Host "  Realms found:" -ForegroundColor Cyan
    foreach ($realm in $realms) {
        Write-Host "    - $($realm.realm)" -ForegroundColor White
    }
    
} catch {
    Write-Host "  Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Possible causes:" -ForegroundColor Yellow
    Write-Host "    1. Keycloak is still starting up" -ForegroundColor Gray
    Write-Host "    2. Admin password is different" -ForegroundColor Gray
    Write-Host "    3. Database connection issue" -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  SETUP COMPLETE" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Keycloak Admin Console:" -ForegroundColor Green
Write-Host "  URL: http://localhost:8088/admin/master/console/" -ForegroundColor White
Write-Host "  Username: admin" -ForegroundColor White
Write-Host "  Password: 123456" -ForegroundColor White
Write-Host ""
Write-Host "To check realm status:" -ForegroundColor Cyan
Write-Host "  cd $projectRoot\keycloak" -ForegroundColor White
Write-Host "  .\check-realm.ps1" -ForegroundColor White
Write-Host ""
Write-Host "To import myRealm:" -ForegroundColor Cyan
Write-Host "  cd $projectRoot\keycloak" -ForegroundColor White
Write-Host "  .\import-realm.ps1" -ForegroundColor White
Write-Host ""
