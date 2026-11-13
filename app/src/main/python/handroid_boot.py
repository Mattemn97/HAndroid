"""Avvia Home Assistant su Android senza modificarne il codice.

Qui si raccolgono gli adattamenti all'ambiente Android, applicati da fuori.
"""
import logging
import os
import re
import sys

import aiodns

import homeassistant.__main__ as ha_main

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


class _Stato(logging.Handler):
    """Scrive l'ultimo passo dell'avvio nel file letto dalla schermata dell'app."""

    MESSAGGI = (
        (re.compile(r"Setting up stage (\S+?):"), "Avvio, fase {0}"),
        (re.compile(r"Setting up (\S+)$"), "Carico {0}"),
        (re.compile(r"Setup of domain (\S+) took"), "{0} pronto"),
        (re.compile(r"Home Assistant initialized"), "Home Assistant pronto"),
    )

    def emit(self, record):
        message = record.getMessage().split("\n")[0]
        for pattern, text in self.MESSAGGI:
            if match := pattern.search(message):
                message = text.format(*match.groups())
                break
        try:
            with open(_STATO, "w", encoding="utf-8") as f:
                f.write(message[:200])
        except OSError:
            pass


for _name in ("homeassistant.bootstrap", "homeassistant.setup"):
    logging.getLogger(_name).setLevel(logging.INFO)
    logging.getLogger(_name).addHandler(_Stato(logging.INFO))

sys.exit(ha_main.main())
