# Dipendenze delle integrazioni dell'indice, Home Assistant 2026.11.1

Generato da `scripts/dipendenze-android.py 2026.11.1 scripts/integrazioni-indice.txt` (arm64, Python 3.14). Queste integrazioni non sono nell'APK: HA scarica le loro librerie con pip quando le aggiungi. I pacchetti nativi qui sotto vanno compilati e pubblicati nell'indice dei wheel.

Totale 157: 120 puri, 0 con wheel Android, **29 da compilare**, 8 solo sorgente.

## Da compilare con cibuildwheel

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| aioesphomeapi | 46.25.0 | integrazione esphome |
| av | 19.0.0 | integrazione stream |
| bleak-esphome | 4.0.0 | integrazione esphome |
| bluetooth-data-tools | 1.29.24 | integrazione bluetooth |
| cached-ipaddress | 1.1.2 | integrazione dhcp |
| cffi | 2.0.0 | pynacl |
| ciso8601 | 2.3.3 | hass-nabucasa |
| cryptography | 50.0.2 | androidtvremote2 |
| dbus-fast | 5.0.22 | integrazione bluetooth |
| fnv-hash-fast | 2.0.3 | integrazione recorder |
| gazetteer-matcher | 1.1.0 | integrazione conversation |
| greenlet | 3.5.6 | sqlalchemy |
| grpcio | 1.83.1 | hass-nabucasa |
| habluetooth | 7.1.2 | integrazione bluetooth |
| jiter | 0.19.0 | openai |
| lru-dict | 1.4.1 | bleak-esphome |
| lxml | 6.1.3 | soco |
| netifaces | 0.11.0 | python-miio |
| numpy | 2.3.2 | integrazione stream |
| orjson | 3.13.0 | matter-python-client |
| pillow | 13.0.0 | integrazione image_upload |
| psutil | 7.2.2 | psutil-home-assistant |
| pycares | 5.2.1 | aiodns |
| pydantic-core | 2.50.1 | pydantic |
| pymicro-vad | 1.0.1 | integrazione assist_pipeline |
| pynacl | 1.6.2 | integrazione mobile_app |
| pyyaml | 6.0.3 | python-miio |
| regex | 2026.11.9 | sentence-stream |
| zeroconf | 0.152.0 | integrazione zeroconf |

## Solo sorgente (da verificare)

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| android-backup | 0.2.0 | python-miio |
| chacha20poly1305 | 0.0.3 | aiohomekit |
| construct | 2.10.68 | integrazione xiaomi_miio |
| micloud | 0.5 | integrazione xiaomi_miio |
| pyric | 0.1.6.3 | bluetooth-auto-recovery |
| pyspeex-noise | 1.0.2 | integrazione assist_pipeline |
| pyturbojpeg | 1.8.3 | integrazione stream |
| samsungctl | 0.7.1 | integrazione samsungtv |

