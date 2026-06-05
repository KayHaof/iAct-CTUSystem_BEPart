# ============================================================================
# KEYCLOAK RESET WITH NEW ADMIN PASSWORD
# ============================================================================

$projectRoot = "D:\Technology_Directory\CT550_LVTN\initialProject"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  RESET KEYCLOAK WITH NEW PASSWORD" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop all containers and remove volumes
Write-Host "[1/5] Stopping containers and removing volumes..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose down -v
Pop-Location
Write-Host "  Done" -ForegroundColor Green

# Step 2: Clean Keycloak data folder
Write-Host "[2/5] Cleaning Keycloak data folder..." -ForegroundColor Yellow
$kcDataPath = Join-Path $projectRoot "keycloak\data"
if (Test-Path $kcDataPath) {
    Remove-Item -Path $kcDataPath -Recurse -Force
    Write-Host "  Keycloak data cleaned" -ForegroundColor Green
}

# Step 3: Start MySQL
Write-Host "[3/5] Starting MySQL..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose up -d my-db
Pop-Location
Write-Host "  Waiting 90 seconds for MySQL..." -ForegroundColor Gray
Start-Sleep -Seconds 90

# Step 4: Start Keycloak
Write-Host "[4/5] Starting Keycloak..." -ForegroundColor Yellow
Push-Location $projectRoot
docker-compose up -d keycloak
Pop-Location
Write-Host "  Waiting 90 seconds for Keycloak to start..." -ForegroundColor Gray
Start-Sleep -Seconds 90

# Step 5: Verify
Write-Host "[5/5] Verifying..." -ForegroundColor Yellow

$maxAttempts = 20
$attempt = 0
$success = $false

while ($attempt -lt $maxAttempts -and -not $success) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8088/realms/master" -Method GET -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $success = $true
            Write-Host "  Keycloak is responding!" -ForegroundColor Green
        }
    } catch {
        $attempt++
        if ($attempt % 5 -eq 0) {
            Write-Host "  Still waiting... ($attempt/$maxAttempts)" -ForegroundColor Gray
        }
        Start-Sleep -Seconds 2
    }
}

# Test login
Write-Host ""
Write-Host "Testing admin login with new password..." -ForegroundColor Yellow
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
    
    Write-Host "  SUCCESS! Admin login works with password '123456'" -ForegroundColor Green
} catch {
    Write-Host "  Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Note: Password '123456' requires .env to be updated" -ForegroundColor Yellow
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
