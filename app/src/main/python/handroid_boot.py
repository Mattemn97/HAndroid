"""Avvia Home Assistant su Android senza modificarne il codice.

Qui si raccolgono gli adattamenti all'ambiente Android, applicati da fuori.
"""
import logging
import os
import re
import subprocess
import sys

import aiodns
import bluetooth_adapters
import bluetooth_adapters.systems

import homeassistant.__main__ as ha_main
import homeassistant.core as ha_core
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
                    f"{', '.join(missing)}. Va aggiunta all'indice dei wheel di HAndroid "
                    "(vedi docs/guida-sviluppo.md, \"Come si installano le integrazioni\").")
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
_LOGGER = logging.getLogger("handroid")
ha_package.install_package = _install_package


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
