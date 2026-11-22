"""Met à jour le .env distant ; paramètres reçus en saisie masquée, jamais affichés."""
import base64
import getpass
import json
from pathlib import Path
import shlex

from pi_remote import ROOT, paramiko

REMOTE = r'''
import json, os, pathlib, re, sys, tempfile
path = pathlib.Path('/etc/wishgifthub/.env')
updates = json.load(sys.stdin)
allowed = {'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME',
           'WISHGIFTHUB_JWT_SECRET', 'WISHGIFTHUB_JWT_EXPIRATION_MS'}
assert updates and set(updates) <= allowed, 'Paramètres non autorisés'
assert all(isinstance(v, str) and not any(c in v for c in '\n\r\x00') for v in updates.values())
lines = path.read_text().splitlines()
seen = set()
for i, line in enumerate(lines):
    key = line.partition('=')[0]
    if key in updates:
        lines[i] = key + '=' + json.dumps(updates[key], ensure_ascii=False)
        seen.add(key)
for key in updates.keys() - seen:
    lines.append(key + '=' + json.dumps(updates[key], ensure_ascii=False))
fd, name = tempfile.mkstemp(prefix='.env-', dir=path.parent)
try:
    with os.fdopen(fd, 'w') as output:
        output.write('\n'.join(lines) + '\n')
        output.flush()
        os.fsync(output.fileno())
    os.chown(name, 0, 0)
    os.chmod(name, 0o600)
    os.replace(name, path)
finally:
    if os.path.exists(name):
        os.unlink(name)
assert (path.stat().st_mode & 0o777) == 0o600
print('Paramètres mis à jour ; mot de passe conservé ; permissions 600 root:root.')
'''


def main():
    payload = getpass.getpass('Configuration JSON (saisie masquee) : ')
    updates = json.loads(payload)
    encoded = base64.b64encode(REMOTE.encode()).decode()
    code = f"import base64; exec(base64.b64decode('{encoded}'))"
    with paramiko.SSHClient() as client:
        client.load_host_keys(str(Path.home() / '.ssh/known_hosts'))
        client.set_missing_host_key_policy(paramiko.RejectPolicy())
        client.connect('192.168.0.50', username='pi',
                       key_filename=str(ROOT / 'rssh/id_ed25519_raspi'),
                       look_for_keys=False, allow_agent=False,
                       timeout=15, auth_timeout=15, banner_timeout=15)
        stdin, stdout, stderr = client.exec_command('sudo -n python3 -c ' + shlex.quote(code), timeout=30)
        stdin.write(json.dumps(updates))
        stdin.flush()
        stdin.channel.shutdown_write()
        output, error = stdout.read().decode(), stderr.read().decode()
        if stdout.channel.recv_exit_status():
            raise RuntimeError('Échec de mise à jour distante (valeurs non affichées).')
        print(output, end='')


if __name__ == '__main__':
    main()
