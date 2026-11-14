#!/usr/bin/env bash
# =============================================================================
#  rilascio.sh
#  Prepara un rilascio di HAndroid: APK firmato in build/rilascio/HAndroid-<versione>.apk, con
#  impronta SHA-256 e note prese dal registro. Con PUBBLICA=1 crea anche il rilascio su GitHub
#  (tag v<versione>), che "Cerca aggiornamenti" nell'app e installa-handroid.sh trovano da soli.
#
#  Prima: scripts/prepara-python-android.sh (python.zip aggiornato) e keystore.properties nella
#  radice del repo (fuori da git):
#     storeFile=/percorso/handroid.jks
#     storePassword=...
#     keyAlias=handroid
#     keyPassword=...
#  La chiave va conservata per sempre: un APK firmato con un'altra chiave non aggiorna quello
#  installato, e per cambiarla bisogna disinstallare (perdendo la configurazione di HA).
#
#  Uso: scripts/rilascio.sh             (solo APK e note)
#       PUBBLICA=1 scripts/rilascio.sh  (anche tag e rilascio su GitHub)
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f keystore.properties ] || { echo "Manca keystore.properties (vedi l'intestazione dello script)." >&2; exit 1; }
[ -f app/src/main/assets/python.zip ] || { echo "Manca python.zip: lancia prima scripts/prepara-python-android.sh" >&2; exit 1; }
VERSIONE=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)
TAG="v$VERSIONE"
if [ -n "${PUBBLICA:-}" ]; then
  [ -z "$(git status --porcelain -- app scripts docs)" ] || { echo "Ci sono modifiche non salvate: fai prima il commit." >&2; exit 1; }
  ! git rev-parse -q --verify "refs/tags/$TAG" >/dev/null || { echo "Il tag $TAG esiste già: alza versionName." >&2; exit 1; }
fi

GRADLE=./gradlew; [ -f gradlew.bat ] && [ "${OS:-}" = Windows_NT ] && GRADLE=./gradlew.bat
rm -rf app/build/intermediates/apk app/build/outputs/apk/release   # evita APK gonfiati dalla build incrementale
"$GRADLE" -q testDebugUnitTest assembleRelease

mkdir -p build/rilascio
APK="build/rilascio/HAndroid-$VERSIONE.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"
SHA=$(sha256sum "$APK" | cut -d' ' -f1)

# note: la voce più recente del registro
NOTE=build/rilascio/note-$VERSIONE.md
{
  awk '/^## /{n++} n==1' docs/registro.md
  echo
  echo "SHA-256 di \`HAndroid-$VERSIONE.apk\`: \`$SHA\`"
  echo
  echo "Installazione: \`scripts/installa-handroid.sh\` dal PC, oppure scarica l'APK sul telefono e aprilo."
} > "$NOTE"
echo "$APK ($(du -h "$APK" | cut -f1)), SHA-256 $SHA"

[ -n "${PUBBLICA:-}" ] || { echo "Prova: niente pubblicato (PUBBLICA=1 per il rilascio su GitHub)"; exit 0; }
git tag -a "$TAG" -m "HAndroid $VERSIONE"
git push -q origin "$TAG"
gh release create "$TAG" "$APK" --title "HAndroid $VERSIONE" --notes-file "$NOTE" \
  $(case "$VERSIONE" in *-*) echo --prerelease ;; esac)
echo "Pubblicato: https://github.com/Mattemn97/HAndroid/releases/tag/$TAG"
