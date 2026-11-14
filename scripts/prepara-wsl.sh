#!/usr/bin/env bash
# =============================================================================
#  prepara-wsl.sh
#  Prepara Ubuntu (WSL o Linux nativo) per compilare i wheel Android e gli add-on nativi.
#  Da lanciare una volta, come utente normale con sudo:
#     bash scripts/prepara-wsl.sh
#
#  Installa:
#   - pacchetti di build (gcc, cmake, autotools, pkg-config, Java 17)
#   - Android SDK command-line tools + NDK della versione richiesta da CPython
#   - Python 3.14 (con uv) e cibuildwheel in ~/.venv-handroid
#   - Rust con il target aarch64-linux-android
#  Alla fine scrive le variabili in ~/.handroid-env, caricato da ~/.bashrc.
# =============================================================================
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
NDK_VERSION="${NDK_VERSION:-27.3.13750724}"  # deve coincidere con ndk_version in android-env.sh di CPython

echo "==> Pacchetti di sistema"
sudo apt-get update -q
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -q \
  build-essential git curl unzip zip pkg-config cmake ninja-build autoconf automake libtool \
  openjdk-17-jdk-headless python3-venv

echo "==> Android SDK in $SDK"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  zip=$(curl -s https://dl.google.com/android/repository/repository2-3.xml \
    | grep -oE 'commandlinetools-linux-[0-9]+_latest.zip' | sort -uV | tail -1)
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSLo /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/$zip"
  unzip -q /tmp/cmdline-tools.zip -d "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm /tmp/cmdline-tools.zip
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platform-tools" "ndk;$NDK_VERSION" | grep -v '^\[' || true

echo "==> Librerie C per Android (~/android-deps)"
# libffi statica, la stessa usata da CPython: serve a cffi (e quindi a cryptography, pynacl, pycares)
DEPS="$HOME/android-deps/aarch64-linux-android"
if [ ! -f "$DEPS/lib/libffi.a" ]; then
  mkdir -p "$DEPS"
  curl -fsSL https://github.com/beeware/cpython-android-source-deps/releases/download/libffi-3.4.4-3/libffi-3.4.4-3-aarch64-linux-android.tar.gz \
    | tar xz -C "$DEPS"
fi

# libsodium statica per pynacl: quella inclusa nel suo sdist non sa compilare in cross per Android
if [ ! -f "$DEPS/lib/libsodium.a" ]; then
  SODIUM=1.0.20
  TC="$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin"
  rm -rf /tmp/libsodium && mkdir -p /tmp/libsodium
  curl -fsSL "https://download.libsodium.org/libsodium/releases/libsodium-$SODIUM.tar.gz" | tar xz -C /tmp/libsodium
  (cd /tmp/libsodium/libsodium-$SODIUM \
    && CC="$TC/aarch64-linux-android24-clang" AR="$TC/llvm-ar" RANLIB="$TC/llvm-ranlib" \
       CFLAGS="-O2 -fPIC" ./configure --host=aarch64-linux-android --prefix="$DEPS" \
         --disable-shared --enable-static --with-pic >/dev/null \
    && make -j"$(nproc)" >/dev/null && make install >/dev/null)
  rm -rf /tmp/libsodium
fi

# zlib c'è già nell'NDK: collegamenti in $DEPS perché pillow la cerca con ZLIB_ROOT
SYSROOT="$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr"
ln -sf "$SYSROOT/include/zlib.h" "$SYSROOT/include/zconf.h" "$DEPS/include/"
ln -sf "$SYSROOT/lib/aarch64-linux-android/24/libz.so" "$DEPS/lib/"

# cibuildwheel riscrive LDFLAGS e clang ignora LIBRARY_PATH in cross: le librerie statiche vanno
# nella cartella dell'NDK dove il linker cerca sempre (header via CPATH, vedi compila-wheel-android.sh)
SYSLIB="$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24"
# librt e libpthread su Android stanno dentro libc: archivi vuoti per chi le chiede (awscrt, miniaudio)
for l in rt pthread; do
  [ -f "$DEPS/lib/lib$l.a" ] || "$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar" rc "$DEPS/lib/lib$l.a"
done
cp "$DEPS"/lib/*.a "$SYSLIB/"

echo "==> Python 3.14 e cibuildwheel"
command -v uv >/dev/null || curl -LsSf https://astral.sh/uv/install.sh | sh
export PATH="$HOME/.local/bin:$PATH"
uv python install 3.14
[ -d "$HOME/.venv-handroid" ] || uv venv --python 3.14 "$HOME/.venv-handroid"
uv pip install --python "$HOME/.venv-handroid/bin/python" -U cibuildwheel pip

echo "==> Rust"
command -v rustup >/dev/null || curl -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal
. "$HOME/.cargo/env"
rustup target add aarch64-linux-android

cat > "$HOME/.handroid-env" <<EOF
export ANDROID_HOME="$SDK"
export PATH="\$HOME/.venv-handroid/bin:\$HOME/.local/bin:\$HOME/.cargo/bin:\$PATH"
EOF
grep -q handroid-env "$HOME/.bashrc" || echo '. "$HOME/.handroid-env"' >> "$HOME/.bashrc"
echo "Pronto. Apri una nuova shell (o: . ~/.handroid-env) e lancia scripts/compila-wheel-android.sh"
