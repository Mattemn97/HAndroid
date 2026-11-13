# 0001 · M0 Fattibilità

**Obiettivo:** Home Assistant Core parte dentro l'APK con CPython 3.14 ufficiale. Gate: UI su `:8123` aperta dal telefono personale.

## Requisiti

- Interprete: tarball ufficiale `python-3.14.x-aarch64-linux-android` di python.org, solo arm64.
- HA non modificato. Gli adattamenti stanno in `app/src/main/python/handroid_boot.py`.
- Elenco delle dipendenze native in `docs/dipendenze-android.md`, rigenerabile con `scripts/dipendenze-android.py`.

## Come funziona

1. `scripts/compila-wheel-android.sh` compila con cibuildwheel i pacchetti nativi senza wheel Android (gira su Linux, macOS o WSL).
2. `scripts/prepara-python-android.sh` mette `libpython` in `jniLibs` e impacchetta stdlib e site-packages in `assets/python.zip`.
3. `HaService` (processo `:ha`, servizio in primo piano con wakelock) estrae lo zip a ogni nuova installazione e chiama `Py_RunMain` via JNI (`handroid.c`).

## Decisioni

- **Processo separato `:ha`.** Python non si reinizializza: a fine esecuzione il processo muore e riparte pulito.
- **`--skip-pip` e niente uv.** uv è un programma che Android non lascia eseguire da una cartella scrivibile. Le integrazioni con requisiti propri restano fuori fino a quando non si decide il punto 2 dei rischi. Superato nella M1: pip al posto di uv (vedi 0002).
- **Pacchetti rinviati** (`scripts/android-rinviati.txt`): psutil, grpcio e uv restano fuori; pillow è incluso perché HA lo usa già all'avvio.
- **Configurazione minima** (`frontend`, `config`, `api`). `default_config` richiede bluetooth e zeroconf, che arrivano con la M2.
- **`validate_os` e `validate_environment` sostituite all'avvio.** Su Android `sys.platform` vale `android`, ma il kernel è Linux; l'APK fa da container al posto del venv.
- **Integrazioni di base nell'APK** (`scripts/integrazioni-base.txt`): senza pip a runtime, le librerie di frontend e delle integrazioni caricate all'avvio vanno incluse.
- **DNS dalla rete di Android.** Android non ha `/etc/resolv.conf`, quindi aiodns (c-ares) non risolve i nomi. `HaService` scrive i DNS della rete attiva in `files/dns.txt`, `handroid_boot.py` li dà ad aiodns (riserva: 1.1.1.1 e 8.8.8.8). Se il telefono cambia rete, i DNS restano quelli dell'avvio.
- **Percorsi di Python passati da JNI.** Le variabili d'ambiente impostate da Kotlin (`Os.setenv`) non arrivano né al codice C né a Python: i dati per Python passano da argomenti e file in `files/`.
- **Pacchetti con versione fissata** dalla tabella generata, installati con `--no-deps`, così la build è ripetibile.

## Conseguenze

- I dati di HA stanno in `filesDir/ha-config` e sopravvivono agli aggiornamenti dell'APK. Python viene estratto di nuovo a ogni aggiornamento.
- I pacchetti "puri" con estensioni C facoltative (aiohttp, yarl, multidict…) girano più lenti. Compilarli è un'ottimizzazione da M1.
