# Guida allo sviluppo di HAndroid

Questa guida serve a chi vuole compilare l'app, lavorare sul codice o creare add-on. Va tenuta aggiornata a ogni modifica: se un passaggio qui non funziona più, è un bug della guida.

Stato attuale: **M0 (fattibilità) in corso**. Sull'emulatore HA parte e mostra la configurazione iniziale su `:8123`; manca la prova sul Xiaomi 12T. Lo storico dei lavori è in [registro.md](registro.md).

## Indice

1. [Come è fatto il progetto](#come-è-fatto-il-progetto)
2. [Preparare il PC](#preparare-il-pc)
3. [Compilare l'app](#compilare-lapp)
4. [Provare sul telefono](#provare-sul-telefono)
5. [Aggiornare Home Assistant](#aggiornare-home-assistant)
6. [Lavorare sulle dipendenze native](#lavorare-sulle-dipendenze-native)
7. [Sviluppare un add-on](#sviluppare-un-add-on)
8. [Problemi noti](#problemi-noti)

## Come è fatto il progetto

L'APK contiene l'interprete CPython ufficiale per Android, Home Assistant con le sue librerie e un'app Kotlin che lo avvia e lo tiene acceso. Il progetto completo, con la roadmap, è in [progetto-handroid.md](progetto-handroid.md).

```
HAndroid/
├── app/                          app Android
│   ├── build.gradle.kts          SDK 35, NDK 27.3, solo arm64
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/it/handroid/
│       │   ├── MainActivity.kt   schermata: avvia HA, mostra l'indirizzo
│       │   ├── HaService.kt      servizio in primo piano, processo ":ha", estrae Python e avvia HA
│       │   └── PythonRunner.kt   ponte JNI verso libpython
│       ├── cpp/handroid.c        Py_InitializeFromConfig + Py_RunMain
│       ├── python/handroid_boot.py  adattamenti di HA ad Android (HA non si modifica)
│       └── assets/configuration.yaml  configurazione iniziale di HA
├── scripts/
│   ├── dipendenze-android.py     analizza le dipendenze di HA → docs/dipendenze-android.md
│   ├── prepara-wsl.sh            prepara Ubuntu/WSL per compilare i wheel
│   ├── compila-wheel-android.sh  compila i pacchetti nativi per Android → wheels/
│   ├── prepara-python-android.sh mette Python + HA nell'APK
│   ├── prepara-server-xiaomi.sh  prepara il telefono via ADB (strada A del README)
│   └── installa-homeassistant-android.sh  piano B con Termux
└── docs/
    ├── guida-sviluppo.md         questa guida
    ├── registro.md               cosa è stato fatto e quando
    ├── progetto-handroid.md      obiettivi, architettura, roadmap
    ├── dipendenze-android.md     generato: pacchetti di HA e stato su Android
    ├── addon/                    formato e guida degli add-on
    └── specs/                    una specifica per funzione (NNNN-titolo.md)
```

Le cartelle `app/python/`, `app/src/main/jniLibs/`, `app/src/main/assets/python.zip`, `wheels/` e `build/` sono **generate dagli script** e non stanno in git.

### Come parte Home Assistant

1. `MainActivity` → "Avvia" fa partire `HaService` come servizio in primo piano.
2. `HaService` gira nel processo separato `:ha`. Al primo avvio dopo ogni installazione estrae `python.zip` in `files/python`. Poi scrive i DNS della rete in `files/dns.txt` e chiama `PythonRunner.runModule`, che passa a Python i percorsi via JNI. `handroid_boot.py` imposta `HOME` e `TMPDIR` partendo da `--config`.
3. `handroid.c` inizializza CPython con `home = files/python` e avvia il modulo `handroid_boot`.
4. `handroid_boot` applica gli adattamenti (oggi: salta il controllo del sistema operativo) e chiama il `main()` di Home Assistant con `--config files/ha-config --skip-pip`.
5. Se HA termina, il processo `:ha` si chiude: Python non si può reinizializzare nello stesso processo.

L'output di Python va in logcat con i tag `python.stdout` e `python.stderr`; i messaggi dell'app hanno il tag `HAndroid`.

## Preparare il PC

Servono due ambienti: **Windows** (o Linux/macOS) per compilare l'APK, e **Linux** (WSL va bene) per compilare i pacchetti nativi con cibuildwheel, che non gira su Windows.

### Windows: app Android

| Cosa | Come | Note |
| --- | --- | --- |
| JDK 17 | `winget install Microsoft.OpenJDK.17` | poi imposta `JAVA_HOME` |
| Android SDK | [command-line tools](https://developer.android.com/studio#command-line-tools-only) in `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` | oppure Android Studio, che usa la stessa cartella |
| Pacchetti SDK | vedi sotto | |
| Variabili | `JAVA_HOME`, `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk` | variabili utente |

Pacchetti SDK. In PowerShell il `;` va passato tramite file, perché `sdkmanager.bat` lo spezza:

```powershell
"platform-tools","platforms;android-35","build-tools;35.0.0","ndk;27.3.13750724","cmake;3.22.1" | Set-Content -Encoding ascii $env:TEMP\pkgs.txt
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --package_file="$env:TEMP\pkgs.txt"
```

Serve anche un Python qualsiasi (≥ 3.10) per gli script: viene usato solo per scaricare pacchetti e creare lo zip.

### WSL: pacchetti nativi

```powershell
wsl --install -d Ubuntu-24.04
```

Poi, dentro Ubuntu, dalla cartella del repo (`/mnt/c/...`):

```bash
bash scripts/prepara-wsl.sh
```

Lo script installa i pacchetti di build, Android SDK con NDK 27.3 in `~/android-sdk`, Python 3.14 con cibuildwheel in `~/.venv-handroid` e Rust con il target `aarch64-linux-android`. Scrive le variabili in `~/.handroid-env`, che viene caricato da `~/.bashrc`.

> La versione dell'NDK deve essere la stessa indicata da CPython in `app/python/android-env.sh` (`ndk_version`). Se cambi versione di Python, controllala e aggiorna `prepara-wsl.sh` e `app/build.gradle.kts`.

## Compilare l'app

Tre passaggi, nell'ordine:

```bash
# 1. in WSL: pacchetti nativi → wheels/   (lungo la prima volta, poi solo i mancanti)
bash scripts/compila-wheel-android.sh

# 2. in WSL (consigliato) o Linux: Python + HA → jniLibs e assets/python.zip
bash scripts/prepara-python-android.sh

# 3. APK
./gradlew assembleDebug          # Windows: .\gradlew.bat assembleDebug
```

- `compila-wheel-android.sh` accetta anche nomi di pacchetti (`... orjson pyyaml`) per ricompilarne solo alcuni. I log stanno in `build/log/<pacchetto>.log`.
- `prepara-python-android.sh` si ferma ed elenca i wheel mancanti se il passo 1 non è completo: non produce mai un APK a metà.
- `prepara-python-android.sh` lavora in `~/.cache/handroid` quando il repo sta su `/mnt/c`: lì pip e zip sono molte volte più veloci, e su Windows si copia solo lo zip finito. Se requisiti, wheel e file in `app/src/main/python/` non cambiano, non rifà nulla.
- Lo zip tiene solo le lingue in `LANGS` (default `en it`) e toglie il frontend per browser vecchi (`frontend_es5`), le mappe `.map` e le copie `.gz`/`.br`: da 603 a circa 200 MB non compressi. Per un'altra lingua: `LANGS="en it de" bash scripts/prepara-python-android.sh`.
- L'APK finisce in `app/build/outputs/apk/debug/app-debug.apk`.

## Provare sul telefono

### Senza telefono: emulatore

Le immagini x86_64 di Android 11 e successivi traducono il codice arm64, quindi l'APK gira anche nell'emulatore (più lento). Prima ferma le compilazioni in WSL: insieme richiedono troppa memoria.

```powershell
# una volta: "emulator" e "system-images;android-35;google_apis;x86_64" con --package_file, poi
avdmanager create avd -n handroid -k "system-images;android-35;google_apis;x86_64" -d pixel_6
# ogni volta
emulator -avd handroid -no-window -no-audio -memory 4096
```

### Sul telefono

Telefono di riferimento: Xiaomi 12T, Android 14 (MIUI). Serve il debug USB attivo (vedi il README, strada A, punto 3).

```bash
./gradlew installDebug
adb shell am start -n it.handroid/.MainActivity
adb logcat -s HAndroid python.stdout python.stderr
```

Su MIUI `installDebug` fallisce con `INSTALL_FAILED_USER_RESTRICTED` se "Installa tramite USB" è spento, e MIUI lo attiva solo con SIM e account Xiaomi. In alternativa copia l'APK e installalo dal telefono (Gestione file → Download, consentendo le app da origini sconosciute):

```bash
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/HAndroid.apk
```

Nell'app tocca **Avvia Home Assistant**, oppure da adb, senza toccare lo schermo:

```bash
adb shell am start -n it.handroid/.MainActivity --ez avvia true
adb forward tcp:8123 tcp:8123          # poi apri http://127.0.0.1:8123 sul PC
```

In Git Bash i percorsi del telefono (`/data/...`) vengono convertiti in percorsi Windows: prima dei comandi adb usa `export MSYS_NO_PATHCONV=1`.

Per provare una modifica a un file Python senza ricostruire l'APK:

```bash
adb push app/src/main/python/handroid_boot.py /data/local/tmp/
adb shell run-as it.handroid cp /data/local/tmp/handroid_boot.py files/python/site-packages/
adb shell am force-stop it.handroid
```

La schermata dell'app mostra l'ultimo passo dell'avvio, da quanti secondi è lì e se l'interfaccia risponde: lo scrivono `HaService` (estrazione) e `handroid_boot.py` (messaggi di avvio di HA) in `files/stato.txt`. Il primo avvio dopo un'installazione estrae Python (circa 30 secondi), poi HA impiega 1-2 minuti a partire sull'emulatore. Gli errori `Setup failed for ...` di alcune integrazioni sono attesi: vedi [Problemi noti](#problemi-noti). Poi dal telefono personale apri l'indirizzo mostrato nell'app (`http://<ip>:8123`).

**Gate della M0:** l'interfaccia di HA si apre dal telefono personale. Quando passa, segnalalo nel [registro](registro.md).

Per ripartire da zero: `adb shell pm clear it.handroid` (cancella anche la configurazione di HA).

## Aggiornare Home Assistant

```bash
python scripts/dipendenze-android.py 2026.12.0 > docs/dipendenze-android.md   # versione nuova
bash scripts/compila-wheel-android.sh      # in WSL: compila i pacchetti nuovi o cambiati
bash scripts/prepara-python-android.sh
```

`docs/dipendenze-android.md` è l'unica fonte delle versioni: gli script di build leggono da lì. Controlla nel diff della tabella i pacchetti nuovi nella sezione "Da compilare".

## Lavorare sulle dipendenze native

`dipendenze-android.py` classifica ogni pacchetto:

| Classe | Significato | Cosa fare |
| --- | --- | --- |
| puro | wheel `py3-none-any` | niente, si installa ovunque |
| android | wheel `android_*_arm64_v8a` per cp314 o abi3 già su PyPI | niente |
| compilare | ha codice nativo e nessun wheel Android | lo compila `compila-wheel-android.sh` |
| sorgente | solo sdist | `compila-wheel-android.sh` lo compila come gli altri; se è puro (es. pyric) si può invece aggiungere a `--no-binary` in `prepara-python-android.sh` |

### Integrazioni di base

HA parte con `--skip-pip`, quindi non installa le librerie delle integrazioni. Quelle che servono all'avvio stanno in `scripts/integrazioni-base.txt` (oggi: frontend, conversation, analytics, radio_frequency, infrared, più tts, assist_satellite, met, radio_browser e google_translate, che l'onboarding crea o carica all'ultimo passo: senza, l'interfaccia resta in caricamento su "Aiutaci ad aiutarti"). `dipendenze-android.py` legge il loro `manifest.json` dalla versione di HA e aggiunge i requisiti alla tabella. Per aggiungere un'integrazione: una riga nel file, poi rigenera la tabella e ricompila. Con `dominio:pacchetto` (es. `stream:numpy`) si prende solo quel pacchetto dal manifest, senza le dependencies: serve quando HA importa un'integrazione per un controllo ma non la avvia.

### Pacchetti rinviati

`scripts/android-rinviati.txt` elenca i pacchetti nativi che per ora restano fuori dall'APK, con il motivo. Gli script li saltano; le integrazioni che li usano non partono. Togli la riga quando il pacchetto si compila.

### Impostazioni per singolo pacchetto

Stanno nel `case` di `compila-wheel-android.sh`, con un commento sul perché. Regole valide per tutti:

- cibuildwheel riscrive `CFLAGS` e `LDFLAGS`: gli header extra passano da `CPATH`, le librerie statiche si copiano nel sysroot dell'NDK (lo fa `prepara-wsl.sh`).
- Le estensioni in Rust devono collegarsi a libpython: `RUSTFLAGS` lo fa per tutte.
- Librerie C per Android (libffi, libsodium, zlib) stanno in `~/android-deps/aarch64-linux-android`.

Se un pacchetto non si compila:

1. Leggi `build/log/<pacchetto>.log`.
2. Spesso manca una libreria C (es. libyaml, libffi, openssl). Le variabili `CIBW_*` per pacchetto vanno aggiunte in `compila-wheel-android.sh`, con un commento che spiega perché.
3. Se il pacchetto ha un'estensione C facoltativa, lo script tiene il wheel solo Python e lo segnala ("Senza estensione nativa"): funziona, ma è più lento.
4. Annota il caso in [Problemi noti](#problemi-noti) e nel registro.

## Sviluppare un add-on

Gli add-on arrivano con la **M3**: il formato è in bozza e può cambiare. La descrizione completa, con il manifest, è in [addon/README.md](addon/README.md).

## Problemi noti

Gradle su Windows, se manca qualcosa:
- `Dependency requires at least JVM runtime version 11`: Gradle usa Java 8. Aggiungi a `%USERPROFILE%\.gradle\gradle.properties` la riga `org.gradle.java.home=C:/Program Files/Microsoft/jdk-17...` (il tuo JDK 17).
- `SDK location not found`: crea `local.properties` (fuori da git) con `sdk.dir=C:/Users/<utente>/AppData/Local/Android/Sdk`.

| Problema | Effetto | Stato |
| --- | --- | --- |
| `uv` è un eseguibile e Android non esegue binari da cartelle scrivibili | HA non può installare a runtime i requisiti delle integrazioni: si avvia con `--skip-pip` | da decidere (vedi rischi nel progetto) |
| `sys.platform` vale `android` | HA rifiuterebbe di partire | aggirato in `handroid_boot.py` |
| `PyConfig.use_system_logger` esiste solo su iOS/macOS | — | su Android il redirect a logcat è automatico |
| tar di Git Bash non crea symlink prima del loro bersaglio | estrazione di CPython fallisce al primo giro | lo script estrae due volte |
| lru-dict ha un wheel Android solo per cp313 | non usabile con Python 3.14 | si compila |
| pyyaml senza libyaml | wheel solo Python, YAML più lento | accettato per la M0 |
| cffi non trova libffi | cffi, cryptography, pynacl e pycares non si compilano | libffi statica di BeeWare in `~/android-deps` |
| grpcio: la sua BoringSSL va in conflitto con gli header OpenSSL di CPython | grpcio non si compila | `GRPC_PYTHON_BUILD_SYSTEM_OPENSSL=1`, da verificare |
| pillow, psutil | non si compilano | rinviati, vedi `scripts/android-rinviati.txt` |
| `Os.setenv` da Kotlin non arriva né al codice C né a `os.environ` | Python non trova site-packages; HOME, TMPDIR e DNS mancanti | percorsi come argomento JNI, il resto via file in `files/` |
| HA esce con "must be run in a Python virtual environment or a container" | HA non parte | `validate_environment` saltata in `handroid_boot.py` |
| Rust ignora `LDFLAGS` | `cannot locate symbol PyLong_Type` all'import di cryptography | `RUSTFLAGS` con `-lpython3.14` |
| pillow serve già all'avvio | HA non parte senza | compilato con PNG, JPEG spento |
| ffmpeg, numpy, aiohasupervisor, psutil, grpcio mancanti | tts, assist, camera, analytics, hardware, cloud non partono | da fare dopo la M0 |
| APK di debug da circa 350 MB | installazione lenta | da ridurre |
| `sdkmanager.bat` spezza i nomi con `;` in PowerShell | pacchetti "not found" | usare `--package_file` |
