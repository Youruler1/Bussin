$BaseUrl = "http://localhost:8080"
$ProxyUrl = "http://localhost:5173"

function Test-ApiCase($title, $url, $method, $body, $headers, $expectedStatus) {
    Write-Host -NoNewline "Testing: $title ($method $url)... "
    try {
        $params = @{
            Uri = $url
            Method = $method
            UseBasicParsing = $true
        }
        if ($body) {
            $params["Body"] = $body
            $params["ContentType"] = "application/json"
        }
        if ($headers) {
            $params["Headers"] = $headers
        }
        $resp = Invoke-WebRequest @params
        if ($resp.StatusCode -eq $expectedStatus) {
            Write-Host "[PASS - Status $($resp.StatusCode)]" -ForegroundColor Green
        } else {
            Write-Host "[UNEXPECTED - Got $($resp.StatusCode), Expected $expectedStatus]" -ForegroundColor Yellow
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $respBody = ""
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $respBody = $reader.ReadToEnd()
        }
        if ($status -eq $expectedStatus) {
            Write-Host "[PASS - Caught expected $status]" -ForegroundColor Green
        } elseif ($status -eq 500) {
            Write-Host "[FATAL 500 ERROR!]: $respBody" -ForegroundColor Red
        } else {
            Write-Host "[INFO - Status $status]: $respBody" -ForegroundColor Cyan
        }
    }
}

Write-Host "=== 1. Auth & Register Edge Cases ===" -ForegroundColor Magenta
# Duplicate registration
$dupBody = @{ email = "customer@demo.local"; password = "Password@123"; fullName = "Demo Customer"; phone = "+919123456789" } | ConvertTo-Json
Test-ApiCase "Duplicate Email" "$BaseUrl/api/v1/auth/register" "POST" $dupBody $null 409

# Short password (< 8 chars)
$shortPass = @{ email = "short@test.com"; password = "123"; fullName = "Short"; phone = "+919123456789" } | ConvertTo-Json
Test-ApiCase "Short Password Validation" "$BaseUrl/api/v1/auth/register" "POST" $shortPass $null 400

# Invalid email
$badEmail = @{ email = "notanemail"; password = "Password@123"; fullName = "Bad Email"; phone = "+919123456789" } | ConvertTo-Json
Test-ApiCase "Invalid Email Validation" "$BaseUrl/api/v1/auth/register" "POST" $badEmail $null 400

# Bad credentials login
$badLogin = @{ email = "customer@demo.local"; password = "WrongPassword" } | ConvertTo-Json
Test-ApiCase "Bad Credentials Login" "$BaseUrl/api/v1/auth/login" "POST" $badLogin $null 401

Write-Host "`n=== 2. Journey Search Edge Cases ===" -ForegroundColor Magenta
# Missing params
Test-ApiCase "Search Missing Params" "$BaseUrl/api/v1/journeys/search" "GET" $null $null 400

# Non-existent stations
Test-ApiCase "Search Non-existent Stations" "$BaseUrl/api/v1/journeys/search?originStationId=9999&destinationStationId=9998&travelDate=2026-11-01" "GET" $null $null 404

# Same station search
Test-ApiCase "Search Same Station Origin=Dest" "$BaseUrl/api/v1/journeys/search?originStationId=1&destinationStationId=1&travelDate=2026-11-01" "GET" $null $null 400

# Invalid Date Format
Test-ApiCase "Search Invalid Date Format" "$BaseUrl/api/v1/journeys/search?originStationId=1&destinationStationId=2&travelDate=not-a-date" "GET" $null $null 400

Write-Host "`n=== 3. Seat Map Edge Cases ===" -ForegroundColor Magenta
# Non-existent trip seat map
Test-ApiCase "Seat Map Invalid Trip" "$BaseUrl/api/v1/trips/9999/seats?boardIndex=0&alightIndex=1" "GET" $null $null 404

Write-Host "`n=== 4. Vite Proxy (Port 5174) Connectivity ===" -ForegroundColor Magenta
Test-ApiCase "Vite Proxy Health" "$ProxyUrl/api/v1/health" "GET" $null $null 200
Test-ApiCase "Vite Proxy Stations" "$ProxyUrl/api/v1/stations" "GET" $null $null 200
Test-ApiCase "Vite Proxy Duplicate Register" "$ProxyUrl/api/v1/auth/register" "POST" $dupBody $null 409
