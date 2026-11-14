# Home Assistant su Android: Xiaomi 12T come server

Questa cartella raccoglie tutto quello che serve per trasformare un vecchio **Xiaomi 12T** nel server Home Assistant di casa. Il server diventa il "master"; il telefono personale fa da "slave" e lo comanda con l'app Companion. Niente mini PC, niente Raspberry.

Stato al 13 novembre 2026: **HAndroid** (strada C) gira sul 12T. Home Assistant parte dall'APK, riparte da solo, trova i dispositivi in rete, espone i sensori del telefono e ha il primo add-on (broker MQTT). Pagina del progetto e indice dei pacchetti: <https://mattemn97.github.io/HAndroid/>.

---

## Contenuto della cartella

```
HAndroid/
├── README.md                               ← questo file
├── app/                                    ← app HAndroid (strada C), in sviluppo
├── docs/
│   ├── guida-sviluppo.md                   ← come compilare, provare e contribuire
│   ├── registro.md                         ← cosa è stato fatto e quando
│   ├── progetto-handroid.md                ← progetto dell'app "HAndroid" (HA nativo su Android)
│   ├── dipendenze-android.md               ← pacchetti di HA e stato su Android (generato)
│   ├── addon/                              ← formato degli add-on (bozza)
│   └── specs/                              ← specifiche delle funzioni
└── scripts/
    ├── prepara-server-xiaomi.sh            ← dal PC via ADB, dopo il reset (strada A)
    ├── installa-homeassistant-android.sh   ← dentro Termux, piano B (strada B)
    └── …                                   ← build di HAndroid: vedi la guida allo sviluppo
```

---

## Il punto di partenza

| Cosa | Dettaglio |
| --- | --- |
| Telefono server | Xiaomi 12T (codename `plato`), Dimensity 8100, 8 GB RAM, HyperOS 2 / Android 15 |
| Dispositivi da collegare | Google (Nest/Cast), un paio di Tuya, Xiaomi, altro |
| Obiettivo | Un solo Home Assistant sul vecchio telefono, gestito dall'app sul telefono personale |
| Vincoli | Niente Docker, niente root, niente mini PC. Il 12T si può resettare e tenere solo per questo |

Cose già verificate:
- Per il 12T non esistono ROM custom e postmarketOS non lo supporta. Si resta quindi su HyperOS, senza root.
- Home Assistant Core richiede Python 3.14 e un sistema a 64 bit (il 12T va bene).
- Senza root, nessuna soluzione esistente dà a Home Assistant l'accesso al **Bluetooth** del telefono o alle **chiavette USB** (Zigbee/Z-Wave).

---

## Le tre strade

### A. Homedroid: quella consigliata adesso

