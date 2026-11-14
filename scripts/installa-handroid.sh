#!/usr/bin/env bash
# =============================================================================
#  installa-handroid.sh
#  Installa HAndroid sul telefono server dal PC, via ADB (va bene anche Git Bash su Windows).
#
#  Uso: scripts/installa-handroid.sh                  ultimo rilascio da GitHub
#       scripts/installa-handroid.sh app.apk          un APK locale
#       scripts/installa-handroid.sh --device-owner   in più rende l'app device owner
#
#  Usa "pm install" e non "adb install": su MIUI senza SIM "adb install" è bloccato.
#  Device owner: solo su un telefono appena resettato, senza account Google o Xiaomi. Così il
#  sistema non chiude l'app, i permessi si concedono da soli e niente schermata di blocco.
# =============================================================================
set -euo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash: non convertire /data/... in percorsi Windows

REPO="Mattemn97/HAndroid"
PKG="it.handroid"
APK="" OWNER=0
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
    | grep -o '"browser_download_url": *"[^"]*\.apk"' | head -1 | cut -d'"' -f4 || true)
  [ -n "$URL" ] || { echo "Nessun rilascio trovato su $REPO: passa un APK locale." >&2; exit 1; }
  APK="${TMPDIR:-/tmp}/handroid.apk"
  echo "Scarico $URL"
  curl -fL -o "$APK" "$URL"
fi

echo "Installo $APK"
adb push "$APK" /data/local/tmp/handroid.apk
out=$(adb shell pm install -r /data/local/tmp/handroid.apk 2>&1 || true)
adb shell rm -f /data/local/tmp/handroid.apk
case "$out" in
  *Success*) ;;
  *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
    echo "C'è già HAndroid con un'altra firma (es. una versione di sviluppo)." >&2
    echo "Fai prima un backup da HA, poi: adb uninstall $PKG  (cancella anche la configurazione)." >&2
    exit 1 ;;
  *) echo "$out" >&2; exit 1 ;;
esac

if [ "$OWNER" = 1 ]; then
  adb shell dpm set-device-owner "$PKG/.AdminReceiver" || {
    echo "Device owner rifiutato: di solito c'è ancora un account sul telefono." >&2
    echo "Rimuovi tutti gli account (o fai il ripristino) e rilancia con --device-owner." >&2
    exit 1
  }
fi

# ognuno di questi può essere rifiutato da MIUI: non è bloccante, si fa a mano dall'app o dalle impostazioni
prova() { adb shell "$@" >/dev/null 2>&1 || echo "  da fare a mano: $*"; }
echo "Tolgo le restrizioni di batteria e di background"
prova dumpsys deviceidle whitelist +$PKG
prova cmd appops set $PKG RUN_IN_BACKGROUND allow
prova cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow
prova settings put global wifi_sleep_policy 2

adb shell am start -S -n "$PKG/.MainActivity" --ez avvia true >/dev/null
IP=$(adb shell ip -4 addr show wlan0 2>/dev/null | grep -o 'inet [0-9.]*' | cut -d' ' -f2 || true)
echo
echo "Fatto. Il primo avvio richiede 1-2 minuti. Home Assistant: http://${IP:-IP-DEL-TELEFONO}:8123"
echo "Sul telefono: concedi notifiche e \"Dispositivi nelle vicinanze\" (Bluetooth), poi"
echo "su Xiaomi Impostazioni > App > HAndroid > Avvio automatico attivo, Risparmio batteria: nessuna restrizione."
