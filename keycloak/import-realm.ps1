# Get admin token
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8088/realms/master/protocol/openid-connect/token" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=admin&password=admin&grant_type=password&client_id=admin-cli"

$accessToken = $tokenResponse.access_token

# Read realm JSON
$realmJson = Get-Content "D:\Technology_Directory\CT550_LVTN\initialProject\keycloak\myRealm-realm.json" -Raw

# Import realm
$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8088/admin/realms" `
        -Method POST `
        -Headers $headers `
        -Body $realmJson `
        -ErrorAction Stop
    
    Write-Host "Realm 'myRealm' imported successfully!" -ForegroundColor Green
} catch {
    $errorDetail = $_.Exception.Message
    Write-Host "Error importing realm: $errorDetail" -ForegroundColor Red
    
    # Check if realm already exists
    if ($errorDetail -like "*already exists*") {
        Write-Host "Realm already exists. Trying to update..." -ForegroundColor Yellow
        # Delete existing realm first
        try {
            Invoke-RestMethod -Uri "http://localhost:8088/admin/realms/myRealm" `
                -Method DELETE `
                -Headers $headers | Out-Null
            Write-Host "Existing realm deleted." -ForegroundColor Yellow
            
            # Import again
            Start-Sleep -Seconds 2
            Invoke-RestMethod -Uri "http://localhost:8088/admin/realms" `
                -Method POST `
                -Headers $headers `
                -Body $realmJson | Out-Null
            Write-Host "Realm 'myRealm' re-imported successfully!" -ForegroundColor Green
        } catch {
            Write-Host "Failed to re-import realm: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
