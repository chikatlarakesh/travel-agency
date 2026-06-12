param([string]$BASE = "http://localhost:8080")

$pass = 0; $fail = 0

function Test-API($label, $method, $path, $body, $token, $expected, $session) {
    $headers = @{"Content-Type"="application/json"}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $bodyJson = if ($body) { $body | ConvertTo-Json -Depth 5 -Compress } else { $null }
    try {
        $params = @{ Uri="$BASE$path"; Method=$method; Headers=$headers; ErrorAction="Stop" }
        if ($bodyJson) { $params["Body"] = $bodyJson }
        if ($session)  { $params["WebSession"] = $session }
        $r = Invoke-WebRequest @params
        $code = [int]$r.StatusCode
        $content = try { $r.Content | ConvertFrom-Json -Depth 10 } catch { $r.Content }
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        $content = $_.ErrorDetails.Message
    }
    $ok = $code -eq $expected
    if ($ok) { $script:pass++ } else { $script:fail++ }
    $color = if ($ok) { "Green" } else { "Red" }
    $status = if ($ok) { "PASS" } else { "FAIL" }
    Write-Host "  [$status] $label  (got $code, expected $expected)" -ForegroundColor $color
    if (-not $ok) {
        $preview = if ($content -is [string]) { $content.Trim() -replace '\s+',' ' } else { $content | ConvertTo-Json -Compress -Depth 3 }
        Write-Host "    -> $($preview.Substring(0,[Math]::Min(200,$preview.Length)))" -ForegroundColor Yellow
    }
    return $content
}

Write-Host "`n============================================" -ForegroundColor White
Write-Host "   TRAVEL AGENCY API  -  ENDPOINT TESTS   " -ForegroundColor White
Write-Host "============================================`n" -ForegroundColor White

# ── AUTH ─────────────────────────────────────────────────────────────────────
Write-Host "[ AUTH ]" -ForegroundColor Magenta

$userEmail = "e2e_$(Get-Random -Maximum 99999)@test.com"
$pwd = "Test@1234!"

# 1. Sign Up
Test-API "POST /api/v1/auth/sign-up (new user -> 201)" POST "/api/v1/auth/sign-up" `
    @{email=$userEmail;password=$pwd;firstName="Test";lastName="User";phoneNumber="+1234567890"} $null 201 | Out-Null

# 2. Sign In with cookie session capture
$loginParams = @{
    Uri="$BASE/api/v1/auth/sign-in"; Method="POST"
    Body=(@{email=$userEmail;password=$pwd}|ConvertTo-Json); ContentType="application/json"
    SessionVariable="cookieSession"; ErrorAction="Stop"
}
$userToken = $null; $refreshTokenCookie = $null
try {
    $loginResp = Invoke-WebRequest @loginParams
    $loginJson = $loginResp.Content | ConvertFrom-Json
    $userToken = $loginJson.idToken
    # Extract raw refreshToken cookie value from Set-Cookie header
    $setCookieHdr = $loginResp.Headers["Set-Cookie"]
    if ($setCookieHdr) { $refreshTokenCookie = ($setCookieHdr -split ";")[0] }
    Write-Host "  [PASS] POST /api/v1/auth/sign-in  (got 200, expected 200)" -ForegroundColor Green
    $pass++
} catch {
    Write-Host "  [FAIL] POST /api/v1/auth/sign-in  (error)" -ForegroundColor Red
    $fail++
}

# 3. Refresh — send cookie manually (HttpOnly cookie extracted from sign-in Set-Cookie header)
if ($refreshTokenCookie) {
    try {
        $rr = Invoke-WebRequest "http://localhost:8080/api/v1/auth/refresh" -Method POST -Headers @{Cookie=$refreshTokenCookie} -ErrorAction Stop
        Write-Host "  [PASS] POST /api/v1/auth/refresh (cookie -> 200)  (got 200, expected 200)" -ForegroundColor Green
        $pass++
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        Write-Host "  [FAIL] POST /api/v1/auth/refresh (cookie -> 200)  (got $code, expected 200)" -ForegroundColor Red
        $fail++
    }
} else {
    Write-Host "  [SKIP] POST /api/v1/auth/refresh - no cookie captured" -ForegroundColor DarkGray
}

# 4. Duplicate sign-up → 409
Test-API "POST /api/v1/auth/sign-up (duplicate -> 409)" POST "/api/v1/auth/sign-up" `
    @{email=$userEmail;password=$pwd;firstName="X";lastName="Y";phoneNumber="+1"} $null 409 | Out-Null

# 5. Wrong password → 400
Test-API "POST /api/v1/auth/sign-in (wrong password -> 400)" POST "/api/v1/auth/sign-in" `
    @{email=$userEmail;password="Wrong@9999!"} $null 400 | Out-Null

# ── TOURS ────────────────────────────────────────────────────────────────────
Write-Host "`n[ TOURS ]" -ForegroundColor Magenta

# 6. Available tours
$toursRes = Test-API "GET /api/v1/tours/available (-> 200)" GET "/api/v1/tours/available" $null $null 200
$tours = if ($toursRes.tours) { $toursRes.tours } elseif ($toursRes -is [array]) { $toursRes } else { @() }
Write-Host "    Found $($tours.Count) tours in DB" -ForegroundColor DarkGray

