#!/usr/bin/env bash
# Installa HAndroid sul telefono server dal PC, via ADB (va bene Git Bash su Windows).
#
#   ./installa-handroid.sh                 ultima release da GitHub
#   ./installa-handroid.sh app.apk         un APK locale
#   ./installa-handroid.sh --device-owner  in più rende l'app device owner
#
# Device owner: solo su telefono appena resettato, senza account Google/Xiaomi.
# Così HyperOS non può più chiudere l'app e i permessi si concedono da soli.
set -euo pipefail

REPO="Mattemn97/HAndroid"
PKG="it.handroid"
APK=""
OWNER=0
for a in "$@"; do
  case "$a" in
    --device-owner) OWNER=1 ;;
    *.apk) APK="$a" ;;
    *) echo "Argomento sconosciuto: $a" >&2; exit 2 ;;
  esac
done

command -v adb >/dev/null || { echo "Serve adb (Android SDK Platform-Tools) nel PATH." >&2; exit 1; }
[ "$(adb get-state 2>/dev/null)" = device ] || { echo "Nessun telefono collegato con il debug USB attivo." >&2; exit 1; }

if [ -z "$APK" ]; then
  URL=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" \
    | grep -o '"browser_download_url": *"[^"]*\.apk"' | head -1 | cut -d'"' -f4)
  [ -n "$URL" ] || { echo "Nessuna release trovata su $REPO." >&2; exit 1; }
  APK="${TMPDIR:-/tmp}/handroid.apk"
  echo "Scarico $URL"
  curl -fsSL -o "$APK" "$URL"
fi

echo "Installo $APK"
adb install -r -g "$APK"

if [ "$OWNER" = 1 ]; then
  adb shell dpm set-device-owner "$PKG/.AdminReceiver" || {
    echo "Device owner rifiutato: di solito c'è ancora un account sul telefono." >&2
    echo "Rimuovi tutti gli account (o fai il reset) e rilancia con --device-owner." >&2
    exit 1
  }
fi

# Ognuno di questi può fallire su HyperOS: non è bloccante, si fa a mano.
prova() { adb shell "$@" >/dev/null 2>&1 || echo "  da fare a mano: $*"; }
echo "Tolgo le restrizioni di batteria e background"
prova dumpsys deviceidle whitelist +$PKG
prova cmd appops set $PKG RUN_IN_BACKGROUND allow
prova cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow
prova cmd appops set $PKG AUTO_START allow
prova settings put global wifi_sleep_policy 2

adb shell am start -n "$PKG/.MainActivity" >/dev/null
IP=$(adb shell ip -4 addr show wlan0 2>/dev/null | grep -o 'inet [0-9.]*' | cut -d' ' -f2 || true)
echo "Fatto. Home Assistant: http://${IP:-IP-DEL-TELEFONO}:8123"
echo "Su Xiaomi controlla anche: Impostazioni > App > HAndroid > Avvio automatico."
