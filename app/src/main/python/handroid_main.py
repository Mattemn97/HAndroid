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
            "--target", SITE, "--find-links", INDICE, package]
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


def avvia_home_assistant():
    from homeassistant.__main__ import main
    from homeassistant.util import package

    package.install_package = installa_pacchetto

    os.makedirs(CONFIG, exist_ok=True)
    # --runner: al riavvio HA esce con codice 100 invece di rieseguirsi (execv è
    # vietato su Android); il servizio chiude il processo e Android lo riavvia.
    sys.argv = ["hass", "--config", CONFIG, "--runner"]
    return main()


def main():
    global ERRORE
    if not SAFE_MODE:
        try:
            # Prima importa il necessario: se manca una dipendenza nativa lo si vede subito.
            import homeassistant.__main__  # noqa: F401
            import homeassistant.bootstrap  # noqa: F401
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
