# 0003 · Ponte USB seriale → TCP (Zigbee/Z-Wave)

12 novembre 2026. Attua D7: HA non vede le porte seriali del telefono, quindi ogni chiavetta diventa un socket TCP locale che ZHA e Z-Wave JS già sanno usare.

## Requisiti

- Chiavetta collegata → `socket://127.0.0.1:6638` (Zigbee) o `socket://127.0.0.1:6639` (Z-Wave, vendor 0x0658), senza riavviare HA.
- Chiavette coperte: Sonoff ZBDongle-P e ZBT-1/SkyConnect (CP210x), Sonoff ZBDongle-E (CH9102, CDC-ACM), ConBee II/III e Aeotec Z-Stick 7 (CDC-ACM).
- Scollegata e ricollegata, il ponte riparte da solo; ZHA si ricollega al socket.

## Dati

- Opzioni facoltative in `config/handroid_usb.json`: `{"baud": 115200, "rtscts": false}`. 115200 vale per EZSP/ZNP/Z-Wave; la ConBee II è CDC-ACM e ignora il baud. `rtscts` serve solo a chiavette CP210x con firmware a controllo di flusso hardware; quello software (XON/XOFF di EZSP) passa nel flusso e lo gestisce bellows.
- Stato per l'utente in `config/handroid_usb_stato.json`: nome, vid/pid, driver, `stato` (`attivo` / `in attesa del permesso` / `errore`) e `url` da incollare in ZHA. Si vede dall'editor file di HA.

## Sicurezza

- Server solo su loopback (`InetAddress.getLoopbackAddress()`), un client per volta: il nuovo chiude il vecchio.
- Si apre solo ciò che sembra seriale (Silicon Labs o interfacce CDC), mai dischi o tastiere.
- Il permesso USB lo dà l'utente: dialog di sistema al collegamento (`USB_DEVICE_ATTACHED` + `usb_device_filter.xml`, con "Usa sempre") o, per chiavette già collegate, `requestPermission`.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| U1 | **Driver minimi propri** (`UsbSeriale.kt`: CP210x e CDC-ACM) invece di usb-serial-for-android (mik3y, JitPack, serie 3.x). | La libreria tira `androidx.annotation` e un repository in più; le chiavette tipiche stanno tutte in due driver da ~100 righe. | CH340 classico e FTDI non sono supportati: se servono si aggiunge il driver, o si passa alla libreria con una nuova spec. |
| U2 | **Ponte nel processo `:ha`**, avviato da `HaService`. | Vive e muore con HA (D4): niente IPC. | Un riavvio del processo chiude e riapre anche la seriale. |
| U3 | **Attività invisibile `UsbAttivita`** sul collegamento. | `USB_DEVICE_ATTACHED` con permesso persistente arriva solo alle attività. | Sveglia il servizio con l'extra `ricontrolla_usb`. |
| U4 | **Porta fissa per tipo** (6638 Zigbee, 6639 Z-Wave), una chiavetta per porta. | URL stabile da scrivere una volta in ZHA. | Una seconda chiavetta Zigbee va in `errore` nello stato. |
| U5 | **Senza client i byte USB si scartano.** | ZHA all'apertura resetta il coordinatore. | Nessun buffer da gestire. |

## Non verificato

Prova su telefono reale con ciascuna chiavetta; comportamento di `requestPermission` dal servizio in background su HyperOS.
