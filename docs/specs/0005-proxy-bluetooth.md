# 0005 · Proxy Bluetooth ESPHome

12 novembre 2026. Estende la [0004](0004-dispositivo-esphome.md) (decisione E6): il dispositivo HAndroid fa anche da proxy Bluetooth, così l'integrazione Bluetooth di HA vede i sensori BLE (es. termometri Xiaomi) attraverso il Bluetooth del telefono.

## Requisiti

- HA rileva un proxy Bluetooth sul dispositivo HAndroid (DeviceInfo campo 15 `bluetooth_proxy_feature_flags`, campo 18 `bluetooth_mac_address`).
- Annunci BLE inoltrati come `BluetoothLERawAdvertisementsResponse` (93), a lotti di 16 o ogni 100 ms; scansione solo finché c'è almeno un client iscritto.
- Connessioni GATT attive: connect/disconnect, elenco servizi, lettura/scrittura di caratteristiche e descrittori, notifiche; al massimo **3** connessioni insieme (`BluetoothConnectionsFreeResponse`).
- Senza permessi Bluetooth (`BLUETOOTH_SCAN`/`CONNECT` da Android 12, posizione prima) o senza adattatore il modulo non si attiva e il resto del dispositivo funziona.

## Messaggi (verificati su `api.proto`, ramo `dev`, novembre 2026)

| Id | Messaggio | Gestione |
|---|---|---|
| 66 / 87 | Subscribe / Unsubscribe LE advertisements | avvia/ferma la scansione |
| 93 | BluetoothLERawAdvertisementsResponse | annunci grezzi (indirizzo, rssi sint32, tipo indirizzo, dati AD ≤ 62 byte) |
| 68 → 69 | BluetoothDeviceRequest → ConnectionResponse | connect V3 (con o senza cache), disconnect; pair/unpair/clear cache rispondono "non supportato" |
| 70 → 71, 72 | GetServices → un messaggio per servizio, poi Done | handle numerati in ordine di scoperta |
| 73/76 → 74 | Read caratteristica/descrittore → ReadResponse | |
| 75/77 → 83 | Write caratteristica/descrittore → WriteResponse | solo se è chiesta la risposta |
| 78 → 84, 79 | Notify → NotifyResponse, poi NotifyData | |
| 80 → 81 | Subscribe connections free → slot liberi, limite, occupati | rimandato a ogni cambio |
| 82 | GATTErrorResponse | errori Android (codici Bluetooth, gli stessi di ESP-IDF) |
| 126 / 127 | ScannerState / ScannerSetMode | stato reale; modo sempre "attivo" |

Feature dichiarate: passive scan, active connections, remote caching, raw advertisements, state and mode (`0x67`).

## Codice (`app/src/main/java/it/handroid/esphome/`)

| File | Ruolo | Dipende da Android |
|---|---|---|
| `Ble.kt` | numeri dei messaggi e flag, codifica, `LottoAnnunci`, `Slot`, `TabellaHandle`, `CodaGatt` | no (test in `BleTest.kt`) |
| `ProxyBle.kt` | `Modulo`: BluetoothLeScanner, BluetoothGatt, ricevitore schermo/adattatore | sì |

Tutto il lavoro BLE gira su un thread (`esphome-ble`): i messaggi di HA e le callback GATT vi vengono postati.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| B1 | **Limite di 3 connessioni GATT.** | Android ne regge poche in tutto (spesso 4-7) e il telefono può averne altre. | HA sceglie un altro proxy o aspetta uno slot. Timeout di 30 s su una connessione che non si completa. |
| B2 | **Remote caching dichiarato, cache di Android mai svuotata.** | Le connessioni V1 non esistono più: senza remote caching HA non si connette. Svuotare la cache GATT richiede `BluetoothGatt.refresh()`, API nascosta. | `cache clearing` non si dichiara; HA scrive da sé il descrittore CCCD, il proxy attiva solo la consegna locale delle notifiche. |
| B3 | **Handle sintetici** (1, 2, 3… in ordine servizio → caratteristiche → descrittori). | Android non espone gli handle ATT dei descrittori. | Gli handle sono stabili per connessione, non tra connessioni: va bene perché HA rilegge i servizi a ogni connect (lo chiede lui). |
| B4 | **Schermo spento: filtri "larghi" invece della scansione senza filtri.** Android ferma le scansioni non filtrate a schermo spento (e lo stesso vale per la scansione con `PendingIntent`, che passa dallo stesso controllo): allo spegnimento si riavvia con filtri a dati vuoti per service data `FE95` (Xiaomi MiBeacon), `181A` (firmware ATC/pvvx), `FCD2` (BTHome), `FDCD` (Qingping), `FE9F`, `FEAA` e manufacturer data Ruuvi `0499`, Govee `EC88`, SwitchBot `0969`. | Un filtro con dati vuoti accetta qualsiasi contenuto, e con più filtri basta che ne combaci uno. | A schermo spento si vedono solo questi formati; a schermo acceso tutto. Ogni cambio di schermo riavvia la scansione (Android limita a 5 avvii in 30 s). |
| B5 | **Niente pairing.** | I sensori tipici non lo richiedono; `createBond` richiederebbe un flusso di broadcast in più. | Le richieste di pair/unpair rispondono con errore. |
| B6 | **Modo sempre attivo.** | Android non ha un'API pubblica per la scansione passiva. | `ScannerSetMode` è accettato ma non cambia nulla; lo stato riporta "attivo". |
| B7 | **Tipo di indirizzo** da `BluetoothDevice.getAddressType()` solo su Android 15+, altrimenti "pubblico"; in connessione si usa `getRemoteLeDevice` (Android 13+) con il tipo chiesto da HA. | API disponibili. | Su Android 10-12 i dispositivi con indirizzo random statico potrebbero non connettersi. |

## Da verificare sul telefono

- Che HyperOS consegni gli annunci a schermo spento con i filtri di B4 (e se serve anche l'esenzione dall'ottimizzazione batteria, già nel device owner).
- Che HA mostri i termometri Xiaomi (integrazione Xiaomi BLE) tramite il proxy HAndroid, e che una connessione attiva (es. lettura di un LYWSD03MMC con firmware originale) vada a buon fine.
