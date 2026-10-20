<#
  Creates/updates the CloudFormation stack (RDS, EC2, S3, CloudFront).
  First run takes ~15-20 min (RDS + CloudFront). Re-running applies template changes.

  Usage:
    .\deploy-infra.ps1                                    # cloudfront.net URL only
    .\deploy-infra.ps1 -Domain ardhana.in -CertificateArn arn:aws:acm:us-east-1:...   # with custom domain
#>
param(
    [string]$Domain,
    [string]$CertificateArn,
    [string]$InstanceType = 't4g.small',
    [switch]$Plan,       # create the change set and list it, without executing
    [switch]$UpgradeAmi  # move to the latest Amazon Linux AMI - REPLACES the instance (redeploy the backend after)
)
. "$PSScriptRoot\common.ps1"

if (-not (Test-ParamExists "$ParamPrefix/env/DB_PASSWORD")) {
    throw "Run .\set-secrets.ps1 first - the stack reads DB_PASSWORD from $ParamPrefix/env/DB_PASSWORD."
}
if ([bool]$Domain -ne [bool]$CertificateArn) { throw 'Pass -Domain and -CertificateArn together (or neither).' }

# Guard against free-plan surprises: the instance type must be free-tier eligible on this account.
$eligible = Invoke-Aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true `
    --query 'InstanceTypes[].InstanceType' --output text
if (($eligible -split '\s+') -notcontains $InstanceType) {
    throw "$InstanceType is not free-tier eligible on this account. Eligible: $eligible"
}
$arch = Invoke-Aws ec2 describe-instance-types --instance-types $InstanceType `
    --query 'InstanceTypes[0].ProcessorInfo.SupportedArchitectures[0]' --output text
# Keep the running instance's AMI so a routine update never replaces the server; only -UpgradeAmi
# (or a first deploy) picks up the latest Amazon Linux 2023 image.
$prev = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
$currentInstance = & aws --region $Region cloudformation describe-stack-resource --stack-name $StackName `
    --logical-resource-id BackendInstance --query 'StackResourceDetail.PhysicalResourceId' --output text 2>$null
$ErrorActionPreference = $prev
if ($LASTEXITCODE -eq 0 -and $currentInstance -and -not $UpgradeAmi) {
    $ami = Invoke-Aws ec2 describe-instances --instance-ids $currentInstance --query 'Reservations[0].Instances[0].ImageId' --output text
} else {
    $ami = Invoke-Aws ssm get-parameter --name "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-$arch" `
        --query Parameter.Value --output text
}

# Default VPC and its subnets (RDS needs subnets in >= 2 AZs).
$vpcId = Invoke-Aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text
if (-not $vpcId -or $vpcId -eq 'None') { throw "No default VPC in $Region." }
$subnets = (Invoke-Aws ec2 describe-subnets --filters Name=vpc-id,Values=$vpcId Name=default-for-az,Values=true `
    --query 'sort_by(Subnets,&AvailabilityZone)[].SubnetId' --output text) -split '\s+' | Where-Object { $_ }
$prefixList = Invoke-Aws ec2 describe-managed-prefix-lists `
    --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing `
    --query 'PrefixLists[0].PrefixListId' --output text

$dbVersion = Invoke-Aws rds describe-db-engine-versions --engine mysql --engine-version 8.4 --default-only `
    --query 'DBEngineVersions[0].EngineVersion' --output text

$overrides = @(
    "VpcId=$vpcId",
    "SubnetIds=$($subnets -join ',')",
    "CloudFrontPrefixListId=$prefixList",
    "InstanceType=$InstanceType",
    "AmiId=$ami",
    "DbEngineVersion=$dbVersion",
    "ParamPrefix=$ParamPrefix"
)
if ($Domain) { $overrides += "DomainNames=$Domain,www.$Domain", "CertificateArn=$CertificateArn" }

Write-Host "Deploying stack $StackName in $Region ($InstanceType/$arch, $ami, MySQL $dbVersion, VPC $vpcId)..."
$planArgs = @(if ($Plan) { '--no-execute-changeset' })
Invoke-Aws cloudformation deploy `
    --stack-name $StackName `
    --template-file "$PSScriptRoot\ardhana.yaml" `
    --capabilities CAPABILITY_IAM `
    --no-fail-on-empty-changeset `
    --parameter-overrides @overrides @planArgs

if ($Plan) {
    $cs = Invoke-Aws cloudformation list-change-sets --stack-name $StackName --query 'reverse(sort_by(Summaries,&CreationTime))[0].ChangeSetName' --output text
    Invoke-Aws cloudformation describe-change-set --stack-name $StackName --change-set-name $cs `
        --query 'Changes[].ResourceChange.[Action,LogicalResourceId,ResourceType,Replacement]' --output table
    Write-Host "Change set '$cs' created but NOT executed. Re-run without -Plan to apply."
    return
}

$out = Get-StackOutputs
$out.GetEnumerator() | Sort-Object Name | Format-Table -AutoSize
Write-Host "Next: .\init-db.ps1 (first time only), then .\deploy-backend.ps1 and .\deploy-frontend.ps1"
