$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$base = "http://localhost:8081"
$agentEmail = "james.obrien@travelagency.com"
$agentPassword = "SecurePass123!"

function Invoke-CurlJson {
    param(
        [string]$Method,
        [string]$Url,
        [string]$JsonBody,
        [string]$Token
    )

    $tmp = Join-Path $PSScriptRoot "tmp-curl-body.json"
    if (Test-Path $tmp) { Remove-Item $tmp -Force }

    $args = @("-s", "-o", $tmp, "-w", "%{http_code}", "-X", $Method, $Url, "-H", "Content-Type: application/json")
    if ($Token) {
        $args += @("-H", "Authorization: Bearer $Token")
    }
    if ($null -ne $JsonBody) {
        $args += @("--data-raw", $JsonBody)
    }

    $codeRaw = & curl.exe @args
    $code = [int]$codeRaw
    $body = if (Test-Path $tmp) { Get-Content $tmp -Raw } else { "" }
    return [PSCustomObject]@{ Code = $code; Body = $body }
}

$signupJson = (@{
    email = $agentEmail
    password = $agentPassword
    firstName = "Marcus"
    lastName = "Rivera"
} | ConvertTo-Json -Compress)

$signup = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/auth/sign-up" -JsonBody $signupJson -Token $null
Write-Output "SIGNUP_STATUS=$($signup.Code)"
Write-Output $signup.Body

$signinJson = (@{
    email = $agentEmail
    password = $agentPassword
} | ConvertTo-Json -Compress)

$signin = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/auth/sign-in" -JsonBody $signinJson -Token $null
Write-Output "SIGNIN_STATUS=$($signin.Code)"
Write-Output $signin.Body
if ($signin.Code -ne 200) {
    exit 1
}

$auth = $signin.Body | ConvertFrom-Json
$token = $auth.idToken
Write-Output "AGENT_ROLE=$($auth.role)"

$getList = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/travel-agent/bookings" -JsonBody $null -Token $token
Write-Output "GET_AGENT_BOOKINGS_STATUS=$($getList.Code)"
Write-Output $getList.Body

$bookingId = $null
try {
    $listObj = $getList.Body | ConvertFrom-Json
    if ($listObj.bookings -and $listObj.bookings.Count -gt 0) {
        if ($listObj.bookings[0].bookingId) { $bookingId = $listObj.bookings[0].bookingId }
        elseif ($listObj.bookings[0].id) { $bookingId = $listObj.bookings[0].id }
    }
} catch {}
Write-Output "AGENT_BOOKING_ID=$(if ($bookingId) { $bookingId } else { 'NONE' })"

if (-not $bookingId) {
    $customerEmail = "agentflow_$(Get-Random -Maximum 999999)@example.com"
    $customerPass = "Test@1234"

    $customerSignupJson = (@{
        email = $customerEmail
        password = $customerPass
        firstName = "Flow"
        lastName = "Customer"
    } | ConvertTo-Json -Compress)

    $customerSignup = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/auth/sign-up" -JsonBody $customerSignupJson -Token $null
    Write-Output "CUSTOMER_SIGNUP_STATUS=$($customerSignup.Code)"

    $customerSigninJson = (@{ email = $customerEmail; password = $customerPass } | ConvertTo-Json -Compress)
    $customerSignin = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/auth/sign-in" -JsonBody $customerSigninJson -Token $null
    Write-Output "CUSTOMER_SIGNIN_STATUS=$($customerSignin.Code)"

    if ($customerSignin.Code -eq 200) {
        $customerAuth = $customerSignin.Body | ConvertFrom-Json
        $customerToken = $customerAuth.idToken

        $parts = $customerToken.Split('.')
        $pad = $parts[1].PadRight($parts[1].Length + (4 - $parts[1].Length % 4) % 4, '=')
        $claims = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad)) | ConvertFrom-Json)
        $userId = $claims.sub

        $tours = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/tours/available" -JsonBody $null -Token $null
        if ($tours.Code -eq 200) {
            $toursObj = $tours.Body | ConvertFrom-Json
            $tour = $toursObj.tours | Select-Object -First 1
            if ($tour) {
                $tourDetail = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/tours/$($tour.id)" -JsonBody $null -Token $null
                $tourInstanceId = $null
                if ($tourDetail.Code -eq 200) {
                    $td = $tourDetail.Body | ConvertFrom-Json
                    if ($td.availableDates -and $td.availableDates.Count -gt 0) {
                        $tourInstanceId = $td.availableDates[0].id
                    }
                }

                if ($tourInstanceId) {
                    $bookingJson = (@{
                        tourId = $tour.id
                        userId = $userId
                        tourInstanceId = $tourInstanceId
                        mealPlan = "BB"
                        adults = 1
                        children = 0
                        leadTraveler = @{
                            firstName = "Flow"
                            lastName = "Customer"
                            email = $customerEmail
                            phone = "+1-555-0101"
                            dateOfBirth = "1990-01-01"
                            passportNumber = "P1234567"
                        }
                        additionalTravelers = @()
                    } | ConvertTo-Json -Depth 8 -Compress)

                    $createBooking = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/bookings" -JsonBody $bookingJson -Token $customerToken
                    Write-Output "CREATE_BOOKING_STATUS=$($createBooking.Code)"
                    Write-Output $createBooking.Body
                } else {
                    Write-Output "CREATE_BOOKING_STATUS=SKIPPED_NO_TOUR_INSTANCE"
                }
            }
        }

        $getListAfter = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/travel-agent/bookings" -JsonBody $null -Token $token
        Write-Output "GET_AGENT_BOOKINGS_AFTER_CREATE_STATUS=$($getListAfter.Code)"
        Write-Output $getListAfter.Body

        try {
            $obj2 = $getListAfter.Body | ConvertFrom-Json
            if ($obj2.bookings -and $obj2.bookings.Count -gt 0) {
                if ($obj2.bookings[0].bookingId) { $bookingId = $obj2.bookings[0].bookingId }
                elseif ($obj2.bookings[0].id) { $bookingId = $obj2.bookings[0].id }
            }
        } catch {}
    }
}

