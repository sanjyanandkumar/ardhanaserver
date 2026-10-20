<#
  Direct-HTTPS mode: serve the domain from the EC2 instance itself with Caddy (automatic
  Let's Encrypt certificates) instead of through CloudFront.

  Why: ardhana.in is still attached to a CloudFront distribution in an old AWS account we can't
  access, so CloudFront refuses it as an alternate domain name. Until AWS Support moves it,
  Caddy terminates TLS on the instance:
      https://ardhana.in/api/*  -> Spring Boot on 127.0.0.1:8085
      https://ardhana.in/*      -> the existing CloudFront distribution (S3 site, SPA rewrite)
  so deploy-frontend.ps1 keeps working unchanged.

  Order:
    1. .\deploy-infra.ps1 -CertificateArn <arn> -DirectHttps     (opens 80/443 on the instance)
    2. Point the domain's A records at the stack's ElasticIp      (Route 53)
    3. .\setup-direct-https.ps1 -Domain ardhana.in               (installs/updates Caddy)

  Re-running is safe; it rewrites the Caddyfile and restarts Caddy.
#>
param([string]$Domain = 'ardhana.in')
. "$PSScriptRoot\common.ps1"

# Pinned release + its published SHA-512 (github.com/caddyserver/caddy/releases, v2.11.7 checksums.txt).
$caddyVersion = '2.11.7'
$caddySha512  = '3db36ba90c7a6e8dda40ee3dd71fa08844c76b5fb08f61b31e5e78d2ed38e71c51dc7baed875e50d1ca1279196e84302967237386ae87c91ae9f2aaceada682e'

$out = Get-StackOutputs
$cdn = $out.DistributionDomain

$caddyfile = @"
$Domain, www.$Domain {
	encode zstd gzip
	request_body {
		max_size 100MB
	}
	handle /api/* {
		reverse_proxy 127.0.0.1:8085
	}
	handle {
		reverse_proxy https://$cdn {
			header_up Host {upstream_hostport}
		}
	}
}
"@

Invoke-OnInstance $out.InstanceId -Comment 'ardhana direct-https' -Commands @(
    'set -euo pipefail',
    "VER=$caddyVersion",
    "SHA=$caddySha512",
    'if ! /usr/local/bin/caddy version 2>/dev/null | grep -q "v$VER"; then',
    '  cd /tmp',
    '  curl -fsSL -o caddy.tgz "https://github.com/caddyserver/caddy/releases/download/v$VER/caddy_${VER}_linux_arm64.tar.gz"',
    '  echo "$SHA  caddy.tgz" | sha512sum -c -',
    '  tar -xzf caddy.tgz caddy && install -m 0755 caddy /usr/local/bin/caddy && rm -f caddy caddy.tgz',
    'fi',
    'id caddy >/dev/null 2>&1 || useradd --system --home-dir /var/lib/caddy --create-home --shell /sbin/nologin caddy',
    'mkdir -p /etc/caddy',
    "cat > /etc/caddy/Caddyfile <<'EOF'`n$caddyfile`nEOF",
    @'
cat > /etc/systemd/system/caddy.service <<'EOF'
[Unit]
Description=Caddy (direct HTTPS for Ardhana)
After=network-online.target
Wants=network-online.target

[Service]
User=caddy
Group=caddy
ExecStart=/usr/local/bin/caddy run --environ --config /etc/caddy/Caddyfile
ExecReload=/usr/local/bin/caddy reload --config /etc/caddy/Caddyfile --force
Environment=XDG_DATA_HOME=/var/lib/caddy XDG_CONFIG_HOME=/var/lib/caddy
AmbientCapabilities=CAP_NET_BIND_SERVICE
LimitNOFILE=1048576
Restart=on-failure
TimeoutStopSec=5s

[Install]
WantedBy=multi-user.target
EOF
'@,
    '/usr/local/bin/caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile',
    'systemctl daemon-reload',
    'systemctl enable caddy',
    'systemctl restart caddy',
    'sleep 20',
    'systemctl is-active caddy',
    'journalctl -u caddy -n 15 --no-pager'
)
Write-Host "Caddy configured for $Domain. Check: https://$Domain"
