<#
  One-time: creates the tables in the fresh RDS database from schema-mysql.sql.
  Runs on the EC2 instance via SSM (RDS is not reachable from the internet).

  Later schema changes are NOT applied by this script - run your ALTER/CREATE by hand
  (see "Database shell" in README.md) and mirror it into schema-mysql.sql + schema.sql, as before.
#>
. "$PSScriptRoot\common.ps1"

$out = Get-StackOutputs
$bucket = $out.ArtifactsBucket
$key = 'schema/schema-mysql.sql'

Invoke-Aws s3 cp "$RepoRoot\src\main\resources\schema-mysql.sql" "s3://$bucket/$key" | Out-Null

Invoke-OnInstance $out.InstanceId -Comment 'ardhana init-db' -Commands @(
    'set -euo pipefail',
    'set -a; . /etc/ardhana/infra.env; set +a',
    "aws s3 cp s3://$bucket/$key /tmp/schema-mysql.sql --region `$AWS_REGION --only-show-errors",
    'export MYSQL_PWD="$(aws ssm get-parameter --region $AWS_REGION --name $ARDHANA_PARAM_PREFIX/env/DB_PASSWORD --with-decryption --query Parameter.Value --output text)"',
    'TABLES=$(mysql --ssl -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=''$DB_NAME''")',
    'if [ "$TABLES" -gt 0 ]; then echo "Database already has $TABLES tables - refusing to re-run schema."; exit 1; fi',
    'mysql --ssl -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" < /tmp/schema-mysql.sql',
    'rm -f /tmp/schema-mysql.sql',
    'mysql --ssl -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -N -e "SELECT COUNT(*) AS tables_created FROM information_schema.tables WHERE table_schema=''$DB_NAME''"'
)
Write-Host 'Schema applied.'
