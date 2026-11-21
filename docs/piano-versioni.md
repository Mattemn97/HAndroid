# Piano delle versioni fino alla 1.0.0

19 novembre 2026. Il *perché* e il modello stanno in [roadmap.md](roadmap.md); qui c'è il *come*: per ogni versione i lavori in ordine, i file che toccano e come si verifica. Ogni lavoro grosso ha la sua spec in `docs/specs/` prima di cominciare.

Regole per tutte le versioni:

- Un lavoro è finito quando è provato sul 12T, non quando compila. Ciò che non si può provare si scrive nel registro.
- Ogni versione chiude con: voce nel registro, guida aggiornata, sito aggiornato, tag su GitHub (dalla 0.6.0 senza APK).
- Si lavora su un branch per versione (`v0.6`, `v0.7`…) con una PR per lavoro, così ogni pezzo si prova da solo.

## 0.6.0 · Affidabile e al sicuro

Gate: 7 giorni sul 12T senza interventi, più un ripristino completo da backup su dati puliti.

| # | Lavoro | Come | File |
| --- | --- | --- | --- |
| 1 | Correzioni dalla prova della 0.5.0 | si parte dai log condivisi dall'app; ogni problema diventa una voce del registro con la causa | da vedere |
| 2 | GitHub senza APK | `rilascio.sh`: tag e note, niente `gh release create` con l'APK. Flavor Gradle `github` e `play` (stesso codice; `BuildConfig.PLAY`). In `github` via "Cerca aggiornamenti"; `installa-handroid.sh` installa solo un APK compilato in locale. I rilasci 0.4.0 e 0.5.0 restano come archivio, con una riga nelle note | `app/build.gradle.kts`, `MainActivity.kt`, `scripts/rilascio.sh`, `scripts/installa-handroid.sh`, `README.md`, `sito/` |
| 3 | Backup fuori dal telefono | ogni notte, dopo il backup automatico di HA (che si attiva dall'onboarding), `Maintenance` copia l'ultimo `.tar` in una cartella scelta dall'utente con il selettore di Android (SAF: Drive, cartella di rete con un'app SMB, chiavetta USB), tenendo gli ultimi N. Pulsante "Ripristina da file": copia il `.tar` in `ha-config/backups`, poi si ripristina dall'onboarding di HA | `Maintenance.kt`, `MainActivity.kt`, nuova spec 0010 |
| 4 | Avviso se il server si ferma | campo "URL di controllo" (healthchecks.io o simili, gratis): il watchdog lo chiama ogni 5 minuti se HA risponde. Il servizio esterno ti scrive se i ping smettono | `HaService.kt` (watchdog), `MainActivity.kt` |
| 5 | APK più leggero | `prepara-python-android.sh`: `.pyc` al posto dei `.py` (compileall con `-b`, poi via i sorgenti), tranne `handroid_boot.py` e `custom_components` (servono leggibili). Va provato che HA carichi tutte le integrazioni di base e che pip installi sopra | `scripts/prepara-python-android.sh`, `AGENTS.md` (regola sui `.pyc`) |
| 6 | Gate dei 7 giorni | registro giornaliero: uptime, crash da `crash.txt`, batteria da `dumpsys batterystats` | `docs/registro.md` |

## 0.7.0 · Il telefono che fa di più

Fatto nella 0.7.0: telecamere cloud (FFmpeg con TLS, `stream`), build `usb`, avviso batteria. La build automatica (riga 1) passa ai lavori del contorno, dopo l'app: serve al sito e al servizio a pagamento, non all'app. Con il repository pubblico GitHub Actions è gratis.

Gate: "Ok Nabu, accendi la luce" funziona a 3 metri dal telefono; una release di HA arriva compilata senza toccare il PC.

