#!/data/data/com.termux/files/usr/bin/bash
# =============================================================================
#  Home Assistant su un vecchio telefono Android  (senza root)
#  Termux + udocker + immagine ufficiale homeassistant/home-assistant
#
#  Uso (dentro Termux):
#     bash installa-homeassistant-android.sh
#
#  Variabili opzionali:
#     HA_TAG=stable|2026.10.x   versione dell'immagine (default: stable)
#     HA_FULL=1                 usa default_config completo (zeroconf, bluetooth,
#                               usb...). Su molti telefoni senza root va in errore.
#     HA_EXECMODE=P1|P2         modalità proot di udocker (P2 se P1 non parte)
#     TZ=Europe/Rome            fuso orario
# =============================================================================
set -euo pipefail

HA_TAG="${HA_TAG:-stable}"
HA_FULL="${HA_FULL:-0}"
HA_EXECMODE="${HA_EXECMODE:-P1}"
TZ_HA="${TZ:-Europe/Rome}"

IMAGE="homeassistant/home-assistant:${HA_TAG}"
CONTAINER="homeassistant"
BASE="$HOME/ha"
CONF="$HOME/ha/config"

verde() { printf '\033[1;32m%s\033[0m\n' "$*"; }
giallo() { printf '\033[1;33m%s\033[0m\n' "$*"; }
rosso() { printf '\033[1;31m%s\033[0m\n' "$*"; }

# --- 0. Controlli -------------------------------------------------------------
if [ -z "${PREFIX:-}" ] || [ ! -d "/data/data/com.termux" ]; then
  rosso "Questo script va eseguito dentro Termux (installato da F-Droid)."
  exit 1
fi

ARCH="$(uname -m)"
if [ "$ARCH" != "aarch64" ]; then
  rosso "Architettura rilevata: $ARCH"
  rosso "Home Assistant non supporta più i sistemi a 32 bit: serve un telefono/Android a 64 bit (aarch64)."
  exit 1
fi

RAM_MB=$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)
FREE_GB=$(df -k "$HOME" | awk 'NR==2 {print int($4/1024/1024)}')
verde "Telefono: $ARCH, RAM ${RAM_MB} MB, spazio libero ${FREE_GB} GB"
[ "$RAM_MB" -lt 2500 ] && giallo "Attenzione: con meno di 3 GB di RAM funziona, ma sarà lento."
if [ "$FREE_GB" -lt 4 ]; then
  rosso "Servono almeno 4 GB liberi."
  exit 1
fi

# --- 1. Pacchetti -------------------------------------------------------------
verde "[1/6] Aggiorno Termux e installo i pacchetti..."
termux-wake-lock 2>/dev/null || true
yes | pkg update -y
pkg upgrade -y
pkg install -y proot python termux-api curl openssh || true

if ! command -v udocker >/dev/null 2>&1; then
  pkg install -y udocker 2>/dev/null || pip install udocker
fi
command -v udocker >/dev/null 2>&1 || { rosso "udocker non installato."; exit 1; }
udocker install >/dev/null 2>&1 || true

# --- 2. Immagine Home Assistant ----------------------------------------------
verde "[2/6] Scarico l'immagine $IMAGE (può volerci un po')..."
udocker pull "$IMAGE"

if udocker ps | grep -q "\['${CONTAINER}'\]"; then
  giallo "Il contenitore '$CONTAINER' esiste già: lo ricreo (la configurazione resta)."
  udocker rm "$CONTAINER" >/dev/null 2>&1 || true
fi
udocker create --name="$CONTAINER" "$IMAGE"
udocker setup --execmode="$HA_EXECMODE" "$CONTAINER"

# --- 3. Configurazione --------------------------------------------------------
verde "[3/6] Preparo la configurazione in $CONF ..."
mkdir -p "$CONF" "$BASE"
touch "$CONF/automations.yaml" "$CONF/scripts.yaml" "$CONF/scenes.yaml"
[ -s "$CONF/automations.yaml" ] || echo "[]" > "$CONF/automations.yaml"

if [ ! -f "$CONF/configuration.yaml" ]; then
  if [ "$HA_FULL" = "1" ]; then
    INTEGRAZIONI="default_config:"
  else
    # default_config senza zeroconf/ssdp/dhcp/bluetooth/usb/stream,
    # che senza root su Android di solito falliscono.
    INTEGRAZIONI="frontend:
