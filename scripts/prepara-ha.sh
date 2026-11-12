#!/usr/bin/env bash
# Prepara Home Assistant Core per l'APK (dopo prepara-python.sh, prima di gradle):
#   app/src/main/assets/site.zip   site-packages pronto: HA, le dipendenze che hanno
#                                  una wheel Android o pure Python, e pip
#   mancanti.txt (dentro lo zip)   requisiti senza wheel: HA li segnala all'avvio
# Le wheel native arrivano dal nostro indice (release "wheels", vedi wheels.yml).
# Non fallisce mai per una wheel mancante: l'APK parte comunque con la pagina di stato.
set -euo pipefail

HA_VERSION="${HA_VERSION:-2026.11.1}"
PYTHON_VERSION="${PYTHON_VERSION:-3.14.10}"
INDICE="${HANDROID_INDICE:-https://github.com/Mattemn97/HAndroid/releases/download/wheels/index.html}"
DEST="app/build/ha"
SITE="$DEST/site"
ASSETS="app/src/main/assets"
PY="${PYTHON:-python3}"

# Tag delle wheel Android (API 21..29) più quelle pure Python.
PIATTAFORMA=()
for api in 21 22 23 24 25 26 27 28 29; do PIATTAFORMA+=(--platform "android_${api}_arm64_v8a"); done
PIP=("$PY" -m pip install -q --disable-pip-version-check --target "$SITE" --only-binary :all:
  --python-version "$PYTHON_VERSION" --implementation cp --abi cp314 --abi abi3 --abi none
  "${PIATTAFORMA[@]}" --find-links "$INDICE" --upgrade)

mkdir -p "$ASSETS"
# In CI $DEST è in cache (chiave: versione di HA + questo script): si salta il lavoro lungo.
if [ -s "$DEST/site.zip" ]; then
  cp "$DEST/site.zip" "$ASSETS/site.zip"
  echo "HA $HA_VERSION dalla cache: $(unzip -p "$DEST/site.zip" mancanti.txt | wc -l) requisiti senza wheel Android"
  exit 0
fi
rm -rf "$DEST" && mkdir -p "$SITE"
"${PIP[@]}" --no-deps "homeassistant==$HA_VERSION" pip

# Requisiti di HA (senza extra), con le versioni fissate da HA come vincoli.
"$PY" - "$SITE" > "$DEST/requisiti.txt" <<'EOF'
import glob, sys
from email.parser import Parser
meta = glob.glob(f"{sys.argv[1]}/homeassistant-*.dist-info/METADATA")[0]
for r in Parser().parse(open(meta, encoding="utf-8")).get_all("Requires-Dist") or []:
    if "extra ==" not in r and not r.startswith("uv"):  # uv è un eseguibile: vietato (D3)
        print(r.replace(" ", ""))
EOF
cp "$DEST/requisiti.txt" "$DEST/vincoli.txt"

# Un requisito alla volta: chi non ha wheel finisce in mancanti.txt e non blocca gli altri.
: > "$SITE/mancanti.txt"
while read -r req; do
  if ! "${PIP[@]}" -c "$DEST/vincoli.txt" "$req" > "$DEST/pip.log" 2>&1; then
    echo "$req $(grep -o 'distribution found for [^ ]*' "$DEST/pip.log" | tail -1 | cut -c24-)" >> "$SITE/mancanti.txt"
  fi
done < "$DEST/requisiti.txt"

rm -rf "$SITE"/bin
find "$SITE" -name __pycache__ -prune -exec rm -rf {} +
(cd "$SITE" && zip -qr -X "$OLDPWD/$DEST/site.zip" .)
cp "$DEST/site.zip" "$ASSETS/site.zip"
echo "HA $HA_VERSION: site.zip $(du -h "$ASSETS/site.zip" | cut -f1), $(wc -l < "$SITE/mancanti.txt") requisiti senza wheel Android"
sed 's/^/  manca: /' "$SITE/mancanti.txt"
