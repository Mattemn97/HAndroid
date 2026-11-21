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

# mbedTLS statica per FFmpeg: le telecamere cloud (Tuya) mandano il video in rtsps://, che senza TLS
# FFmpeg non apre ("Protocol not found"). Licenza Apache 2.0: FFmpeg diventa LGPL 3 (--enable-version3), sempre senza parti GPL.
if [ ! -f "$DEPS/lib/libmbedtls.a" ]; then
  MBEDTLS=3.6.4
  rm -rf /tmp/mbedtls && mkdir -p /tmp/mbedtls
  curl -fsSL "https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-$MBEDTLS/mbedtls-$MBEDTLS.tar.bz2" | tar xj -C /tmp/mbedtls
  cmake -S /tmp/mbedtls/mbedtls-$MBEDTLS -B /tmp/mbedtls/b -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$SDK/ndk/$NDK_VERSION/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=24 -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
    -DENABLE_TESTING=OFF -DENABLE_PROGRAMS=OFF -DCMAKE_INSTALL_PREFIX="$DEPS" >/dev/null
  cmake --build /tmp/mbedtls/b -j"$(nproc)" >/dev/null && cmake --install /tmp/mbedtls/b >/dev/null
  rm -rf /tmp/mbedtls "$DEPS/lib/libavformat.so"   # FFmpeg si ricompila con TLS
fi

# FFmpeg condivisa per av (telecamere e stream di HA) e l'eseguibile ffmpeg (istantanee, integrazione
# ffmpeg). --target-os=android dà nomi senza versione (libavcodec.so): l'APK li porta in jniLibs, dove
# il linker di Android li trova come libpython; ffmpeg le trova con LD_LIBRARY_PATH, impostato da
# handroid_boot.py per i processi figli. Gli add-on GPL (x264...) restano fuori: licenza LGPL.
if [ ! -f "$DEPS/lib/libavformat.so" ]; then
  FFMPEG=8.1.3
  TC="$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin"
  rm -rf /tmp/ffmpeg && mkdir -p /tmp/ffmpeg
  curl -fsSL "https://ffmpeg.org/releases/ffmpeg-$FFMPEG.tar.xz" | tar xJ -C /tmp/ffmpeg
  (cd /tmp/ffmpeg/ffmpeg-$FFMPEG \
    && ./configure --prefix="$DEPS" --target-os=android --arch=aarch64 --enable-cross-compile \
         --cc="$TC/aarch64-linux-android24-clang" \
         --ar="$TC/llvm-ar" --ranlib="$TC/llvm-ranlib" --nm="$TC/llvm-nm" --strip="$TC/llvm-strip" \
         --enable-shared --disable-static --enable-pic --disable-doc --disable-ffplay --disable-ffprobe \
         --disable-debug --enable-mbedtls --enable-version3 \
         --extra-cflags="-I$DEPS/include" --extra-ldflags="-L$DEPS/lib" >/dev/null \
    && make -j"$(nproc)" >/dev/null && make install >/dev/null)
  rm -rf /tmp/ffmpeg
fi

# cloudflared per l'accesso da fuori casa (Tunnel.kt). Compilato per GOOS=android con cgo: quello ufficiale
# per Linux cerca i DNS in /etc/resolv.conf, che su Android non c'è, mentre così usa il resolver di sistema.
if [ ! -f "$DEPS/bin/cloudflared" ]; then
  GO=1.27.4; CLOUDFLARED=2026.11.1
  TC="$SDK/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin"
  if [ ! -x "$HOME/go-sdk/bin/go" ]; then
    mkdir -p "$HOME/go-sdk"
    curl -fsSL "https://go.dev/dl/go$GO.linux-amd64.tar.gz" | tar xz -C "$HOME/go-sdk" --strip-components=1
  fi
  rm -rf /tmp/cloudflared && git clone -q --depth 1 --branch "$CLOUDFLARED" https://github.com/cloudflare/cloudflared /tmp/cloudflared
  (cd /tmp/cloudflared && GOTOOLCHAIN=local GOOS=android GOARCH=arm64 CGO_ENABLED=1 CC="$TC/aarch64-linux-android24-clang" \
    "$HOME/go-sdk/bin/go" build -trimpath -ldflags "-s -w -X main.Version=$CLOUDFLARED" -o "$DEPS/bin/cloudflared" ./cmd/cloudflared)
  rm -rf /tmp/cloudflared
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
cp "$DEPS"/lib/lib{av,sw}*.so "$SYSLIB/"  # FFmpeg, per collegare av

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
