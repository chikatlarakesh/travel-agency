# Travel Booking API - Quick Test Script
# Run this script in PowerShell to test all endpoints

$baseUrl = "http://localhost:8080"
$token = ""

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Travel Booking API - Quick Test Script" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Function to make API calls
function Invoke-API {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = $null,
        [bool]$UseAuth = $false
    )

    $headers = @{
        "Content-Type" = "application/json"
    }

    if ($UseAuth -and $token) {
        $headers["Authorization"] = "Bearer $token"
    }

    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri "$baseUrl$Endpoint" -Method $Method -Headers $headers -Body $Body
        } else {
            $response = Invoke-RestMethod -Uri "$baseUrl$Endpoint" -Method $Method -Headers $headers
        }
        return $response
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
        return $null
    }
}

# Test 1: Sign Up
Write-Host "[1] Testing Sign Up..." -ForegroundColor Yellow
$signUpBody = @{
    email = "test.user@example.com"
    password = "TestPass123!"
    firstName = "Test"
    lastName = "User"
    phone = "+1-555-9999"
    role = "CUSTOMER"
} | ConvertTo-Json

$signUpResponse = Invoke-API -Method "POST" -Endpoint "/api/v1/auth/sign-up" -Body $signUpBody
if ($signUpResponse) {
    Write-Host "✓ Sign up successful!" -ForegroundColor Green
    Write-Host "  User ID: $($signUpResponse.userId)" -ForegroundColor Gray
    $global:token = $signUpResponse.token
    $global:userId = $signUpResponse.userId
} else {
    Write-Host "✗ Sign up failed (user might already exist)" -ForegroundColor Yellow
}
Write-Host ""

# Test 2: Sign In
Write-Host "[2] Testing Sign In..." -ForegroundColor Yellow
$signInBody = @{
    email = "test.user@example.com"
    password = "TestPass123!"
} | ConvertTo-Json

$signInResponse = Invoke-API -Method "POST" -Endpoint "/api/v1/auth/sign-in" -Body $signInBody
if ($signInResponse) {
    Write-Host "✓ Sign in successful!" -ForegroundColor Green
    Write-Host "  User ID: $($signInResponse.userId)" -ForegroundColor Gray
    Write-Host "  Token: $($signInResponse.token.Substring(0, 30))..." -ForegroundColor Gray
    $global:token = $signInResponse.token
    $global:userId = $signInResponse.userId
}
Write-Host ""

# Test 3: Search Destinations
Write-Host "[3] Testing Search Destinations..." -ForegroundColor Yellow
$destinations = Invoke-API -Method "GET" -Endpoint "/api/v1/tours/destinations?destination=phu"
if ($destinations) {
    Write-Host "✓ Found destinations:" -ForegroundColor Green
    $destinations.destinations | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
}
Write-Host ""

# Test 4: Get All Tours
Write-Host "[4] Testing Get All Tours..." -ForegroundColor Yellow
$tours = Invoke-API -Method "GET" -Endpoint "/api/v1/tours/available"
if ($tours) {
    Write-Host "✓ Found $($tours.totalCount) tours:" -ForegroundColor Green
    $tours.tours | ForEach-Object {
        Write-Host "  - $($_.name) ($($_.destination)) - `$$($_.minPrice)" -ForegroundColor Gray
    }
}
Write-Host ""

# Test 5: Filter Tours by Type (RESORT)
Write-Host "[5] Testing Filter Tours (RESORT only)..." -ForegroundColor Yellow
$resortTours = Invoke-API -Method "GET" -Endpoint "/api/v1/tours/available?tourType=RESORT"
if ($resortTours) {
    Write-Host "✓ Found $($resortTours.totalCount) resort tours" -ForegroundColor Green
}
Write-Host ""

