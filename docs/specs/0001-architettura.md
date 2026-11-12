# 0001 · Architettura di HAndroid (decisioni fissate)

12 novembre 2026. Queste scelte valgono per tutto il progetto; si cambiano solo con una nuova specifica.

## Obiettivo

Un telefono senza root diventa un server Home Assistant completo: si installa un APK con un comando, e il telefono espone HA su `:8123`, usa Bluetooth, USB e i propri sensori, e resta acceso da solo.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| D1 | **CPython ufficiale per Android** (python.org, `aarch64-linux-android`), avviato in-process da un launcher JNI (`app/src/main/cpp/launcher.c`). Niente Chaquopy, Termux o proot. | È l'unica build ufficiale di Python 3.14 per Android; nessuna emulazione. | La versione di Python si cambia solo in `PYTHON_VERSION` (workflow + `prepara-python.sh`). |
| D2 | **Home Assistant non modificato.** Si avvia con `hass --config <dir> --runner`. | Aggiornamenti identici a quelli ufficiali. | Ogni adattamento a Android sta fuori da HA: in `handroid_main.py` o nei bridge Kotlin. |
| D3 | **Mai `execve`.** Android 10+ vieta di eseguire file dalla cartella dati; `dlopen` invece è permesso. | Le wheel con `.so` si caricano anche se installate a runtime. | Ogni eseguibile esterno (uv, ffmpeg…) va nell'APK come `lib<nome>.so` in `jniLibs`. Le integrazioni che lanciano programmi non inclusi non funzionano. |
| D4 | **Python gira nel processo `:ha`**, separato dall'interfaccia. | CPython non si reinizializza: per riavviare HA si chiude il processo e `START_STICKY` lo riapre. | Il watchdog (30 s, 3 errori) e la modalità provvisoria (3 avvii falliti) stanno in `HaService`. |
| D5 | **Dati nello storage cifrato per dispositivo** (Direct Boot). | Il server riparte dopo un riavvio anche senza sbloccare il telefono. | Niente PIN sul telefono server. Percorsi: `python/`, `config/`, `site-packages/`. |
| D6 | **L'interfaccia è HA stesso** in una WebView su `127.0.0.1:8123`. | Niente UI da mantenere: l'app *è* Home Assistant anche sul telefono server. | Le schermate native servono solo per stato, add-on e impostazioni del supervisor. |
| D7 | **Hardware tramite protocolli che HA già conosce.** Bluetooth e sensori = un dispositivo **ESPHome** (API nativa su `127.0.0.1:6053`, annunciato via mDNS); Zigbee/Z-Wave USB = `socket://127.0.0.1:6638`. | Zero patch a HA, e il dispositivo viene scoperto da solo. | Il bridge Kotlin implementa solo il sottoinsieme ESPHome necessario: device info, sensori, switch, proxy BLE (annunci passivi + GATT). |
| D8 | **Wheel native da un indice pip nostro** (GitHub Pages), compilate da cibuildwheel in un workflow separato e lanciato a mano. | Le build delle wheel sono lunghe: non devono pesare su ogni release. | `HANDROID_SITE` è la cartella dei pacchetti; pip punta prima al nostro indice. |
| D9 | **Device owner come modalità consigliata**, con un solo comando: `scripts/installa-handroid.sh --device-owner`. | Su HyperOS è l'unico modo affidabile per non far chiudere l'app senza root. | Funziona solo su un telefono senza account. Senza device owner l'app va lo stesso, ma con le impostazioni batteria da sistemare a mano. |
| D10 | **Add-on**: prima quelli Python (stesso interprete); quelli nativi come moduli dentro l'APK; APK companion solo come piano B. | Eseguire binari di un'altra app dipende da SELinux e non è garantito. | Manifest YAML come in `docs/progetto-handroid.md`. |
| D11 | **Release automatica**: ogni push su `master` che tocca l'app pubblica `HAndroid-0.1.<run>-arm64.apk`, firmato con il keystore nei secret. | Aggiornamento = scaricare l'ultima release. | Il keystore non si perde mai: senza, gli APK nuovi non si installano sopra i vecchi. |

## Procedura utente (obiettivo M1)

1. Reset del telefono, nessun account, debug USB attivo.
2. Dal PC: `./scripts/installa-handroid.sh --device-owner`.
3. Aprire `http://IP-DEL-TELEFONO:8123` dal telefono personale (app Companion).

## Stato

- Fatto (M0a): APK con Python 3.14 incorporato, servizio sempre attivo, avvio al boot, watchdog, device owner, pagina di stato su `:8123`, release automatica.
- Prossimo (M0b): installare HA Core e le sue dipendenze con wheel Android; inventario delle wheel mancanti.
- Da verificare sul 12T: che `ifaddr`/zeroconf vedano le interfacce di rete (Android 11+ limita netlink) e che `dlopen` da `site-packages` funzioni con `targetSdk 35`.
