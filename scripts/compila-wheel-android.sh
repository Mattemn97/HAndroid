#!/usr/bin/env bash
# =============================================================================
#  compila-wheel-android.sh
#  Compila per Android (arm64, Python 3.14) i pacchetti della sezione
#  "Da compilare" di docs/dipendenze-android.md. I wheel finiscono in wheels/.
#
#  Requisiti: Linux o macOS (anche WSL), Android SDK con NDK (ANDROID_HOME),
#  pip install cibuildwheel; per i pacchetti Rust anche
#  rustup target add aarch64-linux-android
#
#  Uso: scripts/compila-wheel-android.sh [pacchetto ...]   (default: tutti)
#       DEPS=docs/dipendenze-indice.md scripts/compila-wheel-android.sh   (pacchetti per l'indice)
#  Salta i pacchetti che hanno già un wheel in wheels/ (FORZA=1 per ricompilarli).
#  A fine giro stampa quali sono riusciti e quali no.
# =============================================================================
set -uo pipefail
cd "$(dirname "$0")/.."

DEPS="${DEPS:-docs/dipendenze-android.md}"
mkdir -p wheels build/sdist

# righe "| nome | versione |" delle sezioni "Da compilare" e "Solo sorgente" (hanno solo lo sdist,
# a volte con codice C: es. pyspeex-noise), senza i pacchetti in android-rinviati.txt
SKIP='^('$(awk '!/^#/ && NF {printf "%s%s", s, $1; s="|"}' scripts/android-rinviati.txt)')=='
list=$(awk '/^## /{on=/^## (Da compilare|Solo sorgente)/} on' "$DEPS" \
  | sed -n 's/^| \([a-z0-9._-]*\) | \([0-9][^ ]*\) | .*/\1==\2/p' | grep -vE "$SKIP")