# 7. Destinations autocomplete
Test-API "GET /api/v1/tours/destinations?destination=Par (-> 200)" GET "/api/v1/tours/destinations?destination=Par" $null $null 200 | Out-Null

# 8. Get tour by ID
$tourId = $null; $instanceId = $null; $startDate = $null
if ($tours.Count -gt 0) {
    $tourId  = $tours[0].id
    $tourRes = Test-API "GET /api/v1/tours/$tourId (-> 200)" GET "/api/v1/tours/$tourId" $null $null 200
    $instances = $tourRes.availableDates
    if ($instances -and $instances.Count -gt 0) {
        $fut = $instances | Where-Object { $_.date -gt (Get-Date -Format "yyyy-MM-dd") } | Select-Object -First 1
        if ($fut) { $instanceId = $fut.id; $startDate = $fut.date }
    }
} else {
    Write-Host "  [INFO] No tours in DB — skipping tour detail" -ForegroundColor DarkGray
}

# 9. Non-existent tour → 404
Test-API "GET /api/v1/tours/nonexistent-id (-> 404)" GET "/api/v1/tours/nonexistent-id-xyz" $null $null 404 | Out-Null

# ── BOOKINGS ─────────────────────────────────────────────────────────────────
Write-Host "`n[ BOOKINGS ]" -ForegroundColor Magenta

# 10. No auth → 401
Test-API "GET /api/v1/bookings (no token -> 401)" GET "/api/v1/bookings" $null $null 401 | Out-Null

$bookingId = $null
if ($userToken) {
    # 11. List bookings (authenticated)
        # Decode userId from JWT
    $p = $userToken.Split('.')[1]; $pad = $p.PadRight($p.Length+(4-$p.Length%4)%4,'='); $userId = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad))|ConvertFrom-Json).sub
    # Decode userId from JWT sub claim
    $p = $userToken.Split(".")[1]; $pad = $p.PadRight($p.Length+(4-$p.Length%4)%4,"="); $userId = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad))|ConvertFrom-Json).sub
    Test-API "GET /api/v1/bookings (authenticated -> 200)" GET "/api/v1/bookings?userId=$userId" $null $userToken 200 | Out-Null

    if ($tourId -and $instanceId -and $startDate) {
        # 12. Create booking
        $bookBody = @{
            tourId=$tourId; tourInstanceId=$instanceId; startDate=$startDate
            duration="7 nights"; mealPlan="BB"
            guests=@{adults=2;children=0}
            customerDetails=@{firstName="Test";lastName="User";email=$userEmail;phone="+1234567890"}
        }
        $createRes = Test-API "POST /api/v1/bookings (create -> 201)" POST "/api/v1/bookings" $bookBody $userToken 201
        $bookingId = if ($createRes.bookingId) { $createRes.bookingId } elseif ($createRes.id) { $createRes.id } else { $null }

        if ($bookingId) {
            # 13. Cancel booking
            Test-API "DELETE /api/v1/bookings/$bookingId (cancel -> 200)" DELETE "/api/v1/bookings/$bookingId" $null $userToken 200 | Out-Null
        } else {
            Write-Host "  [INFO] No bookingId returned — skipping cancel" -ForegroundColor DarkGray
        }
    } else {
        Write-Host "  [SKIP] No tour data — skipping create/cancel" -ForegroundColor DarkGray
    }
} else {
    Write-Host "  [SKIP] No token — skipping authenticated booking tests" -ForegroundColor DarkGray
}

# ── TRAVEL AGENT ─────────────────────────────────────────────────────────────
Write-Host "`n[ TRAVEL AGENT ]" -ForegroundColor Magenta

# 14. No token → 401
Test-API "GET /api/v1/travel-agent/bookings (no token -> 401)" GET "/api/v1/travel-agent/bookings" $null $null 401 | Out-Null

# 15. CUSTOMER token → 403
if ($userToken) {
    Test-API "GET /api/v1/travel-agent/bookings (CUSTOMER -> 403)" GET "/api/v1/travel-agent/bookings" $null $userToken 403 | Out-Null
}

# ── SWAGGER ──────────────────────────────────────────────────────────────────
Write-Host "`n[ SWAGGER / DOCS ]" -ForegroundColor Magenta
Test-API "GET /swagger-ui.html (-> 200)"   GET "/swagger-ui.html" $null $null 200 | Out-Null
Test-API "GET /v3/api-docs (-> 200)"       GET "/v3/api-docs"     $null $null 200 | Out-Null

# ── LOGOUT ───────────────────────────────────────────────────────────────────
Write-Host "`n[ LOGOUT ]" -ForegroundColor Magenta
if ($userToken) {
    Test-API "POST /api/v1/auth/logout (-> 200)" POST "/api/v1/auth/logout" @{} $userToken 200 | Out-Null
    Test-API "GET /api/v1/bookings (post-logout -> 401)" GET "/api/v1/bookings?userId=any" $null $userToken 401 | Out-Null
}

# ── SUMMARY ──────────────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor White
$color = if ($fail -eq 0) { "Green" } else { "Yellow" }
Write-Host "  PASSED: $pass   FAILED: $fail" -ForegroundColor $color
Write-Host "============================================`n" -ForegroundColor White
if ($fail -gt 0) { exit 1 }

