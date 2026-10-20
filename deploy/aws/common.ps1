# Shared settings and helpers for the deploy/aws scripts. Dot-source it: . "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Stop'

# The AWS CLI (Python) crashes with "'charmap' codec can't encode character" when remote output
# (e.g. journald logs) contains non-ASCII text and Windows' default code page is in use.
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$Region      = 'ap-south-1'
$StackName   = 'ardhana-prod'
$ParamPrefix = '/ardhana/prod'
$RepoRoot    = (Resolve-Path "$PSScriptRoot\..\..").Path                      # ardhana (backend repo)
$FrontendDir = (Resolve-Path "$PSScriptRoot\..\..\..\dear-digital-space").Path

# PowerShell parses an unquoted a=1,b=2 argument to a function as an array; the CLI needs the
# original comma-joined string back.
function ConvertTo-CliArgs($ArgList) {
    foreach ($a in $ArgList) { if ($a -is [array]) { $a -join ',' } else { $a } }
}

# Runs the AWS CLI and throws on a non-zero exit (PowerShell 5.1 does not).
function Invoke-Aws {
    # 'Continue' while the CLI runs so warnings on stderr are shown, not fatal (PowerShell 5.1);
    # real failures are caught by the exit code below.
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $out = & aws --region $Region @(ConvertTo-CliArgs $args) }
    finally { $ErrorActionPreference = $prev }
    if ($LASTEXITCODE -ne 0) { throw "aws $($args -join ' ') failed (exit $LASTEXITCODE)" }
    return $out
}

# Writes JSON to a temp file without a BOM and returns a file:// URI for --cli-input-json.
# Used instead of inline JSON args because PowerShell 5.1 mangles quotes passed to native exes,
# and so secret values never appear on a command line.
function New-CliInputFile($Object) {
    $path = Join-Path ([IO.Path]::GetTempPath()) ("ardhana-" + [guid]::NewGuid() + ".json")
    [IO.File]::WriteAllText($path, ($Object | ConvertTo-Json -Depth 10), (New-Object Text.UTF8Encoding $false))
    return $path
}

function Get-StackOutputs {
    $json = Invoke-Aws cloudformation describe-stacks --stack-name $StackName --query 'Stacks[0].Outputs' --output json
    $map = @{}
    foreach ($o in ($json | Out-String | ConvertFrom-Json)) { $map[$o.OutputKey] = $o.OutputValue }
    return $map
}

# Runs shell commands on the backend instance through SSM Run Command (no SSH), waits, prints output.
function Invoke-OnInstance([string]$InstanceId, [string[]]$Commands, [string]$Comment = 'ardhana') {
    $file = New-CliInputFile @{
        DocumentName = 'AWS-RunShellScript'
        InstanceIds  = @($InstanceId)
        Comment      = $Comment
        Parameters   = @{ commands = $Commands; executionTimeout = @('900') }
    }
    try {
        $id = Invoke-Aws ssm send-command --cli-input-json "file://$file" --query 'Command.CommandId' --output text
    } finally { Remove-Item $file -Force }
    Write-Host "SSM command $id sent; waiting..."
    # Poll rather than `ssm wait command-executed`, which gives up after ~100 s.
    $deadline = (Get-Date).AddMinutes(15)
    do {
        Start-Sleep -Seconds 5
        $prev = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
        try { $raw = & aws --region $Region ssm get-command-invocation --command-id $id --instance-id $InstanceId --output json 2>$null }
        finally { $ErrorActionPreference = $prev }
        $inv = if ($LASTEXITCODE -eq 0) { $raw | Out-String | ConvertFrom-Json } else { $null }   # InvocationDoesNotExist right after send
    } while ((-not $inv -or $inv.Status -in 'Pending', 'InProgress', 'Delayed') -and (Get-Date) -lt $deadline)
    $waitCode = if ($inv -and $inv.Status -eq 'Success') { 0 } else { 1 }
    if ($inv.StandardOutputContent) { Write-Host $inv.StandardOutputContent }
    if ($inv.StandardErrorContent)  { Write-Host $inv.StandardErrorContent -ForegroundColor DarkYellow }
    if ($waitCode -ne 0 -or $inv.Status -ne 'Success') { throw "Remote command ended with status $($inv.Status)" }
}

function Test-ParamExists([string]$Name) {
    $code = Invoke-AwsQuiet ssm get-parameter --name $Name --query Parameter.Name --output text
    return ($code -eq 0)
}

# Runs the AWS CLI for its exit code only, discarding output. An expected failure (e.g.
# ParameterNotFound) must not become a terminating NativeCommandError, which Windows
# PowerShell 5.1 raises for native stderr while $ErrorActionPreference is 'Stop'.
function Invoke-AwsQuiet {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & aws --region $Region @(ConvertTo-CliArgs $args) 2>&1 | Out-Null }
    finally { $ErrorActionPreference = $prev }
    return $LASTEXITCODE
}

function New-RandomSecret([int]$Length = 40) {
    # Alphanumeric only: RDS rejects / " @ and space in master passwords.
    $chars = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $bytes = New-Object byte[] $Length
    $rng.GetBytes($bytes)
    return -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

function Set-SecureParam([string]$Name, [string]$Value) {
    $file = New-CliInputFile @{ Name = $Name; Value = $Value; Type = 'SecureString'; Overwrite = $true; Tier = 'Standard' }
    try { Invoke-Aws ssm put-parameter --cli-input-json "file://$file" | Out-Null }
    finally { Remove-Item $file -Force }
}
