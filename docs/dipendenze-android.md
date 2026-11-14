# Dipendenze di Home Assistant 2026.11.1 su Android

Generato da `scripts/dipendenze-android.py` (arm64, Python 3.14). Dipendenze del core più i requisiti delle integrazioni in `scripts/integrazioni-base.txt`: sono incluse nell'APK, così HA parte anche senza rete.

Totale 121: 92 puri, 1 con wheel Android, **25 da compilare**, 3 solo sorgente.

## Da compilare con cibuildwheel

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| annotatedyaml | 1.0.2 | homeassistant |
| audioop-lts | 0.2.2 | homeassistant |
| bcrypt | 5.0.0 | homeassistant |
| bluetooth-data-tools | 1.29.24 | habluetooth |
| cffi | 2.0.0 | cryptography |
| ciso8601 | 2.3.3 | homeassistant |
| cryptography | 50.0.2 | homeassistant |
| fnv-hash-fast | 2.0.3 | homeassistant |
| gazetteer-matcher | 1.1.0 | integrazione conversation |
| greenlet | 3.5.6 | sqlalchemy |
| grpcio | 1.83.1 | hass-nabucasa |
| habluetooth | 7.1.2 | home-assistant-bluetooth |
| lru-dict | 1.4.1 | homeassistant |
| numpy | 2.3.2 | integrazione stream |
| orjson | 3.13.0 | homeassistant |
| pillow | 13.0.0 | homeassistant |
| psutil | 7.2.2 | psutil-home-assistant |
| pycares | 5.2.1 | radios |
| pymicro-vad | 1.0.1 | integrazione assist_pipeline |
| pynacl | 1.6.2 | securetar |
| pyyaml | 6.0.3 | homeassistant |
| regex | 2026.11.9 | sentence-stream |
| ulid-transform | 2.2.9 | homeassistant |
| uv | 0.13.6 | homeassistant |
| zeroconf | 0.152.0 | homeassistant |

## Solo sorgente (da verificare)

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| pyric | 0.1.6.3 | bluetooth-auto-recovery |
| pyspeex-noise | 1.0.2 | integrazione assist_pipeline |
| pyturbojpeg | 1.8.3 | integrazione camera |

## Wheel Android già pubblicato

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| markupsafe | 3.0.4 | jinja2 |

## Puri

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| acme | 5.7.0 | hass-nabucasa |
| aiodns | 4.0.4 | homeassistant |
| aiogithubapi | 26.0.0 | homeassistant |
| aiohappyeyeballs | 2.7.1 | aiohttp |
| aiohasupervisor | 0.8.1 | integrazione hassio |
| aiohttp | 3.14.5 | homeassistant |
| aiohttp-asyncmdnsresolver | 0.2.0 | homeassistant |
| aiohttp-cors | 0.8.1 | homeassistant |
| aiohttp-fast-zlib | 0.3.0 | homeassistant |
| aiooui | 0.1.11 | bluetooth-adapters |
| aiosignal | 1.4.0 | aiohttp |
| aiozoneinfo | 0.2.3 | homeassistant |
| anyio | 4.15.1 | httpx2 |
| astral | 2.2 | homeassistant |
| async-interrupt | 1.2.2 | homeassistant |
| async-timeout | 4.0.3 | pymetno |
| atomicwrites-homeassistant | 1.4.1 | homeassistant |
| attrs | 26.1.0 | homeassistant |
| awesomeversion | 25.8.0 | homeassistant |
| backoff | 2.2.1 | aiogithubapi |
| bleak | 3.0.2 | habluetooth |
| bleak-retry-connector | 4.7.1 | habluetooth |
| bluetooth-adapters | 2.4.0 | habluetooth |
| bluetooth-auto-recovery | 1.6.4 | habluetooth |
| boto3 | 1.42.97 | pycognito |
| botocore | 1.42.97 | boto3 |
| btsocket | 0.3.0 | habluetooth |
| certifi | 2026.10.23 | homeassistant |
| charset-normalizer | 3.4.3 | requests |
| click | 8.1.8 | gtts |
| cronsim | 2.7 | homeassistant |
| envs | 1.4 | pycognito |
| fnvhash | 0.2.1 | fnv-hash-fast |
| frozenlist | 1.8.0 | aiohttp |
| gtts | 2.5.4 | integrazione google_translate |
| h11 | 0.16.0 | httpcore2 |
| ha-ffmpeg | 3.2.2 | integrazione ffmpeg |
| hass-nabucasa | 2.8.1 | homeassistant |
| hassil | 3.12.1 | integrazione assist_satellite |
| home-assistant-bluetooth | 2.0.0 | homeassistant |
| home-assistant-frontend | 20261028.5 | integrazione frontend |
| home-assistant-intents | 2026.10.27 | integrazione conversation |
| httpcore2 | 2.14.0 | httpx2 |
| httpx2 | 2.14.0 | homeassistant |
| icmplib | 3.0.4 | hass-nabucasa |
| idna | 3.20 | httpx2 |
| ifaddr | 0.2.0 | homeassistant |
| infrared-protocols | 10.5.0 | integrazione infrared |
| jinja2 | 3.1.6 | homeassistant |
| jmespath | 1.1.0 | boto3 |
| josepy | 2.3.0 | hass-nabucasa |
| mashumaro | 3.23 | aiohasupervisor |
| multidict | 7.2.1 | aiohttp |
| mutagen | 1.48.1 | integrazione tts |
| packaging | 26.3 | homeassistant |
| probatio | 0.13.0 | homeassistant |
| propcache | 0.5.4 | homeassistant |
| psutil-home-assistant | 0.0.1 | homeassistant |
| pycognito | 2024.5.1 | hass-nabucasa |
| pycparser | 3.12 | cffi |
| pyjwt | 2.15.1 | homeassistant |
| pymetno | 0.13.0 | integrazione met |
| pyopenssl | 26.4.0 | homeassistant |
| pyrfc3339 | 2.1.1 | acme |
| pyserial | 3.5 | homeassistant |
| python-dateutil | 2.9.0.post0 | botocore |
| python-slugify | 9.1.3 | homeassistant |
| pytz | 2026.6 | astral |
| radios | 2.0.1 | integrazione radio_browser |
| requests | 2.34.2 | homeassistant |
| rf-protocols | 4.3.0 | integrazione radio_frequency |
| s3transfer | 0.16.1 | boto3 |
| securetar | 2026.10.0 | homeassistant |
| sentence-stream | 1.4.0 | hass-nabucasa |
| six | 1.17.0 | python-dateutil |
| snitun | 0.47.0 | hass-nabucasa |
| sqlalchemy | 2.0.54 | homeassistant |
| standard-aifc | 3.13.0 | homeassistant |
| standard-chunk | 3.13.0 | standard-aifc |
| standard-telnetlib | 3.13.0 | homeassistant |
| text-unidecode | 1.3 | python-slugify |
| truststore | 0.10.4 | homeassistant |
| typing-extensions | 4.16.0 | homeassistant |
| tzdata | 2026.6 | aiozoneinfo |
| uart-devices | 0.1.1 | bluetooth-adapters |
| unicode-rbnf | 2.4.1 | hassil |
| urllib3 | 2.8.0 | homeassistant |
| usb-devices | 0.5.1 | bluetooth-adapters |
| voluptuous | 0.16.0 | annotatedyaml |
| webrtc-models | 0.3.0 | homeassistant |
| xmltodict | 1.0.4 | pymetno |
| yarl | 1.25.1 | homeassistant |