config:
api:
http:
onboarding:
backup:
energy:
history:
logbook:
mobile_app:
person:
sun:
my:
webhook:
media_source:
conversation:
system_health:
homeassistant_alerts:
input_boolean:
input_number:
input_select:
input_text:
input_datetime:
input_button:
counter:
timer:
schedule:
tag:
zone:
map:"
  fi

  cat > "$CONF/configuration.yaml" <<EOF
# Generato da installa-homeassistant-android.sh
homeassistant:
  time_zone: ${TZ_HA}

${INTEGRAZIONI}

recorder:
  purge_keep_days: 7
  commit_interval: 30   # meno scritture sulla memoria del telefono

logger:
  default: warning

automation: !include automations.yaml
script: !include scripts.yaml
scene: !include scenes.yaml

# Batteria del telefono-server (scritta ogni minuto da Termux:API)
command_line:
  - sensor:
      name: "Batteria server"
      unique_id: batteria_server_percentuale
      command: "cat /config/.batteria.json 2>/dev/null || echo '{}'"
      value_template: "{{ value_json.percentage | default(none) }}"
      unit_of_measurement: "%"
      device_class: battery
      scan_interval: 60
      json_attributes:
        - status
        - temperature
        - plugged
EOF
else
  giallo "configuration.yaml esiste già: non lo tocco."
fi

# --- 4. Script di gestione ----------------------------------------------------
verde "[4/6] Creo gli script start/stop/update/log in $BASE ..."

cat > "$BASE/start.sh" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
# Avvia Home Assistant in background
termux-wake-lock 2>/dev/null || true
if pgrep -f "homeassistant --config" >/dev/null; then
  echo "Home Assistant è già in esecuzione."; exit 0
fi

# Batteria -> file letto da HA (serve app Termux:API)
if command -v termux-battery-status >/dev/null; then
  pkill -f "ha-batteria-loop" 2>/dev/null || true
  nohup bash -c 'exec -a ha-batteria-loop bash -c "while true; do termux-battery-status > $CONF/.batteria.json 2>/dev/null; sleep 60; done"' >/dev/null 2>&1 &
fi

nohup udocker --allow-root run \\
  --env="TZ=${TZ_HA}" \\
  --volume="$CONF:/config" \\
  --workdir=/config \\
  $CONTAINER \\
  python3 -m homeassistant --config /config --log-rotate-days 3 \\
  > "$BASE/avvio.log" 2>&1 &

IP=\$(ip -4 addr show wlan0 2>/dev/null | awk '/inet /{print \$2}' | cut -d/ -f1)
echo "Avviato. Primo avvio: 5-15 minuti."
echo "Apri  http://\${IP:-IP-DEL-TELEFONO}:8123  da un altro dispositivo in WiFi."
EOF

cat > "$BASE/stop.sh" <<'EOF'
#!/data/data/com.termux/files/usr/bin/bash
pkill -INT -f "homeassistant --config" 2>/dev/null && echo "Arresto in corso..." || echo "Non era in esecuzione."
sleep 10
pkill -f "homeassistant --config" 2>/dev/null || true
pkill -f "ha-batteria-loop" 2>/dev/null || true
EOF

cat > "$BASE/update.sh" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
# Aggiorna all'ultima versione (fai prima un backup dall'interfaccia!)
set -e
bash "$BASE/stop.sh"
udocker pull $IMAGE
udocker rm $CONTAINER
udocker create --name=$CONTAINER $IMAGE
udocker setup --execmode=$HA_EXECMODE $CONTAINER
bash "$BASE/start.sh"
EOF

cat > "$BASE/log.sh" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
tail -n 80 -f "$CONF/home-assistant.log" "$BASE/avvio.log"
EOF

chmod +x "$BASE"/*.sh

# --- 5. Avvio automatico ------------------------------------------------------
verde "[5/6] Configuro l'avvio automatico (serve l'app Termux:Boot)..."
mkdir -p "$HOME/.termux/boot"
cat > "$HOME/.termux/boot/10-homeassistant" <<EOF
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock
sshd 2>/dev/null || true
sleep 30
bash "$BASE/start.sh"
EOF
chmod +x "$HOME/.termux/boot/10-homeassistant"

# --- 6. Primo avvio -----------------------------------------------------------
verde "[6/6] Primo avvio..."
bash "$BASE/start.sh"

cat <<EOF

=============================================================
 Fatto.
   Avvia:     bash ~/ha/start.sh
   Ferma:     bash ~/ha/stop.sh
   Log:       bash ~/ha/log.sh
   Aggiorna:  bash ~/ha/update.sh
   Config:    ~/ha/config

 Se non parte, rilancia con:  HA_EXECMODE=P2 bash $0
=============================================================
EOF
