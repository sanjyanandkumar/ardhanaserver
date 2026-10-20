<#
  Requests the TLS certificate CloudFront needs (CloudFront only accepts ACM certs from us-east-1)
  and prints the DNS validation records to add at your domain registrar.

  Usage:  .\request-certificate.ps1 [-Domain ardhana.in]
  Re-run it any time to see the status / records again; it reuses an existing request.
#>
param([string]$Domain = 'ardhana.in')
. "$PSScriptRoot\common.ps1"

$certRegion = 'us-east-1'
$arn = & aws --region $certRegion acm list-certificates --certificate-statuses PENDING_VALIDATION ISSUED `
    --query "CertificateSummaryList[?DomainName=='$Domain'].CertificateArn | [0]" --output text
if ($LASTEXITCODE -ne 0) { throw 'acm list-certificates failed' }

if (-not $arn -or $arn -eq 'None') {
    $arn = & aws --region $certRegion acm request-certificate --domain-name $Domain `
        --subject-alternative-names "www.$Domain" --validation-method DNS --query CertificateArn --output text
    if ($LASTEXITCODE -ne 0) { throw 'acm request-certificate failed' }
    Write-Host "Requested certificate $arn"
    Start-Sleep -Seconds 10   # validation records take a few seconds to appear
}

$cert = & aws --region $certRegion acm describe-certificate --certificate-arn $arn --output json | Out-String | ConvertFrom-Json
Write-Host "`nCertificate: $arn"
Write-Host "Status:      $($cert.Certificate.Status)`n"
Write-Host "Add these CNAME records at your DNS provider:"
$cert.Certificate.DomainValidationOptions | ForEach-Object { $_.ResourceRecord } | Sort-Object Name -Unique |
    Format-Table @{n='Name';e={$_.Name}}, @{n='Type';e={$_.Type}}, @{n='Value';e={$_.Value}} -AutoSize -Wrap
Write-Host "When Status is ISSUED, pass the ARN to deploy-infra.ps1 -CertificateArn."
