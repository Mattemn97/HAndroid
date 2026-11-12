# 0002 · Home Assistant Core dentro l'APK (M0b)

12 novembre 2026. Stato: Proposta. Applica D2, D3 e D8 di [0001](0001-architettura.md).

## Perimetro

- Dentro: HA Core `2026.11.1` incluso nell'APK con le dipendenze che hanno una wheel Android; wheel native mancanti compilate in un workflow separato; installazione dei requisiti delle integrazioni a runtime senza `execve`; diagnosi chiara se manca qualcosa.
- Fuori: add-on, bridge ESPHome/USB, ottimizzazione dei tempi di avvio, patch a HA.
- Fatto quando: l'APK installato prova `hass --runner`; se un import fallisce, la pagina di stato su `:8123` (e `/api/handroid`) mostra l'errore e i requisiti mancanti, e logcat li elenca.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| E1 | **Set di base preinstallato** in `assets/site.zip`, creato da `scripts/prepara-ha.sh` con `pip install --target --platform android_*_arm64_v8a --only-binary :all:`, un requisito alla volta. | Niente primo avvio lungo né rete obbligatoria; un requisito senza wheel non blocca gli altri. | Si estrae in `python/site` a ogni aggiornamento dell'APK; sta in `sys.path` dopo `site-packages`, così ciò che si installa a runtime vince. I requisiti saltati finiscono in `mancanti.txt`. |
| E2 | **pip in-process** al posto di `uv`: `handroid_main.py` sostituisce `homeassistant.util.package.install_package` con una funzione che chiama `pip._internal.cli.main` nello stesso processo, solo wheel, target `HANDROID_SITE`, sotto un lock. | HA lancia `python -m uv` come processo (vietato, D3); `uv` è un binario senza wheel Android. Il cambio sta fuori da HA (D2). | `pip` è incluso nel set di base. `uv` è escluso dai requisiti. Le API interne di pip possono cambiare: si fissa la versione inclusa nell'APK. |
| E3 | **Indice = release `wheels`** della repo: wheel come asset più un `index.html` letto da pip con `--find-links`. | Nessuna configurazione di GitHub Pages; stesso effetto di D8. | URL fisso `…/releases/download/wheels/index.html`, usato sia in build sia a runtime. |
| E4 | **`wheels.yml`** separato (a mano + lunedì notte), una job per pacchetto con cibuildwheel 4.x (`CIBW_PLATFORM=android`, `cp314`, `arm64_v8a`), test saltati. | cibuildwheel supporta Android dalla 3.1; le build lunghe non pesano su `release.yml`. | `release.yml` scarica soltanto, con cache settimanale di `app/build/ha` (resta sotto i 25 min). Il passo HA è `continue-on-error`: senza HA l'APK esce lo stesso. |

## Inventario delle dipendenze (HA 2026.11.1, cp314 Android arm64)

Controllato il 12/11/2026 con `prepara-ha.sh` su PyPI. Le dipendenze a cascata (es. `securetar` → `cryptography`) si sbloccano da sole quando c'è la wheel nativa.

| Stato | Pacchetti |
|---|---|
| Wheel Android su PyPI | aiohttp, multidict, markupsafe |
| Pure Python (o con fallback puro) | yarl, propcache, frozenlist, SQLAlchemy, ifaddr, webrtc-models, attrs, Jinja2, awesomeversion, astral, PyJWT, requests, urllib3, certifi, packaging, python-slugify, pyserial, truststore, typing-extensions, cronsim, atomicwrites-homeassistant, async-interrupt, aiozoneinfo, httpx2, standard-aifc, standard-telnetlib, aiohttp_cors, aiohttp-fast-zlib, aiogithubapi |
| Da compilare (C/Cython) | audioop-lts, ciso8601, fnv-hash-fast, lru-dict (solo cp313 su PyPI), ulid-transform, zeroconf, PyYAML, Pillow, psutil, pycares (per aiodns), greenlet, cffi, habluetooth (per home-assistant-bluetooth) |
| Da compilare (Rust) | cryptography (anche pyopenssl, securetar, hass-nabucasa), orjson, bcrypt |
| Non compilabile / escluso | uv (eseguibile, D3) |

## Da verificare

- Che cibuildwheel compili davvero i pacchetti Rust per Android (`CARGO_BUILD_TARGET`): non ancora provato.
- `wheels.yml` si può lanciare solo dopo che è arrivato su `master` (workflow_dispatch legge il branch predefinito).
- Sul telefono: tempo di estrazione di `site.zip` e del primo avvio, `dlopen` delle `.so` da `python/site`.
