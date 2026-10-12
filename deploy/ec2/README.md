# Deploying Ardhana to the shared AWS EC2 host (next to Kettlio)

Ardhana runs on the same EC2 instance as **Kettlio** and **Prakaram**, reusing Kettlio's MySQL and
Kettlio's Caddy (HTTPS on 80/443). This replaces the standalone CloudFormation design in
`../aws/` (own EC2 + RDS + CloudFront) — that one is not used.

```
                         Internet  (browser + Android app)
                            │  80/443
                    ┌───────▼────────┐
                    │  Kettlio Caddy │  automatic HTTPS for every domain on the host
                    └──┬──────┬──────┘
   other domains ──────┘      └──── ardhana.in (ardhana.caddy)
                                  /api/* │          │ /*
                              ┌──────────▼──┐ ┌─────▼───────┐
                              │ ardhana-api │ │ ardhana-web │  static React app
                              └──────┬──────┘ └─────────────┘
                    ┌────────────────▼───────────────────────────────┐
                    │ Kettlio MySQL (`db`): kettlio | prakaram | ardhana │
                    └────────────────────────────────────────────────┘
          all on Kettlio's Docker network `ketlio_default`
```

| What | Where |
|---|---|
| Non-secret config | `deploy/ec2/.env` (copy of `.env.example`) |
| Secrets | AWS Secrets Manager `ardhana/prod` → `fetch-secrets.sh` → `deploy/ec2/secrets/` (mounted read-only) |
| Database | `ardhana` database + user in Kettlio's MySQL; tables from `schema-mysql.sql` (`init-db.sh`) |
| HTTPS / routing | Kettlio's Caddy, via `~/ketlio/deploy/sites/ardhana.caddy` |
| Uploaded files | stored in the database (no volume needed) |

Run every server command **from `~/ardhana/ardhana/deploy/ec2`** — that's where compose finds this
project's `docker-compose.yml` and `.env`.

---

## 0. Before you start

- Kettlio is running on the server, and its Caddy imports extra sites (`~/ketlio/deploy/sites/`
  exists — see `deploy/sites/README.md` in the Kettlio repo).
- The server has room: three apps need a **`t3.large` (8 GB)** plus a swap file. See step 0 of
  Prakaram's `deploy/README.md`.

## 1. Secret (AWS CloudShell)

```bash
aws secretsmanager create-secret --name ardhana/prod --region ap-south-1 \
  --secret-string "$(jq -n \
    --arg db  "$(openssl rand -base64 30 | tr -dc 'A-Za-z0-9')" \
    --arg jwt "$(openssl rand -base64 48)" \
    '{DB_PASSWORD:$db, JWT_SECRET:$jwt}')"
```

Then add the other keys in the console (Secrets Manager → `ardhana/prod` → Retrieve secret value →
Edit → Add row). Names must match `application.properties`:

| Keys | Needed for |
|---|---|
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | course payments |
| `RAZORPAYX_KEY_ID`, `RAZORPAYX_KEY_SECRET`, `RAZORPAYX_ACCOUNT_NUMBER` | teacher payouts |
| `MSG91_AUTH_KEY` (optional `MSG91_OTP_TEMPLATE_ID`) | sign-up OTP SMS |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | school credential emails |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | push notifications — paste the whole key file's JSON as the value |

Firebase key: Firebase console → Project settings → Service accounts → *Generate new private key*.
Paste it into the secret, then delete the downloaded file.

## 2. IAM: let the instance read the secret (console)

IAM → Roles → the instance's role (`kettlio-ec2-secrets`) → its inline policy → Edit → add
(replace `ACCOUNT_ID`):

```json
{
  "Effect": "Allow",
  "Action": "secretsmanager:GetSecretValue",
  "Resource": "arn:aws:secretsmanager:ap-south-1:ACCOUNT_ID:secret:ardhana/prod-*"
}
```

## 3. Get the code (server)

```bash
mkdir -p ~/ardhana && cd ~/ardhana
git clone https://github.com/sanjyanandkumar/ardhanaserver.git ardhana
git clone https://github.com/sanjyanandkumar/dear-digital-space.git dear-digital-space
cd ~/ardhana/ardhana/deploy/ec2
cp .env.example .env
nano .env          # PUBLIC_URL (e.g. https://ardhana.in)
bash fetch-secrets.sh
```

