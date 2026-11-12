"""Punto di ingresso Python di HAndroid.

Se Home Assistant è installato lo avvia, altrimenti (o in modalità provvisoria)
serve sulla porta 8123 una pagina di stato: così l'APK è utile e verificabile
fin dalla M0a, prima che le wheel Android di HA siano pronte.
"""

import html
import http.server
import json
import os
import platform
import sys
import threading
import traceback

PORT = 8123
CONFIG = os.environ.get("HANDROID_CONFIG", os.path.expanduser("~/config"))
SITE = os.environ.get("HANDROID_SITE", os.path.expanduser("~/site-packages"))
SAFE_MODE = os.environ.get("HANDROID_SAFE_MODE") == "1"
BASE = os.path.dirname(CONFIG)
# Aggiornamento di HA dall'app (spec 0006): una versione installata con pip in HA_DIR
# sta in testa a sys.path e vince su site-packages e site.zip; HA_PREV serve al rollback.
HA_DIR = os.path.join(BASE, "ha")
HA_PREV = os.path.join(BASE, "ha.prev")
HA_NUOVO = os.path.join(BASE, "ha.nuovo")
RICHIESTA = os.path.join(BASE, "aggiorna_ha")  # contiene la versione chiesta dall'app
IN_PROVA = os.path.join(BASE, "ha.prova")      # c'è finché il watchdog non vede HA sano
INDICE = "https://github.com/Mattemn97/HAndroid/releases/download/wheels/index.html"
ERRORE = None  # perché HA non è partito, mostrato nella pagina di stato


def mancanti():
    """Requisiti di HA senza wheel Android al momento della build (da prepara-ha.sh)."""
    for p in sys.path:
        f = os.path.join(p, "mancanti.txt")
        if os.path.isfile(f):
            with open(f, encoding="utf-8") as fh:
                return [r.strip() for r in fh if r.strip()]
    return []


def stato():
    try:
        import homeassistant.const as c
        ha = c.__version__
    except ImportError:
        ha = None
    return {
        "python": sys.version.split()[0],
        "piattaforma": platform.platform(),
        "home_assistant": ha,
        "config": CONFIG,
        "modalita_provvisoria": SAFE_MODE,
        "errore": ERRORE,
        "requisiti_mancanti": mancanti(),
    }


class Stato(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        s = stato()
        if self.path.startswith("/api/handroid"):
            body, tipo = json.dumps(s).encode(), "application/json"
        else:
            righe = "".join(f"<tr><th>{k}</th><td>{html.escape(str(v))}</td></tr>" for k, v in s.items())
            body = (
                "<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width'>"
                "<title>HAndroid</title><body style='font-family:sans-serif;padding:16px'>"
                f"<h1>HAndroid</h1><p>Python gira dentro l'APK.</p><table>{righe}</table>"
            ).encode()
            tipo = "text/html; charset=utf-8"
        self.send_response(200)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


_pip_lock = threading.Lock()


def installa_pacchetto(package, upgrade=True, target=None, constraints=None, timeout=None):
    """Sostituto di homeassistant.util.package.install_package: HA lancia `uv` come
    processo, ma su Android niente execve (D3). Qui pip gira nello stesso processo,
    solo wheel, prima dal nostro indice, sempre in HANDROID_SITE."""
    from pip._internal.cli.main import main as pip

    args = ["install", "--quiet", "--disable-pip-version-check", "--only-binary", ":all:",
            "--target", target or SITE, "--find-links", INDICE, package]
    if upgrade:
        args.append("--upgrade")
    if constraints:
        args += ["--constraint", constraints]
    if timeout:
        args += ["--timeout", str(timeout)]
    print("pip", package, flush=True)
    with _pip_lock:
        try:
            return pip(args) == 0
        except SystemExit as e:
            return e.code == 0


def log_supervisore(msg):
    """Stesso registro scritto dal lato Kotlin (Supervisore.kt), mostrato nella schermata di stato."""
    import time
    with open(os.path.join(BASE, "supervisore.log"), "a", encoding="utf-8") as f:
        f.write(time.strftime("%Y-%m-%d %H:%M:%S") + " " + msg + "\n")
    print(msg, flush=True)


def applica_aggiornamento():
    """Se l'app ha chiesto una versione di HA, la installa in HA_NUOVO e scambia le cartelle.
    Se pip fallisce non cambia niente: resta la versione di prima."""
    import shutil
    if not os.path.isfile(RICHIESTA):
        return
    with open(RICHIESTA, encoding="utf-8") as f:
        versione = f.read().strip()
    os.remove(RICHIESTA)
    shutil.rmtree(HA_NUOVO, ignore_errors=True)
    log_supervisore(f"Installo homeassistant=={versione}")
    if not installa_pacchetto(f"homeassistant=={versione}", target=HA_NUOVO):
        shutil.rmtree(HA_NUOVO, ignore_errors=True)
        log_supervisore(f"Aggiornamento a {versione} fallito: resta la versione attuale")
        return
    shutil.rmtree(HA_PREV, ignore_errors=True)
    if os.path.isdir(HA_DIR):
        os.rename(HA_DIR, HA_PREV)
    os.rename(HA_NUOVO, HA_DIR)
    open(IN_PROVA, "w").close()
    log_supervisore(f"Home Assistant {versione} installato, in prova")


def copia_integrazione():
    """Mette custom_components/handroid (pannello add-on, 0007) nella config di HA,
    aggiornata a ogni avvio: è l'APK a dettarne la versione, non l'utente."""
    import shutil

    src = os.path.join(os.path.dirname(os.path.abspath(__file__)), "custom_components", "handroid")
    if os.path.isdir(src):
        dst = os.path.join(CONFIG, "custom_components", "handroid")
        shutil.rmtree(dst, ignore_errors=True)
        shutil.copytree(src, dst)


def avvia_home_assistant():
    from homeassistant.__main__ import main
    from homeassistant.util import package

    package.install_package = installa_pacchetto

    os.makedirs(CONFIG, exist_ok=True)
    copia_integrazione()
    # --runner: al riavvio HA esce con codice 100 invece di rieseguirsi (execv è
    # vietato su Android); il servizio chiude il processo e Android lo riavvia.
    sys.argv = ["hass", "--config", CONFIG, "--runner"]
    return main()


def main():
    global ERRORE
    if not SAFE_MODE:
        try:
            applica_aggiornamento()
        except Exception:
            traceback.print_exc()
        if os.path.isdir(HA_DIR):
            sys.path.insert(0, HA_DIR)
        try:
            # Prima importa il necessario: se manca una dipendenza nativa lo si vede subito.
            import homeassistant.__main__  # noqa: F401
            import homeassistant.bootstrap  # noqa: F401
            from homeassistant.const import __version__
            with open(os.path.join(BASE, "versione_ha"), "w", encoding="utf-8") as f:
                f.write(__version__)
        except Exception as e:
            ERRORE = f"{type(e).__name__}: {e}"
            traceback.print_exc()
            print("Home Assistant non avviabile, pagina di stato su", PORT, flush=True)
            for r in mancanti():
                print("  manca:", r, flush=True)
        else:
            return avvia_home_assistant()
    http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Stato).serve_forever()


if __name__ == "__main__":
    sys.exit(main())
