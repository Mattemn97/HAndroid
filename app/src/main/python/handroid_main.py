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

PORT = 8123
CONFIG = os.environ.get("HANDROID_CONFIG", os.path.expanduser("~/config"))
SAFE_MODE = os.environ.get("HANDROID_SAFE_MODE") == "1"


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


def avvia_home_assistant():
    from homeassistant.__main__ import main

    os.makedirs(CONFIG, exist_ok=True)
    # --runner: al riavvio HA esce con codice 100 invece di rieseguirsi (execv è
    # vietato su Android); il servizio chiude il processo e Android lo riavvia.
    sys.argv = ["hass", "--config", CONFIG, "--runner"]
    return main()


def main():
    if not SAFE_MODE:
        try:
            import homeassistant  # noqa: F401
        except ImportError:
            print("Home Assistant non installato: pagina di stato su", PORT)
        else:
            return avvia_home_assistant()
    http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Stato).serve_forever()


if __name__ == "__main__":
    sys.exit(main())
