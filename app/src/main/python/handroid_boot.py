"""Avvia Home Assistant su Android senza modificarne il codice.

Qui si raccolgono gli adattamenti all'ambiente Android, applicati da fuori.
"""
import logging
import os
import re
import shutil
import subprocess
import sys

# Android non ha i certificati CA dove li cerca OpenSSL: HA usa certifi, ma le integrazioni che
# creano un contesto SSL proprio (es. Xiaomi Home) fallirebbero con CERTIFICATE_VERIFY_FAILED.
# Va fatto prima di importare HA: aiohttp prepara il suo contesto SSL già all'import.
import certifi

os.environ.setdefault("SSL_CERT_FILE", certifi.where())

import aiodns
import bluetooth_adapters
import bluetooth_adapters.systems
import probatio

import homeassistant.__main__ as ha_main
import homeassistant.components.recorder as ha_recorder
import homeassistant.core as ha_core
import homeassistant.core_config as ha_core_config
import homeassistant.requirements as ha_requirements
import homeassistant.util.package as ha_package

# HA accetta solo sys.platform "linux" o "darwin"; su Android vale "android", ma il kernel è Linux.
ha_main.validate_os = lambda: None
# HA vuole un venv o un container; qui l'APK fa da container (Python privato, nessun pip di sistema).
ha_main.validate_environment = lambda: None

# Le variabili d'ambiente impostate da Kotlin non arrivano qui: i percorsi si ricavano da --config
# (files/ha-config). files/ e cache/ sono le cartelle private dell'app.
_FILES = os.path.dirname(os.path.abspath(sys.argv[sys.argv.index("--config") + 1]))
os.environ.setdefault("HOME", _FILES)
os.environ.setdefault("TMPDIR", os.path.join(os.path.dirname(_FILES), "cache"))
_STATO = os.path.join(_FILES, "stato.txt")

# integrazione "handroid" (gestione degli add-on, M3): si copia a ogni avvio, così segue l'APK
_CUSTOM = os.path.join(sys.argv[sys.argv.index("--config") + 1], "custom_components", "handroid")
_CUSTOM_APK = os.path.join(os.path.dirname(__file__), "custom_components")
# Poi la copia nell'APK si toglie: in site-packages, prima della cartella di HA in sys.path, nasconde
# ha-config/custom_components e HA non vede le integrazioni personalizzate (Xiaomi Home, ble_adv).
# Torna a ogni aggiornamento, quando HaService estrae di nuovo python.zip.
if os.path.isdir(_CUSTOM_APK):
    shutil.rmtree(_CUSTOM, ignore_errors=True)
    shutil.copytree(os.path.join(_CUSTOM_APK, "handroid"), _CUSTOM, ignore=shutil.ignore_patterns("__pycache__"))
    shutil.rmtree(_CUSTOM_APK, ignore_errors=True)

# Plancia "Telefono" con le entità del dispositivo ESPHome HAndroid (spec 0004): si crea una volta,
# prima che HA parta, negli stessi file che HA scrive per le plance fatte dall'interfaccia. Se
# l'utente la cancella o la modifica, resta come l'ha lasciata.
_STORAGE = os.path.join(sys.argv[sys.argv.index("--config") + 1], ".storage")
_PLANCIA = {"id": "handroid_telefono", "url_path": "handroid-telefono", "mode": "storage",
            "title": "Telefono", "icon": "mdi:cellphone", "show_in_sidebar": True, "require_admin": False}


def _tile(entita, **altro):
    return {"type": "tile", "entity": entita} | altro


