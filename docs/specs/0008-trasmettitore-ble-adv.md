# 0008 · Trasmettitore BLE per ble_adv (lampade FanLamp)

15 novembre 2026. Estende la [0004](0004-dispositivo-esphome.md) (decisione E6) e usa la scansione della [0005](0005-proxy-bluetooth.md).

Le lampade e i ventilatori "a annunci" (FanLamp Pro, LampSmart, Zhijia…) non hanno connessione: ricevono comandi come annunci BLE. Li gestisce l'integrazione personalizzata [ble_adv](https://github.com/NicoIIT/ha-ble-adv), che di solito trasmette da un ESP32 con il componente `ble_adv_proxy`. Il dispositivo HAndroid imita quel componente, così trasmette il telefono.

## Requisiti

- Il dispositivo ESPHome "HAndroid" espone ciò che ble_adv cerca: sensore di testo `ble_adv_proxy_name` (valore `handroid`, diagnostico) e le azioni `setup_svc_v0` e `adv_svc_v1`, con gli stessi argomenti di `ble_adv_proxy`. In HA diventano `esphome.handroid_setup_svc_v0` e `esphome.handroid_adv_svc_v1`.
- `adv_svc_v1` trasmette l'annuncio (`raw` in esadecimale, massimo 31 byte) come annuncio legacy non connettibile, uno alla volta in coda.
- Gli annunci visti dal proxy Bluetooth tornano a HA come evento `esphome.ble_adv.raw_adv` (`raw`, `orig`) solo dopo `setup_svc_v0`, filtrati come fa `ble_adv_proxy` (duplicati, produttori e MAC esclusi, annunci appena trasmessi). Servono per abbinare il telecomando originale.
- Senza il permesso `BLUETOOTH_ADVERTISE` ("Dispositivi nelle vicinanze") o con il Bluetooth spento all'avvio il modulo non si attiva e il sensore non compare.

## Messaggi aggiunti (api.proto)

| Id | Messaggio | Uso |
|---|---|---|
| 18 / 27 | ListEntitiesTextSensor / TextSensorState | `ble_adv_proxy_name` |
| 41 | ListEntitiesServicesResponse | le due azioni, con gli argomenti (`float`, `string`, `float[]`, `string[]`) |
| 42 | ExecuteServiceRequest | argomenti nell'ordine dichiarato; i `float[]` si leggono packed e non |
| 34 → 35 | SubscribeHomeassistantServices → HomeassistantActionRequest con `is_event` | evento `esphome.ble_adv.raw_adv`; HA aggiunge `device_id` |

## Codice (`app/src/main/java/it/handroid/esphome/`)

| File | Ruolo | Dipende da Android |
|---|---|---|
| `BleAdv.kt` | azioni, lettura degli argomenti, evento, scomposizione dell'annuncio, filtro | no (test in `BleAdvTest.kt`) |
| `TrasmettitoreBle.kt` | `Modulo`: coda e `BluetoothLeAdvertiser.startAdvertisingSet` | sì |

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| A1 | **L'annuncio grezzo si scompone** in UUID a 16 bit, service data a 16 bit e manufacturer data; i flag si tolgono. | `AdvertiseData` di Android non accetta byte grezzi. I formati FanLamp usano una lista di UUID (tipo `0x03`) o manufacturer data (`0xFF`). | Un annuncio con altri tipi (es. nome) non si trasmette: resta un avviso nel log. Android tiene l'ordine degli UUID, non la posizione dei flag. |
| A2 | **Finestra di trasmissione = `duration` × `repeat`, tra 300 ms e 2 s**, intervallo minimo di Android (100 ms). | ESPHome scende a 20 ms; Android no. Con una finestra breve la lampada vedrebbe una copia sola. | Comandi uno ogni ~0,3 s: va bene per luci e ventilatori, non per sequenze rapide. |
| A3 | **Ricezione dal proxy Bluetooth** (spec 0005), non da una scansione propria. | Android limita le scansioni per app; HA tiene già attiva quella del proxy. | Gli eventi arrivano solo mentre la scansione è attiva, e a schermo spento solo per i formati dei filtri B4. |
| A4 | **ble_adv non sta nell'APK**: si installa con `scripts/installa-integrazione.sh NicoIIT/ha-ble-adv`. Il suo `pycryptodome` (AES, nativo) è nell'indice dei wheel (`pip:pycryptodome` in `integrazioni-indice.txt`). | Integrazione di terzi con un suo ciclo di rilasci. | Dopo l'installazione: Aggiungi integrazione → "BLE ADV Ceiling Fan / Lamps", adattatore `esp/handroid`. |

## Provato sul 12T (15 novembre)

- Con `aioesphomeapi` sul telefono: sensore `ble_adv_proxy_name` e le due azioni con gli argomenti giusti; `adv_svc_v1` con un annuncio FanLamp v2 avvia e ferma l'annuncio (`status=0` nel log di Android, finestra di 300 ms).
- pip sul telefono installa pycryptodome dall'APK e l'AES funziona.
- Da provare: la lampada vera, e l'abbinamento dall'interfaccia di ble_adv.
