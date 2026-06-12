# Quick-Start: Forgot / Reset Password Feature

## What was built

Three new public endpoints (no JWT required):

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/auth/forgot-password` | Send a 6-digit OTP to the user's email |
| `POST` | `/api/v1/auth/verify-code` | Validate the OTP (does NOT consume it) |
| `POST` | `/api/v1/auth/reset-password` | Validate the OTP + set new password |

---

## Prerequisites checklist

### 1. AWS Account & SES setup

#### a) Verify your sender email in SES
1. Go to **AWS Console → SES → Verified identities**
2. Click **Create identity → Email address**
3. Enter the email you want to send FROM (e.g. `no-reply@yourdomain.com`)
4. Click the verification link AWS sends to that address

#### b) Verify recipient emails (sandbox mode only)
By default, your SES account is in **sandbox mode**, which means you can only
send to pre-verified email addresses.

- Either verify every tester's email the same way as step (a)
- **OR** request production access:
  AWS Console → SES → Account dashboard → Request production access

#### c) Create an IAM user / policy for SES

Create an IAM policy with the minimum required permission:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ses:SendEmail",
      "Resource": "*"
    }
  ]
}
```

Attach it to an IAM user and generate **Access Key ID** + **Secret Access Key**.

---

### 2. Required environment variables

| Variable | Example | Notes |
|----------|---------|-------|
| `AWS_ACCESS_KEY_ID` | `AKIA...` | IAM user key |
| `AWS_SECRET_ACCESS_KEY` | `wJ...` | IAM user secret |
| `AWS_SES_REGION` | `us-east-1` | SES region where sender is verified |
| `AWS_SES_SENDER_EMAIL` | `no-reply@yourdomain.com` | Must be verified in SES |

Existing variables (unchanged):

| Variable | Notes |
|----------|-------|
| `MONGODB_URI` | e.g. `mongodb://localhost:27017/authdb` |
| `JWT_PRIVATE_KEY` | RSA private key (PEM) |
| `JWT_PUBLIC_KEY` | RSA public key (PEM) |

---

## Running locally

```powershell
# 1. Build (skip tests to avoid needing a running MongoDB/AWS)
mvn package -DskipTests

# 2. Set env vars in PowerShell
$env:AWS_ACCESS_KEY_ID     = "AKIA..."
$env:AWS_SECRET_ACCESS_KEY = "wJ..."
$env:AWS_SES_REGION        = "us-east-1"
$env:AWS_SES_SENDER_EMAIL  = "no-reply@yourdomain.com"
$env:MONGODB_URI           = "mongodb://localhost:27017/authdb"
$env:JWT_PRIVATE_KEY       = "-----BEGIN RSA PRIVATE KEY-----..."
$env:JWT_PUBLIC_KEY        = "-----BEGIN PUBLIC KEY-----..."

# 3. Run
.\run.ps1
```

Or with Maven directly:

```powershell
mvn spring-boot:run `
  -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED" `
  -Dspring-boot.run.environmentVariables="AWS_ACCESS_KEY_ID=AKIA...,AWS_SECRET_ACCESS_KEY=wJ...,AWS_SES_REGION=us-east-1,AWS_SES_SENDER_EMAIL=no-reply@yourdomain.com"
```

---

## Kubernetes / Helm deployment

Create a Kubernetes Secret for the SES credentials:

```bash
kubectl create secret generic api-handler-ses-secret \
  --from-literal=AWS_ACCESS_KEY_ID=AKIA... \
  --from-literal=AWS_SECRET_ACCESS_KEY=wJ... \
  --from-literal=AWS_SES_REGION=us-east-1 \
  --from-literal=AWS_SES_SENDER_EMAIL=no-reply@yourdomain.com \
  -n <your-namespace>
```

The `deploy-templates/values.yaml` already references `api-handler-ses-secret` via `envFrom`.

> **Tip (EKS):** Instead of long-lived IAM keys, attach an IAM Role to the pod's
> Service Account (IRSA). The AWS SDK picks it up automatically via the Default
> Credential Provider Chain — no key/secret needed.

---

## MongoDB TTL index (automatic)

`spring.data.mongodb.auto-index-creation=true` is already set.
Spring Data MongoDB creates the TTL index on `password_reset_tokens.expiresAt`
at startup automatically. No manual action required.

---

## Testing the flow

### Step 1 – Request OTP

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'
```

Expected response (always 200, even for unknown emails):
```json
{ "message": "If this email exists, a reset link has been sent." }
```

Check the inbox of the registered email for a 6-digit code.

---

### Step 2 – Verify OTP

```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-code \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "verificationCode": "123456"}'
```

Success (200):
```json
{ "message": "Verification code validated successfully." }
```

Failure (400):
```json
{ "error": "Invalid or expired verification code." }
```

---

### Step 3 – Reset Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "verificationCode": "123456", "newPassword": "NewPass1!"}'
```

Success (200):
```json
{ "message": "Password reset successfully." }
```

Password rules (existing policy):
- 8–16 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (`!@#$%^&*` etc.)

---

## Security properties

| Property | Implementation |
|----------|---------------|
| Account enumeration prevention | `forgot-password` always returns 200 |
| OTP never stored in plain text | SHA-256 hash stored in MongoDB |
| OTP expires in 15 minutes | Enforced in code + MongoDB TTL auto-purge |
| Replay prevention | Token marked `used=true` after reset |
| Old OTPs invalidated | `deleteByEmail` called before issuing a new code |
| Password BCrypt hashed | Strength 12 (existing `BCryptPasswordEncoder`) |