_PLANCIA_CONFIG = {"views": [{"title": "Telefono", "type": "sections", "max_columns": 3, "sections": [
    {"type": "grid", "cards": [
        {"type": "heading", "heading": "Batteria", "icon": "mdi:battery"},
        {"type": "gauge", "entity": "sensor.handroid_batteria", "needle": True,
         "severity": {"green": 50, "yellow": 20, "red": 0}},
        _tile("binary_sensor.handroid_in_carica"),
        _tile("sensor.handroid_potenza_batteria"),
        _tile("sensor.handroid_temperatura_batteria"),
        _tile("sensor.handroid_corrente_batteria"),
        _tile("sensor.handroid_tensione_batteria"),
        {"type": "history-graph", "title": "Ultime 24 ore", "hours_to_show": 24,
         "entities": ["sensor.handroid_batteria", "sensor.handroid_potenza_batteria"]},
    ]},
    {"type": "grid", "cards": [
        {"type": "heading", "heading": "Sistema", "icon": "mdi:cellphone-cog"},
        _tile("sensor.handroid_ram_libera"),
        _tile("sensor.handroid_memoria_libera"),
        _tile("sensor.handroid_segnale_wifi"),
        _tile("sensor.handroid_uptime"),
        _tile("button.handroid_riavvia_home_assistant"),
    ]},
    {"type": "grid", "cards": [
        {"type": "heading", "heading": "Sensori", "icon": "mdi:motion-sensor"},
        _tile("sensor.handroid_luce_ambiente"),
        _tile("binary_sensor.handroid_prossimita"),
        _tile("switch.handroid_torcia", features=[{"type": "toggle"}]),
        {"type": "markdown", "content": "Le entità compaiono dopo aver aggiunto il dispositivo ESPHome "
         "**HAndroid** (Impostazioni → Dispositivi e servizi)."},
    ]},
]}]}


def _crea_plancia():
    import json
    elenco = os.path.join(_STORAGE, "lovelace_dashboards")
    try:
        with open(elenco, encoding="utf-8") as f:
            dati = json.load(f)
    except (OSError, ValueError):
        dati = {"version": 1, "minor_version": 1, "key": "lovelace_dashboards", "data": {"items": []}}
    voci = dati.setdefault("data", {}).setdefault("items", [])
    if any(v.get("id") == _PLANCIA["id"] or v.get("url_path") == _PLANCIA["url_path"] for v in voci):
        return
    os.makedirs(_STORAGE, exist_ok=True)
    with open(os.path.join(_STORAGE, "lovelace." + _PLANCIA["id"]), "w", encoding="utf-8") as f:
        json.dump({"version": 1, "minor_version": 1, "key": "lovelace." + _PLANCIA["id"],
                   "data": {"config": _PLANCIA_CONFIG}}, f, indent=2)
    voci.append(_PLANCIA)
    with open(elenco, "w", encoding="utf-8") as f:
        json.dump(dati, f, indent=2)


try:
    _crea_plancia()
except OSError as e:
    logging.getLogger("handroid").warning("Plancia Telefono non creata: %s", e)


