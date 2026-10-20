<#
  Stores the backend's environment variables in SSM Parameter Store (SecureString)
  under /ardhana/prod/env/<NAME>. The EC2 instance loads them on every service start.

  Run it yourself in a terminal - it prompts for each value with hidden input.
  Press Enter on a prompt to keep the existing value (or skip it if none exists).
  DB_PASSWORD and JWT_SECRET are generated automatically the first time.

  Usage:  .\set-secrets.ps1 [-Domain ardhana.in] [-FirebaseKeyFile C:\path\service-account.json]
#>
param(
    [string]$Domain = 'ardhana.in',
    [string]$FirebaseKeyFile
)
. "$PSScriptRoot\common.ps1"

$envPrefix = "$ParamPrefix/env"

# Generated once, never prompted. Changing DB_PASSWORD later also needs the RDS password changed.
foreach ($name in 'DB_PASSWORD', 'JWT_SECRET') {
    if (Test-ParamExists "$envPrefix/$name") { Write-Host "$name already set - keeping it." }
    else { Set-SecureParam "$envPrefix/$name" (New-RandomSecret 40); Write-Host "$name generated." }
}

# Non-secret defaults derived from the domain (overwritten each run so they track -Domain).
$defaults = [ordered]@{
    CORS_ALLOWED_ORIGINS = "https://$Domain,https://www.$Domain,https://localhost"   # https://localhost = Android app (ISS-08)
    FRONTEND_URL         = "https://$Domain"
}
foreach ($k in $defaults.Keys) { Set-SecureParam "$envPrefix/$k" $defaults[$k]; Write-Host "$k = $($defaults[$k])" }

# Prompted values. Names match application.properties.
$prompted = @(
    'RAZORPAY_KEY_ID', 'RAZORPAY_KEY_SECRET', 'RAZORPAY_WEBHOOK_SECRET',
    'RAZORPAYX_KEY_ID', 'RAZORPAYX_KEY_SECRET', 'RAZORPAYX_ACCOUNT_NUMBER',
    'MSG91_AUTH_KEY', 'MSG91_OTP_TEMPLATE_ID',
    'MAIL_HOST', 'MAIL_PORT', 'MAIL_USERNAME', 'MAIL_PASSWORD', 'MAIL_FROM'
)
foreach ($name in $prompted) {
    $exists = Test-ParamExists "$envPrefix/$name"
    $hint = if ($exists) { 'Enter = keep current' } else { 'Enter = skip' }
    $secure = Read-Host "$name ($hint)" -AsSecureString
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    $plain = $plain.Trim()
    if ($plain -ieq 'skip') { $plain = '' }   # people type "skip" at a prompt that says "Enter = skip"
    if ($plain) { Set-SecureParam "$envPrefix/$name" $plain; Write-Host "  saved." }
    else { Write-Host "  skipped." }
}

if ($FirebaseKeyFile) {
    $json = Get-Content -Raw $FirebaseKeyFile
    $null = $json | ConvertFrom-Json   # fail fast on a wrong file
    Set-SecureParam "$ParamPrefix/firebase-service-account-json" $json
    Write-Host "Firebase service-account key stored (push notifications will be enabled)."
}

Write-Host "`nDone. Restart the backend to pick up changes: .\deploy-backend.ps1 -RestartOnly"