| # | Lavoro | Come | File |
| --- | --- | --- | --- |
| 1 | Build automatica (base del servizio a pagamento) | GitHub Actions su runner Linux: un workflow al giorno controlla l'ultima release di HA; se è nuova rigenera `dipendenze-android.md`, compila i wheel mancanti (cache dei wheel tra un giro e l'altro), `prepara-python-android.sh`, test e AAB. Lo script di WSL diventa lo stesso usato in CI. Prova di avvio: Firebase Test Lab su un telefono arm64 vero (quota gratuita giornaliera), che apre l'app e aspetta "Home Assistant pronto" | `.github/workflows/`, `scripts/`, spec 0011 |
| 2 | Satellite vocale | il dispositivo ESPHome espone le funzioni voice assistant del protocollo ESPHome (messaggi VoiceAssistant*): microfono con `AudioRecord` a 16 kHz, parola di attivazione con microWakeWord (modello TFLite nel telefono), risposta audio con `AudioTrack`. HA lo vede come un satellite ESP32 | `esphome/VoceAssist.kt`, `ApiServer.kt`, permesso `RECORD_AUDIO`, spec 0012 |
| 3 | Altoparlante come media player | entità media player ESPHome: HA manda un URL (TTS, suoni) e il telefono lo riproduce | `esphome/` |
| 4 | Fotocamera in HA | entità camera ESPHome: istantanea con Camera2 su richiesta, spenta di default. Lo stream continuo resta fuori (batteria) | `esphome/Fotocamera.kt`, permesso `CAMERA` |
| 5 | Sensori in più | movimento (accelerometro con soglia, sensore *significant motion* che non sveglia la CPU) e rumore ambiente (dB dal microfono, solo se il satellite è spento) | `esphome/EspHome.kt` |

## 0.8.0 · Più dispositivi, e i primi servizi del server

Gate: una presa Matter e una lampadina Zigbee in HA; video in diretta nell'app Companion; accesso da fuori casa senza dominio proprio.

| # | Lavoro | Come | File |
| --- | --- | --- | --- |
| 1 | Add-on nativi | estende la spec 0006: un add-on può dichiarare un eseguibile già nell'APK (`libgo2rtc.so`, `libmatter.so`…), avviato come `cloudflared` in `Tunnel.kt`. Niente download di binari: arrivano con l'app (regola di Play) | `addon/`, `docs/addon/README.md` (nuovo campo), spec 0006 |
| 2 | go2rtc | compilato in Go per `GOOS=android` con cgo, come cloudflared. HA lo trova su `127.0.0.1:1984` (integrazione go2rtc) | `prepara-wsl.sh`, add-on `go2rtc` |
| 3 | Matter Server | python-matter-server con il suo SDK nativo (CHIP) compilato per Android: il lavoro più incerto. Se non si compila in tempi ragionevoli, passa alla 0.9.0 | `scripts/`, add-on `matter` |
| 4 | Zigbee2MQTT | Node.js per Android (build ufficiale di Node per Android arm64) + Z2M; usa il ponte USB (6638) e l'add-on MQTT. Se Node pesa troppo, ZHA resta la via consigliata e Z2M si rimanda | add-on `zigbee2mqtt` |
| 5 | Pannello degli add-on | ingress: l'integrazione `handroid` registra un pannello iframe per gli add-on con interfaccia web | `custom_components/handroid/` |
| 6 | Accesso remoto senza dominio (servizio a pagamento) | il server crea un tunnel Cloudflare su `<nome>.<nostro dominio>` con l'API di Cloudflare e dà il token all'app; l'app lo usa con il `Tunnel.kt` di oggi. Autenticazione: acquisto Play verificato dal server | nuovo repo `handroid-server`, `Tunnel.kt`, spec 0013 |

## 0.9.0 · Pronto per il Play Store

Gate: prova chiusa su Play con almeno 12 tester per 14 giorni (richiesta da Google per i nuovi account), su almeno 3 marche di telefono.

