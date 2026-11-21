#!/usr/bin/env bash
# =============================================================================
#  rilascio.sh
#  Prepara un rilascio di HAndroid: APK firmato in build/rilascio/HAndroid-<versione>.apk, con
#  impronta SHA-256 e note prese dal registro. Con PUBBLICA=1 crea anche il rilascio su GitHub
#  (tag v<versione>, segnato come ultimo rilascio), che "Cerca aggiornamenti" nell'app e installa-handroid.sh trovano da soli.
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
rm -rf app/build/intermediates/apk app/build/outputs/apk/github/usb   # evita APK gonfiati dalla build incrementale
"$GRADLE" -q testGithubDebugUnitTest assembleGithubUsb

mkdir -p build/rilascio
APK="build/rilascio/HAndroid-$VERSIONE.apk"
# "usb": la release firmata ma debuggable, così dal PC si può lavorare sui file di HA (AGENTS.md)
cp app/build/outputs/apk/github/usb/app-github-usb.apk "$APK"
SHA=$(sha256sum "$APK" | cut -d' ' -f1)

# note: la voce più recente del registro
NOTE=build/rilascio/note-$VERSIONE.md
{
  awk '/^## /{n++} n==1' docs/registro.md
  echo
  echo "SHA-256 di \`HAndroid-$VERSIONE.apk\`: \`$SHA\`"
  echo
  echo "Su GitHub c'è solo il codice: per compilare e installare l'app vedi \`docs/guida-sviluppo.md\`. I rilasci 0.4.0 e 0.5.0 restano come archivio con l'APK."
} > "$NOTE"
echo "$APK ($(du -h "$APK" | cut -f1)), SHA-256 $SHA"

[ -n "${PUBBLICA:-}" ] || { echo "Prova: niente pubblicato (PUBBLICA=1 per il rilascio su GitHub)"; exit 0; }
git tag -a "$TAG" -m "HAndroid $VERSIONE"
git push -q origin "$TAG"
# sempre "latest": l'app ("Cerca aggiornamenti") e installa-handroid.sh leggono /releases/latest,
# che salta le pre-release
# dalla 0.6.0 su GitHub solo codice e note (docs/roadmap.md, "Modello"): l'APK resta in build/rilascio
# per il proprio telefono; quello pronto per tutti arriva dal Play Store
gh release create "$TAG" --title "HAndroid $VERSIONE" --notes-file "$NOTE" --latest
echo "Pubblicato: https://github.com/Mattemn97/HAndroid/releases/tag/$TAG"
