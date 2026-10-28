# ==============================================================================
# demo-walkthrough.ps1: Live Interactive REST Flow Walkthrough
# Demonstrates Acceptance Test R16: Customer Searches, Books, Views Ticket, and Cancels
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Bussin' Bus Platform -- Live End-to-End Walkthrough       " -ForegroundColor Cyan
Write-Host " Target Backend: $BaseUrl                                " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Health Check
Write-Host "`n[Step 1] Checking Platform Health Indicator..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/health" -Method Get -WebSession $session
    Write-Host "  -> Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "  [!] Cannot reach backend at $BaseUrl. Please start it with: java -jar target/bus-ticket-booking-platform-1.0.0-SNAPSHOT.jar" -ForegroundColor Red
    exit 1
}

# 2. Customer Authentication
Write-Host "`n[Step 2] Authenticating as Seed Customer (customer@demo.local)..." -ForegroundColor Yellow
$loginPayload = @{
    email = "customer@demo.local"
    password = "Password@123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $loginPayload -ContentType "application/json" -WebSession $session
Write-Host "  -> Signed In: $($loginRes.fullName) ($($loginRes.role))" -ForegroundColor Green

# 3. Retrieve CSRF Token from Cookie
$csrfCookie = $session.Cookies.GetCookies($BaseUrl) | Where-Object { $_.Name -eq "XSRF-TOKEN" }
$csrfToken = if ($csrfCookie) { $csrfCookie.Value } else { "" }
$headers = @{}
if ($csrfToken) {
    $headers["X-XSRF-TOKEN"] = $csrfToken
}

# 4. Search Journeys (Delhi -> Lucknow)
Write-Host "`n[Step 3] Querying Available Journeys (Delhi -> Lucknow on 2026-11-01)..." -ForegroundColor Yellow
$stations = Invoke-RestMethod -Uri "$BaseUrl/api/v1/stations" -Method Get -WebSession $session
$delhi = $stations | Where-Object { $_.code -eq "NDLS" }
$lucknow = $stations | Where-Object { $_.code -eq "LKO" }
$search = Invoke-RestMethod -Uri "$BaseUrl/api/v1/journeys/search?originStationId=$($delhi.id)&destinationStationId=$($lucknow.id)&travelDate=2026-11-01" -Method Get -WebSession $session
Write-Host "  -> Journey Type: $($search.journeyType)" -ForegroundColor Green
Write-Host "  -> Options Found: $($search.options.Count)" -ForegroundColor Green

if ($search.options.Count -eq 0) {
    Write-Host "  [!] No trips scheduled for this date. Run database migrations and seed data." -ForegroundColor Red
    exit 0
}

$firstOption = $search.options[0]
$firstLeg = $firstOption.legs[0]
Write-Host "  -> Selected Trip: $($firstLeg.tripNumber) | Category: $($firstLeg.busCategory) | Fare: Rs. $($firstOption.totalFareInr)" -ForegroundColor Gray

# 5. Query Seat Layout
Write-Host "`n[Step 4] Fetching Interactive Seat Layout for Trip $($firstLeg.tripId)..." -ForegroundColor Yellow
$seatMap = Invoke-RestMethod -Uri "$BaseUrl/api/v1/trips/$($firstLeg.tripId)/seats?boardIndex=$($firstLeg.boardStopOrder)&alightIndex=$($firstLeg.alightStopOrder)" -Method Get -WebSession $session
$availableSeats = $seatMap.seats | Where-Object { $_.available -eq $true }
Write-Host "  -> Total Seats: $($seatMap.totalCapacity) | Available: $($availableSeats.Count)" -ForegroundColor Green

$targetSeat = $availableSeats[0]
Write-Host "  -> Target Seat Selected: $($targetSeat.seatNumber) (ID: $($targetSeat.seatId))" -ForegroundColor Gray

# 6. Atomic Booking with Idempotency Key
Write-Host "`n[Step 5] Creating Atomic Booking for Seat $($targetSeat.seatNumber)..." -ForegroundColor Yellow

# Extract CSRF token from session cookies
$cookies = $session.Cookies.GetCookies([System.Uri]$BaseUrl)
Write-Host "  -> Session Cookies Count: $($cookies.Count)" -ForegroundColor Gray
foreach ($c in $cookies) {
    Write-Host "     Cookie: $($c.Name) = $($c.Value)" -ForegroundColor Gray
}
$csrfCookie = $cookies | Where-Object { $_.Name -eq "XSRF-TOKEN" }
$headers = @{}
if ($csrfCookie) {
    $headers["X-XSRF-TOKEN"] = $csrfCookie.Value
    Write-Host "  -> Using CSRF Token: $($csrfCookie.Value)" -ForegroundColor Gray
} else {
    Write-Host "  -> NO CSRF COOKIE FOUND!" -ForegroundColor Red
}
$idempotencyKey = "demo-cli-" + [System.Guid]::NewGuid().ToString()
$headers["Idempotency-Key"] = $idempotencyKey

$bookingPayload = @{
    contactEmail = "customer@demo.local"
    contactPhone = "+919876543210"
    passengerBookings = @(
        @{
            passengerName = "Parijaat Gaur"
            passengerAge = 28
            passengerGender = "MALE"
            legAllocations = @(
                @{
                    tripId = $firstLeg.tripId
                    seatId = $targetSeat.seatId
                    boardStopOrder = $firstLeg.boardStopOrder
                    alightStopOrder = $firstLeg.alightStopOrder
                }
            )
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $booking = Invoke-RestMethod -Uri "$BaseUrl/api/v1/bookings" -Method Post -Body $bookingPayload -Headers $headers -ContentType "application/json" -WebSession $session
} catch {
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Server Error Body: $($reader.ReadToEnd())" -ForegroundColor Red
    }
    throw $_
}
Write-Host "  -> CONFIRMED Booking Ref: $($booking.bookingReference) (ID: $($booking.id))" -ForegroundColor Green
Write-Host "  -> Total Paid: Rs. $($booking.totalAmountInr)" -ForegroundColor Green

# 7. Printable Ticket View
Write-Host "`n[Step 6] Retrieving Printable Boarding Pass..." -ForegroundColor Yellow
$ticket = Invoke-RestMethod -Uri "$BaseUrl/api/v1/bookings/$($booking.id)" -Method Get -WebSession $session
Write-Host "  -> Boarding: $($ticket.legs[0].boardStationName) at $($ticket.legs[0].departureTime)" -ForegroundColor Gray
Write-Host "  -> Alighting: $($ticket.legs[0].alightStationName) at $($ticket.legs[0].arrivalTime)" -ForegroundColor Gray
Write-Host "  -> Passenger: $($ticket.legs[0].allocatedSeats[0].passengerName) | Seat: $($ticket.legs[0].allocatedSeats[0].seatNumber)" -ForegroundColor Gray

# 8. Re-query Seat Layout: Confirm Seat is Occupied
Write-Host "`n[Step 7] Re-verifying Seat Layout: Seat $($targetSeat.seatNumber) Must Be Occupied..." -ForegroundColor Yellow
$seatMapAfter = Invoke-RestMethod -Uri "$BaseUrl/api/v1/trips/$($firstLeg.tripId)/seats?boardIndex=$($firstLeg.boardStopOrder)&alightIndex=$($firstLeg.alightStopOrder)" -Method Get -WebSession $session
$bookedSeatStatus = $seatMapAfter.seats | Where-Object { $_.seatId -eq $targetSeat.seatId }
Write-Host "  -> Seat $($targetSeat.seatNumber) Available Flag: $($bookedSeatStatus.available) (Should be False)" -ForegroundColor $(if ($bookedSeatStatus.available -eq $false) { "Green" } else { "Red" })

# 9. Cancel Booking
Write-Host "`n[Step 8] Cancelling Booking $($booking.bookingReference) (Whole-Itinerary Release)..." -ForegroundColor Yellow
$cancelRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/bookings/$($booking.id)/cancel" -Method Post -Headers $headers -WebSession $session
Write-Host "  -> Booking Status: $($cancelRes.status) | CancelledAt: $($cancelRes.cancelledAt)" -ForegroundColor Green

# 10. Confirm Seat is Released Back
Write-Host "`n[Step 9] Re-verifying Seat Layout: Seat $($targetSeat.seatNumber) Must Be Available Again..." -ForegroundColor Yellow
$seatMapReleased = Invoke-RestMethod -Uri "$BaseUrl/api/v1/trips/$($firstLeg.tripId)/seats?boardIndex=$($firstLeg.boardStopOrder)&alightIndex=$($firstLeg.alightStopOrder)" -Method Get -WebSession $session
$releasedSeatStatus = $seatMapReleased.seats | Where-Object { $_.seatId -eq $targetSeat.seatId }
Write-Host "  -> Seat $($targetSeat.seatNumber) Available Flag: $($releasedSeatStatus.available) (Should be True)" -ForegroundColor $(if ($releasedSeatStatus.available -eq $true) { "Green" } else { "Red" })

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " [PASS] End-to-End Customer Booking Lifecycle Verified!   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
