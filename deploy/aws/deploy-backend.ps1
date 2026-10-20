<#
  Builds the Spring Boot jar, uploads it to the artifacts bucket and swaps it in on the EC2
  instance via SSM. The previous jar is kept as app.jar.prev for a quick rollback.

  Usage:
    .\deploy-backend.ps1               # build + deploy
    .\deploy-backend.ps1 -SkipBuild    # deploy the jar already in target/
    .\deploy-backend.ps1 -RestartOnly  # just restart (e.g. after set-secrets.ps1)
    .\deploy-backend.ps1 -Rollback     # restore app.jar.prev and restart
#>
param([switch]$SkipBuild, [switch]$RestartOnly, [switch]$Rollback)
. "$PSScriptRoot\common.ps1"

$out = Get-StackOutputs
$instance = $out.InstanceId

# Waits up to ~2 min for the app to answer on its port, then prints recent logs either way.
$healthCheck = @(
    'for i in $(seq 1 60); do',
    '  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/api/courses | grep -q "^[2-4]"; then echo "Backend is up."; ok=1; break; fi',
    '  sleep 2',
    'done',
    'journalctl -u ardhana -n 25 --no-pager',
    'if [ -z "${ok:-}" ]; then echo "Backend did not come up in time"; exit 1; fi'
)

if ($Rollback) {
    Invoke-OnInstance $instance -Comment 'ardhana rollback' -Commands (@(
        'set -eu',
        'test -f /opt/ardhana/app.jar.prev',
        'mv /opt/ardhana/app.jar.prev /opt/ardhana/app.jar',
        'systemctl restart ardhana'
    ) + $healthCheck)
    return
}

if ($RestartOnly) {
    Invoke-OnInstance $instance -Comment 'ardhana restart' -Commands (@('set -eu', 'systemctl restart ardhana') + $healthCheck)
    return
}

if (-not $SkipBuild) {
    Push-Location $RepoRoot
    try {
        $ErrorActionPreference = 'Continue'   # Maven may warn on stderr; exit code checked below
        & .\mvnw.cmd -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
    } finally { $ErrorActionPreference = 'Stop'; Pop-Location }
}

$jar = Get-ChildItem "$RepoRoot\target\*.jar" | Where-Object { $_.Name -notlike '*.original' -and $_.Name -notlike '*-plain.jar' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'No jar found in target/' }

$key = "backend/releases/$(Get-Date -Format 'yyyyMMdd-HHmmss')-$($jar.Name)"
Write-Host "Uploading $($jar.Name) ($([math]::Round($jar.Length / 1MB, 1)) MB) -> s3://$($out.ArtifactsBucket)/$key"
Invoke-Aws s3 cp $jar.FullName "s3://$($out.ArtifactsBucket)/$key" --only-show-errors | Out-Null

Invoke-OnInstance $instance -Comment 'ardhana deploy' -Commands (@(
    'set -eu',
    '. /etc/ardhana/infra.env',
    "aws s3 cp s3://$($out.ArtifactsBucket)/$key /opt/ardhana/app.jar.new --region `$AWS_REGION --only-show-errors",
    'if [ -f /opt/ardhana/app.jar ]; then cp /opt/ardhana/app.jar /opt/ardhana/app.jar.prev; fi',
    'mv /opt/ardhana/app.jar.new /opt/ardhana/app.jar',
    'chown ardhana:ardhana /opt/ardhana/app.jar',
    'systemctl restart ardhana'
) + $healthCheck)
