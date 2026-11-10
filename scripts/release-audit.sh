#!/bin/sh
set -eu
python3 - <<'PY'
import json
from pathlib import Path
root = Path('.')
pkg = json.load(open(root / 'frontend/package.json'))
tsconfig = json.load(open(root / 'frontend/tsconfig.json'))
node_tsconfig = json.load(open(root / 'frontend/tsconfig.node.json'))
assert 'references' not in tsconfig, 'root tsconfig must not reference a noEmit project'
assert node_tsconfig.get('compilerOptions', {}).get('composite') is not True
assert (root / 'frontend/src/vite-env.d.ts').exists()
assert 'tsc --noEmit' in pkg.get('scripts', {}).get('build', '')
json.load(open(root / 'observability/grafana/dashboards/atlasshard-api.json'))
assert (root / 'frontend/src/assets/datacenter-bg.svg').exists()
assert (root / 'frontend/src/assets/network-bg.svg').exists()
assert (root / 'observability/grafana/provisioning/datasources/prometheus.yml').exists()
assert (root / 'observability/grafana/provisioning/dashboards/dashboard.yml').exists()
ignore = (root / '.gitignore').read_text()
assert '.env' in ignore
readme = (root / 'README.md').read_text().lower()
assert 'placement simulator' in readme
app = (root / 'frontend/src/App.tsx').read_text()
assert '24.8K' not in app
assert '99.999%' not in app
assert 'demoFiles' not in app
assert 'demoNodes' not in app
api = (root / 'frontend/src/lib/api.ts').read_text()
assert 'localStorage.getItem(\'atlasshard_token\')' not in api
upload = (root / 'backend/src/main/java/com/atlasshard/storage/service/UploadSessionService.java').read_text()
assert 'readOwned' in upload
assert 'authentication.getName()' in (root / 'backend/src/main/java/com/atlasshard/storage/controller/FileController.java').read_text()
makefile = (root / 'Makefile').read_text()
assert 'frontend-lock' in makefile
assert 'package-lock.json' in makefile
print('json, assets, security, truthfulness, and reproducibility checks passed')
PY
if grep -RInE 'TODO|FIXME|nexus90|soccer intelligence|SHARDLINE|Shardline|shardline' backend frontend README.md docker-compose.yml .env.example >/dev/null; then
  echo 'unexpected placeholder or copied branding found' >&2
  exit 1
fi
printf '%s\n' 'release audit passed'
