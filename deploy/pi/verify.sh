set -eu
python3 - <<'PY'
import urllib.request, urllib.error, json, re, os
base = 'http://127.0.0.1'
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None
opener = urllib.request.build_opener(NoRedirect)
checks = [
    ('/wishgifthub', 301),
    ('/wishgifthub/', 200),
    ('/wishgifthub/group/deployment-check/dashboard', 200),
    ('/wishgifthub/invite/deployment-check', 200),
    ('/wishgifthub/version.json', 200),
    ('/wishgifthub/favicon.svg', 200),
    ('/wishgifthub/assets/missing.js', 404),
    ('/wishgifthub/api/version', int(os.environ.get('WISHGIFTHUB_EXPECT_API', '503'))),
    ('/flightsnipper/', 200),
    ('/', 404),
]
with urllib.request.urlopen(base + '/wishgifthub/', timeout=10) as response:
    html = response.read().decode('utf-8')
assets = re.findall(r'(?:src|href)="(/wishgifthub/assets/[^\"]+)"', html)
assert assets, 'Aucune ressource trouvée dans le HTML'
checks.extend((asset, 200) for asset in assets)
for path, expected in checks:
    try:
        response = opener.open(base + path, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    body = response.read()
    print(f'{path}: HTTP {response.code} (attendu {expected})', flush=True)
    assert response.code == expected
    if expected == 301:
        assert response.headers['Location'].endswith('/wishgifthub/')
    if path.endswith('version.json') or expected == 503:
        json.loads(body)
for url in ('http://192.168.0.50/wishgifthub/', 'http://192.168.0.50:8765/'):
    with urllib.request.urlopen(url, timeout=10) as response:
        print(f'{url}: HTTP {response.code}', flush=True)
        assert response.code == 200
request = urllib.request.Request(base, headers={'Host': 'gabrielle.coaching'})
with urllib.request.urlopen(request, timeout=10) as response:
    print(f'gabrielle.coaching via Nginx: HTTP {response.code}', flush=True)
    assert response.code == 200
PY
sudo -n nginx -t
systemctl is-active nginx flightsniper gabrielle-coaching
systemctl is-enabled wishgifthub
systemctl show wishgifthub -p ActiveState -p SubState -p ExecCondition
sudo -n journalctl -u wishgifthub -n 6 --no-pager
sudo -n stat -c '%a %U:%G %n' /etc/wishgifthub /etc/wishgifthub/.env
sudo -n -u wishgifthub test ! -r /etc/wishgifthub/.env
sha256sum /opt/wishgifthub/backend/app.jar
readlink -f /opt/wishgifthub/frontend
readlink -f /opt/wishgifthub/backend
