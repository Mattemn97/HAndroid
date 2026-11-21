# HAndroid


## Stack

- **Linguaggio**: Kotlin (app), C (JNI verso libpython), Python 3.14 (Home Assistant)
- **Framework**: Android SDK 35, NDK/CMake, Gradle con Kotlin DSL
- **Dipendenze chiave**: CPython ufficiale per Android, Home Assistant Core, cibuildwheel per i wheel Android

## Comandi

Guida completa: `docs/guida-sviluppo.md`. I wheel si compilano in WSL (Ubuntu 24.04, preparato da `scripts/prepara-wsl.sh`).

```bash
# Dipendenze di HA per Android (rigenera docs/dipendenze-android.md)
python scripts/dipendenze-android.py > docs/dipendenze-android.md
# Wheel nativi per Android (Linux/macOS/WSL, con NDK e cibuildwheel)
scripts/compila-wheel-android.sh
# Python + HA dentro l'APK
scripts/prepara-python-android.sh
# Build e installazione sul telefono
./gradlew installGithubDebug
# Test
./gradlew testGithubDebugUnitTest
# Log
adb logcat -s HAndroid python.stdout python.stderr
```

## Regole del progetto

- Home Assistant non si modifica: gli adattamenti ad Android vanno in `app/src/main/python/handroid_boot.py`.
- Solo arm64 (Xiaomi 12T). `app/python`, `jniLibs`, `python.zip` e `wheels/` sono generati e stanno fuori da git.
- Ogni modifica aggiorna `docs/guida-sviluppo.md` (se cambia come si compila, prova o sviluppa) e aggiunge una voce a `docs/registro.md`. Il repo deve bastare a chi sviluppa l'app o un add-on.
- Un cambio al formato degli add-on va in `docs/addon/README.md`, con una riga nello storico in fondo.
- In Git Bash usa `export MSYS_NO_PATHCONV=1` prima dei comandi adb, altrimenti `/data/...` diventa un percorso Windows. Così però curl e adb non capiscono `/tmp/...`: passa i file temporanei da `cygpath -m`, e a tar con `-f -` (un `C:/...` per tar è un host remoto).
- Per riavviare HA dal PC: `adb shell am force-stop it.handroid` e poi `adb shell monkey -p it.handroid -c android.intent.category.LAUNCHER 1` (MIUI ignora `am start` dopo un force-stop). Un kill normale di `:ha` il supervisor lo prende per uno stop voluto.
- I `.pyc` nell'APK sono `unchecked-hash`: un `.py` copiato sul telefono non vale finché non cancelli il suo `.pyc` in `__pycache__`. Sul telefono i `.py` sono vuoti (tranne `handroid_boot.py` e `custom_components`): per leggere un sorgente guardalo nel PC, in `~/.cache/handroid/site-packages`.
- Su Android OpenSSL non ha certificati CA: `handroid_boot.py` imposta `SSL_CERT_FILE` a certifi. Un `CERTIFICATE_VERIFY_FAILED` in un'integrazione parte da lì.
- Niente `custom_components` in `site-packages`: nasconde quella di HA. `handroid_boot.py` copia la nostra integrazione e la toglie.
- `Os.setenv` in Kotlin non arriva a Python: i dati per Python passano da argomenti JNI o da file in `files/`.
- Le integrazioni installate a runtime con pacchetti nativi vanno in `scripts/integrazioni-indice.txt` (tabella `docs/dipendenze-indice.md`); per quelle personalizzate, `pip:pacchetto`.
- Le integrazioni personalizzate (Xiaomi Home, ble_adv) non vanno nell'APK: si installano con `scripts/installa-integrazione.sh`. La licenza di Xiaomi Home vieta di usarla per sviluppare app.
- La pagina del progetto sta in `sito/` (tema `mf-theme` copiato da mattemn97.github.io); `scripts/pubblica-indice.sh` la pubblica insieme ai wheel sul ramo `gh-pages` con un commit normale (niente push forzati).
- Le versioni dei wheel seguono `package_constraints.txt` di HA (lo applica `dipendenze-android.py`): un wheel con un'altra versione per pip sul telefono non esiste.
- Sul telefono dell'utente va la build `usb` (`assembleGithubUsb`, è quella di `rilascio.sh`): firmata come la release ma debuggable, così `run-as` legge e scrive i file di HA dal PC e le versioni si installano una sopra l'altra. Prima di ogni installazione fai un backup di `files/ha-config` sul PC (`Documenti/HAndroid-backup`). Cambiare firma richiede di disinstallare: avvisa prima.
- Sul 12T senza SIM: installa con `adb push` in `/data/local/tmp` + `pm install -r`; `input` e `pm grant` sono bloccati.
- Non modificare uno script bash mentre gira in background: bash lo legge a pezzi e si rompe. Lancia una copia.
- Stato condiviso tra il processo dell'app e `:ha`: file in `files/`, non SharedPreferences.
- Le librerie delle integrazioni caricate all'avvio vanno in `scripts/integrazioni-base.txt`; i pacchetti nativi che non si compilano in `scripts/android-rinviati.txt`, col motivo.
- Su Windows `sdkmanager.bat` spezza gli argomenti al `;`: passare i pacchetti con `--package_file`.
- La dimensione vera dell'APK si misura dopo `./gradlew clean`: il pacchetto incrementale lascia spazio vuoto (anche il doppio).
- Quando cambia la versione di HA, rigenera `docs/dipendenze-android.md`: gli script di build leggono le versioni da lì.

<!-- Solo quello che non si capisce leggendo il codice. Ogni regola dice cosa fare e cosa si rompe altrimenti.
Di solito valgono: cosa va aggiornato insieme, dove sta una certa logica, cosa non va toccato, cosa viene controllato da un test.
Esempio: "Un campo nuovo del profilo va aggiunto anche alle regole del database, altrimenti il salvataggio viene rifiutato." -->

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
