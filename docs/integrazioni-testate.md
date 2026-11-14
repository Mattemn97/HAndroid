# Integrazioni provate su HAndroid

Provate sul Xiaomi 12T (Android 14, MIUI), HAndroid 0.3.0-m3, Home Assistant 2026.11.1. "Funziona" vuol dire che l'integrazione si carica e fa ciò che serve; per quelle con un dispositivo, che il dispositivo risponde.

| Integrazione | Stato | Note |
| --- | --- | --- |
| Interfaccia, onboarding, backup | funziona | |
| Meteo (met), Radio Browser, Google Translate TTS | funziona | incluse nell'APK |
| zeroconf, SSDP | funziona | trovati da soli: Chromecast, Xbox, il dispositivo ESPHome del telefono |
| ESPHome | funziona | dispositivo "HAndroid" del telefono: 9 entità, torcia comandata da HA |
| MQTT | funziona | con l'add-on Broker MQTT sul telefono, anche il discovery MQTT |
| Add-on HAndroid | funziona | stato e accensione degli add-on da HA |
| mobile_app (app Companion) | si carica | da provare con l'app Companion su un altro telefono |
| hardware | funziona | psutil compilato per Android |
| Bluetooth | si carica | nessun adattatore diretto: i sensori BLE passano dal proxy ESPHome, da provare con un sensore vero |
| USB | si carica | il ponte per chiavette Zigbee/Z-Wave è da provare con una chiavetta |
| Philips Hue | configurazione avviata | provata sull'emulatore, senza bridge |
| Cast | trovato | da aggiungere e provare |
| cloud (Nabu Casa) | non funziona | serve grpcio, non ancora compilato per Android |
| dhcp | non funziona | servono socket raw, cioè root |
| stream, go2rtc, telecamere | non funziona | servono FFmpeg e l'eseguibile go2rtc per Android |

Le integrazioni con pacchetti nativi non ancora compilati mostrano una notifica "HAndroid: pacchetto mancante". L'elenco dei pacchetti rinviati e il motivo stanno in [`scripts/android-rinviati.txt`](../scripts/android-rinviati.txt).

Per aggiungere una riga: prova sul telefono, poi scrivi qui stato e versione.
