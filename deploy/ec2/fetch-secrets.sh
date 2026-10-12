#!/usr/bin/env bash
# Fetch Ardhana's secrets from AWS Secrets Manager into ./secrets/ (run from deploy/ec2):
#   secrets/app/<KEY>                   one file per key (no trailing newline) — Spring config tree
#   secrets/firebase/firebase-sa.json   the Firebase service-account JSON, if present (push notifications)
# Uses the EC2 instance's IAM role — no static AWS keys.
#
#   Requires: aws CLI v2 and jq on the host.
#   Config (override via env): SECRET_ID (default ardhana/prod), AWS_REGION.
#
# The secret is a JSON object. Required keys: DB_PASSWORD, JWT_SECRET. Every other non-empty key is
# passed through under its own name, so use the names application.properties reads, e.g.
#   RAZORPAY_KEY_ID RAZORPAY_KEY_SECRET RAZORPAY_WEBHOOK_SECRET
#   RAZORPAYX_KEY_ID RAZORPAYX_KEY_SECRET RAZORPAYX_ACCOUNT_NUMBER
#   MSG91_AUTH_KEY MSG91_OTP_TEMPLATE_ID
#   MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_FROM
# plus FIREBASE_SERVICE_ACCOUNT_JSON (the whole key file as a string) for push notifications.
# Keys left empty or absent are skipped, so the defaults in application.properties apply.
set -euo pipefail
cd "$(dirname "$0")"

SECRET_ID="${SECRET_ID:-ardhana/prod}"
REGION="${AWS_REGION:-ap-south-1}"

command -v aws >/dev/null || { echo "aws CLI not found"; exit 1; }
command -v jq  >/dev/null || { echo "jq not found"; exit 1; }

echo "Fetching $SECRET_ID from Secrets Manager ($REGION)..."
json="$(aws secretsmanager get-secret-value \
  --secret-id "$SECRET_ID" --region "$REGION" \
  --query SecretString --output text)"

for k in DB_PASSWORD JWT_SECRET; do
  jq -e --arg k "$k" '.[$k] // "" | length > 0' <<<"$json" >/dev/null || { echo "Secret is missing key: $k"; exit 1; }
done

umask 077
# Start clean so keys removed from the secret disappear from the container too.
rm -rf secrets/app secrets/firebase
mkdir -p secrets/app secrets/firebase

for k in $(jq -r 'to_entries[] | select(.key != "FIREBASE_SERVICE_ACCOUNT_JSON")
                  | select((.value | tostring) != "") | .key' <<<"$json"); do
  [[ "$k" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "Skipping key with unexpected characters: $k"; continue; }
  jq -j --arg k "$k" '.[$k] | tostring' <<<"$json" > "secrets/app/$k"   # -j => no trailing newline
done

fb="$(jq -r '.FIREBASE_SERVICE_ACCOUNT_JSON // ""' <<<"$json")"
if [ -n "$fb" ]; then
  printf '%s' "$fb" > secrets/firebase/firebase-sa.json
  jq -e . secrets/firebase/firebase-sa.json >/dev/null || { echo "FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON"; exit 1; }
  echo "Firebase key written (push notifications enabled)."
else
  echo "No FIREBASE_SERVICE_ACCOUNT_JSON — push notifications disabled."
fi

echo "Wrote secrets/app/: $(ls secrets/app | tr '\n' ' ')"
