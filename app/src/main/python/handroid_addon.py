"""Punto di ingresso di un add-on Python, in un processo del pool `:addonN`.

Il servizio Kotlin passa la cartella dell'add-on come primo argomento: lì ci sono
manifest.json, opzioni.json e i file scaricati. Qui si installano i requisiti pip
(solo wheel, in `site/`, una volta per versione) e si esegue `entry`.
L'add-on legge le opzioni da HANDROID_ADDON_OPTIONS (JSON) o con `opzioni()`.
"""

import json
import os
import runpy
import sys
import traceback

DIR = sys.argv[1]
# come handroid_boot: i wheel nell'APK e l'indice online
INDICI = [os.path.join(sys.prefix, "wheels"), "https://mattemn97.github.io/HAndroid/wheels/"]


def opzioni():
    with open(os.path.join(DIR, "opzioni.json"), encoding="utf-8") as fh:
        return json.load(fh)


def installa_requisiti(manifest):
    req = manifest.get("pip") or []
    firma = manifest["version"] + "\n" + "\n".join(req)
    stamp = os.path.join(DIR, ".pip")
    if not req or (os.path.isfile(stamp) and open(stamp, encoding="utf-8").read() == firma):
        return
    from pip._internal.cli.main import main as pip

    site = os.path.join(DIR, "site")
    print("pip install", *req, flush=True)
    # Come in handroid_main: pip nello stesso processo (niente execve, D3), solo wheel.
    args = ["install", "--quiet", "--disable-pip-version-check", "--only-binary", ":all:",
            "--upgrade", "--target", site, *[a for i in INDICI for a in ("--find-links", i)], *req]
    try:
        rc = pip(args)
    except SystemExit as e:
        rc = e.code
    if rc not in (0, None):
        raise RuntimeError(f"pip non è riuscito a installare {req} (codice {rc})")
    with open(stamp, "w", encoding="utf-8") as fh:
        fh.write(firma)


def main():
    log = open(os.path.join(DIR, "log.txt"), "a", buffering=1, encoding="utf-8")
    sys.stdout = sys.stderr = log
    with open(os.path.join(DIR, "manifest.json"), encoding="utf-8") as fh:
        manifest = json.load(fh)
    print(f"== {manifest['name']} {manifest['version']} (pid {os.getpid()})", flush=True)
    try:
        installa_requisiti(manifest)
        os.environ["HANDROID_ADDON_OPTIONS"] = os.path.join(DIR, "opzioni.json")
        os.chdir(DIR)
        entry = manifest["entry"]
        if entry.endswith(".py"):
            sys.argv = [entry]
            runpy.run_path(os.path.join(DIR, entry), run_name="__main__")
        else:
            sys.argv = [entry]
            runpy.run_module(entry, run_name="__main__", alter_sys=True)
        return 0
    except SystemExit as e:
        return e.code if isinstance(e.code, int) else 0
    except BaseException:
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
