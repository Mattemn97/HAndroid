# 0006 · Add-on (M3)

12 novembre 2026, portata nella M3 il 13 novembre. Stato: attuata e provata sul 12T. Vincoli: Android esegue solo codice che arriva dall'APK (niente binari scaricati), CPython non si reinizializza nello stesso processo, HA non si modifica.

## Perimetro

- Dentro: manifest YAML e indice dei repository; add-on Python (wheel pip + file del repository) in processi separati; infrastruttura per add-on nativi `lib<nome>.so`; API locale del gestore; integrazione `custom_components/handroid` per gestirli da HA; primo add-on reale (broker MQTT).
- Fuori: binari nativi veri (Mosquitto, Node, Matter), APK companion, pannello nella barra laterale (iframe/ingress), schermata nativa (la fa la M1 sopra `AddonManager`).
- Fatto quando: da HA si installa, avvia, ferma e rimuove un add-on senza riavviare HA.

## Formato

Repository = una cartella HTTPS con `index.json` (`{"name": "...", "addons": ["mqtt", ...]}`) e una sottocartella per add-on con `config.yaml`. Ufficiale: `https://mattemn97.github.io/HAndroid/addons` (la cartella `addons/` del repo, pubblicata da `scripts/pubblica-indice.sh`); altri si aggiungono a runtime.

```yaml
name: Broker MQTT
version: 0.1.0
type: python              # python | native
ports: [1883]
permissions: [network]    # network | bluetooth | usb | storage
boot: auto                # auto | manual
pip: []                   # requisiti pip, solo wheel (anche dall'indice dei wheel Android)
files: [broker.py]        # scaricati accanto al manifest
entry: broker.py          # python: file .py o modulo; native: <nome> di lib<nome>.so
args: []                  # solo native
options: {port: 1883}
schema: {port: port}      # bool int float str password email url port list(a|b) int(min,max); "?" = facoltativo
```

Il parser YAML è un sottoinsieme scritto a mano (`MiniYaml`): mappe per indentazione, liste `[a, b]` e `- a`, scalari, commenti. Niente ancore né stringhe su più righe.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| A1 | **Un processo Android per add-on**, da un pool fisso dichiarato nel manifest: `AddonService1..3` con `android:process=":addonN"`, che caricano libpython con lo stesso `PythonRunner.runModule` di `:ha` (`handroid_addon.py`, cartella dell'add-on come argomento). | Un processo per add-on lo isola da HA; CPython non si reinizializza nello stesso processo. I subinterpreter (PEP 734) condividono il processo di HA: un crash o una wheel nativa non isolata li porta giù entrambi, e molte estensioni C non li supportano. Un task asyncio dentro HA ha lo stesso problema e in più blocca il loop. | Fermare = chiudere il processo. Gli add-on sopravvivono a un riavvio di HA. Al massimo 3 add-on attivi: per averne di più si aggiunge un servizio. |
| A2 | **Requisiti pip installati dall'add-on stesso** al primo avvio, in `addons/<slug>/site`, con pip nello stesso processo. | Kotlin non ha un interprete; il processo dell'add-on sì. | Isolati da HA (niente conflitti di versioni); il primo avvio è più lento. Si reinstallano quando cambia versione o elenco. |
| A3 | **Gestore nel processo `:ha`** (`AddonManager`, unico scrittore di `addons/stato.json`), con **API HTTP** su `127.0.0.1:8099` e token in `addons/token`. | Il token è leggibile solo dall'uid dell'app, cioè da HA; nessun'altra app sul telefono può comandare gli add-on. | L'aggancio è una riga in `HaService`. Le schermate native di altri processi usano l'API HTTP; quelle in `:ha` chiamano `AddonManager` direttamente. |
| A4 | **Pannello = integrazione custom** `handroid`, copiata da `handroid_boot.py` in `ha-config/custom_components` a ogni avvio: un dispositivo per add-on con sensore di stato e interruttore, servizi `install/start/stop/uninstall/set_options/repository/refresh`, options flow generato dallo schema. | Esperienza "come HA OS" senza modificare HA. | La si aggiunge una volta da Impostazioni → Integrazioni. Niente pagina nella barra laterale per ora. |
| A5 | **Add-on nativi**: `ProcessBuilder` su `nativeLibraryDir/lib<entry>.so`; l'installazione rifiuta l'add-on se il binario non è nell'APK. | È l'unico percorso eseguibile senza root: Android esegue solo codice installato con l'APK. | Nessun binario incluso finora; un modulo nuovo = una `.so` in jniLibs e una nuova release. |
| A6 | **Primo add-on: broker MQTT 3.1.1 nostro, in puro Python** (`addons/mqtt/broker.py`). | `amqtt` 0.12 richiede psutil, argon2-cffi, PyYAML e websockets, senza wheel Android; `mqttools` è solo sdist e solo MQTT 5. | QoS 0/1/2 in ingresso (consegna fino a QoS 1), retained, will, keepalive, utente/password. Niente sessioni persistenti né TLS. |

## Stato e sicurezza

- `addons/stato.json`: installati (manifest, repository, opzioni, avvio automatico), slot del pool, repository. Scrittura atomica (tmp + rename).
- `addons/<slug>/`: `manifest.json`, `opzioni.json`, file scaricati, `site/`, `log.txt`, `uscita.txt` (codice dell'ultima uscita).
- Stati esposti: `non_installato`, `fermo`, `avviato`, `errore` (uscita ≠ 0). "In esecuzione" = esiste il processo `it.handroid:addonN`.
- Solo repository `https`; slug `[a-z0-9_]`, nomi file senza `..`; due add-on con la stessa porta non girano insieme.
- Il codice di un add-on gira con i permessi dell'app (come in HA OS gira con quelli del container): si installano solo repository di cui ci si fida.

## Da verificare

- Il consumo di RAM per processo nel tempo, e che MIUI non chiuda i processi `:addonN` (sono servizi in primo piano).

## Provato sul 12T (13 novembre)

- API su `127.0.0.1:8099`: catalogo dal repository ufficiale, installazione e avvio del broker MQTT in `:addon1` (circa 120 MB di RAM).
- Broker: pubblicazione e iscrizione QoS 0/1 e messaggi retained, da un client sul PC.
- Integrazione "Add-on HAndroid" aggiunta in HA: sensore di stato e interruttore; spento e riacceso da HA, il processo si ferma e riparte.
- Integrazione MQTT di HA collegata a `127.0.0.1:1883` (protocollo 3.1.1): un sensore pubblicato con il discovery MQTT compare in HA con il suo valore.
