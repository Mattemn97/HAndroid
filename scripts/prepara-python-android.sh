#!/usr/bin/env bash
# =============================================================================
#  prepara-python-android.sh
#  Prepara Python e Home Assistant da mettere nell'APK. Da lanciare sul PC prima della build.
#
#  1. scarica CPython ufficiale per Android (arm64) in app/python
#  2. copia libpython e le sue librerie in app/src/main/jniLibs/arm64-v8a e compila con l'NDK
#     il lanciatore libhpython.so (app/src/main/cpp/hpython.c), il "python" dei processi figli
#  3. installa Home Assistant e le dipendenze per Android in $WORK/site-packages,
#     usando i wheel compilati in wheels/ (vedi compila-wheel-android.sh)
#  4. crea app/src/main/assets/python.zip (stdlib + site-packages), senza traduzioni nelle altre
#     lingue, frontend per browser vecchi, mappe di debug e copie .gz/.br
#  5. mette in python.zip (cartella wheels/) i wheel Android dei pacchetti di docs/dipendenze-indice.md
#     non già installati: pip li trova lì quando HA installa un'integrazione (vedi handroid_boot.py)
#  Se requisiti, wheel, file Python dell'app e lingue non cambiano, i passi 3 e 4 si saltano.
#
#  Variabili: PY_VERSION (default 3.14.10), PIP_VERSION (pip incluso per le installazioni
#    a runtime, default 26.3), ANDROID_HOME e NDK_VERSION (per il lanciatore), PYTHON (python del PC, default python3 o python),
#    LANGS (lingue tenute, default "en it"), WORK (cartella di lavoro: su WSL default
#    ~/.cache/handroid, perché i file su /mnt/c sono molto più lenti; altrove build/python)
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

PY_VERSION="${PY_VERSION:-3.14.10}"
PIP_VERSION="${PIP_VERSION:-26.3}"
NDK_VERSION="${NDK_VERSION:-27.3.13750724}"
PYTHON="${PYTHON:-$(command -v python3 || command -v python)}"
PLATFORM=android_24_arm64_v8a
DEPS=docs/dipendenze-android.md
LANGS="${LANGS:-en it}"
case "$PWD" in /mnt/*) WORK="${WORK:-$HOME/.cache/handroid}" ;; *) WORK="${WORK:-build/python}" ;; esac

echo "==> CPython $PY_VERSION per Android"
if [ ! -f app/python/prefix/lib/libpython3.14.so ]; then
  rm -rf app/python && mkdir -p app/python
  curl -fL -o app/python/python.tar.gz \
    "https://www.python.org/ftp/python/$PY_VERSION/python-$PY_VERSION-aarch64-linux-android.tar.gz"
  # su Git Bash i symlink diventano copie e falliscono se il bersaglio non è ancora estratto: secondo giro
  tar xzf app/python/python.tar.gz -C app/python 2>/dev/null || tar xzf app/python/python.tar.gz -C app/python
  rm app/python/python.tar.gz
fi
mkdir -p app/src/main/jniLibs/arm64-v8a
cp app/python/prefix/lib/libpython3*.so app/python/prefix/lib/*_python.so app/src/main/jniLibs/arm64-v8a/

echo "==> Lanciatore python (libhpython.so)"
# $ORIGIN: l'eseguibile trova libpython3.14.so nella sua stessa cartella
CLANG="${ANDROID_HOME:?manca ANDROID_HOME (Android SDK con NDK)}/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang"
"$CLANG" -O2 -fPIE -pie -I app/python/prefix/include/python3.14 app/src/main/cpp/hpython.c   -L app/python/prefix/lib -lpython3.14 -Wl,-rpath,'$ORIGIN' -o app/src/main/jniLibs/arm64-v8a/libhpython.so

echo "==> Home Assistant e dipendenze (versioni da $DEPS)"
# i pacchetti in android-rinviati.txt restano fuori dall'APK (motivi nel file)
SKIP='^('$(awk '!/^#/ && NF {printf "%s%s", s, $1; s="|"}' scripts/android-rinviati.txt)')=='
HA_VERSION=$(sed -n 's/^# Dipendenze di Home Assistant \([^ ]*\) .*/\1/p' "$DEPS")
mkdir -p build wheels "$WORK"
{
  echo "homeassistant==$HA_VERSION"
  echo "pip==$PIP_VERSION"
  sed -n 's/^| \([a-z0-9._-]*\) | \([0-9][^ ]*\) | .*/\1==\2/p' "$DEPS" | grep -vE "$SKIP"
} > build/requisiti-android.txt

