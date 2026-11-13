"""Elenca le dipendenze di Home Assistant Core e dice quali hanno già un pacchetto per Android.

Legge i metadati da PyPI, segue le dipendenze ricorsivamente con i marker di Android
(arm64, Python 3.14) e classifica ogni pacchetto:
  puro      wheel py3-none-any: si installa ovunque
  android   esiste già un wheel per android arm64
  compilare c'è codice nativo ma nessun wheel Android: va compilato con cibuildwheel
  sorgente  solo sdist: probabilmente puro, da verificare

Uso:  python scripts/dipendenze-android.py [versione-ha] > docs/dipendenze-android.md
      python scripts/dipendenze-android.py <versione-ha> scripts/integrazioni-indice.txt > docs/dipendenze-indice.md
      python scripts/dipendenze-android.py <versione-ha> tutte > docs/dipendenze-tutte.md
Con il secondo argomento elenca le dipendenze delle integrazioni di quel file (non incluse nell'APK, i
loro pacchetti nativi vanno nell'indice dei wheel) invece di quelle del core. "tutte" = ogni integrazione di HA.
"""
import json
import pathlib
import sys
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

from pip._vendor.packaging.requirements import Requirement
from pip._vendor.packaging.utils import canonicalize_name
from pip._vendor.packaging.version import InvalidVersion, Version

ENV = {
    "sys_platform": "android",
    "platform_system": "Android",
    "os_name": "posix",
    "platform_machine": "aarch64",
    "python_version": "3.14",
    "python_full_version": "3.14.10",
    "implementation_name": "cpython",
    "platform_python_implementation": "CPython",
    "extra": "",
}


def pypi(name, version=None):
    url = f"https://pypi.org/pypi/{name}/{version}/json" if version else f"https://pypi.org/pypi/{name}/json"
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.load(r)


def choose(data, spec):
    """Versione più recente non pre-release che rispetta lo specifier."""
    versions = []
    for v, files in data["releases"].items():
        try:
            pv = Version(v)
        except InvalidVersion:
            continue
        if files and not pv.is_prerelease and spec.contains(pv):
            versions.append(pv)
    return str(max(versions)) if versions else data["info"]["version"]


def classify(files):
    wheels = [f["filename"] for f in files if f["filename"].endswith(".whl")]
    tags = [w[:-4].split("-")[-2:] for w in wheels]  # [abi, piattaforma]
    if any(p == "any" for _, p in tags):
        return "puro"
    if any("android" in p and "arm64" in p and a in ("cp314", "abi3") for a, p in tags):
        return "android"
    if wheels:
        return "compilare"
    return "sorgente"


def deps(info):
    out = []
    for line in info.get("requires_dist") or []:
        req = Requirement(line)
        if req.marker is None or req.marker.evaluate(ENV):
            out.append(req)
    return out


def ha_wheel(ha_version):
    """Wheel di Home Assistant (da PyPI, in cache in build/): da lì si leggono i manifest delle integrazioni.
    raw.githubusercontent.com limita le richieste e con molti manifest risponde 429."""
    cache = pathlib.Path(__file__).resolve().parent.parent / "build" / f"homeassistant-{ha_version}.whl"
    if not cache.exists():
        url = next(f["url"] for f in pypi("homeassistant", ha_version)["urls"] if f["filename"].endswith(".whl"))
        cache.parent.mkdir(exist_ok=True)
        with urllib.request.urlopen(url, timeout=120) as r:
            cache.write_bytes(r.read())
    return zipfile.ZipFile(cache)