Write-Output "AGENT_BOOKING_ID_AFTER_CREATE=$(if ($bookingId) { $bookingId } else { 'NONE' })"

if ($bookingId) {
    $detail = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/travel-agent/bookings/$bookingId" -JsonBody $null -Token $token
    Write-Output "GET_AGENT_BOOKING_BY_ID_STATUS=$($detail.Code)"

    $approvalJson = (@{
        approvalMode = "OFFLINE"
        approvalGiven = $true
        approvalNote = "Approved"
    } | ConvertTo-Json -Compress)
    $approval = Invoke-CurlJson -Method "PATCH" -Url "$base/api/v1/bookings/$bookingId/customer-approval" -JsonBody $approvalJson -Token $token
    Write-Output "PATCH_CUSTOMER_APPROVAL_STATUS=$($approval.Code)"

    $editJson = (@{
        adults = 1
        children = 0
        mealPlan = "HB"
        duration = 7
    } | ConvertTo-Json -Compress)
    $edit = Invoke-CurlJson -Method "PATCH" -Url "$base/api/v1/bookings/$bookingId/edit" -JsonBody $editJson -Token $token
    Write-Output "PATCH_EDIT_BOOKING_STATUS=$($edit.Code)"

    $confirm = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/bookings/$bookingId/confirm" -JsonBody "{}" -Token $token
    Write-Output "POST_CONFIRM_BOOKING_STATUS=$($confirm.Code)"

    $cancelJson = (@{
        reason = "CUSTOMERS_EMERGENCY"
        reasonNote = "test"
    } | ConvertTo-Json -Compress)
    $cancel = Invoke-CurlJson -Method "DELETE" -Url "$base/api/v1/bookings/$bookingId/cancel" -JsonBody $cancelJson -Token $token
    Write-Output "DELETE_CANCEL_BOOKING_STATUS=$($cancel.Code)"
} else {
    Write-Output "SKIP_DETAIL_AND_MUTATION_ENDPOINTS=NO_AGENT_BOOKING_FOUND"
    $placeholderBookingId = "nonexistent-booking-id"
    $detail = Invoke-CurlJson -Method "GET" -Url "$base/api/v1/travel-agent/bookings/$placeholderBookingId" -JsonBody $null -Token $token
    Write-Output "GET_AGENT_BOOKING_BY_ID_STATUS=$($detail.Code)"

    $approvalJson = (@{
        approvalMode = "OFFLINE"
        approvalGiven = $true
        approvalNote = "Approved"
    } | ConvertTo-Json -Compress)
    $approval = Invoke-CurlJson -Method "PATCH" -Url "$base/api/v1/bookings/$placeholderBookingId/customer-approval" -JsonBody $approvalJson -Token $token
    Write-Output "PATCH_CUSTOMER_APPROVAL_STATUS=$($approval.Code)"

    $editJson = (@{
        adults = 1
        children = 0
        mealPlan = "HB"
        duration = 7
    } | ConvertTo-Json -Compress)
    $edit = Invoke-CurlJson -Method "PATCH" -Url "$base/api/v1/bookings/$placeholderBookingId/edit" -JsonBody $editJson -Token $token
    Write-Output "PATCH_EDIT_BOOKING_STATUS=$($edit.Code)"

    $confirm = Invoke-CurlJson -Method "POST" -Url "$base/api/v1/bookings/$placeholderBookingId/confirm" -JsonBody "{}" -Token $token
    Write-Output "POST_CONFIRM_BOOKING_STATUS=$($confirm.Code)"

    $cancelJson = (@{
        reason = "CUSTOMERS_EMERGENCY"
        reasonNote = "test"
    } | ConvertTo-Json -Compress)
    $cancel = Invoke-CurlJson -Method "DELETE" -Url "$base/api/v1/bookings/$placeholderBookingId/cancel" -JsonBody $cancelJson -Token $token
    Write-Output "DELETE_CANCEL_BOOKING_STATUS=$($cancel.Code)"

    $verifyJson = (@{
        documentId = "doc-1"
        action = "APPROVE"
        note = "ok"
    } | ConvertTo-Json -Compress)
    $verify = Invoke-CurlJson -Method "PATCH" -Url "$base/api/v1/bookings/$placeholderBookingId/documents/verify" -JsonBody $verifyJson -Token $token
    Write-Output "PATCH_DOCUMENT_VERIFY_STATUS=$($verify.Code)"
}
