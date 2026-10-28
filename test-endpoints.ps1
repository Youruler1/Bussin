$BaseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Test-Endpoint($name, $script) {
    Write-Host -NoNewline "Testing $name... "
    try {
        & $script
        Write-Host "[OK]" -ForegroundColor Green
    } catch {
        Write-Host "[FAILED]: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "   Response Body: $($reader.ReadToEnd())" -ForegroundColor Yellow
        }
    }
}

Test-Endpoint "Health" { Invoke-RestMethod "$BaseUrl/api/v1/health" -WebSession $session }
Test-Endpoint "Stations" { Invoke-RestMethod "$BaseUrl/api/v1/stations" -WebSession $session }
Test-Endpoint "Categories" { Invoke-RestMethod "$BaseUrl/api/v1/categories" -WebSession $session }
Test-Endpoint "Buses" { Invoke-RestMethod "$BaseUrl/api/v1/buses" -WebSession $session }
Test-Endpoint "Routes" { Invoke-RestMethod "$BaseUrl/api/v1/routes" -WebSession $session }
Test-Endpoint "Direct Journey Search" { Invoke-RestMethod "$BaseUrl/api/v1/journeys/search?originStationId=1&destinationStationId=3&travelDate=2026-11-01" -WebSession $session }
Test-Endpoint "Connecting Journey Search" { Invoke-RestMethod "$BaseUrl/api/v1/journeys/search?originStationId=1&destinationStationId=4&travelDate=2026-11-01" -WebSession $session }
Test-Endpoint "Trip Seats" { Invoke-RestMethod "$BaseUrl/api/v1/trips/1/seats?boardIndex=0&alightIndex=2" -WebSession $session }

# Test Register
Test-Endpoint "Register Customer" {
    $rand = Get-Random -Minimum 1000 -Maximum 9999
    $body = @{
        email = "newcustomer$rand@demo.local"
        password = "Password@123"
        fullName = "New Customer $rand"
        phone = "+91987654$rand"
    } | ConvertTo-Json
    Invoke-RestMethod "$BaseUrl/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json"
}

# Test Login
Test-Endpoint "Login Customer" {
    $body = @{ email = "customer@demo.local"; password = "Password@123" } | ConvertTo-Json
    Invoke-RestMethod "$BaseUrl/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -WebSession $session
}

Test-Endpoint "Customer Profile (Me)" { Invoke-RestMethod "$BaseUrl/api/v1/me" -WebSession $session }
Test-Endpoint "Customer Bookings History" { Invoke-RestMethod "$BaseUrl/api/v1/me/bookings" -WebSession $session }

# Test Register
Test-Endpoint "Register Customer" {
    $rand = Get-Random -Minimum 1000 -Maximum 9999
    $body = @{
        email = "newcustomer$rand@demo.local"
        password = "Password@123"
        fullName = "New Customer $rand"
        phone = "+91987654$rand"
    } | ConvertTo-Json
    Invoke-RestMethod "$BaseUrl/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json"
}

# Test Login Admin
$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Test-Endpoint "Login Admin" {
    $body = @{ email = "admin@busbooking.local"; password = "Password@123" } | ConvertTo-Json
    Invoke-RestMethod "$BaseUrl/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -WebSession $adminSession
}
Test-Endpoint "Admin Trips" { Invoke-RestMethod "$BaseUrl/api/v1/admin/trips" -WebSession $adminSession }
Test-Endpoint "Admin Bookings" { Invoke-RestMethod "$BaseUrl/api/v1/admin/bookings" -WebSession $adminSession }
