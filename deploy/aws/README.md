# Ardhana on AWS

**This is the production deployment.** It replaces the GCP setup (Cloud Run + Cloud SQL + Firebase
Hosting); the Docker-on-Kettlio design in `../ec2/` is superseded. Region: **ap-south-1 (Mumbai)**.

```
Browser / Android app
        │  https://ardhana.in
        ▼
CloudFront ──── /*      ──► S3 (React SPA, private, OAC)
           └─── /api/*  ──► EC2 t4g.small :8085 (Spring Boot, systemd)  ──► RDS MySQL 8.4 db.t4g.micro
                              ▲ security group: CloudFront origin-facing IPs only     (private, EC2-only)
                              └ secrets: SSM Parameter Store /ardhana/prod/env/*
```

- Spring profile: `aws` (`src/main/resources/application-aws.properties`). The `cloud` profile is the old GCP one.
- No SSH: the instance is managed through **SSM** (Run Command / Session Manager).
- Firebase is still used for **push notifications only** (FCM) — the backend needs a Firebase
  service-account key on AWS because there is no GCP metadata server.

## Cost on the AWS Free plan

| Item | Free plan | After credits / paid plan (approx.) |
|---|---|---|
| EC2 t4g.small + 20 GB gp3 + public IPv4 | eligible (paid from credits) | ~$17/mo |
| RDS db.t4g.micro, 20 GB, single-AZ | eligible | ~$15/mo |
| S3 + CloudFront | tiny | <$2/mo at low traffic |
| SSM Parameter Store (standard) | free | free |

Set a **budget alert** (Billing → Budgets) — still the cheapest safety net (the OTP endpoint is unthrottled, F-05).

## Prerequisites

1. AWS CLI v2, configured: `aws configure` (or `aws sso login`), default region `ap-south-1`.
   Check with `aws sts get-caller-identity`.
2. JDK 21 and Node 18+ on this machine (both builds run locally).
3. Optional, for interactive shells: the [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html).

## First deployment

Run from `ardhana/deploy/aws` in PowerShell:

```powershell
# 1. Secrets -> SSM (prompts with hidden input; generates DB_PASSWORD and JWT_SECRET)
.\set-secrets.ps1 -Domain ardhana.in -FirebaseKeyFile C:\path\to\firebase-service-account.json

# 2. TLS certificate for the domain (must be us-east-1 for CloudFront) - prints CNAMEs to add at your DNS provider
.\request-certificate.ps1 -Domain ardhana.in

# 3. Infrastructure (~15-20 min). Can run before the cert is issued - omit -Domain/-CertificateArn and re-run later.
.\deploy-infra.ps1
.\deploy-infra.ps1 -Domain ardhana.in -CertificateArn arn:aws:acm:us-east-1:...:certificate/...

# 4. Tables in the fresh database (one time)
.\init-db.ps1

# 5. Backend
.\deploy-backend.ps1

# 6. Frontend - before DNS cutover, build against the CloudFront URL to test
.\deploy-frontend.ps1 -ApiHost https://dxxxxxxxxxxxx.cloudfront.net
```

Firebase key: Firebase console → Project settings → Service accounts → *Generate new private key*.
Store it with step 1, then delete the downloaded file.

### DNS cutover (ardhana.in)

At your DNS provider:

| Record | Type | Value |
|---|---|---|
| `www.ardhana.in` | CNAME | `dxxxxxxxxxxxx.cloudfront.net` |
| `ardhana.in` (apex) | ALIAS / ANAME / flattened CNAME | `dxxxxxxxxxxxx.cloudfront.net` |

If the provider cannot alias the apex, move the zone to Route 53 (~$0.50/mo) and use an A-alias record.
After cutover, rebuild the frontend without `-ApiHost` (`.\deploy-frontend.ps1`) so it uses `https://ardhana.in`,
and update the **Razorpay webhook** URL to `https://ardhana.in/api/payments/webhook` (same path as before).

## Everyday operations

| Task | Command |
|---|---|
| Deploy backend | `.\deploy-backend.ps1` |
| Roll back backend to the previous jar | `.\deploy-backend.ps1 -Rollback` |
| Change a secret / env var | `.\set-secrets.ps1` then `.\deploy-backend.ps1 -RestartOnly` |
| Deploy frontend | `.\deploy-frontend.ps1` |
| Apply template changes | `.\deploy-infra.ps1 [-Domain ... -CertificateArn ...]` |
| Logs | `aws ssm start-session --target <InstanceId>` then `sudo journalctl -u ardhana -f` |

Any env var the app reads can be added as `/ardhana/prod/env/<NAME>` (SecureString); it is loaded on every
service restart. **Never set `PAYMENTS_MOCK_ENABLED` / `PAYOUTS_MOCK_ENABLED` in production.**

### Database shell

```bash
aws ssm start-session --target <InstanceId>
sudo -i
set -a; . /etc/ardhana/infra.env; set +a
MYSQL_PWD="$(aws ssm get-parameter --region $AWS_REGION --name $ARDHANA_PARAM_PREFIX/env/DB_PASSWORD --with-decryption --query Parameter.Value --output text)" \
  mysql --ssl -h "$DB_HOST" -u "$DB_USER" "$DB_NAME"
```

Schema changes are still manual (`ddl-auto=none`): apply the `ALTER`/`CREATE` here **before** deploying the
backend that needs it, and mirror it into `schema-mysql.sql` and `schema.sql`.

## Backups

RDS automated backups are on with **1-day retention** (the Free plan's maximum) and a final snapshot is taken
if the instance is ever deleted (`DeletionProtection` is on). Raise `BackupRetentionPeriod` to 7+ in
`ardhana.yaml` once on a paid plan.

## Known trade-offs

- Single EC2 instance: a reboot or deploy causes ~30-60 s of API downtime. Fine at this stage.
- CloudFront → EC2 hop is plain HTTP on port 8085 (restricted to CloudFront IPs). Viewer traffic is HTTPS.
- Logs live in journald on the instance only; add the CloudWatch agent if you need retention/alerts.
