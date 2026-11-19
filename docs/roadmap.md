# Roadmap: dalla 0.5.0 alla 1.0.0 sul Play Store

18 novembre 2026. Continua le milestone M0-M4 di [progetto-handroid.md](progetto-handroid.md). Ogni versione ha un **gate**: una prova sul telefono che deve passare prima di aprire la successiva, come per le milestone.

Il piano lavoro per lavoro (come, file, verifiche) sta in [piano-versioni.md](piano-versioni.md).

## In breve

| Versione | Tema | Cosa ottiene chi la usa | Gate |
| --- | --- | --- | --- |
| **0.6.0** | Affidabile e al sicuro | i dati non si perdono, sai subito se il server si ferma | 7 giorni sul 12T senza interventi, con un ripristino da backup riuscito |
| **0.7.0** | Il telefono che fa di più | comandi vocali, fotocamera e sirena dal telefono server | "Ok Nabu, accendi la luce" funziona a 3 metri |
| **0.8.0** | Più dispositivi | Matter, Zigbee2MQTT, telecamere WebRTC | una presa Matter e una lampadina Zigbee in HA, video in diretta nella Companion |
| **0.9.0** | Pronto per il Play Store | prima configurazione guidata, app in inglese, conforme alle regole di Google | prova chiusa su Play con 12 tester per 14 giorni, su almeno 3 modelli di telefono |
| **1.0.0** | Ufficiale | app su Play con abbonamento e servizi, codice libero su GitHub | pubblicata, prima settimana senza crash sopra l'1% |

## 0.6.0 · Affidabile e al sicuro

Prima di aggiungere funzioni, il server deve essere a prova di guasto: un vecchio telefono è un solo punto di rottura.