## Puri

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| acme | 5.7.0 | hass-nabucasa |
| aiocoap | 0.4.17 | aiohomekit |
| aiodhcpwatcher | 1.2.7 | integrazione dhcp |
| aiodiscover | 3.3.2 | integrazione dhcp |
| aiodns | 4.0.4 | aiodiscover |
| aiofiles | 25.1.0 | androidtvremote2 |
| aiohappyeyeballs | 2.7.1 | aiohomekit |
| aiohomekit | 4.0.1 | integrazione homekit_controller |
| aiohttp | 3.14.5 | aiowebostv |
| aiohue | 4.9.0 | integrazione hue |
| aiooui | 0.1.11 | bluetooth-adapters |
| aioshelly | 14.0.1 | integrazione shelly |
| aiosignal | 1.4.0 | aiohttp |
| aiousbwatcher | 1.1.2 | integrazione usb |
| aiowebostv | 1.0.1 | integrazione webostv |
| androidtvremote2 | 0.3.3 | integrazione androidtv_remote |
| annotated-types | 0.8.0 | pydantic |
| anyio | 4.15.1 | openai |
| appdirs | 1.4.4 | python-miio |
| async-interrupt | 1.2.2 | aiohomekit |
| async-timeout | 4.0.3 | ha-ffmpeg |
| async-upnp-client | 0.49.0 | integrazione samsungtv |
| asyncclick | 8.4.2.1 | python-kasa |
| asyncinotify | 4.4.4 | aiousbwatcher |
| asyncio-throttle | 1.0.2 | aiohue |
| atomicwrites-homeassistant | 1.4.1 | hass-nabucasa |
| attrs | 26.1.0 | python-miio |
| awesomeversion | 25.8.0 | wled |
| bleak | 3.0.2 | integrazione bluetooth |
| bleak-retry-connector | 4.7.1 | integrazione bluetooth |
| bluetooth-adapters | 2.4.0 | integrazione bluetooth |
| bluetooth-auto-recovery | 1.6.4 | integrazione bluetooth |
| boto3 | 1.42.97 | pycognito |
| botocore | 1.42.97 | boto3 |
| broadlink-async | 1.2.1 | integrazione broadlink |
| btsocket | 0.3.0 | bluetooth-auto-recovery |
| casttube | 0.2.1 | pychromecast |
| certifi | 2026.10.23 | requests |
| chacha20poly1305-reuseable | 0.13.2 | aiohomekit |
| charset-normalizer | 3.4.3 | requests |
| click | 8.5.1 | python-miio |
| croniter | 7.0.0 | python-miio |
| dacite | 1.9.2 | matter-python-client |
| defusedxml | 0.7.1 | integrazione sonos |
| envs | 1.4 | pycognito |
| esphome-dashboard-api | 1.4.0 | integrazione esphome |
| file-read-backwards | 2.0.0 | integrazione file |
| fnvhash | 0.2.1 | fnv-hash-fast |
| frozenlist | 1.8.0 | aiohttp |
| getmac | 0.9.6 | integrazione samsungtv |
| go2rtc-client | 0.4.0 | integrazione go2rtc |
| h11 | 0.16.0 | httpcore2 |
| ha-ffmpeg | 3.2.2 | integrazione ffmpeg |
| hass-nabucasa | 2.8.1 | integrazione cloud |
| hassil | 3.12.1 | integrazione conversation |
| hatasmota | 0.10.2 | integrazione tasmota |
| home-assistant-frontend | 20261028.5 | integrazione frontend |
| home-assistant-intents | 2026.10.27 | integrazione conversation |
| httpcore2 | 2.14.0 | httpx2 |
| httpx2 | 2.14.0 | openai |
| icmplib | 3.0.4 | hass-nabucasa |
| idna | 3.20 | requests |
| ifaddr | 0.2.0 | integrazione network |
| jmespath | 1.1.0 | boto3 |
| josepy | 2.3.0 | hass-nabucasa |
| mashumaro | 3.23 | wled |
| matter-ble-proxy | 0.7.1 | integrazione matter |
| matter-python-client | 1.5.5 | integrazione matter |
| multidict | 7.2.1 | aiohttp |
| mutagen | 1.48.1 | integrazione tts |
| noiseprotocol | 0.3.1 | aioesphomeapi |
| openai | 3.26.0 | integrazione cloud |
| paho-mqtt | 2.1.0 | integrazione mqtt |
| propcache | 0.5.4 | cached-ipaddress |
| protobuf | 7.36.0 | androidtvremote2 |
| psutil-home-assistant | 0.0.1 | integrazione recorder |
| pychromecast | 14.1.1 | integrazione cast |
| pycognito | 2024.5.1 | hass-nabucasa |
| pycparser | 3.12 | cffi |
| pydantic | 2.14.1 | openai |
| pyjwt | 2.15.1 | hass-nabucasa |
| pyopenssl | 26.4.0 | acme |
| pyrfc3339 | 2.1.1 | acme |
| pyroute2 | 0.9.7 | aiodiscover |
| python-backoff | 2.3.1 | wled |
| python-dateutil | 4.0.0 | croniter |
| python-didl-lite | 1.5.2 | async-upnp-client |
| python-kasa | 0.12.0 | integrazione tplink |
| python-miio | 0.5.12 | integrazione xiaomi_miio |
| pytz | 2026.6 | python-miio |
| requests | 2.34.2 | samsungtvws |
| s3transfer | 0.16.1 | boto3 |
| samsungtvws | 3.0.7 | integrazione samsungtv |
| scapy | 2.8.0 | aiodhcpwatcher |
| sentence-stream | 1.4.0 | hass-nabucasa |
| serialx | 1.15.1 | integrazione usb |
| sniffio | 1.3.1 | openai |
| snitun | 0.47.0 | hass-nabucasa |
| soco | 0.32.1 | integrazione sonos |
| sonos-websocket | 0.2.0 | integrazione sonos |
| sqlalchemy | 2.0.54 | integrazione recorder |
| tqdm | 4.70.1 | python-miio |
| truststore | 0.10.4 | httpx2 |
| tuya-device-handlers | 0.0.34 | integrazione tuya |
| tuya-device-sharing-sdk | 0.2.15 | integrazione tuya |
| typing-extensions | 4.16.0 | serialx |
| typing-inspection | 0.4.5 | pydantic |
| tzdata | 2026.6 | aioesphomeapi |
| tzlocal | 5.4.5 | aioesphomeapi |
| uart-devices | 0.1.1 | bluetooth-adapters |
| unicode-rbnf | 2.4.1 | gazetteer-matcher |
| urllib3 | 2.8.0 | requests |
| usb-devices | 0.5.1 | bluetooth-adapters |
| voluptuous | 0.16.0 | async-upnp-client |
| webrtc-models | 0.3.0 | go2rtc-client |
| websocket-client | 1.9.2 | samsungtvws |
| wled | 1.1.1 | integrazione wled |
| xmltodict | 1.0.4 | soco |
| yarl | 1.25.1 | samsungtvws |
| yeelight | 0.7.16 | integrazione yeelight |