[Homedroid](https://github.com/sudiptacmd/homedroid) è un'app (un solo APK, GPL-3.0) che installa Home Assistant con un tocco. Docker e root non servono.
- È ancora in beta (0.9.x) e il progetto è piccolo: fai backup spesso.
- Non può usare Bluetooth né chiavette USB, e non ha add-on.
- Supporta Tailscale per l'accesso da fuori casa.

**Procedura:**

1. Fai un backup di quello che ti serve dal 12T, poi vai su **Impostazioni → Informazioni sul telefono → Ripristino dati di fabbrica**.
2. Alla prima configurazione **salta l'account Google**. Collega solo il WiFi e non impostare un PIN.
3. Per attivare le **Opzioni sviluppatore** tocca 7 volte "Versione OS". Poi accendi:
   - Debug USB
   - Installa tramite USB
   - Debug USB (impostazioni di sicurezza)

   Su Xiaomi queste ultime due chiedono un account Mi e una SIM inserita. Accedi, attivale, poi esci dall'account.
4. Scarica l'APK arm64 di Homedroid dalle [Release su GitHub](https://github.com/sudiptacmd/homedroid/releases).
5. Collega il telefono al PC (servono gli Android SDK Platform-Tools) e lancia:
   ```bash
   cd scripts
   ./prepara-server-xiaomi.sh homedroid-<versione>-arm64-v8a.apk --debloat
   ```
   Lo script:
   - installa l'APK;
   - lo toglie da risparmio energetico e restrizioni in background;
   - disattiva il phantom process killer di Android 12+;
   - mette il telefono in modalità server (luminosità minima, niente aggiornamenti automatici);
   - con `--debloat` rimuove le app Xiaomi inutili. La rimozione si annulla con `adb shell cmd package install-existing <pacchetto>`.
6. Sul telefono:
   - in Impostazioni → App → Homedroid attiva **Avvio automatico** e imposta Batteria su **Nessuna restrizione**;
   - blocca l'app nelle recenti (tieni premuto → lucchetto).
7. Dal router assegna un **IP fisso** al 12T.
8. In Homedroid avvia il server e installa Home Assistant dal catalogo. Poi apri `http://IP-DEL-12T:8123`.

### B. Termux + udocker: il piano B

Se Homedroid dà problemi c'è `scripts/installa-homeassistant-android.sh`, da lanciare **dentro Termux**:
- Installa Termux, Termux:Boot e Termux:API **da F-Droid**.
- Lo script scarica l'immagine ufficiale di Home Assistant e la fa girare con udocker.
- Prepara anche l'avvio automatico e un sensore "Batteria server".
- Crea i comandi `~/ha/start.sh`, `stop.sh`, `update.sh` e `log.sh`.
- Se non parte, riprova con `HA_EXECMODE=P2`.

Limiti: zeroconf, Bluetooth e USB sono disattivati di default per evitare errori, quindi i dispositivi si aggiungono a mano con l'IP.

### C. HAndroid: il progetto da costruire

Un'app tutta nostra che fa girare il **vero** Home Assistant dentro l'APK, con Python 3.14 ufficiale per Android, senza emulazione. In più:
- Bluetooth, chiavette Zigbee e sensori del telefono, tramite bridge locali.
- Add-on scaricabili solo quando servono.

Il progetto completo (architettura, bridge, add-on, roadmap, rischi) è in [`docs/progetto-handroid.md`](docs/progetto-handroid.md). Per compilarlo, provarlo o sviluppare add-on parti da [`docs/guida-sviluppo.md`](docs/guida-sviluppo.md).

Stato: milestoni M0-M3 fatte e provate sul 12T, M4 (rilascio) pronta da pubblicare. Manca il gate finale: 7 giorni di fila senza interventi. Dettagli nel [registro](docs/registro.md), integrazioni provate in [`docs/integrazioni-testate.md`](docs/integrazioni-testate.md).

Installazione dal PC, con il telefono collegato in debug USB:

```bash
scripts/installa-handroid.sh                  # ultimo rilascio da GitHub
scripts/installa-handroid.sh HAndroid.apk     # un APK locale
``` Esiste anche come documento modificabile: [HAndroid su Claude](https://claude.ai/artifact/SYwCcowPZbvN4x8w671skB).

---

## Collegare i dispositivi

Funziona tutto ciò che passa da WiFi o cloud. Con le strade A e B il Bluetooth e l'USB diretti non funzionano.

| Dispositivo | Integrazione | Note |
| --- | --- | --- |
| Tuya | **Tuya** (ufficiale) | Si collega con il QR dall'app Smart Life. Per il controllo locale c'è LocalTuya (HACS) |
| Xiaomi WiFi (luci, prese, purificatori, robot) | **Xiaomi Home** (ufficiale Xiaomi, via HACS) | Si collega con l'account Mi |
| Sensori Xiaomi Bluetooth | ESPHome **Bluetooth proxy** | Serve un ESP32 da 5-10 € che passa tutto in WiFi |
| Google Nest speaker / Chromecast | **Google Cast** | Se non vengono trovati da soli, aggiungili con l'IP |
| Termostati / telecamere Nest | **Nest** | Richiede la registrazione a Google Device Access (a pagamento, una tantum) |
| Zigbee | Coordinatore **di rete** (Ethernet/WiFi) | Non una chiavetta USB |
| Shelly, Hue (bridge), Sonos, TV, Matter WiFi, MQTT | Integrazioni standard | — |

Conviene aggiungere i dispositivi uno alla volta e fare un backup dopo ogni passo importante.

---

## Master / slave

- **Server (master):** il 12T, fermo in casa e attaccato alla corrente.
- **Client (slave):** l'app ufficiale **Home Assistant** (Companion) sul telefono personale, puntata a `http://IP-DEL-12T:8123`. L'app trasforma anche il telefono personale in un sensore (posizione, presenza, batteria).
- **Da fuori casa:** usa **Tailscale** (gratis) oppure Home Assistant Cloud (Nabu Casa, a pagamento).

## Batteria

Un telefono sempre in carica al 100% rischia che la batteria si gonfi. Le soluzioni:
- attivare la ricarica ottimizzata, se HyperOS la offre;
- oppure collegare il caricatore a una **presa smart** e fare un'automazione che la accende sotto il 30% e la spegne all'80%, usando il sensore batteria del server.

---

## Prossimi passi

- [ ] Reset del 12T e strada A (Homedroid) con `prepara-server-xiaomi.sh`
- [ ] IP fisso dal router
- [ ] App Companion sul telefono personale + Tailscale
- [ ] Aggiungere Tuya, Xiaomi Home e Google Cast
- [ ] Presa smart + automazione 30-80% per la batteria
- [ ] (Opzionale) ESP32 come proxy Bluetooth per i sensori Xiaomi BLE
- [x] (Progetto) HAndroid M0-M3: Home Assistant nell'APK, server affidabile, hardware del telefono, add-on
- [ ] HAndroid M4: primo rilascio su GitHub, poi 7 giorni di prova

## Note sugli script

Entrambi gli script sono stati controllati nella sintassi (`bash -n`), ma **non sono ancora stati provati su un 12T vero**. Su HyperOS alcune impostazioni potrebbero non accettare i comandi ADB: in quel caso vanno messe a mano, e lo script elenca quali sono.

## Fonti

- [Homedroid](https://github.com/sudiptacmd/homedroid)
- [Guida HA Core su Android senza root (udocker)](https://community.home-assistant.io/t/simple-and-fast-installing-home-assistant-core-and-matter-server-on-android-no-root-no-qemu/788933)
- [Xiaomi 12T (plato): ROM custom](https://customrombay.org/xiaomi-plato/)
- [BeeWare: binari Android ufficiali di CPython](https://beeware.org/news/buzz/august-2025-status-update)
- [cibuildwheel: supporto Android](https://cibuildwheel.pypa.io/en/stable/)
