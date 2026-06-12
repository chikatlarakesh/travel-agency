param([string]$BaseUrl = "http://localhost:8080")

$pass = 0; $fail = 0
$results = @()

function Check($label, $expected, $actual, $body) {
    $short = if ($body) { $body.Substring(0, [Math]::Min(200, $body.Length)) } else { "" }
    if ($actual -eq $expected) {
        Write-Host "[PASS] $label (HTTP $actual)" -ForegroundColor Green
        $script:pass++
        $script:results += [pscustomobject]@{Test=$label;Status="PASS";HTTP=$actual;Notes=""}
    } else {
        Write-Host "[FAIL] $label -- expected $expected, got $actual`n       $short" -ForegroundColor Red
        $script:fail++
        $script:results += [pscustomobject]@{Test=$label;Status="FAIL";HTTP=$actual;Notes=$short}
    }
}

function Info($label, $code, $body) {
    $short = if ($body) { $body.Substring(0, [Math]::Min(200, $body.Length)) } else { "" }
    Write-Host "[INFO] $label (HTTP $code) $short" -ForegroundColor Yellow
    $script:results += [pscustomobject]@{Test=$label;Status="INFO";HTTP=$code;Notes=$short}
}

function Req($method, $path, $body = $null, $headers = @{}) {
    $uri = "$BaseUrl$path"
    $params = @{
        Uri = $uri
        Method = $method
        SkipHttpErrorCheck = $true
        ErrorAction = "Stop"
    }
    if ($body) {
        $params["Body"] = $body
        $params["ContentType"] = "application/json"
    }
    if ($headers.Count -gt 0) { $params["Headers"] = $headers }
    return Invoke-WebRequest @params
}

Write-Host ""
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "  Travel Agency API - Full Endpoint Test Suite" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# ─────────────────────────────────────────────────────────
# AUTH
# ─────────────────────────────────────────────────────────
Write-Host "`n--- AUTH ----------------------------------------" -ForegroundColor Magenta

