# 0004 · Il telefono come dispositivo ESPHome

12 novembre 2026. Attua la decisione D7 della [0001](0001-architettura.md) per sensori e switch; il proxy Bluetooth si aggiunge sopra (spec successiva).

## Requisiti

- HA vede un dispositivo **HAndroid** (integrazione ESPHome), scoperto da solo via mDNS (`_esphomelib._tcp`) o aggiunto a mano con host `127.0.0.1`, porta `6053`, senza chiave.
- Entità: batteria %, in carica, temperatura batteria, luce ambiente e prossimità (se il sensore c'è), segnale WiFi, uptime, interruttore torcia (se c'è il flash), pulsante "Riavvia Home Assistant".
- Aggiornamenti solo a cambio valore, con soglia per sensore e al massimo uno ogni 5 s per entità; sensori fisici campionati a 1 s con consegna a lotti; WiFi e uptime letti ogni 60 s.

## Codice (`app/src/main/java/it/handroid/esphome/`)

| File | Ruolo | Dipende da Android |
|---|---|---|
| `Protobuf.kt` | codec protobuf a mano (varint, fixed32, float, bool, stringhe) | no |
| `Frame.kt` | framing in chiaro: `0x00`, varint lunghezza, varint tipo, payload | no |
| `Entita.kt` | numeri dei messaggi (`Msg`), entità, `Registro` con soglia e limite di frequenza | no |
| `ApiServer.kt` | server TCP, handshake, dispatch, interfaccia `Modulo` per le estensioni | solo `Log` |
| `EspHome.kt` | avvio dal processo `:ha`, mDNS con `NsdManager`, sensori del telefono | sì |

I test JVM (`app/src/test`) coprono codec, framing, chiavi, limite di frequenza e una sessione completa su socket; la CI li esegue con `:app:testReleaseUnitTest`.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| E1 | **Protocollo in chiaro, niente Noise né password.** | HA accetta un dispositivo ESPHome senza chiave (campo vuoto nel config flow); il traffico resta sul telefono. | `api_encryption_supported` non si dichiara, così HA non propone di impostare una chiave. Un frame Noise (`0x01`) chiude la connessione. |
| E2 | **Ascolto su tutte le interfacce, ma solo connessioni dal telefono stesso** (loopback o indirizzo locale = remoto). Preferenza `lan` (file `esphome`) per aprire alla rete. | Via mDNS HA vede l'indirizzo LAN del telefono, non `127.0.0.1`. | Dalla LAN la porta rifiuta: senza chiave chiunque potrebbe accendere la torcia o riavviare HA. |
| E3 | **API 1.10** in HelloResponse; numeri dei messaggi e campi verificati su `api.proto` (ramo `dev`, novembre 2026). | Basta per sensori, switch e pulsanti; i client più nuovi gestiscono i dispositivi più vecchi. | Il proxy BLE potrà alzarla se servono messaggi più recenti. |
| E4 | **Protobuf scritto a mano** (~90 righe) invece di protobuf-javalite. | Pochi tipi di campo, nessuna dipendenza né generazione di codice. | Ogni messaggio nuovo si scrive a mano con `ProtoWriter`/`ProtoReader`. |
| E5 | **Chiave entità = FNV-1a dell'object_id**, MAC casuale locale generato una volta e salvato. | Android non espone il MAC; HA usa MAC e chiavi come identità stabili. | Reinstallare l'app (dati cancellati) crea un dispositivo nuovo in HA. |
| E6 | **Estensioni tramite `Modulo`**: riceve i messaggi che il nucleo non gestisce, aggiunge campi a DeviceInfo, viene avvisato alla chiusura. | Il proxy BLE (messaggi 66+, `bluetooth_proxy_feature_flags` = campo 15) si aggiunge senza toccare il server. | `EspHome.server?.moduli += ProxyBle(...)` prima della prima connessione. |

## Da verificare sul telefono

- Che lo zeroconf di HA (Python nel processo `:ha`) riceva l'annuncio di `NsdManager` (dipende da netlink, vedi 0001). Se no, si aggiunge a mano con `127.0.0.1`.
- Che HyperOS consegni i sensori luce e prossimità anche a schermo spento.