# Test 6: Get Tour Details
Write-Host "[6] Testing Get Tour Details (t-001)..." -ForegroundColor Yellow
$tourDetails = Invoke-API -Method "GET" -Endpoint "/api/v1/tours/t-001"
if ($tourDetails) {
    Write-Host "✓ Tour Details Retrieved:" -ForegroundColor Green
    Write-Host "  Name: $($tourDetails.name)" -ForegroundColor Gray
    Write-Host "  Rating: $($tourDetails.rating) stars" -ForegroundColor Gray
    Write-Host "  Reviews: $($tourDetails.reviewCount)" -ForegroundColor Gray
}
Write-Host ""

# Test 7: Get Tour Reviews
Write-Host "[7] Testing Get Tour Reviews..." -ForegroundColor Yellow
$reviews = Invoke-API -Method "GET" -Endpoint "/api/v1/tours/t-001/reviews?page=1&pageSize=2"
if ($reviews) {
    Write-Host "✓ Found $($reviews.totalCount) reviews (showing $($reviews.reviews.Count)):" -ForegroundColor Green
    $reviews.reviews | ForEach-Object {
        Write-Host "  - $($_.author) ($($_.rating)★): $($_.comment.Substring(0, 50))..." -ForegroundColor Gray
    }
}
Write-Host ""

# Test 8: Create Booking (requires auth)
if ($token) {
    Write-Host "[8] Testing Create Booking..." -ForegroundColor Yellow
    $bookingBody = @{
        tourId = "t-001"
        userId = $userId
        tourInstanceId = "ti-001-dec01"
        mealPlan = "AI"
        adults = 2
        children = 0
        leadTraveler = @{
            firstName = "Test"
            lastName = "User"
            email = "test.user@example.com"
            phone = "+1-555-9999"
            dateOfBirth = "1990-01-01"
            passportNumber = "P12345678"
        }
        additionalTravelers = @(
            @{
                firstName = "Jane"
                lastName = "User"
                email = "jane.user@example.com"
                phone = "+1-555-8888"
                dateOfBirth = "1992-05-15"
                passportNumber = "P87654321"
            }
        )
    } | ConvertTo-Json -Depth 10

    $booking = Invoke-API -Method "POST" -Endpoint "/api/v1/bookings" -Body $bookingBody -UseAuth $true
    if ($booking) {
        Write-Host "✓ Booking Created:" -ForegroundColor Green
        Write-Host "  Booking ID: $($booking.bookingId)" -ForegroundColor Gray
        Write-Host "  Tour: $($booking.tourName)" -ForegroundColor Gray
        Write-Host "  Total Price: `$$($booking.totalPrice)" -ForegroundColor Gray
        Write-Host "  Status: $($booking.status)" -ForegroundColor Gray
        $global:bookingId = $booking.bookingId
    }
    Write-Host ""

    # Test 9: Get User Bookings
    Write-Host "[9] Testing Get User Bookings..." -ForegroundColor Yellow
    $userBookings = Invoke-API -Method "GET" -Endpoint "/api/v1/bookings?userId=$userId" -UseAuth $true
    if ($userBookings) {
        Write-Host "✓ Found $($userBookings.bookings.Count) booking(s):" -ForegroundColor Green
        $userBookings.bookings | ForEach-Object {
            Write-Host "  - $($_.tourName) - `$$($_.totalPrice) ($($_.status))" -ForegroundColor Gray
        }
    }
    Write-Host ""
} else {
    Write-Host "[8-9] Skipping booking tests (no authentication token)" -ForegroundColor Yellow
    Write-Host ""
}

# Summary
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Test Summary" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "All tests completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Open Swagger UI: http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "  2. View API_TESTING_GUIDE.md for detailed examples" -ForegroundColor White
Write-Host "  3. Use Postman collection: test-collection.json" -ForegroundColor White
Write-Host ""

if ($token) {
    Write-Host "Your JWT Token (save this for testing):" -ForegroundColor Yellow
    Write-Host $token -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Your User ID:" -ForegroundColor Yellow
    Write-Host $userId -ForegroundColor Cyan
}

