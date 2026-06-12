$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$base = "http://localhost:8080"
$agentEmail = "sarahjohnson@travelagency.com"
$agentPassword = "SecurePass123!"

function Req([string]$method, [string]$url, $body=$null, $headers=$null) {
  try {
    if ($body -ne $null -and $headers -ne $null) {
      $r = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -ContentType "application/json" -Body $body -ErrorAction Stop
    } elseif ($body -ne $null) {
      $r = Invoke-WebRequest -Uri $url -Method $method -ContentType "application/json" -Body $body -ErrorAction Stop
    } elseif ($headers -ne $null) {
      $r = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -ErrorAction Stop
    } else {
      $r = Invoke-WebRequest -Uri $url -Method $method -ErrorAction Stop
    }
    return @{ StatusCode=[int]$r.StatusCode; Content=$r.Content }
  } catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $status = [int]$_.Exception.Response.StatusCode
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $content = $reader.ReadToEnd()
      $reader.Dispose()
      return @{ StatusCode=$status; Content=$content }
    }
    throw
  }
}

$signupBody = @{email=$agentEmail;password=$agentPassword;firstName="Sarah";lastName="Johnson";phone="+1-212-555-0134"} | ConvertTo-Json
$signup = Req "POST" "$base/api/v1/auth/sign-up" $signupBody
"SIGNUP_STATUS=$($signup.StatusCode)"

$signinBody = @{email=$agentEmail;password=$agentPassword} | ConvertTo-Json
$signin = Req "POST" "$base/api/v1/auth/sign-in" $signinBody
"SIGNIN_STATUS=$($signin.StatusCode)"
if ($signin.StatusCode -ne 200) { $signin.Content; exit 1 }

$signinJson = $signin.Content | ConvertFrom-Json
$token = $signinJson.idToken
"AGENT_ROLE=$($signinJson.role)"
$h = @{ Authorization = "Bearer $token" }

$r1 = Req "GET" "$base/api/v1/travel-agent/bookings" $null $h
"GET_AGENT_BOOKINGS_STATUS=$($r1.StatusCode)"

$bookingId = $null
try {
  $obj = $r1.Content | ConvertFrom-Json
  if ($obj.bookings -and $obj.bookings.Count -gt 0) {
    $bookingId = if ($obj.bookings[0].bookingId) { $obj.bookings[0].bookingId } else { $obj.bookings[0].id }
  }
} catch {}
"AGENT_BOOKING_ID=$(if($bookingId){$bookingId}else{"NONE"})"

if (-not $bookingId) {
  $customerEmail = "agentflow_$(Get-Random -Maximum 999999)@example.com"
  $customerPass = "Test@1234"

  $csu = @{email=$customerEmail;password=$customerPass;firstName="Flow";lastName="Customer";phone="+1-555-0101"} | ConvertTo-Json
  $csuRes = Req "POST" "$base/api/v1/auth/sign-up" $csu
  "CUSTOMER_SIGNUP_STATUS=$($csuRes.StatusCode)"

  $csi = @{email=$customerEmail;password=$customerPass} | ConvertTo-Json
  $csiRes = Req "POST" "$base/api/v1/auth/sign-in" $csi
  "CUSTOMER_SIGNIN_STATUS=$($csiRes.StatusCode)"

  if ($csiRes.StatusCode -eq 200) {
    $cjson = $csiRes.Content | ConvertFrom-Json
    $ctoken = $cjson.idToken
    $parts = $ctoken.Split('.')
    $pad = $parts[1].PadRight($parts[1].Length + (4 - $parts[1].Length % 4) % 4, '=')
    $claims = ([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($pad)) | ConvertFrom-Json)
    $userId = $claims.sub
    $ch = @{ Authorization = "Bearer $ctoken" }

    $tours = Req "GET" "$base/api/v1/tours/available"
    if ($tours.StatusCode -eq 200) {
      $tobj = $tours.Content | ConvertFrom-Json
      $tour = $tobj.tours | Select-Object -First 1
      if ($tour) {
        $tourInstanceId = $null
        if ($tour.tourInstanceId) { $tourInstanceId = $tour.tourInstanceId }
        if (-not $tourInstanceId -and $tour.id) {
          $tourDetail = Req "GET" "$base/api/v1/tours/$($tour.id)"
          if ($tourDetail.StatusCode -eq 200) {
            $td = $tourDetail.Content | ConvertFrom-Json
            if ($td.availableDates -and $td.availableDates.Count -gt 0) {
              $tourInstanceId = $td.availableDates[0].id
            }
          }
        }

        if ($tourInstanceId) {
          $createObj = @{
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
          }
          $create = $createObj | ConvertTo-Json -Depth 8
          $bRes = Req "POST" "$base/api/v1/bookings" $create $ch
          "CREATE_BOOKING_STATUS=$($bRes.StatusCode)"
        } else {
          "CREATE_BOOKING_STATUS=SKIPPED_NO_TOUR_INSTANCE"
        }
      }
    }

    $r1 = Req "GET" "$base/api/v1/travel-agent/bookings" $null $h
    "GET_AGENT_BOOKINGS_AFTER_CREATE_STATUS=$($r1.StatusCode)"
    try {
      $obj = $r1.Content | ConvertFrom-Json
      if ($obj.bookings -and $obj.bookings.Count -gt 0) {
        $bookingId = if ($obj.bookings[0].bookingId) { $obj.bookings[0].bookingId } else { $obj.bookings[0].id }
      }
    } catch {}
    "AGENT_BOOKING_ID_AFTER_CREATE=$(if($bookingId){$bookingId}else{"NONE"})"
  }
}

if ($bookingId) {
  $r2 = Req "GET" "$base/api/v1/travel-agent/bookings/$bookingId" $null $h
  "GET_AGENT_BOOKING_BY_ID_STATUS=$($r2.StatusCode)"

  $approveBody = @{approvalMode="OFFLINE";approvalGiven=$true;approvalNote="Approved by customer"} | ConvertTo-Json
  $r3 = Req "PATCH" "$base/api/v1/bookings/$bookingId/customer-approval" $approveBody $h
  "PATCH_CUSTOMER_APPROVAL_STATUS=$($r3.StatusCode)"

  $editBody = @{adults=1;children=0;mealPlan="HB";duration=7} | ConvertTo-Json
  $r4 = Req "PATCH" "$base/api/v1/bookings/$bookingId/edit" $editBody $h
  "PATCH_EDIT_BOOKING_STATUS=$($r4.StatusCode)"

  $r5 = Req "POST" "$base/api/v1/bookings/$bookingId/confirm" "{}" $h
  "POST_CONFIRM_BOOKING_STATUS=$($r5.StatusCode)"

  $cancelBody = @{reason="CUSTOMERS_EMERGENCY";reasonNote="Test run"} | ConvertTo-Json
  $r6 = Req "DELETE" "$base/api/v1/bookings/$bookingId/cancel" $cancelBody $h
  "DELETE_CANCEL_BOOKING_STATUS=$($r6.StatusCode)"
} else {
  "SKIP_DETAIL_AND_MUTATION_ENDPOINTS=NO_AGENT_BOOKING_FOUND"
}