# Sign-up duplicate email
$b = @{ firstName="Test"; lastName="User"; email="vipultester99@example.com"; password="Secure@999" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-up" $b
Check "POST /sign-up - duplicate email -> 409" 409 $r.StatusCode $r.Content

# Sign-up new user
$newEmail = "apitest_$([System.Guid]::NewGuid().ToString().Substring(0,8))@example.com"
$b = @{ firstName="Api"; lastName="Test"; email=$newEmail; password="Test@1234" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-up" $b
Check "POST /sign-up - new user -> 201" 201 $r.StatusCode $r.Content

# Sign-up missing firstName/lastName
$b = @{ email="x@example.com"; password="Test@1234" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-up" $b
Check "POST /sign-up - missing fields -> 400" 400 $r.StatusCode $r.Content

# Sign-up invalid email
$b = @{ firstName="A"; lastName="B"; email="not-an-email"; password="Test@1234" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-up" $b
Check "POST /sign-up - invalid email -> 400" 400 $r.StatusCode $r.Content

# Sign-up short password
$b = @{ firstName="A"; lastName="B"; email="pw@example.com"; password="abc" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-up" $b
Check "POST /sign-up - short password -> 400" 400 $r.StatusCode $r.Content

# Sign-in valid
$b = @{ email="vipultester99@example.com"; password="Secure@999" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-in" $b
Check "POST /sign-in - valid credentials -> 200" 200 $r.StatusCode $r.Content
$authData = $r.Content | ConvertFrom-Json
$TOKEN = $authData.idToken
Write-Host "       Role=$($authData.role) User=$($authData.userName)" -ForegroundColor DarkGray

# Decode userId from JWT sub claim
$parts = $TOKEN.Split(".")
$pad = $parts[1].PadRight(($parts[1].Length + 3) -band -bnot 3, "=")
$USERID = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad)) | ConvertFrom-Json).sub
Write-Host "       userId=$USERID" -ForegroundColor DarkGray

# Sign-in wrong password
$b = @{ email="vipultester99@example.com"; password="WrongPass99" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-in" $b
Check "POST /sign-in - wrong password -> 400" 400 $r.StatusCode $r.Content

# Sign-in nonexistent user
$b = @{ email="nobody@example.com"; password="Test@1234" } | ConvertTo-Json
$r = Req "POST" "/api/v1/auth/sign-in" $b
Check "POST /sign-in - unknown user -> 400" 400 $r.StatusCode $r.Content

# Refresh without cookie
$r = Req "POST" "/api/v1/auth/refresh"
Check "POST /refresh - no cookie -> 401" 401 $r.StatusCode $r.Content

# ─────────────────────────────────────────────────────────
# TOURS
# ─────────────────────────────────────────────────────────
Write-Host "`n--- TOURS ---------------------------------------" -ForegroundColor Magenta

# Destinations autocomplete
$r = Req "GET" "/api/v1/tours/destinations?destination=Punta"
Check "GET /tours/destinations - Punta -> 200" 200 $r.StatusCode $r.Content
$dests = ($r.Content | ConvertFrom-Json).destinations
Write-Host "       Destinations: $($dests -join ', ')" -ForegroundColor DarkGray

# Destinations - 2-char query
$r = Req "GET" "/api/v1/tours/destinations?destination=Pu"
Info "GET /tours/destinations - 2-char query" $r.StatusCode $r.Content

# Destinations - missing param
$r = Req "GET" "/api/v1/tours/destinations"
Info "GET /tours/destinations - missing param" $r.StatusCode $r.Content

# Available tours - defaults
$r = Req "GET" "/api/v1/tours/available"
Check "GET /tours/available - defaults -> 200" 200 $r.StatusCode $r.Content
$tourList = $r.Content | ConvertFrom-Json
Write-Host "       count=$($tourList.tours.Count) total=$($tourList.totalCount)" -ForegroundColor DarkGray
$T1 = $tourList.tours[0]
$T1_ID = $T1.id
$T1_DATE = $T1.startDate
$T1_DUR = if ($T1.duration) { $T1.duration } else { "7 days" }
Write-Host "       Tour: id=$T1_ID date=$T1_DATE dur=$T1_DUR" -ForegroundColor DarkGray

# Available tours - tourType filter
$r = Req "GET" "/api/v1/tours/available?tourType=RESORT"
Check "GET /tours/available - tourType=RESORT -> 200" 200 $r.StatusCode $r.Content

# Available tours - sortBy PRICE_ASC
$r = Req "GET" "/api/v1/tours/available?sortBy=PRICE_ASC"
Check "GET /tours/available - sortBy=PRICE_ASC -> 200" 200 $r.StatusCode $r.Content

# Available tours - mealPlan filter
$r = Req "GET" "/api/v1/tours/available?mealPlan=AI"
Check "GET /tours/available - mealPlan=AI -> 200" 200 $r.StatusCode $r.Content

# Available tours - pagination page 2
$r = Req "GET" "/api/v1/tours/available?page=2&pageSize=3"
Check "GET /tours/available - page 2 -> 200" 200 $r.StatusCode $r.Content

# Tour detail - valid
$r = Req "GET" "/api/v1/tours/$T1_ID"
Check "GET /tours/{id} - valid -> 200" 200 $r.StatusCode $r.Content

# Tour detail - not found
$r = Req "GET" "/api/v1/tours/nonexistent-tour-xyz"
Check "GET /tours/{id} - not found -> 404" 404 $r.StatusCode $r.Content

# Reviews - valid
$r = Req "GET" "/api/v1/tours/$T1_ID/reviews?page=1&pageSize=5"
Check "GET /tours/{id}/reviews - valid -> 200" 200 $r.StatusCode $r.Content
$revData = $r.Content | ConvertFrom-Json
Write-Host "       Reviews=$($revData.reviews.Count)" -ForegroundColor DarkGray

# Reviews - not found
$r = Req "GET" "/api/v1/tours/nonexistent-tour-xyz/reviews"
Check "GET /tours/{id}/reviews - not found -> 404" 404 $r.StatusCode $r.Content

# ─────────────────────────────────────────────────────────
# BOOKINGS
# ─────────────────────────────────────────────────────────
Write-Host "`n--- BOOKINGS ------------------------------------" -ForegroundColor Magenta

# Sign-in with the freshly-created user so we get a userId with zero prior bookings
$signInBody = @{ email = $newEmail; password = "Test@1234" } | ConvertTo-Json
$signInResp = Req "POST" "/api/v1/auth/sign-in" $signInBody
$bookingAuthData = $signInResp.Content | ConvertFrom-Json
$BOOKING_TOKEN = $bookingAuthData.idToken
$parts2 = $BOOKING_TOKEN.Split(".")
$pad2 = $parts2[1].PadRight(($parts2[1].Length + 3) -band -bnot 3, "=")
$BOOKING_USERID = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad2)) | ConvertFrom-Json).sub
Write-Host "       Booking userId=$BOOKING_USERID (fresh user, zero prior bookings)" -ForegroundColor DarkGray

$AUTH = @{ Authorization = "Bearer $BOOKING_TOKEN" }

# Get bookings - valid token
$r = Req "GET" "/api/v1/bookings?userId=$BOOKING_USERID" $null $AUTH
Check "GET /bookings - valid token -> 200" 200 $r.StatusCode $r.Content
$beforeCount = ($r.Content | ConvertFrom-Json).bookings.Count
Write-Host "       Existing bookings: $beforeCount" -ForegroundColor DarkGray

# Get bookings - no token
$r = Req "GET" "/api/v1/bookings?userId=$BOOKING_USERID"
Check "GET /bookings - no token -> 401" 401 $r.StatusCode $r.Content

# Get bookings - invalid token
$r = Req "GET" "/api/v1/bookings?userId=$BOOKING_USERID" $null @{ Authorization = "Bearer bad.tok.en" }
Check "GET /bookings - invalid token -> 401" 401 $r.StatusCode $r.Content

# Create booking - valid
$createBody = @{
    userId = $BOOKING_USERID
    tourId = $T1_ID
    date = $T1_DATE
    duration = $T1_DUR
    mealPlan = "BB"
    guests = @{ adult = 1; children = 0 }
    personalDetails = @(@{ firstName = "Alice"; lastName = "Tester" })
} | ConvertTo-Json -Depth 3
Write-Host "       Create body: $createBody" -ForegroundColor DarkGray
$r = Req "POST" "/api/v1/bookings" $createBody $AUTH
Check "POST /bookings - valid -> 201" 201 $r.StatusCode $r.Content
Write-Host "       Response: $($r.Content)" -ForegroundColor DarkGray

# Get bookings after create
$r = Req "GET" "/api/v1/bookings?userId=$BOOKING_USERID" $null $AUTH
$listAfter = ($r.Content | ConvertFrom-Json).bookings
Write-Host "       Bookings after create: $($listAfter.Count)" -ForegroundColor DarkGray
$BOOKING_ID = if ($listAfter -and $listAfter.Count -gt 0) { $listAfter[0].id } else { $null }
Write-Host "       bookingId=$BOOKING_ID state=$($listAfter[0].state)" -ForegroundColor DarkGray

# Create booking - no token
$r = Req "POST" "/api/v1/bookings" $createBody
Check "POST /bookings - no token -> 401" 401 $r.StatusCode $r.Content

# Create booking - nonexistent tourId
$b = @{
    userId = $BOOKING_USERID; tourId = "nonexistent-tour"; date = $T1_DATE
    duration = "7 days"; mealPlan = "BB"
    guests = @{ adult = 1; children = 0 }
    personalDetails = @(@{ firstName = "Test"; lastName = "User" })
} | ConvertTo-Json -Depth 3
$r = Req "POST" "/api/v1/bookings" $b $AUTH
Check "POST /bookings - nonexistent tourId -> 404" 404 $r.StatusCode $r.Content

# Create booking - missing mealPlan
$b = @{
    userId = $BOOKING_USERID; tourId = $T1_ID; date = $T1_DATE; duration = "7 days"
    guests = @{ adult = 1; children = 0 }
    personalDetails = @(@{ firstName = "Test"; lastName = "User" })
} | ConvertTo-Json -Depth 3
$r = Req "POST" "/api/v1/bookings" $b $AUTH
Check "POST /bookings - missing mealPlan -> 400" 400 $r.StatusCode $r.Content

# Create booking - 0 adults
$b = @{
    userId = $BOOKING_USERID; tourId = $T1_ID; date = $T1_DATE; duration = "7 days"; mealPlan = "BB"
    guests = @{ adult = 0; children = 0 }
    personalDetails = @(@{ firstName = "Test"; lastName = "User" })
} | ConvertTo-Json -Depth 3
$r = Req "POST" "/api/v1/bookings" $b $AUTH
Check "POST /bookings - 0 adults -> 400" 400 $r.StatusCode $r.Content

# Create booking - no matching instance date
$b = @{
    userId = $BOOKING_USERID; tourId = $T1_ID; date = "2099-01-01"; duration = "7 days"; mealPlan = "BB"
    guests = @{ adult = 1; children = 0 }
    personalDetails = @(@{ firstName = "Test"; lastName = "User" })
} | ConvertTo-Json -Depth 3
$r = Req "POST" "/api/v1/bookings" $b $AUTH
Check "POST /bookings - no matching instance -> 400" 400 $r.StatusCode $r.Content

# Cancel booking - nonexistent ID
$r = Req "DELETE" "/api/v1/bookings/nonexistent-booking-id" $null $AUTH
Check "DELETE /bookings/{id} - not found -> 404" 404 $r.StatusCode $r.Content

# Cancel booking - no token
$r = Req "DELETE" "/api/v1/bookings/some-id"
Check "DELETE /bookings/{id} - no token -> 401" 401 $r.StatusCode $r.Content

# Cancel booking - the booking we just created
if ($BOOKING_ID) {
    $r = Req "DELETE" "/api/v1/bookings/$BOOKING_ID" $null $AUTH
    if ($r.StatusCode -eq 204) {
        Check "DELETE /bookings/{id} - cancel -> 204" 204 $r.StatusCode ""
        # Verify state = CANCELED
        $r2 = Req "GET" "/api/v1/bookings?userId=$BOOKING_USERID" $null $AUTH
        $b2 = ($r2.Content | ConvertFrom-Json).bookings | Where-Object { $_.id -eq $BOOKING_ID }
        if ($b2) { Write-Host "       State after cancel: $($b2.state)" -ForegroundColor DarkGray }
    } elseif ($r.StatusCode -eq 400) {
        Info "DELETE /bookings/{id} - within 10-day cancel window (tour date $T1_DATE)" $r.StatusCode $r.Content
    } else {
        Check "DELETE /bookings/{id} - unexpected status" 204 $r.StatusCode $r.Content
    }
}

# ─────────────────────────────────────────────────────────
# MISC
# ─────────────────────────────────────────────────────────
Write-Host "`n--- MISC ----------------------------------------" -ForegroundColor Magenta

$r = Req "GET" "/api/hello"
Check "GET /api/hello -> 200" 200 $r.StatusCode $r.Content

# Unknown route: Spring Security returns 401 for unauthenticated requests to unknown paths
$r = Req "GET" "/api/v1/nonexistent"
Info "GET /nonexistent -> 401 (Spring Security blocks before 404)" $r.StatusCode $r.Content

# ─────────────────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "  RESULTS: PASS=$pass  FAIL=$fail" -ForegroundColor $(if ($fail -gt 0) { "Red" } else { "Green" })
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""
$results | Format-Table -AutoSize -Wrap
if ($fail -gt 0) { exit 1 } else { exit 0 }