- **Correzioni dalla prova della 0.5.0**: consumo, telecamere, Cloud, tunnel, segno della corrente su MIUI. Prima voce della versione, qualunque cosa emerga.
- **Backup fuori dal telefono**: copia automatica notturna su una cartella di rete (SMB) o su un PC di casa. Facoltativa su Google Drive. Pulsante "Ripristina da file" nell'app per un telefono nuovo.
- **Avviso se il server si ferma**: ping ogni 5 minuti verso un servizio esterno gratuito (es. healthchecks.io, URL incollato nell'app), che ti scrive se il ping manca.
- **GitHub senza APK** (vedi "Modello"): rilasci solo con codice e note; via "Cerca aggiornamenti" e l'installer da PC.
- **APK più leggero**: niente sorgenti `.py` accanto ai `.pyc` (circa 100 MB in meno sul telefono). Il log di HA resta leggibile, mancano solo le righe di codice nei traceback.
- **Gate dei 7 giorni** della M1, mai verificato: diventa il gate di questa versione.

## 0.7.0 · Il telefono che fa di più

Sfrutta l'hardware che un Raspberry non ha.

- **Satellite vocale di Assist**: microfono e altoparlante del telefono, parola di attivazione locale (microWakeWord, già fra le dipendenze di HA) e pipeline di Assist. Si espone dal dispositivo ESPHome "HAndroid", come fanno i satelliti ESP32.
- **Fotocamera del telefono in HA**: istantanee e stream (FFmpeg c'è già). Spenta di default, per batteria e privacy.
- **Altoparlante come media player**: TTS e suoni da HA (sirena antifurto, campanello, avvisi).
- **Sensori in più**: movimento (accelerometro: qualcuno tocca il telefono), rumore ambiente in dB.

## 0.8.0 · Più dispositivi

Il primo lavoro su add-on nativi, che la spec 0006 aveva lasciato fuori. Lo schema c'è già: binari compilati per Android dentro l'APK, come `ffmpeg` e `cloudflared`.

- **Matter Server**: le prese e lampadine Matter in HA. Thread solo con un border router esterno.
- **go2rtc**: telecamere WebRTC senza ritardo, e RTSP/ONVIF convertite per l'app Companion.
- **Zigbee2MQTT**: serve Node.js per Android, il lavoro più pesante della versione. Usa il ponte USB e l'add-on MQTT che ci sono già. Se Node si rivela troppo pesante, si rimanda e ZHA resta la via per Zigbee.
- **Pannello degli add-on nella barra laterale** (ingress), così la loro interfaccia si apre dentro HA.

## 0.9.0 · Pronto per il Play Store

Le regole di Google chiedono cambiamenti veri, non solo il caricamento. Da verificare sulle regole in vigore al momento:

- **Codice eseguibile scaricato**: Play vieta di scaricare `.so` fuori da Play. Oggi pip scarica wheel nativi dall'indice e gli add-on scaricano binari. Nella versione Play tutti i pacchetti nativi vanno dentro l'app (asset pack). I pacchetti puri Python sono codice interpretato: da confermare con la policy.
- **Aggiornamenti**: nella versione Play niente "Cerca aggiornamenti" né installazione di APK. Si aggiorna solo da Play. Due *flavor* Gradle: `github` e `play`.
- **Dimensioni**: un AAB ha un limite per il modulo base; `python.zip` va in un *install-time asset pack* (Play Asset Delivery).
- **Permessi delicati**: servizio in primo piano `specialUse`, esclusione dall'ottimizzazione batteria, Bluetooth, device owner. Ognuno con motivazione nella scheda Play e un video di dimostrazione.
- **targetSdk** all'ultimo livello richiesto da Play (oggi 35).
- **Prima configurazione guidata**: permessi, batteria, avvio automatico su MIUI/Samsung, account HA, accesso da fuori casa. Chi compra dal Play Store non legge `guida-sviluppo.md`.
- **Inglese** oltre all'italiano, nell'app e nelle schede.
- **Altri telefoni**: almeno Pixel e Samsung oltre allo Xiaomi, perché ogni produttore chiude le app in background a modo suo.
- **Privacy**: informativa (il sito va bene), scheda "Sicurezza dei dati", schermata delle licenze (Apache di HA, LGPL di FFmpeg con il link ai sorgenti).
- **Segnalazione dei crash** facoltativa e anonima, per sapere cosa si rompe sui telefoni degli altri.

## 1.0.0 · Ufficiale

- **Play Store con abbonamento**, codice su GitHub senza APK pronti (vedi "Modello" qui sotto).
- Sito con pagina di supporto, domande frequenti e lista dei telefoni provati.
- Canale di segnalazione (GitHub Issues o e-mail) e un ritmo di aggiornamento: una versione ogni release mensile di HA.

## Modello

**Chi paga ha tutto pronto; chi smanetta compila da sé.**

| | Play Store | GitHub |
| --- | --- | --- |
| Cosa trovi | app pronta, si installa in un tocco | solo codice sorgente e indice dei pacchetti |
| Prezzo | abbonamento **2,99 €/mese** (prova gratuita di 7 giorni) | gratis, licenza Apache 2.0 |
| Aggiornamenti | automatici da Play, a ogni release di HA | li compili tu (`guida-sviluppo.md`) |
| Configurazione | guidata nell'app | a mano, con la guida |
| Supporto | e-mail | Issues, senza garanzie |

Conti in breve (da verificare al lancio): su 2,99 € l'IVA (22% in Italia) e la quota di Google sugli abbonamenti (15%) lasciano circa **2,08 € al mese per abbonato**. Le spese fisse (account Play una volta 25 $, dominio circa 10 €/anno, sito) si coprono con pochi abbonati. Il resto paga il tempo per stare dietro alle release di HA.

Cosa cambia per arrivarci:

- **0.6.0: GitHub senza APK.** `rilascio.sh` crea il tag e le note, ma non carica più l'APK. "Cerca aggiornamenti" e `installa-handroid.sh` spariscono dal flavor `github`: chi compila aggiorna compilando. I rilasci 0.4.0 e 0.5.0 già pubblicati restano come archivio.
- **0.9.0: abbonamento con Google Play Billing** nel flavor `play`. Se l'abbonamento scade, **HA continua a funzionare**: una casa non si spegne per un pagamento saltato. Si fermano i servizi a pagamento (vedi sotto) e compare un promemoria nell'app.
- La licenza Apache permette a chiunque di compilare e anche ridistribuire l'APK: è una scelta consapevole. Si paga la comodità, non il codice.

### Cosa giustifica un abbonamento

Un abbonamento regge se dà qualcosa di continuo, non solo l'app. Il cuore è il **servizio di aggiornamenti automatici**, pagato dagli abbonamenti:

- **Aggiornamenti automatici**: un nostro server di build (il "sito pagato dall'app") controlla ogni giorno le release di HA. Quando ne esce una, rigenera dipendenze e wheel, compila, fa girare i test e una prova di avvio su un telefono o emulatore arm64, e pubblica su Play in un canale di prova. Dopo 48 ore senza crash la promuove a tutti. L'utente non fa niente: Play aggiorna l'app di notte e HA riparte già aggiornato, con un backup fatto prima.
- Gli aggiornamenti passano **sempre da Play**, mai scaricati dall'app: è la regola di Google sul codice eseguibile, e così chi paga ha anche la sicurezza della firma di Play.
- **Accesso da fuori casa senza dominio**: un indirizzo `https://<nome>.handroid.<dominio>` creato in automatico dal nostro server con l'API di Cloudflare, senza account né dominio per l'utente (la stessa idea di Nabu Casa, a meno).
- **Backup automatici nel cloud**, cifrati sul telefono prima dell'invio.
- **Avviso se il server si ferma** gestito da noi, senza servizi esterni da configurare.

Tutto questo gira su un nostro piccolo server: è il sito pagato dall'app. La build arm64 con WSL oggi dura ore e occupa un PC, quindi va spostata lì (una macchina arm64 in affitto costa pochi euro al mese, o le ore gratuite di GitHub Actions per i repo pubblici).

## Decisioni da prendere

| Decisione | Quando | Perché conta |
| --- | --- | --- |
| **Nome**: deciso, **HAndroid**. Resta da evitare "Home Assistant" nel titolo della scheda e il suo logo nell'icona; nella descrizione basta "esegue Home Assistant Core", da verificare con la policy sui marchi della Open Home Foundation | prima della 0.9.0 | Google rimuove le app che usano marchi altrui |
| **Prezzo**: deciso, abbonamento 2,99 €/mese su Play; GitHub solo codice | — | — |
| **Rilasci 0.4.0 e 0.5.0 su GitHub**: deciso, restano come archivio | — | — |
| **Server degli aggiornamenti automatici** (e poi accesso remoto, backup, avvisi): dove ospitarlo, GitHub Actions o macchina in affitto | 0.7.0 per la build automatica, 0.8.0 per i servizi | l'abbonamento deve offrirli al lancio della 1.0.0 |
| **Account sviluppatore Play** (25 $ una volta), verifica d'identità, conto e partita IVA o regime per gli incassi | 0.9.0 | serve qualche settimana tra verifica e prova chiusa; le entrate vanno dichiarate |

## Rischi

- **La policy sul codice scaricato** è il rischio più grande per la versione Play. Se anche i pacchetti puri Python non sono ammessi, la versione Play può installare solo le integrazioni i cui pacchetti sono già inclusi.
- **Produttori aggressivi con la batteria** (MIUI, Samsung, OnePlus): servono prove su più telefoni e una guida per ciascuno.
- **Ogni release di HA** cambia dipendenze native: la pipeline dei wheel deve restare automatica.
