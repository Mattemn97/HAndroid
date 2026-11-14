# Add-on di HAndroid

Gli add-on di Home Assistant OS sono immagini Docker, e Android senza root non le può eseguire. HAndroid usa pacchetti piccoli, descritti da un manifest YAML e scaricati da un repository HTTPS. Le decisioni sono nella [spec 0006](../specs/0006-addon.md).

## Come gira un add-on

- Ogni add-on attivo ha un **processo Android tutto suo** (`it.handroid:addon1`…`:addon3`): al massimo 3 insieme. Un crash dell'add-on non tocca HA, e gli add-on sopravvivono a un riavvio di HA.
- Il gestore sta nel processo di HA e risponde su `127.0.0.1:8099` (token in `files/addons/token`, leggibile solo dall'app).
- In HA l'integrazione **Add-on HAndroid** (Impostazioni → Dispositivi e servizi → Aggiungi → HAndroid) mostra un dispositivo per add-on, con sensore di stato e interruttore, più i servizi `handroid.install`, `start`, `stop`, `uninstall`, `set_options`, `repository`, `refresh`.

## Due tipi

| Tipo | Cosa contiene | Come gira |
| --- | --- | --- |
| `python` | file Python e requisiti pip | con lo stesso Python di HA, nel processo dell'add-on. I requisiti si installano al primo avvio in `addons/<slug>/site`, solo da wheel (anche dall'[indice Android](https://mattemn97.github.io/HAndroid/wheels/)) |
| `native` | un eseguibile arm64 | deve essere **dentro l'APK** come `lib<nome>.so` in `jniLibs`: Android esegue solo codice installato con l'app. Un add-on nativo nuovo richiede quindi una nuova versione dell'APK |

## Repository

Una cartella HTTPS con:

```
index.json              {"name": "Il mio repository", "addons": ["mqtt"]}
mqtt/config.yaml        manifest dell'add-on
mqtt/broker.py          file elencati in "files"
```

Il repository ufficiale è la cartella [`addons/`](../../addons) di questo repo, pubblicata su `https://mattemn97.github.io/HAndroid/addons` da `scripts/pubblica-indice.sh`. Altri si aggiungono con il servizio `handroid.repository`.

## Manifest (`config.yaml`)

```yaml
name: Broker MQTT
version: 0.1.0
type: python              # python | native
description: Broker MQTT 3.1.1 leggero, sulla porta 1883.
ports: [1883]             # due add-on con la stessa porta non girano insieme
permissions: [network]    # network | bluetooth | usb | storage
boot: auto                # auto: riparte con HA | manual
pip: []                   # requisiti pip (solo wheel)
files: [broker.py]        # scaricati accanto al manifest
entry: broker.py          # python: file .py o modulo; native: <nome> di lib<nome>.so
args: []                  # solo native
options:                  # valori di default
  port: 1883
schema:                   # tipi delle opzioni, come in HA OS
  port: port
```

Tipi dello schema: `bool int float str password email url port list(a|b) int(min,max)`; un `?` finale rende l'opzione facoltativa.

Il parser YAML è ridotto: mappe per indentazione, liste `[a, b]` e `- a`, scalari, commenti. Niente ancore né stringhe su più righe.

## Scrivere un add-on Python

- Le opzioni stanno nel file JSON indicato da `HANDROID_ADDON_OPTIONS` (cartella di lavoro: quella dell'add-on).
- `print` e gli errori finiscono in `addons/<slug>/log.txt`, leggibile con `GET /addons/<slug>/log`.
- Il codice d'uscita va in `uscita.txt`: diverso da 0 = stato `errore`.
- Esempio completo: [`addons/mqtt/broker.py`](../../addons/mqtt/broker.py).

## Storico del formato

- 2026-11-13: prima bozza, presa da `docs/progetto-handroid.md`.
- 2026-11-13: formato attuato nella M3 (`config.yaml` per add-on, `index.json` per repository, campi `pip`, `files`, `entry`, `boot`); sostituisce la bozza (`addon.yaml`, APK companion).
