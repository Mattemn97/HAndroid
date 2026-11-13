# Registro dei lavori

Cosa è stato fatto, cosa si è scoperto e cosa resta aperto. Le voci più recenti stanno in alto. Ogni sessione di lavoro aggiunge una voce.

## 2026-11-13 · M0: Home Assistant risponde sull'emulatore

**Risultato:** sull'emulatore Android 15 (x86_64 con traduzione arm64) HA 2026.11.1 parte dentro l'APK e serve la pagina di configurazione iniziale su `:8123` (HTTP 200, file dell'interfaccia compresi). Manca la prova sul Xiaomi 12T da un altro telefono, che è il gate vero della M0.

**Corretto per arrivarci**
- cryptography: la parte Rust non era collegata a libpython (`cannot locate symbol PyLong_Type`) → `RUSTFLAGS` in `compila-wheel-android.sh`.
- pynacl: libsodium compilata a parte; cibuildwheel riscrive `CFLAGS`/`LDFLAGS`, quindi header via `CPATH` e librerie statiche copiate nel sysroot dell'NDK.
- pillow: serve già all'avvio (auth → onboarding → person → image_upload). Compilato con PNG, JPEG spento.
- Python non vedeva site-packages: le variabili impostate da Kotlin con `Os.setenv` non arrivano a `getenv` nel codice C. Il percorso ora è un argomento JNI.
- HA rifiuta di partire fuori da venv/container: `validate_environment` saltata in `handroid_boot.py`.
- L'interfaccia mancava (404): con `--skip-pip` le librerie delle integrazioni non si installano. Nuovo `scripts/integrazioni-base.txt`; `dipendenze-android.py` legge i loro manifest.
- Avvio da adb per i test: `am start -n it.handroid/.MainActivity --ez avvia true`.

**Ancora aperto**
- Integrazioni che non partono: ffmpeg/tts (manca ha-ffmpeg e il binario ffmpeg), camera/stream (numpy), analytics (aiohasupervisor), hardware (psutil), cloud (grpcio, numpy).
- APK di debug da 348 MB: `python.zip` (219 MB) non è compresso dentro l'APK. Da ridurre (bytecode, togliere test e file inutili).
- JPEG in pillow (serve libjpeg per Android).

## 2026-11-13 · M0 avviata

**Fatto**
- Progetto Android (Kotlin + JNI): servizio in primo piano nel processo `:ha` che estrae Python e avvia Home Assistant. Specifica: [0001-fattibilita](specs/0001-fattibilita.md).
- Script: analisi delle dipendenze, preparazione di WSL, compilazione dei wheel Android, impacchettamento di Python + HA.
- Ambiente di sviluppo: JDK 17, Android SDK (platform 35, build-tools 35, NDK 27.3.13750724, CMake 3.22.1) su Windows; Ubuntu 24.04 in WSL con NDK, Python 3.14, cibuildwheel e Rust.
- L'APK compila (`assembleDebug`), per ora senza `python.zip`.
- Guida per sviluppatori ([guida-sviluppo.md](guida-sviluppo.md)) e bozza degli add-on ([addon/README.md](addon/README.md)).

**Scoperto**
- CPython 3.14.10 ufficiale per Android è su python.org e richiede l'NDK 27.3.13750724.
- Home Assistant 2026.11.1 ha 101 dipendenze: 78 si installano già, 22 vanno compilate (vedi [dipendenze-android.md](dipendenze-android.md)).
- I wheel Android già su PyPI non sempre valgono: lru-dict esiste solo per cp313.
- `PyConfig.use_system_logger` non esiste su Android: il redirect a logcat è automatico.

**Compilazione dei wheel (WSL)**
- Compilati: annotatedyaml, audioop-lts, bcrypt, bluetooth-data-tools, cffi, ciso8601, fnv-hash-fast, greenlet, habluetooth, lru-dict, orjson, regex, ulid-transform, zeroconf. pyyaml senza estensione C.
- cffi richiedeva libffi: si usa quella statica di BeeWare (`~/android-deps`, scaricata da `prepara-wsl.sh`).
- Rinviati (`scripts/android-rinviati.txt`): pillow, psutil, uv.
- Da rifare: cryptography, pynacl, pycares, grpcio. Il giro è stato interrotto da Claude Code per memoria scarsa (emulatore e compilazione insieme).
- Emulatore `handroid` (API 35 x86_64, con traduzione arm64) creato per provare l'APK senza telefono.

**Aperto**
- Prima prova sul Xiaomi 12T (gate della M0).
- uv a runtime: HA parte con `--skip-pip`.

## 2026-11-13 · Onboarding bloccato all'ultimo passo

- L'interfaccia restava in caricamento su "Aiutaci ad aiutarti": l'onboarding crea met, radio_browser e google_translate, e la lista dei servizi falliva perché tts non trovava `mutagen`.
- Aggiunte a `scripts/integrazioni-base.txt`: tts, assist_satellite, met, radio_browser, google_translate. Nuovi pacchetti nativi compilati: pymicro-vad, pyspeex-noise.
- `compila-wheel-android.sh` compila anche la sezione "Solo sorgente" della tabella.

## 2026-11-13 · Build più veloce e APK più leggero

- `prepara-python-android.sh` lavora sul disco di WSL (`~/.cache/handroid`) e salta tutto se nulla è cambiato.
- Lo zip tiene solo italiano e inglese e toglie frontend_es5, mappe e copie compresse.

## 2026-11-13 · Prima prova sul 12T

- L'APK si installa (copiandolo nel telefono: MIUI blocca l'installazione via USB senza SIM) e HA risponde dal PC sulla rete di casa.
- Onboarding ancora fermo su "Aiutaci ad aiutarti": il controllo dei servizi importa camera → stream → numpy. Aggiunto `stream:numpy` alle integrazioni di base (nuova sintassi `dominio:pacchetto`).
- HA non risolveva i nomi (Android non ha resolv.conf): DNS della rete passati ad aiodns da `handroid_boot.py`.
- La schermata dell'app mostra l'avanzamento dell'avvio e se l'interfaccia risponde.
- numpy 2.3.2 compilato per Android senza BLAS (`scripts/android-numpy.cross` per il formato dei long double). Aggiunti camera (pyturbojpeg, puro) e `hassio:aiohasupervisor` per analytics.
- `Os.setenv` non arriva a Python: HOME, TMPDIR, stato e DNS ora passano da `--config` e da file in `files/`.
- Sull'emulatore, da dati puliti, l'onboarding via API arriva in fondo (`get_services` e `analytics` rispondono) in circa 2 minuti dal primo avvio.

## 2026-11-13 · M0 chiusa

- Gate superato: sul Xiaomi 12T l'onboarding arriva in fondo e l'interfaccia si apre dal PC sulla rete di casa.
- Limite noto, primo lavoro della M1: le integrazioni con requisiti propri non si aggiungono (HA parte con `--skip-pip`).
