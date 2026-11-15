#!/usr/bin/env bash
# =============================================================================
#  installa-integrazione.sh
#  Installa sul telefono un'integrazione personalizzata pubblicata su GitHub (come fa HACS),
#  nella cartella custom_components di HA. Dal PC, via ADB (va bene anche Git Bash su Windows).
#
#  Uso: scripts/installa-integrazione.sh XiaoMi/ha_xiaomi_home        ultimo rilascio
#       scripts/installa-integrazione.sh NicoIIT/ha-ble-adv v2.2.0     un rilascio preciso
#
#  Prende lo zip allegato al rilascio, o in mancanza il sorgente con custom_components/<dominio>.
#  Poi riavvia HA: l'integrazione compare in Impostazioni → Dispositivi → Aggiungi integrazione.
#  Usa "run-as": funziona con l'APK di debug. Le integrazioni non stanno nell'APK perché alcune
#  (Xiaomi Home) hanno licenze che non ne permettono la ridistribuzione dentro un'app.
# =============================================================================
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash: non convertire /data/... in percorsi Windows

REPO="${1:?Uso: $0 proprietario/repo [versione]}"
TAG="${2:-}"
PKG="it.handroid"

command -v adb >/dev/null || { echo "Serve adb (Android SDK Platform-Tools) nel PATH." >&2; exit 1; }
[ "$(adb get-state 2>/dev/null)" = device ] || { echo "Nessun telefono collegato con il debug USB attivo." >&2; exit 1; }

tmp=$(mktemp -d)
# con MSYS_NO_PATHCONV i programmi Windows (curl, adb) non capiscono /tmp: serve il percorso C:/...
command -v cygpath >/dev/null && tmp=$(cygpath -m "$tmp")
trap 'rm -rf "$tmp"' EXIT
api="https://api.github.com/repos/$REPO/releases/${TAG:+tags/$TAG}"
[ -n "$TAG" ] || api+="latest"
curl -fsSL "$api" -o "$tmp/rilascio.json"
url=$(grep -o '"browser_download_url": *"[^"]*\.zip"' "$tmp/rilascio.json" | head -1 | cut -d'"' -f4 || true)
[ -n "$url" ] || url=$(grep -o '"zipball_url": *"[^"]*"' "$tmp/rilascio.json" | cut -d'"' -f4)
echo "==> $url"
curl -fsSL "$url" -o "$tmp/i.zip"
mkdir "$tmp/x" && unzip -q "$tmp/i.zip" -d "$tmp/x"

# Zip del rilascio: i file dell'integrazione stanno nella radice. Sorgente: in custom_components/<dominio>.
man=$(find "$tmp/x" -name manifest.json -path "*custom_components/*" | head -1)
[ -n "$man" ] || man=$(find "$tmp/x" -maxdepth 1 -name manifest.json)
[ -n "$man" ] || { echo "Nello zip non c'è un manifest.json di integrazione." >&2; exit 1; }
dir=$(dirname "$man")
dominio=$(grep -o '"domain": *"[^"]*"' "$man" | cut -d'"' -f4)
echo "==> $dominio $(grep -o '"version": *"[^"]*"' "$man" | cut -d'"' -f4)"

# adb push non scrive nei file privati dell'app: si passa da /data/local/tmp e da run-as.
tar -C "$dir" -cf - . > "$tmp/i.tar"   # -f "C:/..." per tar sarebbe un host remoto
adb push "$tmp/i.tar" /data/local/tmp/handroid-integrazione.tar >/dev/null
adb shell "run-as $PKG sh -c 'rm -rf files/ha-config/custom_components/$dominio && mkdir -p files/ha-config/custom_components/$dominio && tar -xf /data/local/tmp/handroid-integrazione.tar -C files/ha-config/custom_components/$dominio'"
adb shell rm /data/local/tmp/handroid-integrazione.tar

# Riavvio di HA: si chiude l'app e la si riapre (con l'avvio automatico attivo riparte anche HA).
# Un kill del solo processo :ha non basta: il supervisor lo prende per uno stop voluto. Dopo un
# force-stop MIUI ignora "am start": "monkey" invece apre l'app.
adb shell am force-stop $PKG
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
echo "Installata $dominio. HA riparte in circa un minuto; i requisiti pip si installano quando aggiungi l'integrazione."
