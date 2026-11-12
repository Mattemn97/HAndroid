# HAndroid

App Android che fa girare Home Assistant Core vero dentro l'APK, senza root. Decisioni fissate in [docs/specs/0001-architettura.md](docs/specs/0001-architettura.md), visione completa in [docs/progetto-handroid.md](docs/progetto-handroid.md).

## Stack

- **Linguaggio**: Kotlin (app), C (launcher JNI), Python 3.14 (avvio di HA)
- **Framework**: Android SDK puro, senza AndroidX; minSdk 29, targetSdk 35, solo arm64-v8a
- **Dipendenze chiave**: CPython ufficiale per Android (python.org), Home Assistant Core

## Comandi

```bash
# Prepara Python (scarica CPython per Android, popola jniLibs e assets) — serve prima di ogni build
bash scripts/prepara-python.sh
# Prepara HA Core e le dipendenze con wheel Android (assets/site.zip); le wheel native si compilano con .github/workflows/wheels.yml
bash scripts/prepara-ha.sh
# Build (JDK 17, Android SDK + NDK, Gradle 8.10)
gradle :app:assembleRelease
# Test JVM della logica pura (Regole.kt)
gradle :app:testReleaseUnitTest
# Installa sul telefono collegato via ADB (ultima release, o un APK locale)
bash scripts/installa-handroid.sh [file.apk] [--device-owner]
# Release: automatica a ogni push su master che tocca l'app (.github/workflows/release.yml)
```

## Regole del progetto

- La versione di Python sta in due posti: `PYTHON_VERSION` in `.github/workflows/release.yml` e il default in `scripts/prepara-python.sh`. Vanno cambiati insieme, e anche `python3.14` in `CMakeLists.txt` e `PythonRuntime.kt` se cambia la minore.
- Mai lanciare eseguibili dalla cartella dati: Android li blocca. Un binario nuovo va in `jniLibs` come `lib<nome>.so`.
- Home Assistant non si patcha: gli adattamenti vanno in `app/src/main/python/handroid_main.py` o nei bridge Kotlin.
- Il keystore di release vive solo nei secret della repo e nella copia di backup dell'utente: se si perde, le nuove release non si installano sopra le vecchie.
- `jniLibs/`, `assets/python.zip` e `assets/site.zip` sono generati: non si committano.
- Lo stato condiviso tra il processo dell'app e `:ha` sta in file nella cartella base (vedi spec 0006), mai nelle SharedPreferences.
- Un commit su master con `[skip release]` nel messaggio non pubblica l'APK.

## Specifiche

Stanno in `docs/specs/NNNN-titolo.md`: requisiti, dati, sicurezza, decisioni e conseguenze, in poche righe.

## Come lavorare

- **Leggi poco e bene.** Cerca prima di aprire, poi leggi solo le parti che servono. Non rileggere quello che hai già in conversazione.
- **Raggruppa.** Le letture indipendenti vanno in parallelo, i controlli piccoli in un solo comando. Filtra l'output di test e build: errori e totali, non tutto il log.
- **Niente subagenti** se non li chiedo: ripartono da zero e costano molto.
- **Decidi le cose piccole.** Se una scelta ha un default ragionevole, prendilo e segnalalo nel report. Chiedi solo se la risposta cambia davvero il lavoro.
- **Finisci il lavoro.** Non fermarti a metà per proporre: implementa, verifica, poi riferisci.
- **Resta nel perimetro.** Fai quello che ho chiesto; le idee in più scrivile in una riga, senza realizzarle.
- **Segui il codice che c'è.** Stessi nomi, stesso stile, stessa densità di commenti. Riusa quello che esiste.
- **Di' come stanno le cose.** Un test fallito o un passaggio saltato si dichiarano. Afferma solo ciò che hai controllato.
- **Commit, push, merge e deploy solo se te lo chiedo.** Un permesso vale per quella volta.
- **Scrivimi semplice.** Prima il risultato, poi il resto, senza gergo inutile. Durante il lavoro bastano aggiornamenti di una riga.
- **Skill di processo solo a richiesta.** `/scope`, `/architect`, `/develop`, `/check`, `/test`, `/debug`, `/document`, `/sync` e `/audit` partono quando le scrivo: sono più complete, ma consumano di più. Per il lavoro normale bastano `implementa`, `pubblica` e `proponi`.
- **Tieni vivo questo file.** Quando scopri una regola non ovvia, aggiungila in una riga nella sezione giusta.