def manifest_requirements(ha_version, path):
    """Requisiti delle integrazioni nel file (una per riga) e delle loro dependencies."""
    wheel = ha_wheel(ha_version)
    if path == "tutte":  # tutte le integrazioni di HA: è l'elenco dell'indice online
        todo = [n.split("/")[2] for n in wheel.namelist()
                if n.startswith("homeassistant/components/") and n.endswith("/manifest.json")]
    else:
        todo = [l.strip() for l in open(path, encoding="utf-8") if l.strip() and not l.startswith("#")]
    seen, reqs = set(), []
    while todo:
        entry = todo.pop()
        if entry in seen:
            continue
        seen.add(entry)
        # "dominio:pacchetto" prende dal manifest solo quel pacchetto, senza le dependencies
        domain, _, only = entry.partition(":")
        manifest = json.loads(wheel.read(f"homeassistant/components/{domain}/manifest.json"))
        if manifest.get("disabled"):  # integrazioni spente da HA stesso (requisiti rotti)
            continue
        reqs += [(Requirement(line), "integrazione " + domain) for line in manifest.get("requirements", [])
                 if not only or canonicalize_name(Requirement(line).name) == canonicalize_name(only)]
        if not only:
            todo += manifest.get("dependencies", [])
    return reqs


def main():
    sys.stdout.reconfigure(encoding="utf-8", newline=chr(10))
    ha_version = sys.argv[1] if len(sys.argv) > 1 else pypi("homeassistant")["info"]["version"]
    here = __file__.rsplit("/", 1)[0].rsplit("\\", 1)[0]
    index_list = sys.argv[2] if len(sys.argv) > 2 else None
    found = {}  # nome -> (versione, classe, richiesto da)
    if index_list:
        queue = manifest_requirements(ha_version, index_list)
    else:
        queue = [(Requirement(f"homeassistant=={ha_version}"), "-")] + manifest_requirements(
            ha_version, here + "/integrazioni-base.txt")
    pool = ThreadPoolExecutor(16)
    while queue:
        batch, queue = queue, []
        new = []
        for req, parent in batch:
            key = canonicalize_name(req.name)
            if key not in found:
                found[key] = None
                new.append((key, req, parent))
        def fetch(item):
            key, req, parent = item
            data = pypi(key)
            version = choose(data, req.specifier)
            detail = pypi(key, version)
            return key, version, parent, classify(detail["urls"]), deps(detail["info"])
        for key, version, parent, cls, reqs in pool.map(fetch, new):
            found[key] = (version, cls, parent)
            queue += [(r, key) for r in reqs]

    rows = sorted((k, *v) for k, v in found.items() if k != "homeassistant")
    count = {c: sum(1 for r in rows if r[2] == c) for c in ("puro", "android", "compilare", "sorgente")}
    if index_list:
        print(f"# Dipendenze delle integrazioni dell'indice, Home Assistant {ha_version}\n")
        print(f"Generato da `scripts/dipendenze-android.py {ha_version} {index_list}` (arm64, Python 3.14). "
              "Queste integrazioni non sono nell'APK: HA scarica le loro librerie con pip quando le aggiungi. "
              "I pacchetti nativi qui sotto vanno compilati e pubblicati nell'indice dei wheel.\n")
    else:
        print(f"# Dipendenze di Home Assistant {ha_version} su Android\n")
        print("Generato da `scripts/dipendenze-android.py` (arm64, Python 3.14). "
              "Dipendenze del core più i requisiti delle integrazioni in `scripts/integrazioni-base.txt`: "
              "sono incluse nell'APK, così HA parte anche senza rete.\n")
    print(f"Totale {len(rows)}: {count['puro']} puri, {count['android']} con wheel Android, "
          f"**{count['compilare']} da compilare**, {count['sorgente']} solo sorgente.\n")
    for cls, title in (("compilare", "Da compilare con cibuildwheel"), ("sorgente", "Solo sorgente (da verificare)"),
                       ("android", "Wheel Android già pubblicato"), ("puro", "Puri")):
        sel = [r for r in rows if r[2] == cls]
        if not sel:
            continue
        print(f"## {title}\n\n| Pacchetto | Versione | Richiesto da |\n| --- | --- | --- |")
        for name, version, _, parent in sel:
            print(f"| {name} | {version} | {parent} |")
        print()


if __name__ == "__main__":
    main()