def _read(name):
    try:
        with open(os.path.join(_FILES, name), encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


# Android non ha /etc/resolv.conf: c-ares (aiodns) non saprebbe a chi chiedere i nomi.
# HaService scrive in dns.txt i DNS della rete attiva; senza, si usano quelli pubblici.
_DNS = [s for s in _read("dns.txt").split(",") if s] or ["1.1.1.1", "8.8.8.8"]
_dns_init = aiodns.DNSResolver.__init__


def _dns_init_android(self, nameservers=None, *args, **kwargs):
    _dns_init(self, nameservers or _DNS, *args, **kwargs)


aiodns.DNSResolver.__init__ = _dns_init_android


def _native_dir():
    """Cartella delle librerie native dell'APK: l'unica da cui Android lascia eseguire programmi."""
    with open("/proc/self/maps", encoding="utf-8") as f:
        for line in f:
            if line.rstrip().endswith("/libpython3.14.so"):
                return os.path.dirname(line.split()[-1])
    return None


# Processi figli (pip, build dei pacchetti, add-on): "python" è libhpython.so, compilato da
# prepara-python-android.sh; trova stdlib e site-packages da queste variabili.
_NATIVE = _native_dir()
if _NATIVE:
    sys.executable = sys._base_executable = os.path.join(_NATIVE, "libhpython.so")
    os.environ["PYTHONHOME"] = sys.prefix
    os.environ["PYTHONPATH"] = os.path.join(sys.prefix, "site-packages")
    os.environ["LD_LIBRARY_PATH"] = _NATIVE
    # ffmpeg (telecamere, istantanee): nell'APK è libffmpeg.so; HA lo cerca come "ffmpeg" nel PATH
    if os.path.exists(os.path.join(_NATIVE, "libffmpeg.so")):
        _BIN = os.path.join(_FILES, "bin")
        os.makedirs(_BIN, exist_ok=True)
        if os.path.realpath(os.path.join(_BIN, "ffmpeg")) != os.path.join(_NATIVE, "libffmpeg.so"):
            # la cartella delle librerie cambia a ogni aggiornamento dell'APK
            try:
                os.remove(os.path.join(_BIN, "ffmpeg"))
            except OSError:
                pass
            os.symlink(os.path.join(_NATIVE, "libffmpeg.so"), os.path.join(_BIN, "ffmpeg"))
        os.environ["PATH"] = _BIN + os.pathsep + os.environ.get("PATH", "")

# Pacchetti installati a runtime: nella cartella utente di Python dentro la configurazione di HA,
# così sopravvivono agli aggiornamenti dell'APK. In testa a sys.path: un aggiornamento vince sulla
# versione inclusa nell'APK.
os.environ["PYTHONUSERBASE"] = os.path.join(sys.argv[sys.argv.index("--config") + 1], "deps")
_USER_SITE = os.path.join(os.environ["PYTHONUSERBASE"], "lib", "python3.14", "site-packages")
os.makedirs(_USER_SITE, exist_ok=True)
sys.path.insert(0, _USER_SITE)

# Wheel Android compilati da noi: nell'APK (files/python/wheels, da docs/dipendenze-indice.md) e
# nell'indice online pubblicato con i rilasci. pip li preferisce ai sorgenti, che qui non può compilare.
WHEELS = [os.path.join(sys.prefix, "wheels"), "https://mattemn97.github.io/HAndroid/wheels/"]


def _install_package(package, upgrade=True, target=None, constraints=None, timeout=None):
    """Sostituisce package.install_package di HA: pip al posto di uv, che su Android non c'è."""
    _LOGGER.info("Installo %s", package)
    args = [sys.executable, "-m", "pip", "install", "--quiet", "--disable-pip-version-check",
            "--no-warn-script-location", "--prefer-binary", package]
    for source in WHEELS:
        args += ["--find-links", source]
    args += ["--target", os.path.abspath(target)] if target else ["--user"]
    if upgrade:
        args.append("--upgrade")
    if constraints is not None:
        args += ["--constraint", constraints]
    if timeout:
        args += ["--timeout", str(timeout)]
    result = subprocess.run(args, capture_output=True, text=True, env=os.environ.copy(), check=False)
    if result.returncode:
        _LOGGER.error("Installazione di %s fallita: %s", package, result.stderr.strip()[-2000:])
        # pip non riesce a compilare sul telefono un pacchetto nativo che manca nell'indice dei wheel:
        # lo si dice chiaro, nell'interfaccia di HA, invece del solo "requisiti non trovati"
        if missing := sorted(set(re.findall(r"Failed to build '([^']+)'", result.stderr))):
            _avvisa(f"Il pacchetto {package} non si installa su Android: manca la versione per Android di "
                    f"{', '.join(missing)}, quindi questa integrazione per ora non funziona. "
                    "Segnalalo su https://github.com/Mattemn97/HAndroid/issues: va compilata e "
                    "aggiunta all'indice https://mattemn97.github.io/HAndroid/")
        return False
    return True


def _avvisa(message):
    """Notifica persistente nell'interfaccia di HA (chiamata da un thread: create è thread-safe)."""
    from homeassistant.components import persistent_notification
    if _HASS:
        persistent_notification.create(_HASS[0], message, "HAndroid: pacchetto mancante", "handroid_pacchetto")
    _LOGGER.error(message)


# l'istanza di HA, per le notifiche: async_get_hass non funziona dai thread dove gira pip
_HASS = []
_ha_init = ha_core.HomeAssistant.__init__


def _ricorda_hass(self, *args, **kwargs):
    _ha_init(self, *args, **kwargs)
    _HASS[:] = [self]


ha_core.HomeAssistant.__init__ = _ricorda_hass

# Accesso da fuori casa: se l'URL Internet di HA è vuoto, si usa l'indirizzo Tailscale che l'app ha
# trovato (url-remoto.txt). Una scelta già fatta in Impostazioni → Rete non si tocca.
_carica_config = ha_core_config.Config.async_load


async def _carica_config_con_remoto(self):
    await _carica_config(self)
    url = (await self.hass.async_add_executor_job(_read, "url-remoto.txt")).strip()
    if url and not self.external_url:
        _LOGGER.info("URL Internet impostato a %s (Tailscale)", url)
        await self.async_update(external_url=url)


ha_core_config.Config.async_load = _carica_config_con_remoto

# Batteria: il recorder scrive il database ogni 5 s, e ogni scrittura sveglia CPU e memoria flash.
# Ogni 30 s basta (si perdono al massimo 30 s di storico in un crash). commit_interval in
# configuration.yaml vince comunque. Lo schema di HA è già compilato (cambiarne il default non ha
# effetto): lo si avvolge, aggiungendo il valore prima della validazione originale.
ha_recorder.CONFIG_SCHEMA = probatio.Schema(
    {probatio.Optional(ha_recorder.DOMAIN, default=dict): probatio.All(
        lambda c: {ha_recorder.CONF_COMMIT_INTERVAL: 30} | dict(c or {}),
        ha_recorder.CONFIG_SCHEMA.schema[ha_recorder.DOMAIN])},
    extra=probatio.ALLOW_EXTRA,
)
_LOGGER = logging.getLogger("handroid")
ha_package.install_package = _install_package

# HA installa anche i requisiti delle after_dependencies e, se falliscono, non carica l'integrazione.
# Su Android alcune mancano (cloud vuole grpcio): mobile_app, che carica cloud "dopo" se c'è, non
# partirebbe più. Qui un requisito mancante di una after_dependency diventa solo un avviso.
_processa = ha_requirements.RequirementsManager._async_process_integration


async def _processa_tollerante(self, integration, done):
    try:
        await _processa(self, integration, done)
    except ha_requirements.RequirementsNotFound as ex:
        if ex.domain in integration.dependencies or ex.domain not in integration.after_dependencies:
            raise
        _LOGGER.warning("%s parte senza %s: %s", integration.domain, ex.domain, ex)


ha_requirements.RequirementsManager._async_process_integration = _processa_tollerante


class _NessunAdattatore(bluetooth_adapters.BluetoothAdapters):
    """Nessun adattatore Bluetooth locale: su Android non c'è BlueZ (D-Bus), e il sistema "linux"
    di bluetooth_adapters fallisce. Così l'integrazione bluetooth parte vuota e quelle che ne
    dipendono (es. Shelly) funzionano via rete; il Bluetooth vero arriva dal bridge (M2)."""

    @property
    def adapters(self):
        return {}

    @property
    def default_adapter(self):
        return "hci0"


bluetooth_adapters.get_adapters = bluetooth_adapters.systems.get_adapters = _NessunAdattatore


class _Stato(logging.Handler):
    """Scrive l'ultimo passo dell'avvio nel file letto dalla schermata dell'app."""

    MESSAGGI = (
        (re.compile(r"Setting up stage (\S+?):"), "Avvio, fase {0}"),
        (re.compile(r"Setting up (\S+)$"), "Carico {0}"),
        (re.compile(r"Setup of domain (\S+) took"), "{0} pronto"),
        (re.compile(r"Home Assistant initialized"), "Home Assistant pronto"),
    )

    pronto = False

    def emit(self, record):
        # dopo "pronto" lo stato non cambia: il watchdog dell'app aspetta proprio quel testo,
        # e le integrazioni caricate dopo l'avvio lo sovrascriverebbero
        if _Stato.pronto:
            return
        message = record.getMessage().split("\n")[0]
        for pattern, text in self.MESSAGGI:
            if match := pattern.search(message):
                message = text.format(*match.groups())
                _Stato.pronto = text == "Home Assistant pronto"
                break
        # os.open/os.write e non open(): HA segnala open() nel ciclo di eventi come chiamata bloccante
        try:
            fd = os.open(_STATO, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
            try:
                os.write(fd, message[:200].encode())
            finally:
                os.close(fd)
        except OSError:
            pass


for _name in ("homeassistant.bootstrap", "homeassistant.setup"):
    logging.getLogger(_name).setLevel(logging.INFO)
    logging.getLogger(_name).addHandler(_Stato(logging.INFO))

sys.exit(ha_main.main())