| # | Lavoro | Come | File |
| --- | --- | --- | --- |
| 1 | Account e marchio | account sviluppatore Play, verifica d'identità, profilo pagamenti; icona originale; nome scheda "HAndroid – server domotico" | — |
| 2 | Formato AAB | `bundleRelease` con `python.zip` in un *install-time asset pack* (modulo `:python`); `HaService.extractPython` legge dall'asset pack | `settings.gradle.kts`, nuovo modulo, `HaService.kt` |
| 3 | Niente codice scaricato in `play` | pip usa solo i wheel inclusi (`--no-index` sui pacchetti nativi); le integrazioni con pacchetti nativi non inclusi mostrano "non disponibile nella versione Play". Si allarga `integrazioni-indice.txt` per includerne di più | `handroid_boot.py` (`_install_package`), `scripts/` |
| 4 | Abbonamento | Google Play Billing: prodotto `handroid_mensile` a 2,99 € con 7 giorni di prova. Se è scaduto HA continua; si fermano solo i servizi del server e compare un promemoria | `Abbonamento.kt`, `MainActivity.kt`, spec 0014 |
| 5 | Prima configurazione guidata | schermate in sequenza: permessi, esclusione batteria, avvio automatico del produttore (MIUI, Samsung, OnePlus: apre la pagina giusta), creazione account HA, accesso da fuori casa, backup | `Benvenuto.kt` |
| 6 | Interfaccia rinnovata e in inglese | dalla schermata a pulsanti a una vera interfaccia (Material 3); testi in `strings.xml` italiano e inglese | `res/`, `MainActivity.kt` |
| 7 | Regole di Play | targetSdk all'ultimo richiesto; dichiarazioni per servizio `specialUse`, esclusione batteria, Bluetooth, device owner (con video); informativa privacy e "Sicurezza dei dati" sul sito; schermata licenze (Apache, LGPL di FFmpeg con link ai sorgenti) | `AndroidManifest.xml`, `sito/`, `Licenze.kt` |
| 8 | Segnalazione crash | facoltativa e anonima, proposta nella configurazione guidata; invio al nostro server, senza SDK di terzi | `HaService.kt`, server |
| 9 | Altri telefoni | Pixel e Samsung in prova chiusa; una guida per produttore nella configurazione guidata | `docs/` |

## 1.0.0 · Ufficiale

Gate: pubblicata su Play; prima settimana con crash sotto l'1% e nessuna recensione su dati persi.

| # | Lavoro | Come |
| --- | --- | --- |
| 1 | Aggiornamenti automatici a regime | il workflow della 0.7.0 pubblica su Play (API di pubblicazione, canale interno), aspetta 48 ore di prova, poi promuove in produzione. Backup di HA prima di ogni aggiornamento, fatto dall'app quando Play la sostituisce |
| 2 | Backup nel cloud e avviso del server (servizi a pagamento) | backup cifrati sul telefono e caricati sul nostro server; avviso via e-mail o notifica se il telefono smette di dare segni di vita |
| 3 | Sito e supporto | pagine supporto, domande frequenti, telefoni provati, informativa; e-mail di supporto |
| 4 | Lancio | pubblicazione, prezzo attivo, annuncio su forum e community di HA (senza usare il loro marchio come nostro) |

## Dipendenze tra le versioni

- La **build automatica** (0.7.0) serve prima degli add-on nativi (0.8.0): ogni binario nuovo allunga la build.
- Il **server** nasce nella 0.8.0 con l'accesso remoto e cresce nella 0.9.0-1.0.0 con abbonamento, crash, backup e avvisi.
- I **flavor** (0.6.0) servono a tutto quello che cambia tra GitHub e Play nella 0.9.0.

## Da decidere strada facendo

- 0.7.0: GitHub Actions basta per i tempi di build? Se no, una macchina arm64 in affitto.
- 0.8.0: dominio del servizio di accesso remoto; dove ospitare il server (stessa macchina della build o un hosting piccolo).
- 0.9.0: partita IVA o regime per gli incassi.