# Librerie C per Android preparate da prepara-wsl.sh. PIP_FIND_LINKS serve ai pacchetti che per
# compilarsi hanno bisogno di altri wheel Android (cryptography e pynacl usano cffi).
# GRPC_PYTHON_BUILD_SYSTEM_OPENSSL: la BoringSSL di grpcio va in conflitto con gli header OpenSSL di CPython
# (grpcio è comunque rinviato: vedi android-rinviati.txt).
# PY_INCLUDE: la parte Rust di cryptography compila codice C senza ricevere gli header di Python.
# SODIUM_INSTALL=system: pynacl usa la libsodium compilata da prepara-wsl.sh invece della sua.
ANDROID_DEPS="$HOME/android-deps/aarch64-linux-android"
PY_INCLUDE='$(python -c "import sysconfig; print(sysconfig.get_path(\"include\"))")'
# cibuildwheel riscrive CFLAGS e LDFLAGS dopo questo ambiente: i percorsi passano da CPATH e
# LIBRARY_PATH, che clang legge comunque.
# RUSTFLAGS: su Android ogni estensione deve essere collegata a libpython; cibuildwheel lo fa via
# LDFLAGS, che Rust (maturin) ignora. Senza, cryptography fallisce all'import: "cannot locate symbol PyLong_Type".
PY_LIBDIR='$(python -c "import sysconfig; print(sysconfig.get_config_var(\"LIBDIR\"))")'
export CIBW_ENVIRONMENT_ANDROID="RUSTFLAGS=\"-C link-arg=-L$PY_LIBDIR -C link-arg=-lpython3.14\" CPATH=\"$ANDROID_DEPS/include:$PY_INCLUDE\" LIBRARY_PATH=$ANDROID_DEPS/lib PKG_CONFIG_PATH=$ANDROID_DEPS/lib/pkgconfig PIP_FIND_LINKS=$PWD/wheels GRPC_PYTHON_BUILD_SYSTEM_OPENSSL=1 SODIUM_INSTALL=system"
if [ $# -gt 0 ]; then
  list=$(for p in "$@"; do echo "$list" | grep "^$p=="; done)
fi

mkdir -p build/log
ok=() puri=() ko=() gia=()
build() {  # $1 = log; variabili CIBW_* aggiuntive dall'ambiente
  CIBW_BUILD="cp314-*" cibuildwheel --platform android --archs arm64_v8a \
    --output-dir wheels build/sdist/* >"$1" 2>&1
}
for req in $list; do
  name=${req%%==*} version=${req##*==}
  if [ -z "${FORZA:-}" ] && { compgen -G "wheels/${name//-/_}-$version-*.whl" || compgen -G "wheels/$name-$version-*.whl"; } >/dev/null; then
    gia+=("$req"); continue
  fi
  echo "==> $req"
  log="build/log/${req%%==*}.log"
  rm -rf build/sdist/*
  if ! pip download --no-binary "${req%%==*}" --no-deps -d build/sdist "$req" >"$log" 2>&1; then
    ko+=("$req"); continue
  fi
  # impostazioni per singolo pacchetto
  extra=()
  case "${req%%==*}" in
    # pillow: lo script before-all non è nello sdist; JPEG spento (manca libjpeg per Android);
    # platform-guessing=disable evita che aggiunga gli header di Linux (/usr/include); zlib arriva da ZLIB_ROOT
    pillow) extra=(CIBW_BEFORE_ALL_ANDROID="" CIBW_CONFIG_SETTINGS_ANDROID="jpeg=disable platform-guessing=disable"
                   CIBW_ENVIRONMENT_ANDROID="$CIBW_ENVIRONMENT_ANDROID ZLIB_ROOT=$ANDROID_DEPS") ;;
    # numpy: il suo pyproject abilita "cpython-freethreading", gruppo che cibuildwheel non conosce più e
    # che CIBW_ENABLE non scavalca: si toglie la riga dallo sdist
    # psutil: Android è un Linux, ma psutil guarda sys.platform == "linux" e si rifiuta di compilare
    psutil) (cd build/sdist && f=$(ls psutil-*.tar.gz) && d=${f%.tar.gz} && tar xzf "$f" && rm "$f" \
             && sed -i 's/^LINUX = sys.platform.startswith("linux")/LINUX = sys.platform.startswith(("linux", "android"))/' "$d/psutil/_common.py" \
             && grep -q '"android"' "$d/psutil/_common.py" && tar czf "$f" "$d" && rm -rf "$d")
           extra=(CIBW_TEST_SKIP="*") ;;
    numpy) (cd build/sdist && f=$(ls numpy-*.tar.gz) && d=${f%.tar.gz} && tar xzf "$f" && rm "$f" \
             && sed -i '/^enable = \["cpython-freethreading"/d' "$d/pyproject.toml" && tar czf "$f" "$d" && rm -rf "$d")
           # senza OpenBLAS (non c'è per Android; a HA basta numpy di base), come per Pyodide; niente test sul telefono
           extra=(CIBW_BEFORE_BUILD_ANDROID="" CIBW_TEST_SKIP="*"
                  CIBW_CONFIG_SETTINGS_ANDROID="build-dir=build setup-args=-Dblas=none setup-args=-Dlapack=none setup-args=-Dallow-noblas=true setup-args=--cross-file=$PWD/scripts/android-numpy.cross") ;;
  esac
  if env "${extra[@]}" bash -c "$(declare -f build); build '$log'"; then
    ok+=("$req")
  # estensione C facoltativa non compilata (es. pyyaml senza libyaml): auditwheel rifiuta un wheel
  # senza codice nativo, quindi si tiene così com'è
  elif grep -q "does not look like a platform wheel" "$log" \
       && CIBW_REPAIR_WHEEL_COMMAND_ANDROID="cp {wheel} {dest_dir}" build "$log"; then
    puri+=("$req")
  # nessun codice nativo (solo sdist, ma puro): wheel py3-none-any, così il telefono non deve costruirlo
  elif grep -q "pure Python wheel was generated" "$log" \
       && { pip wheel --no-deps --no-binary "$name" -w wheels "$req" >>"$log" 2>&1
            # pip a volte esce con errore dopo aver salvato il wheel: conta il file
            compgen -G "wheels/${name//-/_}-$version-*.whl" >/dev/null || compgen -G "wheels/$name-$version-*.whl" >/dev/null; }; then
    puri+=("$req")
  else
    ko+=("$req")
    tail -5 "$log"
  fi
done
echo
echo "Già in wheels/:           ${#gia[@]}"
echo "Compilati:                ${ok[*]:-nessuno}"
echo "Senza estensione nativa:  ${puri[*]:-nessuno}"
echo "Falliti (log in build/log): ${ko[*]:-nessuno}"
