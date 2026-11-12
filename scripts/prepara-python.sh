#!/usr/bin/env bash
# Scarica CPython ufficiale per Android e lo prepara per la build dell'APK:
#   app/build/python/prefix         header e libpython per il launcher JNI
#   app/src/main/jniLibs/arm64-v8a  librerie native caricate da Android
#   app/src/main/assets/python.zip  stdlib + codice Python dell'app
# Si lancia dalla radice della repo, prima di gradle. Il download resta in cache.
set -euo pipefail

PYTHON_VERSION="${PYTHON_VERSION:-3.14.10}"
NOME="python-${PYTHON_VERSION}-aarch64-linux-android.tar.gz"
CACHE="app/build/python-dl"
DEST="app/build/python"
JNI="app/src/main/jniLibs/arm64-v8a"
ASSETS="app/src/main/assets"

mkdir -p "$CACHE" "$JNI" "$ASSETS"
if [ ! -s "$CACHE/$NOME" ]; then
  curl -fsSL -o "$CACHE/$NOME" "https://www.python.org/ftp/python/${PYTHON_VERSION}/${NOME}"
fi

rm -rf "$DEST" && mkdir -p "$DEST"
tar xzf "$CACHE/$NOME" -C "$DEST"

# Solo i file *.so: i nomi con versione (libsqlite3.so.0) Android non li installa.
rm -f "$JNI"/*.so
cp "$DEST"/prefix/lib/*.so "$JNI"/

STAGE="$DEST/stage"
mkdir -p "$STAGE/lib" "$STAGE/app"
cp -r "$DEST/prefix/lib/python3.14" "$STAGE/lib/"
rm -rf "$STAGE"/lib/python3.14/{test,idlelib,tkinter,turtledemo,config-3.14-*}
find "$STAGE" -name __pycache__ -prune -exec rm -rf {} +
# Codice app, compresa custom_components/handroid (integrazione add-on, 0007)
cp -r app/src/main/python/. "$STAGE/app/"

rm -f "$ASSETS/python.zip"
(cd "$STAGE" && zip -qr -X "$OLDPWD/$ASSETS/python.zip" .)
echo "Python $PYTHON_VERSION pronto: $(ls "$JNI" | wc -l) librerie, python.zip $(du -h "$ASSETS/python.zip" | cut -f1)"
