# ============================================================================
# Keycloak Connection Test
# ============================================================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  KEYCLOAK CONNECTION TEST" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8088"
$allPassed = $true

# Test 1: Check if Keycloak is responding
Write-Host "[Test 1] Checking if Keycloak is responding..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/realms/master" -Method GET -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "  PASS - Keycloak is responding" -ForegroundColor Green
    } else {
        Write-Host "  FAIL - Unexpected status code: $($response.StatusCode)" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "  FAIL - Cannot connect to Keycloak: $($_.Exception.Message)" -ForegroundColor Red
    $allPassed = $false
}

# Test 2: Check admin login
Write-Host "[Test 2] Testing admin login..." -ForegroundColor Yellow
try {
    $body = @{
        username = "admin"
        password = "123456"
        grant_type = "password"
        client_id = "admin-cli"
    }
    $tokenResponse = Invoke-RestMethod -Uri "$baseUrl/realms/master/protocol/openid-connect/token" `
        -Method POST `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $body `
        -ErrorAction Stop
    
    Write-Host "  PASS - Admin login successful" -ForegroundColor Green
    $accessToken = $tokenResponse.access_token
    
    $headers = @{
        "Authorization" = "Bearer $accessToken"
    }
    
    # Test 3: Check master realm
    Write-Host "[Test 3] Checking master realm..." -ForegroundColor Yellow
    try {
        $masterRealm = Invoke-RestMethod -Uri "$baseUrl/admin/realms/master" -Headers $headers
        Write-Host "  PASS - Master realm accessible" -ForegroundColor Green
        Write-Host "    Users count: $($masterRealm.users.Count)" -ForegroundColor Gray
    } catch {
        Write-Host "  FAIL - Cannot access master realm: $($_.Exception.Message)" -ForegroundColor Red
        $allPassed = $false
    }
    
    # Test 4: Check myRealm
    Write-Host "[Test 4] Checking myRealm..." -ForegroundColor Yellow
    try {
        $myRealm = Invoke-RestMethod -Uri "$baseUrl/admin/realms/myRealm" -Headers $headers
        Write-Host "  PASS - myRealm exists" -ForegroundColor Green
        Write-Host "    Display name: $($myRealm.displayName)" -ForegroundColor Gray
        Write-Host "    Enabled: $($myRealm.enabled)" -ForegroundColor Gray
    } catch {
        Write-Host "  WARN - myRealm not found (may need to import)" -ForegroundColor Yellow
        Write-Host "    Run .\import-realm.ps1 to import myRealm" -ForegroundColor Gray
    }
    
    # Test 5: Check clients
    Write-Host "[Test 5] Checking myRealm clients..." -ForegroundColor Yellow
    try {
        $clients = Invoke-RestMethod -Uri "$baseUrl/admin/realms/myRealm/clients" -Headers $headers
        Write-Host "  PASS - Clients accessible" -ForegroundColor Green
        Write-Host "    Total clients: $($clients.Count)" -ForegroundColor Gray
        $iactClients = $clients | Where-Object { $_.clientId -like "*iact*" }
        if ($iactClients) {
            foreach ($client in $iactClients) {
                Write-Host "    - $($client.clientId)" -ForegroundColor Cyan
            }
        }
    } catch {
        Write-Host "  WARN - Cannot check clients (myRealm may not exist)" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "  FAIL - Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "  ALL TESTS PASSED" -ForegroundColor Green
} else {
    Write-Host "  SOME TESTS FAILED" -ForegroundColor Red
}
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
