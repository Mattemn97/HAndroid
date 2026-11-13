# Add-on di HAndroid

> **Bozza.** Gli add-on arrivano con la milestone M3: il formato è descritto qui per chi vuole iniziare a progettarne uno, ma l'app non li gestisce ancora. Ogni cambio di formato va segnato in fondo a questo file.

## Perché un formato nostro

Gli add-on di Home Assistant OS sono immagini Docker, e Android senza root non le può eseguire. HAndroid usa invece pacchetti piccoli, scaricati solo quando servono e descritti da un manifest YAML.

## Due tipi

| Tipo | Cosa contiene | Come gira | Esempi |
| --- | --- | --- | --- |
| `python` | un servizio Python | processo separato, con lo stesso interprete di HA | bridge, piccoli server |
| `native` | binari arm64 compilati | dentro un **APK companion**: Android esegue solo codice che arriva da un APK installato | Mosquitto, Zigbee2MQTT (Node), Matter Server, go2rtc |

Le librerie native di un add-on `python` si compilano come quelle di HA, con `scripts/compila-wheel-android.sh`.

## Manifest

Un file `addon.yaml` nella radice dell'add-on:

```yaml
slug: mosquitto            # identificativo unico, minuscolo
name: Mosquitto
version: 2.0.20
type: native               # python | native
description: Broker MQTT
ports: [1883]              # porte TCP aperte su 127.0.0.1 (o su tutte le interfacce se dichiarato)
permissions: [network]     # network | bluetooth | usb | storage
options:                   # valori di default
  allow_anonymous: false
schema:                    # tipi delle opzioni: genera la schermata delle impostazioni
  allow_anonymous: bool
```

Campi per tipo:

- `python`: `entrypoint: pacchetto.modulo` e `requirements:` (come in un requirements.txt).
- `native`: `apk: <url dell'APK companion>` e `exec: <nome del binario>` dentro le librerie native dell'APK.

Dal manifest l'app genera la schermata delle opzioni e il pannello nella barra laterale di HA.

## Indice degli add-on

Un file JSON su GitHub elenca gli add-on di un repository. Chiunque può aggiungere un repository, come in HA OS. Il formato dell'indice verrà definito nella M3, in `docs/specs/`.

## Cose da decidere (M3)

- Su Android 15, l'app principale può eseguire i binari nativi di un APK companion? Il piano B è includere gli add-on nativi principali nell'APK come moduli opzionali.
- Come vengono passate le opzioni all'add-on: variabili d'ambiente, file JSON, o entrambi.
- Come si aggiorna un add-on senza riavviare HA.

## Storico del formato

- 2026-11-13: prima bozza, presa da `docs/progetto-handroid.md`.