If the repos are private, use a GitHub personal access token (read-only Contents on both repos) as
the password.

## 4. Database (server)

Creates the `ardhana` database and user inside Kettlio's MySQL and loads the tables:

```bash
cd ~/ardhana/ardhana/deploy/ec2 && bash init-db.sh
```

## 5. Build and start (server)

```bash
cd ~/ardhana/ardhana/deploy/ec2
docker compose up -d --build
docker compose ps
docker compose logs -f ardhana-api     # look for "Started ArdhanaApplication"
```

## 6. Route the domain (server)

Point DNS for `ardhana.in` and `www.ardhana.in` (**A records**) at the server's Elastic IP — the
same IP as Kettlio. Then hand the site to Kettlio's Caddy:

```bash
cp ~/ardhana/ardhana/deploy/ec2/ardhana.caddy ~/ketlio/deploy/sites/ardhana.caddy
#   (different domain? sed 's/ardhana.in/YOUR-DOMAIN/g' ardhana.caddy > ~/ketlio/deploy/sites/ardhana.caddy)
cd ~/ketlio
docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
```

Caddy fetches the HTTPS certificate once DNS resolves. Open `https://ardhana.in`.

## 7. After first start

- **Razorpay webhook:** `https://ardhana.in/api/payments/webhook`.
- **Android app:** it calls `https://ardhana.in/api`; CORS already allows its `https://localhost`
  origin. Rebuild the app only if the domain differs from what it was built with.
- **Admin / school accounts:** created the same way as before (see the handover docs).

---

## Day-2 operations

From `~/ardhana/ardhana/deploy/ec2`:

```bash
git -C ../.. pull && git -C ../../../dear-digital-space pull && docker compose up -d --build   # deploy
docker compose logs -f ardhana-api                                                              # logs
docker compose restart ardhana-api                                                              # restart
docker compose down                                       # stop Ardhana only (data is in Kettlio's MySQL)
```

**Rotate / add a secret:** edit it in Secrets Manager, then
`bash fetch-secrets.sh && docker compose up -d --force-recreate ardhana-api`. After rotating
`DB_PASSWORD`, also re-run `bash init-db.sh` to update the MySQL user.

**Database shell** (for manual schema changes):
```bash
docker exec -it ketlio-db-1 sh -c 'MYSQL_PWD="$(cat /run/secrets/db_root_password)" mysql -uroot ardhana'
```

**Back up the database:**
```bash
docker exec ketlio-db-1 sh -c 'MYSQL_PWD="$(cat /run/secrets/db_root_password)" exec mysqldump -uroot ardhana' > ardhana-$(date +%F).sql
```
Uploaded files (photos, attachments, APKs) are in the database, so this backs them up too.

> **Never run `docker compose down -v` in `~/ketlio`** — it deletes the MySQL volume holding all
> three apps' databases.

---

## Troubleshooting

**`network ketlio_default not found`.** Kettlio isn't running, or its folder isn't `ketlio`. Check
`docker network ls`; set `SHARED_NETWORK` in `.env` to the right `<folder>_default`.

**`ardhana-api` restarts with an access-denied DB error.** Run `bash init-db.sh` (also after
rotating `DB_PASSWORD`). Check the MySQL container name with `docker ps` (default `ketlio-db-1`;
override with `DB_CONTAINER=…`).

**Web build fails at `npm ci`.** `package-lock.json` in dear-digital-space is out of sync with
`package.json` — run `npm install` locally, commit the lock file, pull on the server.

**Login/API calls fail with CORS errors.** `PUBLIC_URL` must exactly match the address bar. Fix
`.env`, then `docker compose up -d --build`.

**Push notifications don't send.** `docker compose logs ardhana-api | grep -i firebase` — add
`FIREBASE_SERVICE_ACCOUNT_JSON` to the secret and re-run `fetch-secrets.sh`.

**APK upload fails.** The limit is 100 MB in both the API and `ardhana.caddy`.

**Out of memory during build.** Resize to `t3.large` and add swap; build one app at a time.
