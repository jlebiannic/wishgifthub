#!/bin/bash
# Exécuter en root depuis le dossier contenant frontend/, backend/ et deploy/pi/.
set -euo pipefail
test "$(id -u)" = 0
stage=$(pwd)
test -f "$stage/backend/app.jar"
test -f "$stage/frontend/wishgifthub/index.html"
release=$(date +%Y%m%d-%H%M%S)
backup="/var/backups/wishgifthub/$release"
target="/opt/wishgifthub/releases/$release"
site=$(readlink -f /etc/nginx/sites-enabled/flightsniper)
test -f "$site"
install -d -m 700 "$backup"
cp -a "$site" "$backup/nginx-default.conf"
for file in /etc/nginx/snippets/wishgifthub.conf /etc/systemd/system/wishgifthub.service; do
    if test -f "$file"; then cp -a "$file" "$backup/$(basename "$file")"; fi
done
getent passwd wishgifthub >/dev/null || useradd --system --home-dir /opt/wishgifthub --shell /usr/sbin/nologin wishgifthub
install -d -m 755 "$target" /usr/local/libexec /etc/nginx/snippets
cp -a "$stage/frontend" "$stage/backend" "$target/"
chown -R root:root "$target"
chmod -R a+rX "$target"
ln -sfn "$target/frontend" /opt/wishgifthub/frontend
ln -sfn "$target/backend" /opt/wishgifthub/backend
install -d -m 700 /etc/wishgifthub
if ! test -f /etc/wishgifthub/.env; then
    install -m 600 "$stage/deploy/pi/wishgifthub.env.example" /etc/wishgifthub/.env
fi
install -m 755 "$stage/deploy/pi/wishgifthub-check-env" /usr/local/libexec/wishgifthub-check-env
install -m 644 "$stage/deploy/pi/wishgifthub.service" /etc/systemd/system/wishgifthub.service
install -m 644 "$stage/deploy/pi/nginx-wishgifthub.conf" /etc/nginx/snippets/wishgifthub.conf
python3 - "$site" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
text = path.read_text()
include = '    include /etc/nginx/snippets/wishgifthub.conf;\n'
if include.strip() not in text:
    marker = '    location / {\n'
    if text.count(marker) != 1:
        raise SystemExit('Configuration Nginx inattendue : insertion interrompue')
    path.write_text(text.replace(marker, include + '\n' + marker))
PY
if ! nginx -t; then
    cp -a "$backup/nginx-default.conf" "$site"
    if test -f "$backup/wishgifthub.conf"; then
        cp -a "$backup/wishgifthub.conf" /etc/nginx/snippets/wishgifthub.conf
    fi
    echo "Validation Nginx échouée : fichier du serveur restauré, pas de rechargement." >&2
    exit 1
fi
systemctl reload nginx
systemctl daemon-reload
systemctl enable --now wishgifthub
printf 'Release: %s\nSauvegarde: %s\n' "$target" "$backup"
sha256sum /opt/wishgifthub/backend/app.jar
systemctl is-active nginx flightsniper gabrielle-coaching
systemctl show wishgifthub -p ActiveState -p SubState -p ConditionResult