# i pacchetti nativi devono arrivare da wheels/: pip altrimenti li compilerebbe per il PC
missing=()
for req in $(sed -n '/^## Da compilare/,/^## /p' "$DEPS" \
    | sed -n 's/^| \([a-z0-9._-]*\) | \([0-9][^ ]*\) | .*/\1==\2/p' | grep -vE "$SKIP"); do
  name=${req%%==*} version=${req##*==}
  ls wheels/"${name//-/_}"-"$version"-*android*arm64*.whl >/dev/null 2>&1 || missing+=("$req")
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "Mancano ${#missing[@]} wheel Android in wheels/ (lancia scripts/compila-wheel-android.sh):"
  printf '  %s\n' "${missing[@]}"
  exit 1
fi

# impronta di tutto ciò che finisce nello zip: se non cambia, lo zip attuale va già bene
stamp=$(cat build/requisiti-android.txt app/src/main/python/*.py "$0" <(ls -l wheels) <(echo "$LANGS")   docs/dipendenze-indice.md 2>/dev/null | sha1sum)
if [ -f app/src/main/assets/python.zip ] && [ "$stamp" = "$(cat "$WORK/impronta" 2>/dev/null)" ]; then
  echo "Nessuna modifica: python.zip è già aggiornato"
  exit 0
fi
rm -rf "$WORK/site-packages" "$WORK/impronta" && mkdir -p "$WORK/site-packages"

# --only-binary blocca la compilazione per il PC; pyric e pyturbojpeg hanno solo il sorgente ma sono puri
"$PYTHON" -m pip install -q --no-deps --no-compile --only-binary=:all: --no-binary=pyric,pyturbojpeg \
  --target "$WORK/site-packages" \
  --platform "$PLATFORM" --platform android_21_arm64_v8a \
  --python-version "$PY_VERSION" --implementation cp --abi cp314 --abi abi3 --abi none \
  --find-links wheels \
  -r build/requisiti-android.txt
cp app/src/main/python/*.py "$WORK/site-packages/"

# wheel per le integrazioni installate a runtime (sezione "Da compilare" di dipendenze-indice.md)
rm -rf "$WORK/wheels" && mkdir -p "$WORK/wheels"
if [ -f docs/dipendenze-indice.md ]; then
  for req in $(awk '/^## /{on=/^## (Da compilare|Solo sorgente)/} on' docs/dipendenze-indice.md \
      | sed -n 's/^| \([a-z0-9._-]*\) | \([0-9][^ ]*\) | .*/\1==\2/p'); do
    grep -qx "$req" build/requisiti-android.txt && continue
    name=${req%%==*} version=${req##*==}
    cp wheels/"${name//-/_}"-"$version"-*android*arm64*.whl "$WORK/wheels/" 2>/dev/null \
      || cp wheels/"${name//-/_}"-"$version"-*none-any.whl "$WORK/wheels/" 2>/dev/null       || echo "  manca il wheel di $req (integrazioni che lo usano non si installeranno)"
  done
fi

# .pyc compilati qui: sul telefono il primo avvio non deve compilare migliaia di file.
# unchecked-hash: Python non confronta le date dei sorgenti, che cambiano quando l'app estrae lo zip.
# Serve un Python del PC della stessa versione minore (3.14).
"$PYTHON" -c 'import sys; sys.exit(sys.version_info[:2] != (3, 14))'   || { echo "Per i .pyc serve Python 3.14 sul PC (PYTHON=...)"; exit 1; }
# la stdlib si copia in $WORK: compilare dentro /mnt/c è lentissimo
rm -rf "$WORK/stdlib" && cp -r app/python/prefix/lib/python3.14 "$WORK/stdlib"
"$PYTHON" -m compileall -q -j0 --invalidation-mode unchecked-hash "$WORK/site-packages" "$WORK/stdlib" >/dev/null || true

echo "==> python.zip"
mkdir -p app/src/main/assets
# lo zip si scrive in $WORK e si copia alla fine: una sola scrittura grande su /mnt/c
WORK="$WORK" LANGS="$LANGS" "$PYTHON" - <<'PY'
import os, pathlib, re, zipfile
skip = {"test", "idlelib", "tkinter", "turtledemo", "frontend_es5"}
langs = os.environ["LANGS"].split()
work = pathlib.Path(os.environ["WORK"])

def keep(rel):
    if skip & set(rel.parts) or rel.suffix in (".map", ".gz", ".br"):
        return False
    # in __pycache__ solo i .pyc di questa versione (compileall li ha appena creati)
    if "__pycache__" in rel.parts:
        return ".cpython-314." in rel.name and ".opt-" not in rel.name
    # traduzioni: homeassistant/.../translations/it.json, hass_frontend/static/translations/[...]/it-<hash>.json
    if "translations" in rel.parts and rel.suffix == ".json":
        return re.split(r"[-_.]", rel.name)[0] in langs
    return True

out = work / "python.zip"
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for root, prefix in ((work / "stdlib", "lib/python3.14"),
                         (work / "site-packages", "site-packages"),
                         (work / "wheels", "wheels")):
        base = pathlib.Path(root)
        for f in base.rglob("*"):
            rel = f.relative_to(base)
            if f.is_file() and keep(rel):
                z.write(f, f"{prefix}/{rel.as_posix()}")
    n = len(z.infolist())
print(f"python.zip: {n} file, {out.stat().st_size // 2**20} MB")
PY
cp "$WORK/python.zip" app/src/main/assets/python.zip
echo "$stamp" > "$WORK/impronta"
echo "Pronto: ora compila l'APK da Android Studio o con ./gradlew assembleDebug"
