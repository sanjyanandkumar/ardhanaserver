#!/usr/bin/env bash
# One-time (safe to re-run): create Ardhana's database and user inside Kettlio's MySQL on the
# shared host, then load the tables from src/main/resources/schema-mysql.sql if the database is
# still empty. Run from deploy/ec2 after fetch-secrets.sh. Re-running resets the user's password to
# the current DB_PASSWORD (handy after rotating it) and never re-applies the schema.
#
# Later schema changes are NOT applied here — run them by hand (see README.md, "Database shell")
# and mirror them into schema-mysql.sql + schema.sql, as before.
#
#   Config (override via env): DB_CONTAINER (default ketlio-db-1), DB_NAME, DB_USER (default
#   ardhana — must match .env).
set -euo pipefail
cd "$(dirname "$0")"

DB_CONTAINER="${DB_CONTAINER:-ketlio-db-1}"
DB_NAME="${DB_NAME:-ardhana}"
DB_USER="${DB_USER:-ardhana}"
SCHEMA=../../src/main/resources/schema-mysql.sql

[ -s secrets/app/DB_PASSWORD ] || { echo "secrets/app/DB_PASSWORD missing — run ./fetch-secrets.sh first"; exit 1; }
[ -f "$SCHEMA" ] || { echo "$SCHEMA not found"; exit 1; }
docker inspect "$DB_CONTAINER" >/dev/null 2>&1 || { echo "MySQL container '$DB_CONTAINER' not found — is Kettlio running? (docker ps)"; exit 1; }

root_mysql() {  # run mysql as root inside Kettlio's db container
  docker exec -i "$DB_CONTAINER" sh -c 'MYSQL_PWD="$(cat /run/secrets/db_root_password)" exec mysql -uroot "$@"' -- "$@"
}

PW="$(cat secrets/app/DB_PASSWORD)"
PW="${PW//\'/\'\'}"   # escape single quotes for the SQL literal

echo "Creating database '$DB_NAME' and user '$DB_USER' in $DB_CONTAINER..."
root_mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$PW';
ALTER USER '$DB_USER'@'%' IDENTIFIED BY '$PW';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'%';
SQL

TABLES="$(root_mysql -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")"
if [ "$TABLES" -gt 0 ]; then
  echo "Database already has $TABLES tables — schema not re-applied."
else
  echo "Applying $SCHEMA..."
  root_mysql "$DB_NAME" < "$SCHEMA"
  echo "Tables created: $(root_mysql -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")"
fi
echo "Done. Start the app with: docker compose up -d --build"
