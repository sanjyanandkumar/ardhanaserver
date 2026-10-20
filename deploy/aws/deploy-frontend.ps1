<#
  Builds dear-digital-space and publishes dist/ to the S3 site bucket behind CloudFront.

  The build reads VITE_* values from dear-digital-space/.env.production (which points at
  https://ardhana.in). Before DNS is switched to CloudFront, pass -ApiHost to build against
  the CloudFront URL instead so you can test end to end:

    .\deploy-frontend.ps1 -ApiHost https://dxxxxxxxx.cloudfront.net
    .\deploy-frontend.ps1                     # normal build once ardhana.in points at CloudFront
#>
param([string]$ApiHost, [switch]$SkipBuild)
. "$PSScriptRoot\common.ps1"

$out = Get-StackOutputs

if (-not $SkipBuild) {
    Push-Location $FrontendDir
    try {
        if ($ApiHost) {
            # Process env vars take priority over .env files in Vite.
            $ApiHost = $ApiHost.TrimEnd('/')
            $env:VITE_ARDHANA_API_URL = "$ApiHost/"
            $env:VITE_API_BASE_URL = "$ApiHost/api"
            Write-Host "Building against $ApiHost"
        }
        $ErrorActionPreference = 'Continue'   # npm writes warnings to stderr; exit codes are checked below
        & npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw 'npm run build failed' }
    } finally {
        Remove-Item Env:VITE_ARDHANA_API_URL, Env:VITE_API_BASE_URL -ErrorAction SilentlyContinue
        $ErrorActionPreference = 'Stop'
        Pop-Location
    }
}

$dist = Join-Path $FrontendDir 'dist'
$bucket = "s3://$($out.SiteBucket)"

# Hashed assets: cache for a year. index.html and everything else: always revalidate.
Invoke-Aws s3 sync "$dist\assets" "$bucket/assets" --delete --cache-control 'public,max-age=31536000,immutable' --only-show-errors | Out-Null
Invoke-Aws s3 sync $dist $bucket --delete --exclude 'assets/*' --cache-control 'no-cache' --only-show-errors | Out-Null

Invoke-Aws cloudfront create-invalidation --distribution-id $out.DistributionId --paths '/*' --query 'Invalidation.Id' --output text | Out-Null
Write-Host "Frontend published. https://$($out.DistributionDomain)"
