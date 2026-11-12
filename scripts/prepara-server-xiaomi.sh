#!/usr/bin/env bash
# =============================================================================
#  prepara-server-xiaomi.sh
#  Da eseguire sul PC (Linux / macOS / Git Bash su Windows) con il telefono
#  collegato via USB, DOPO il ripristino di fabbrica.
#
#  Cosa fa:
#   1. installa l'APK di Homedroid (o qualsiasi APK server gli passi)
#   2. lo esenta da ottimizzazione batteria e restrizioni in background
#   3. disattiva il "phantom process killer" di Android 12+
#   4. riduce consumo: luminosità minima, niente aggiornamenti automatici
#   5. (opzionale, --debloat) rimuove per l'utente le app Xiaomi inutili
#      -> reversibile con:  adb shell cmd package install-existing <pacchetto>
#
#  Uso:
#     ./prepara-server-xiaomi.sh homedroid-x.y.z-arm64-v8a.apk [--debloat]
# =============================================================================
set -euo pipefail

APK="${1:-}"
DEBLOAT=0
[ "${2:-}" = "--debloat" ] && DEBLOAT=1

ok()   { printf '\033[1;32m✔ %s\033[0m\n' "$*"; }
info() { printf '\033[1;34m→ %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m! %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31m✘ %s\033[0m\n' "$*"; exit 1; }

sh() { adb shell "$@" 2>/dev/null || true; }

# --- Controlli ---------------------------------------------------------------
command -v adb >/dev/null || die "adb non trovato. Installa 'Android SDK Platform-Tools'."
[ -n "$APK" ] && [ -f "$APK" ] || die "Uso: $0 <file.apk> [--debloat]"

info "Attendo il telefono (accetta l'autorizzazione al debug sullo schermo)..."
adb wait-for-device
MODEL=$(sh getprop ro.product.model | tr -d '\r')
SDK=$(sh getprop ro.build.version.sdk | tr -d '\r')
ok "Collegato: $MODEL (API $SDK)"

# --- 1. Installazione --------------------------------------------------------
info "Installo $APK ..."
BEFORE=$(adb shell pm list packages -3 | tr -d '\r' | sort)
if ! adb install -r -g "$APK"; then
  die "Installazione rifiutata. Su Xiaomi attiva in Opzioni sviluppatore:
   'Installa tramite USB' e 'Debug USB (impostazioni di sicurezza)'."
fi
AFTER=$(adb shell pm list packages -3 | tr -d '\r' | sort)
PKG=$(comm -13 <(echo "$BEFORE") <(echo "$AFTER") | head -n1 | sed 's/^package://')
if [ -z "$PKG" ]; then
  PKG=$(echo "$AFTER" | grep -i -m1 homedroid | sed 's/^package://' || true)
fi
[ -n "$PKG" ] || die "Non riesco a capire il nome del pacchetto installato."
ok "Pacchetto: $PKG"

# --- 2. Background e batteria ------------------------------------------------
info "Esento l'app da risparmio energetico e restrizioni..."
sh dumpsys deviceidle whitelist +"$PKG"
sh cmd appops set "$PKG" RUN_IN_BACKGROUND allow
sh cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND allow
sh cmd appops set "$PKG" START_FOREGROUND allow
sh am set-standby-bucket "$PKG" active
sh pm grant "$PKG" android.permission.POST_NOTIFICATIONS
ok "Fatto."

# --- 3. Phantom process killer (Android 12+) ---------------------------------
if [ "${SDK:-0}" -ge 31 ]; then
  info "Disattivo il phantom process killer..."
  sh device_config set_sync_disabled_for_tests persistent
  sh device_config put activity_manager max_phantom_processes 2147483647
  sh settings put global settings_enable_monitor_phantom_procs false
  ok "Fatto."
fi

# --- 4. Impostazioni da server -----------------------------------------------
info "Imposto il telefono in modalità server..."
sh settings put system screen_brightness_mode 0
sh settings put system screen_brightness 1
sh settings put system screen_off_timeout 15000
sh settings put global wifi_sleep_policy 2
sh settings put global ota_disable_automatic_update 1
sh settings put global auto_time 1
sh svc bluetooth disable   # il server non lo usa: meno consumi
ok "Fatto."

# --- 5. Debloat (opzionale) --------------------------------------------------
if [ "$DEBLOAT" = "1" ]; then
  info "Rimuovo app inutili (solo per l'utente, reversibile)..."
  BLOAT=(
    com.miui.msa.global          # pubblicità di sistema
    com.miui.analytics
    com.xiaomi.mipicks           # GetApps
    com.mi.globalbrowser
    com.miui.player
    com.miui.videoplayer
    com.miui.android.fashiongallery
    com.miui.yellowpage
    com.xiaomi.glgm              # Games
    com.miui.bugreport
    com.miui.cleaner
    com.facebook.appmanager
    com.facebook.services
    com.facebook.system
    com.facebook.katana
    com.netflix.mediaclient
    com.netflix.partner.activation
    com.amazon.mShop.android.shopping
    com.spotify.music
    com.linkedin.android
    com.google.android.youtube
    com.google.android.apps.youtube.music
    com.google.android.apps.photos
    com.google.android.apps.tachyon
    com.google.android.videos
    com.google.android.apps.docs
    com.google.android.apps.subscriptions.red
  )
  for p in "${BLOAT[@]}"; do
    if adb shell pm list packages | tr -d '\r' | grep -qx "package:$p"; then
      if adb shell pm uninstall --user 0 "$p" 2>/dev/null | grep -q Success; then
        echo "   rimosso $p"
      fi
    fi
  done
  ok "Debloat completato."
fi

# --- Fine --------------------------------------------------------------------
IP=$(sh ip -4 addr show wlan0 | awk '/inet /{print $2}' | cut -d/ -f1)
cat <<EOF

==============================================================
 Telefono pronto.
  • Apri l'app e avvia il server, poi installa Home Assistant
    dal suo catalogo.
  • Indirizzo:  http://${IP:-IP-DEL-TELEFONO}:8123
  • Sul telefono: Impostazioni → App → $PKG →
      Avvio automatico ON, Risparmio batteria "Nessuna restrizione"
    (su HyperOS alcune voci si attivano solo a mano)
  • Blocca l'app nelle recenti (tieni premuto → lucchetto).
  • Dal router assegna un IP fisso al telefono.
==============================================================
EOF
