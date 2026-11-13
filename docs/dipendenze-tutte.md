# Dipendenze delle integrazioni dell'indice, Home Assistant 2026.11.1

Generato da `scripts/dipendenze-android.py 2026.11.1 tutte` (arm64, Python 3.14). Queste integrazioni non sono nell'APK: HA scarica le loro librerie con pip quando le aggiungi. I pacchetti nativi qui sotto vanno compilati e pubblicati nell'indice dei wheel.

Totale 1620: 1429 puri, 2 con wheel Android, **59 da compilare**, 130 solo sorgente.

## Da compilare con cibuildwheel

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| aioesphomeapi | 46.25.0 | integrazione esphome |
| aiokafka | 0.10.0 | integrazione apache_kafka |
| audioop-lts | 0.2.2 | nextcord |
| av | 19.0.0 | integrazione stream |
| awscrt | 0.36.1 | awsiotsdk |
| bcrypt | 5.0.0 | solarlog-cli |
| bitarray | 3.12.3 | airtouch5py |
| bitstruct | 8.24.0 | python-otbr-api |
| bleak-esphome | 4.0.0 | integrazione esphome |
| bleak-smlight | 1.1.0 | integrazione smlight |
| bluetooth-data-tools | 1.29.24 | integrazione bluetooth |
| cached-ipaddress | 1.1.2 | integrazione dhcp |
| cbor2 | 6.2.1 | airthings-ble |
| cffi | 2.1.1 | pynacl |
| ciso8601 | 2.4.1 | yalexs |
| convertertools | 1.1.0 | uiprotect |
| cryptography | 50.0.2 | yalexs-ble |
| dbus-fast | 5.0.22 | integrazione bluetooth |
| deebot-client | 18.7.0 | integrazione ecovacs |
| epaper-dithering | 6.1.1 | py-opendisplay |
| ephem | 4.1.6 | integrazione season |
| faust-cchardet | 3.2.0 | pytouchline-extended |
| fnv-hash-fast | 2.0.3 | integrazione recorder |
| gazetteer-matcher | 1.1.0 | integrazione conversation |
| greenlet | 3.5.6 | sqlalchemy |
| grpcio | 1.85.1 | hass-nabucasa |
| guppy3 | 3.1.7 | integrazione profiler |
| habluetooth | 7.1.2 | integrazione bluetooth |
| ijson | 3.6.0 | azure-kusto-data |
| isal | 1.8.0 | integrazione isal |
| jiter | 0.19.0 | openai |
| kasa-crypt | 1.1.0 | sense-energy |
| lru-dict | 1.4.1 | yalexs-ble |
| lxml | 6.1.3 | integrazione scrape |
| miniaudio | 1.73 | pyatv |
| msgpack | 1.2.4 | sense-energy |
| netifaces | 0.11.0 | python-miio |
| numpy | 2.3.2 | integrazione trend |
| orjson | 3.13.0 | pyzonneplan |
| ormsgpack | 1.13.1 | fish-audio-sdk |
| pandas | 2.3.3 | noaa-coops |
| pillow | 13.0.0 | integrazione sighthound |
| psutil | 7.2.2 | integrazione systemmonitor |
| pycares | 5.2.1 | radios |
| pycrc16 | 0.1.2 | pyoppleio-legacy |
| pycryptodome | 4.4.0 | aiovodafone |
| pycryptodomex | 3.24.2 | xiaomi-ble |
| pydantic-core | 2.50.4 | elevenlabs |
| pymicro-vad | 1.0.1 | integrazione assist_pipeline |
| pynacl | 1.6.2 | integrazione owntracks |
| pyyaml | 6.0.3 | python-miio |
| regex | 2026.11.9 | sentence-stream |
| rpds-py | 2026.9.1 | jsonschema |
| scipy | 1.18.1 | reverse-geocode |
| slixmpp | 1.17.0 | integrazione xmpp |
| tibs | 2.0.2 | bitstring |
| ujson | 6.1.0 | ayla-iot-unofficial |
| watchdog | 6.0.0 | integrazione folder_watcher |
| zeroconf | 0.152.0 | integrazione zeroconf |

## Solo sorgente (da verificare)

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| adb-shell | 0.4.4 | integrazione androidtv |
| android-backup | 0.2.0 | python-miio |
| androidtv | 0.0.75 | integrazione androidtv |
| anel-pwrctrl-homeassistant | 0.0.1.dev2 | integrazione anel_pwrctrl |
| anthemav | 1.4.2 | integrazione anthemav |
| aqualogic | 2.6 | integrazione aqualogic |
| aranet4 | 2.6.0 | integrazione aranet |
| auroranoaa | 0.0.5 | integrazione aurora |
| baidu-aip | 1.6.6.0 | integrazione baidu |
| batinfo | 0.4.2 | integrazione linux_battery |
| bizkaibus | 0.1.1 | integrazione bizkaibus |
| blockchain | 1.4.4 | integrazione bitcoin |
| chacha20poly1305 | 0.0.3 | aiohomekit |
| chirpstack-api | 4.19.0 | integrazione chirpstack |
| clearpasspy | 1.0.2 | integrazione cppm_tracker |
| connect-box | 0.3.1 | integrazione upc_connect |
| connio | 0.2.0 | async-modbus |
| construct | 2.10.68 | integrazione xiaomi_miio |
| crcmod | 1.7 | pymysensors |
| demjson3 | 3.0.6 | pysyncthru |
| docopt | 0.6.2 | thingspeak |
| dynalite-devices | 0.1.47 | integrazione dynalite |
| ebusdpy | 0.0.17 | integrazione ebusd |
| elvia | 0.1.0 | integrazione elvia |
| epicstore-api | 0.1.9 | integrazione epic_games_store |
| epion | 0.0.3 | integrazione epion |
| esprima | 4.0.1 | quantum-gateway |
| evdev | 1.9.3 | integrazione keyboard_remote |
| events | 0.4 | pysnooz |
| fitbit | 0.3.1 | integrazione fitbit |
| foobot-async | 1.0.1 | integrazione foobot |
| hikvision | 0.4 | integrazione hikvisioncam |
| http-ece | 1.2.1 | pywebpush |
| huawei-lte-api | 1.11.0 | integrazione huawei_lte |
| imutils | 0.5.4 | dremel3dpy |
| iperf3 | 0.1.11 | integrazione iperf3 |
| jsonrpc-async | 2.1.3 | pykodi |
| jsonrpc-base | 2.2.0 | jsonrpc-async |
| jsonrpc-websocket | 4.1.0 | pykodi |
| krakenex | 2.2.2 | integrazione kraken |
| limitlessled | 1.1.3 | integrazione limitlessled |
| locationsharinglib | 5.0.1 | integrazione google_maps |
| lw12 | 0.9.2 | integrazione lw12wifi |
| messagebird | 1.2.1 | integrazione message_bird |
| micloud | 0.5 | integrazione xiaomi_miio |
| microbeespy | 0.3.5 | integrazione microbees |
| motionblindsble | 0.1.4 | integrazione motionblinds_ble |
| ms-cv | 0.1.1 | python-xbox |
| ndms2-client | 0.1.2 | integrazione keenetic_ndms2 |
| neurio | 0.3.1 | integrazione neurio_energy |
| nsw-fuel-api-client | 1.1.0 | integrazione nsw_fuel_station |
| omnilogic | 0.4.9 | integrazione omnilogic |
| openwrt-ubus-rpc | 0.0.3 | integrazione ubus |
| orvibo | 1.1.2 | integrazione orvibo |
| pescea | 1.0.12 | integrazione escea |
| pmsensor | 0.4 | integrazione serial_pm |
| proliphix | 0.4.1 | integrazione proliphix |
| psychrolib | 2.5.0 | pyweatherflowudp |
| pure-python-adb | 0.3.0.dev0 | androidtv |
| py-nightscout | 1.2.2 | integrazione nightscout |
| py-schluter | 0.1.7 | integrazione schluter |
| pyads | 3.4.0 | integrazione ads |
| pyaes | 1.6.1 | crownstone-core |
| pyatome | 0.1.2 | integrazione atome |
| pybbox | 0.0.5-alpha | integrazione bbox |
| pycec | 0.5.2 | integrazione hdmi_cec |
| pycmus | 0.1.1 | integrazione cmus |
| pyebox | 1.1.4 | integrazione ebox |
| pyemby | 1.10 | integrazione emby |
| pyfttt | 0.3 | integrazione ifttt |
| pygatt | 4.0.5 | integrazione skybeacon |
| pyhomematic | 0.1.78 | integrazione homematic |
| pyiss | 1.0.1 | integrazione iss |
| pyitachip2ir2 | 0.0.8 | integrazione itach |
| pykmtronic | 0.3.0 | integrazione kmtronic |
| pykrakenapi | 0.1.9 | integrazione kraken |
| pykulersky | 0.5.8 | integrazione kulersky |
| pylacrosse | 0.4 | integrazione lacrosse |
| pymailgunner | 1.4 | integrazione mailgun |
| pymediaroom | 0.6.5.4 | integrazione mediaroom |
| pymochad | 0.2.0 | integrazione mochad |
| pynetio | 0.1.9.1 | integrazione netio |
| pynzbgetapi | 0.2.0 | integrazione nzbget |
| pyprof2calltree | 1.4.5 | integrazione profiler |
| pyprosegur | 0.0.14 | integrazione prosegur |
| pyqrcode | 1.2.1 | integrazione homekit |
| pyqwikswitch | 0.93 | integrazione qwikswitch |
| pyric | 0.1.6.3 | bluetooth-auto-recovery |
| pysoma | 0.0.12 | integrazione soma |
| pyspeex-noise | 1.0.2 | integrazione assist_pipeline |
| pysyncthru | 0.8.0 | integrazione syncthru |
| pythinkingcleaner | 0.0.3 | integrazione thinkingcleaner |
| python-blockchain-api | 0.0.2 | integrazione blockchain |
| python-digitalocean | 1.13.2 | integrazione digital_ocean |
| python-family-hub-local | 0.0.2 | integrazione familyhub |
| python-gitlab | 1.6.0 | integrazione gitlab_ci |
| python-opendata-transport | 0.5.0 | integrazione swiss_public_transport |
| python-ripple-api | 0.0.3 | integrazione ripple |
| pythonegardia | 1.0.52 | integrazione egardia |
| pyturbojpeg | 1.8.3 | integrazione stream |
| pyversasense | 0.0.6 | integrazione versasense |
| pyzerproc | 0.4.8 | integrazione zerproc |
| radiotherm | 2.1.0 | integrazione radiotherm |
| ratelimit | 2.2.1 | pyflume |
| ritassist | 0.9.2 | integrazione fleetgo |
| rova | 0.4.1 | integrazione rova |
| samsungctl | 0.7.1 | integrazione samsungtv |
| sgmllib3k | 1.0.0 | feedparser |
| sharp-aquos-rc | 0.3.2 | integrazione aquostv |
| shodan | 1.28.0 | integrazione shodan |
| snapcast | 2.3.8 | integrazione snapcast |
| starline | 0.1.5 | integrazione starline |
| streamlabswater | 1.0.1 | integrazione streamlabswater |
| tailer | 0.4.1 | dsmr-parser |
| tapsaff | 0.2.1 | integrazione tapsaff |
| tellcore-net | 0.4 | integrazione tellstick |
| tellcore-py | 1.1.3 | integrazione tellstick |
| tellduslive | 0.10.12 | integrazione tellduslive |
| temescal | 0.5 | integrazione lg_soundbar |
| temperusb | 1.6.1 | integrazione temper |
| tesla-powerwall | 0.5.3 | integrazione powerwall |
| titlecase | 2.4.1 | aioridwell |
| twilio | 6.32.0 | integrazione twilio |
| twitterapi | 2.7.12 | integrazione twitter |
| vincenty | 0.1.4 | buienradar |
| volkszaehler | 0.4.0 | integrazione volkszaehler |
| vsure | 2.10.0 | integrazione verisure |
| webio-api | 0.1.12 | integrazione nasweb |
| wsdiscovery | 2.1.2 | integrazione onvif |
| yeelightsunflower | 0.0.10 | integrazione yeelightsunflower |

## Wheel Android già pubblicato

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| curl-cffi | 0.17.0 | pywaze |
| markupsafe | 3.0.4 | flask |

## Puri

| Pacchetto | Versione | Richiesto da |
| --- | --- | --- |
| accuweather | 5.1.0 | integrazione accuweather |
| acme | 5.7.0 | hass-nabucasa |
| actron-neo-api | 0.5.16 | integrazione actron_air |
| adax | 0.4.0 | integrazione adax |
| adax-local | 0.3.0 | integrazione adax |
| adext | 0.4.7 | integrazione alarmdecoder |
| adguardhome | 1.0.0 | integrazione adguard |
| advantage-air | 0.4.4 | integrazione advantage_air |
| aemet-opendata | 0.6.4 | integrazione aemet |
| aenum | 3.1.17 | wallbox |
| aerosmart-modbus | 0.3.2 | integrazione aerosmart |
| afsapi | 1.0.2 | integrazione frontier_silicon |
| agent-py | 0.0.24 | integrazione agent_dvr |
| aio-geojson-client | 2026.6.0 | aio-geojson-usgs-earthquakes |
| aio-geojson-generic-client | 0.5 | integrazione geo_json_events |
| aio-geojson-geonetnz-quakes | 0.16 | integrazione geonetnz_quakes |
| aio-geojson-geonetnz-volcano | 0.9 | integrazione geonetnz_volcano |
| aio-geojson-nsw-rfs-incidents | 0.7 | integrazione nsw_rural_fire_service_feed |
| aio-geojson-usgs-earthquakes | 0.3 | integrazione usgs_earthquakes_feed |
| aio-georss-client | 2026.6.0 | aio-georss-gdacs |
| aio-georss-gdacs | 0.10 | integrazione gdacs |
| aio-ownet | 0.1.0 | integrazione onewire |
| aio-wattwaechter | 1.3.0 | integrazione wattwaechter |
| aioabrp | 0.6.0 | integrazione abrp |
| aioacaia | 0.2.2 | integrazione acaia |
| aioairq | 0.4.8 | integrazione airq |
| aioairzone | 1.0.5 | integrazione airzone |
| aioairzone-cloud | 0.7.2 | integrazione airzone_cloud |
| aioamazondevices | 17.2.7 | integrazione alexa_devices |
| aioambient | 2024.8.0 | integrazione ambient_station |
| aioanylist | 1.0.2 | integrazione any_list |
| aioapcaccess | 1.0.0 | integrazione apcupsd |
| aioaquacell | 1.0.0 | integrazione aquacell |
| aioaquarite | 0.13.1 | integrazione vistapool |
| aioaseko | 1.0.0 | integrazione aseko_pool_live |
| aioasuswrt | 1.5.4 | integrazione asuswrt |
| aioautomower | 3.1.0 | integrazione husqvarna_automower |
| aioaxlevpp | 0.2.0 | integrazione axle_energy |
| aioazuredevops | 2.2.2 | integrazione azure_devops |
| aiobafi6 | 0.9.0 | integrazione baf |
| aiobotocore | 3.7.0 | integrazione idrive_e2 |
| aiocentriconnect | 0.2.3 | integrazione centriconnect |
| aiocoap | 0.4.17 | aiohomekit |
| aiocomelit | 3.0.0 | integrazione comelit |
| aioconsole | 0.9.0 | melnor-bluetooth |
| aiodhcpwatcher | 1.2.7 | integrazione dhcp |
| aiodiscover | 3.3.2 | integrazione dhcp |
| aiodns | 4.0.4 | integrazione dnsip |
| aioeafm | 0.1.2 | integrazione eafm |
| aioeagle | 1.1.1 | integrazione rainforest_eagle |
| aioecosmart | 0.1.0 | integrazione ecosmart |
| aioecowitt | 2026.6.0 | integrazione ecowitt |
| aioelectricitymaps | 1.1.1 | integrazione co2signal |
| aioemonitor | 1.0.5 | integrazione emonitor |
| aioengiebelgium | 0.8.2 | integrazione engie_be |
| aiofarmad | 0.6.5 | integrazione mijn_farmad_apotheek |
| aiofiles | 25.1.0 | integrazione slack |
| aioflo | 2026.9.4 | integrazione flo |
| aioftp | 0.21.3 | integrazione yi |
| aioghost | 0.4.16 | integrazione ghost |
| aiogithubapi | 26.0.0 | integrazione marketplace |
| aioguardian | 2026.1.1 | integrazione guardian |
| aiohappyeyeballs | 2.7.1 | aiohomekit |
| aioharmanluxury | 0.2.3 | integrazione harman_luxury |
| aioharmony | 1.0.10 | integrazione harmony |
| aiohasupervisor | 0.8.1 | integrazione hassio |
| aiohomeconnect | 0.39.1 | integrazione home_connect |
| aiohomekit | 4.0.1 | integrazione homekit_controller |
| aiohortos | 0.3.1 | integrazione hortimax |
| aiohpprinter | 3.1.0 | integrazione hp_printer |
| aiohttp | 3.14.5 | zwave-js-server-python |
| aiohttp-retry | 2.9.1 | weheat |
| aiohttp-socks | 0.12.0 | matrix-nio |
| aiohttp-sse | 2.2.0 | integrazione mcp_server |
| aiohttp-sse-client | 0.2.1 | iometer |
| aiohttp-sse-client2 | 0.3.0 | pysmlight |
| aiohue | 4.9.0 | integrazione hue |
| aioimaplib | 2.0.3 | integrazione imap |
| aioimmich | 0.18.0 | integrazione immich |
| aioindiallsky | 0.1.7 | integrazione indi_allsky |
| aioipp | 0.20.0 | integrazione ipp |
| aioitertools | 0.13.0 | aiobotocore |
| aiokarakeep | 0.3.0 | integrazione karakeep |
| aiokef | 0.2.16 | integrazione kef |
| aiokem | 1.0.1 | integrazione rehlko |
| aiolibrenms | 0.0.4 | integrazione librenms |
| aiolibresync | 0.5.1 | integrazione libresync |
| aiolichess | 1.3.0 | integrazione lichess |
| aiolimiter | 1.3.0 | tesla-fleet-api |
| aiolookin | 1.0.0 | integrazione lookin |
| aiolyric | 2.2.1 | integrazione lyric |
| aiomealie | 2.1.3 | integrazione mealie |
| aiomelcloudhome | 0.3.1 | integrazione melcloud_home |
| aiomodernforms | 0.3.1 | integrazione modern_forms |
| aiomqtt | 2.5.1 | yoto-api |
| aiomusiccast | 0.15.0 | integrazione yamaha_musiccast |
| aionanoleaf2 | 1.2.1 | integrazione nanoleaf |
| aionatureremo | 0.4.0 | integrazione nature_remo |
| aionotion | 2024.3.0 | integrazione notion |
| aiontfy | 0.8.6 | integrazione ntfy |
| aionut | 4.3.4 | integrazione nut |
| aioonkyo | 0.4.0 | integrazione onkyo |
| aioopenexchangerates | 0.6.8 | integrazione openexchangerates |
| aiooui | 0.1.9 | integrazione nmap_tracker |
| aiopegelonline | 0.1.1 | integrazione pegel_online |
| aiopnsense | 1.0.10 | integrazione opnsense |
| aiopowerwall | 0.10.0 | integrazione teslemetry |
| aioptdevices | 2026.3.2 | integrazione ptdevices |
| aiopulse | 0.4.7 | integrazione acmeda |
| aiopurpleair | 2025.8.1 | integrazione purpleair |
| aiopvapi | 3.4.1 | integrazione hunterdouglas_powerview |
| aiopyarr | 23.4.0 | integrazione sonarr |
| aioqsw | 0.4.3 | integrazione qnap_qsw |
| aioraven | 0.7.1 | integrazione rainforest_raven |
| aiorecollect | 2023.9.0 | integrazione recollect_waste |
| aioridwell | 2025.9.0 | integrazione ridwell |
| aiortm | 0.20.1 | integrazione remember_the_milk |
| aiortsp | 1.4.0 | reolink-aio |
| aioruckus | 0.46.3 | integrazione ruckus_unleashed |
| aiorussound | 5.0.2 | integrazione russound_rnet |
| aioruuvigateway | 0.1.0 | integrazione ruuvi_gateway |
| aiosaj | 0.2.2 | integrazione saj |
| aioseafile | 0.1.1 | integrazione seafile |
| aioserial | 1.3.0 | phone-modem |
| aioshelly | 14.0.1 | integrazione shelly |
| aiosignal | 1.4.0 | aiohttp |
| aioskybell | 23.12.0 | integrazione skybell |
| aioslimproto | 3.3.0 | integrazione slimproto |
| aiosmtpd | 1.4.6 | jaraco-email |
| aiosmtplib | 5.1.3 | integrazione smtp |
| aiosolaredge | 1.0.2 | integrazione solaredge |
| aiosomecomfort | 0.0.38 | integrazione honeywell |
| aiospinel | 1.3.0 | universal-silabs-flasher |
| aiosqlite | 0.22.1 | zigpy |
| aiosteamist | 1.0.1 | integrazione steamist |
| aiostreammagic | 2.13.2 | integrazione cambridge_audio |
| aioswitcher | 6.5.0 | integrazione switcher_kis |
| aiosyncthing | 0.7.1 | integrazione syncthing |
| aiotankerkoenig | 0.5.5 | integrazione tankerkoenig |
| aiotedee | 0.3.0 | integrazione tedee |
| aiothreema | 0.2.0 | integrazione threema |
| aiotractive | 2.0.0 | integrazione tractive |
| aiounifi | 98 | integrazione unifi |
| aiousbwatcher | 1.1.2 | integrazione usb |
| aiovitesy | 1.4.8 | integrazione vitesy |
| aiovlc | 0.8.0 | integrazione vlc_telnet |
| aiovodafone | 3.3.11 | integrazione vodafone_station |
| aiowaqi | 3.1.0 | integrazione waqi |
| aiowatttime | 2024.6.0 | integrazione watttime |
| aiowebdav2 | 0.6.2 | integrazione webdav |
| aiowebostv | 1.0.1 | integrazione webostv |
| aiowithings | 3.1.6 | integrazione withings |
| aioymaps | 1.2.5 | integrazione yandex_transport |
| aiozoneinfo | 0.2.3 | pytrafikverket |
| airgradient | 0.11.0 | integrazione airgradient |
| airly | 1.1.0 | integrazione airly |
| airos | 0.6.13 | integrazione airos |
| airpatrol | 0.1.0 | integrazione airpatrol |
| airthings-ble | 1.3.1 | integrazione airthings_ble |
| airthings-cloud | 0.3.1 | integrazione airthings |
| airtouch4pyapi | 1.0.5 | integrazione airtouch4 |
| airtouch5py | 0.5.1 | integrazione airtouch5 |
| alarmdecoder | 1.13.14 | adext |
| alpha-vantage | 2.3.1 | integrazione alpha_vantage |
| altruistclient | 0.1.1 | integrazione altruist |
| amberelectric | 2.0.12 | integrazione amberelectric |
| amcrest | 1.9.9 | integrazione amcrest |
| androidtvremote2 | 0.3.3 | integrazione androidtv_remote |
| annotated-doc | 0.0.6 | typer |
| annotated-types | 0.8.0 | todoist-api-python |
| anova-wifi | 1.0.1 | integrazione anova |
| anthropic | 2.3.0 | integrazione anthropic |
| anyascii | 0.3.3 | music-assistant-models |
| anyio | 4.15.1 | integrazione mcp_server |
| apischema | 0.19.0 | aioaseko |
| appdirs | 1.4.4 | python-miio |
| apple-weatherkit | 1.1.3 | integrazione weatherkit |
| apprise | 2.1.0 | integrazione apprise |
| aprslib | 0.7.2 | integrazione aprs |
| apsystems-ez1 | 2.7.0 | integrazione apsystems |
| aqipy-atmotech | 0.1.5 | nettigo-air-monitor |
| arcam-fmj | 3.1.1 | integrazione arcam_fmj |
| argcomplete | 3.7.2 | amcrest |
| arris-tg2492lg | 2.2.0 | integrazione arris_tg2492lg |
| arrow | 1.4.0 | opower |
| arwn-client | 0.2.1 | integrazione arwn |
| asn1crypto | 1.5.1 | dlms-cosem |
| asusrouter | 1.21.3 | integrazione asuswrt |
| async-interrupt | 1.2.2 | yalexs-ble |
| async-modbus | 0.2.3 | nibe |
| async-timeout | 4.0.3 | zamg |
| async-upnp-client | 0.49.0 | integrazione yeelight |
| asyncarve | 0.1.1 | integrazione arve |
| asyncclick | 8.4.2.1 | python-kasa |
| asyncinotify | 4.4.4 | integrazione keyboard_remote |
| asyncio-dgram | 3.0.0 | mcstatus |
| asyncio-throttle | 1.0.2 | aiohue |
| asyncpysupla | 0.0.5 | integrazione supla |
| asyncsleepiq | 1.7.2 | integrazione sleepiq |
| asyncssh | 2.24.0 | integrazione sftp_storage |
| asyncstdlib | 3.14.0 | denonavr |
| asynctest | 0.13.0 | pyopenuv |
| atomicwrites-homeassistant | 1.4.1 | hass-nabucasa |
| atpublic | 9.0.0 | aiosmtpd |
| attrs | 26.1.0 | zhong-hong-hvac |
| aurorapy | 0.2.7 | integrazione aurora_abb_powerone |
| autarco | 3.2.0 | integrazione autarco |
| auth0-python | 6.10.0 | sharkiq |
| authlib | 1.9.5 | pyvicare |
| autocommand | 2.2.2 | oathtool |
| automower-ble | 0.2.9 | integrazione husqvarna_automower_ble |
| autoskope-client | 1.4.1 | integrazione autoskope |
| avea | 1.8.0 | integrazione avea |
| awesomeversion | 25.8.0 | wled |
| aws-request-signer | 1.2.0 | aioaquacell |
| awsiotsdk | 1.31.0 | thinqconnect |
| axis | 74 | integrazione axis |
| ayla-iot-unofficial | 1.5.2 | integrazione fujitsu_fglair |
| azure-core | 1.41.0 | azure-storage-blob |
| azure-eventhub | 5.11.1 | integrazione azure_event_hub |
| azure-identity | 1.27.0 | azure-kusto-data |
| azure-kusto-data | 4.5.1 | integrazione azure_data_explorer |
| azure-kusto-ingest | 4.5.1 | integrazione azure_data_explorer |
| azure-servicebus | 7.15.0 | integrazione azure_service_bus |
| azure-storage-blob | 12.24.0 | integrazione azure_storage |
| azure-storage-queue | 12.18.1 | azure-kusto-ingest |
| b2sdk | 2.10.4 | integrazione backblaze_b2 |
| babel | 2.18.0 | integrazione holiday |
| backoff | 2.2.1 | gotailwind |
| base36 | 0.1.1 | integrazione homekit |
| beautifulsoup4 | 4.15.0 | integrazione scrape |
| bellows | 1.2.1 | zha |
| besen | 0.6.1 | integrazione besen |
| bidict | 0.24.1 | python-socketio |
| bitstring | 5.0.0 | pysml |
| bitvis-protobuf | 2.0.5 | integrazione bitvis |
| bleach | 6.4.0 | fints |
| bleak | 3.0.2 | integrazione bluetooth |
| bleak-retry-connector | 4.7.1 | integrazione bluetooth |
| blebox-uniapi | 2.5.9 | integrazione blebox |
| blinker | 1.9.0 | flask |
| blinkpy | 0.25.9 | integrazione blink |
| bluecurrent-api | 1.3.3 | integrazione blue_current |
| bluemaestro-ble | 0.4.1 | integrazione bluemaestro |
| bluetooth-adapters | 2.4.0 | integrazione bluetooth |
| bluetooth-auto-recovery | 1.6.4 | integrazione bluetooth |
| bluetooth-sensor-state-data | 1.9.2 | xiaomi-ble |
| bluetti-modbus | 0.52.0 | integrazione bluetti_modbus |
| bond-async | 0.2.1 | integrazione bond |
| bosch-alarm-mode2 | 0.4.11 | integrazione bosch_alarm |
| bosch-shc-camera-client | 0.6.2 | integrazione bosch_smart_camera |
| boschshcpy | 0.6.25 | integrazione bosch_shc |
| boto3 | 1.42.97 | integrazione route53 |
| botocore | 1.42.97 | boto3 |
| bring-api | 1.1.2 | integrazione bring |
| broadlink-async | 1.2.1 | integrazione broadlink |
| brother | 6.2.0 | integrazione brother |
| brottsplatskartan | 1.0.5 | integrazione brottsplatskartan |
| brunt | 1.2.0 | integrazione brunt |
| bthome-ble | 3.25.0 | integrazione bthome |
| bthomehub5-devicelist | 0.1.1 | integrazione bt_home_hub_5 |
| btsmarthub-devicelist | 0.2.3 | integrazione bt_smarthub |
| btsocket | 0.3.0 | bluetooth-auto-recovery |
| buienradar | 1.0.9 | integrazione buienradar |
| bx-py-utils | 121 | jaraco-abode |
| cachetools | 8.0.0 | pysmappee |
| caldav | 3.7.0 | integrazione caldav |
| calendaring-config | 0.5.0 | caldav |
| casttube | 0.2.1 | pychromecast |
| cattrs | 26.2.1 | pyoverkiz |
| certifi | 2026.10.23 | aiowatttime |
| cfgv | 3.5.0 | pre-commit |
| chacha20poly1305-reuseable | 0.13.2 | aiohomekit |
| charset-normalizer | 3.5.2 | requests |
| chefiq-ble | 1.1.1 | integrazione chef_iq |
| chess-com-api | 1.1.0 | integrazione chess_com |
| cielo-connect-api | 1.0.6 | integrazione cielo_home |
| ciscomobilityexpress | 0.3.9 | integrazione cisco_mobility_express |
| click | 8.5.1 | python-miio |
| click-log | 0.4.0 | bellows |
| clx-sdk-xms | 1.0.0 | integrazione sinch |
| codecov | 2.1.13 | pyefergy |
| coinbase-advanced-py | 1.2.2 | integrazione coinbase |
| coloredlogs | 15.0.1 | zigpy-znp |
| colorlog | 6.12.0 | universal-silabs-flasher |
| colorthief | 0.2.1 | integrazione color_extractor |
| colorzero | 2.0 | gpiozero |
| compit-inext-api | 0.9.2 | integrazione compit |
| concord232 | 0.15.1 | integrazione concord232 |
| construct-typing | 0.8.1 | eq3btsmart |
| cookidoo-api | 0.18.4 | integrazione cookidoo |
| coverage | 7.17.3 | codecov |
| crc | 8.0.0 | universal-silabs-flasher |
| crccheck | 1.3.1 | zigpy |
| croniter | 7.0.0 | python-miio |
| cronsim | 2.7 | integrazione utility_meter |
| crownstone-cloud | 1.4.11 | integrazione crownstone |
| crownstone-core | 3.2.1 | crownstone-uart |
| crownstone-sse | 2.0.5 | integrazione crownstone |
| crownstone-uart | 2.1.0 | integrazione crownstone |
| cssselect | 1.6.0 | pyquery |
| dacite | 1.9.2 | nettigo-air-monitor |
| data-grand-lyon-ha | 0.9.0 | integrazione data_grand_lyon |
| dataclass-wizard | 0.39.1 | todoist-api-python |
| dataclasses-json | 0.6.7 | weatherflow4py |
| datadog | 0.52.1 | integrazione datadog |
| datapoint | 0.12.1 | integrazione metoffice |
| dateparser | 1.5.0 | georss-ign-sismologia-client |
| debugpy | 1.8.22 | integrazione debugpy |
| decora-wifi | 1.4 | integrazione decora_wifi |
| decorator | 5.3.1 | mastodon-py |
| deepmerge | 4.0.0 | aioipp |
| defusedxml | 0.7.1 | integrazione sonos |
| deluge-client | 1.10.2 | integrazione deluge |
| demetriek | 2.1.0 | integrazione lametric |
| denon-rs232 | 4.3.1 | integrazione denon_rs232 |
| denonavr | 1.3.3 | integrazione denonavr |
| deprecated | 1.3.1 | pyvicare |
| devialet | 1.5.7 | integrazione devialet |
| devolo-home-control-api | 0.19.1 | integrazione devolo_home_control |
| devolo-plc-api | 1.6.0 | integrazione devolo_home_network |
| dicttoxml | 1.7.16 | ismartgate |
| dicttoxml2 | 2.1.0 | pyasyncialarm |
| dio-chacon-wifi-api | 1.3.0 | integrazione chacon_dio |
| directv | 0.4.0 | integrazione directv |
| discogs-client | 2.3.0 | integrazione discogs |
| discovery30303 | 0.3.3 | integrazione steamist |
| distlib | 0.4.4 | virtualenv |
| distro | 1.9.0 | google-genai |
| dlms-cosem | 21.3.2 | dsmr-parser |
| dnspython | 2.9.2 | mcstatus |
| docstring-parser | 0.18.0 | anthropic |
| doorbirdpy | 3.0.12 | integrazione doorbird |
| dpkt | 1.9.8 | aiortsp |
| dremel3dpy | 2.1.1 | integrazione dremel_3d_printer |
| dropmqttapi | 1.0.3 | integrazione drop_connect |
| dsmr-parser | 1.11.1 | integrazione dsmr |
| dwdwfsapi | 1.1.0 | integrazione dwd_weather_warnings |
| dynalite-panel | 0.0.4 | integrazione dynalite |
| eagle100 | 0.1.1 | integrazione rainforest_eagle |
| earn-e-p1 | 0.3.0 | integrazione earn_e_p1 |
| easyenergy | 3.0.1 | integrazione easyenergy |
| ecoaliface | 0.4.0 | integrazione ecoal_boiler |
| egauge-async | 0.4.0 | integrazione egauge |
| eheimdigital | 1.7.1 | integrazione eheimdigital |
| ekey-bionyxpy | 1.0.1 | integrazione ekeybionyx |
| electrickiwi-api | 0.9.14 | integrazione electric_kiwi |
| elementpath | 5.1.4 | xmlschema |
| elevenlabs | 2.51.0 | integrazione elevenlabs |
| elgato | 6.1.1 | integrazione elgato |
| elkm1-lib | 2.2.15 | integrazione elkm1 |
| elkoep-aio-mqtt | 0.1.0b5 | integrazione inels |
| elmax-api | 0.0.6.3 | integrazione elmax |
| emoji | 2.8.0 | integrazione xmpp |
| emulated-roku | 0.3.0 | integrazione emulated_roku |
| energieleser | 0.1.7 | integrazione energieleser |
| energyflip-api-client | 0.3.0 | integrazione huisbaasje |
| energyid-webhooks | 0.0.16 | integrazione energyid |
| energyzero | 5.1.1 | integrazione energyzero |
| enocean-async | 0.4.2 | integrazione enocean |
| enturclient | 0.2.4 | integrazione entur_public_transport |
| enum-compat | 0.0.3 | libsoundtouch |
| env-canada | 0.20.3 | integrazione environment_canada |
| envoy-utils | 0.0.1 | pyenphase |
| envs | 1.4 | pycognito |
| epson-projector | 0.6.0 | integrazione epson |
| eq3btsmart | 2.3.0 | integrazione eq3btsmart |
| esios-api | 4.4.0 | integrazione pvpc_hourly_pricing |
| esphome-dashboard-api | 1.4.0 | integrazione esphome |
| essent-dynamic-pricing | 0.3.1 | integrazione essent |
| eternalegypt | 0.0.18 | integrazione netgear_lte |
| eufylife-ble-client | 0.1.10 | integrazione eufylife_ble |
| eurotronic-cometblue-ha | 2.0.0 | integrazione eurotronic_cometblue |
| evohome-async | 3.1.0 | integrazione evohome |
| evolutionhttp | 0.0.19 | integrazione bryant_evolution |
| exceptiongroup | 1.3.1 | nibe |
| faadelays | 2023.9.1 | integrazione faa_delays |
| fastdotcom | 0.0.6 | integrazione fastdotcom |
| feedparser | 6.0.12 | integrazione feedreader |
| fido2 | 2.2.1 | pyicloud |
| file-read-backwards | 2.0.0 | integrazione file |
| filelock | 4.2.10 | virtualenv |
| fing-agent-api | 1.1.0 | integrazione fing |
| fints | 3.1.0 | integrazione fints |
| firebase-messaging | 0.4.5 | ring-doorbell |
| fish-audio-sdk | 1.1.0 | integrazione fish_audio |
| fitbit-web-api | 2.13.5 | integrazione fitbit |
| fivem-api | 0.1.2 | integrazione fivem |
| fixerio | 0.1.1 | integrazione fixer |
| fjaraskupan | 2.3.4 | integrazione fjaraskupan |
| flask | 3.1.3 | pynx584 |
| flexcache | 0.3 | pint |
| flexit-bacnet | 2.2.3 | integrazione flexit_bacnet |
| flexit-modbus | 0.1.0 | integrazione flexit |
| flexparser | 0.4 | pint |
| flow-it-api | 0.0.1.1 | integrazione flow_it |
| fluss-api | 0.2.5 | integrazione fluss |
| flux-led | 1.2.0 | integrazione flux_led |
| fnvhash | 0.2.1 | fnv-hash-fast |
| forecast-solar | 5.0.1 | integrazione forecast_solar |
| fortiosapi | 1.0.5 | integrazione fortios |
| freebox-api | 1.3.1 | integrazione freebox |
| freenub | 0.1.2 | yalexs |
| freesms | 0.2.0 | integrazione free_mobile |
| fressnapftracker | 0.3.1 | integrazione fressnapf_tracker |
| fritzconnection | 1.15.1 | integrazione fritzbox_callmonitor |
| fronius-modbus | 0.2.0 | integrazione fronius |
| fronius-solarapi | 0.1.1 | integrazione fronius |
| frozendict | 2.5.1 | zha |
| frozenlist | 1.8.0 | regenmaschine |
| ftfy | 6.3.1 | denonavr |
| fumis | 0.4.2 | integrazione fumis |
| furl | 2.1.4 | mvg |
| future | 1.0.0 | aurorapy |
| fyta-cli | 0.7.3 | integrazione fyta |
| gardena-bluetooth | 2.10.1 | integrazione husqvarna_automower_ble |
| gassist-text | 0.1.0 | integrazione google_assistant_sdk |
| gatus-api | 1.2.0 | integrazione gatus |
| gcal-sync | 9.2.0 | integrazione google |
| genie-partner-sdk | 1.0.11 | integrazione aladdin_connect |
| geniushub-client | 0.7.4 | integrazione geniushub |
| geocachingapi | 0.3.0 | integrazione geocaching |
| geographiclib | 2.1 | geopy |
| geojson | 3.3.0 | datapoint |
| geopy | 2.5.0 | integrazione aprs |
| georss-client | 2026.6.1 | georss-qld-bushfire-alert-client |
| georss-generic-client | 0.8 | integrazione geo_rss_events |
| georss-ign-sismologia-client | 0.8 | integrazione ign_sismologia |
| georss-qld-bushfire-alert-client | 0.8 | integrazione qld_bushfire |
| getmac | 0.9.6 | integrazione upnp |
| gios | 7.1.1 | integrazione gios |
| glances-api | 0.10.0 | integrazione glances |
| go2rtc-client | 0.4.0 | integrazione go2rtc |
| goalzero | 0.2.2 | integrazione goalzero |
| goodwe | 0.4.10 | integrazione goodwe |
| google-air-quality-api | 3.0.1 | integrazione google_air_quality |
| google-api-core | 2.44.0 | google-api-python-client |
| google-api-python-client | 2.71.0 | integrazione google_tasks |
| google-auth | 2.62.2 | aioaquarite |
| google-auth-httplib2 | 0.4.4 | google-api-python-client |
| google-auth-oauthlib | 1.5.0 | google-nest-sdm |
| google-cloud-core | 2.8.0 | google-cloud-firestore |
| google-cloud-firestore | 2.34.1 | aioaquarite |
| google-cloud-pubsub | 2.39.2 | integrazione google_pubsub |
| google-cloud-speech | 2.40.0 | integrazione google_cloud |
| google-cloud-texttospeech | 2.37.0 | integrazione google_cloud |
| google-genai | 2.25.0 | integrazione google_generative_ai_conversation |
| google-health-api | 0.9.0 | integrazione google_health |
| google-nest-sdm | 9.2.1 | integrazione nest |
| google-photos-library-api | 0.12.1 | integrazione google_photos |
| googleapis-common-protos | 1.76.0 | tesla-protocol |
| googlewifiapi | 0.1.3 | integrazione google_wifi |
| goslide-api | 0.7.4 | integrazione slide_local |
| gotailwind | 0.4.0 | integrazione tailwind |
| govee-ble | 1.4.0 | integrazione govee_ble |
| govee-local-api | 3.3.1 | integrazione govee_light_local |
| gpiozero | 1.6.2 | integrazione remote_rpi_gpio |
| gps3 | 0.33.3 | integrazione gpsd |
| gql | 4.4.0 | pytibber |
| grandstream-home-api | 0.1.7 | integrazione grandstream_home |
| graphql-core | 3.3.2 | pydrawise |
| graphviz | 0.21 | objgraph |
| greeclimate | 3.5.2 | integrazione gree |
| greencell-client | 1.0.3 | integrazione greencell |
| greeneye-monitor | 3.0.3 | integrazione greeneye_monitor |
| greenplanet-energy-api | 0.1.10 | integrazione green_planet_energy |
| gridnet | 5.0.1 | integrazione pure_energie |
| growattserver | 2.2.0 | integrazione growatt_server |
| grpc-google-iam-v1 | 0.14.6 | google-cloud-pubsub |
| grpcio-reflection | 1.85.1 | yagrc |
| grpcio-status | 1.85.1 | google-cloud-pubsub |
| gspread | 6.2.1 | integrazione google_sheets |
| gtfs-realtime-bindings | 2.0.0 | py-nymta |
| gtts | 2.5.4 | integrazione google_translate |
| guntamatic | 1.12.0 | integrazione guntamatic |
| h11 | 0.16.0 | matrix-nio |
| h2 | 4.4.1 | integrazione iaqualink |
| ha-ffmpeg | 3.2.2 | integrazione ffmpeg |
| ha-iotawattpy | 0.3.0 | integrazione iotawatt |
| ha-philipsjs | 3.3.0 | integrazione philips_js |
| ha-silabs-firmware-client | 0.4.0 | integrazione homeassistant_hardware |
| ha-xthings-cloud | 1.0.5 | integrazione xthings_cloud |
| habiticalib | 0.4.7 | integrazione habitica |
| hanna-cloud | 0.0.7 | integrazione hanna |
| hap-python | 5.0.0 | integrazione homekit |
| harbor-python | 1.5.0 | integrazione harbor |
| hass-nabucasa | 2.8.1 | integrazione cloud |
| hass-splunk | 0.1.4 | integrazione splunk |
| hassil | 3.12.1 | integrazione conversation |
| hatasmota | 0.10.2 | integrazione tasmota |
| haveniaq | 0.1.0 | integrazione haven_iaq |
| haversine | 2.9.0 | aio-geojson-client |
| hdate | 1.2.2 | integrazione jewish_calendar |
| hdfury | 1.6.1 | integrazione hdfury |
| heatmiserv3 | 2.0.6 | integrazione heatmiser |
| hegel-ip-client | 0.1.4 | integrazione hegel |
| here-routing | 1.2.0 | integrazione here_travel_time |
| here-transit | 1.2.1 | integrazione here_travel_time |
| hkavr | 0.0.5 | integrazione harman_kardon_avr |
| hko | 0.3.2 | integrazione hko |
| hlk-sw16 | 0.0.9 | integrazione hlk_sw16 |
| hole | 0.9.2 | integrazione pi_hole |
| holidays | 0.108 | integrazione workday |
| home-assistant-bluetooth | 2.0.0 | xiaomi-ble |
| home-assistant-frontend | 20261028.5 | integrazione frontend |
| home-assistant-intents | 2026.10.27 | integrazione conversation |
| homekit-audio-proxy | 1.2.1 | integrazione homekit |
| homelink-integration-api | 0.1.0 | integrazione gentex_homelink |
| homematicip | 2.20.0 | integrazione homematicip_cloud |
| homevolt | 0.7.0 | integrazione homevolt |
| horimote | 0.4.1 | integrazione horizon |
| hpack | 4.2.0 | h2 |
| html-table-parser-python3 | 0.1.5 | bthomehub5-devicelist |
| html5lib | 1.1 | mechanize |
| httpcore | 1.0.9 | python-telegram-bot |
| httpcore2 | 2.16.0 | httpx2 |
| httplib2 | 0.32.1 | pytouchline-extended |
| httpx | 0.28.1 | python-xbox |
| httpx-retries | 0.6.0 | iaqualink |
| httpx-sse | 0.4.3 | aiohomeconnect |
| httpx-ws | 0.9.0 | fish-audio-sdk |
| httpx2 | 2.16.0 | openai |
| hueble | 2.3.0 | integrazione hue_ble |
| humanfriendly | 10.0 | coloredlogs |
| huum | 0.8.2 | integrazione huum |
| hydropeak-opendata | 0.2.0 | integrazione hydroquebec_peak |
| hyperframe | 6.1.0 | h2 |
| hyperion-py | 0.7.6 | integrazione hyperion |
| hyponcloud | 1.0.2 | integrazione hypontech |
| iammeter | 0.2.1 | integrazione iammeter |
| iaqualink | 0.7.0 | integrazione iaqualink |
| ibeacon-ble | 1.2.0 | integrazione ibeacon |
| ical | 14.3.1 | integrazione remote_calendar |
| icalendar | 6.3.1 | integrazione caldav |
| icalendar-searcher | 1.3.0 | caldav |
| icmplib | 3.0.4 | integrazione ping |
| idasen | 0.14.0 | idasen-ha |
| idasen-ha | 3.0.2 | integrazione idasen_desk |
| identify | 2.6.20 | pre-commit |
| idna | 3.20 | url-normalize |
| idrive-e2-client | 0.1.1 | integrazione idrive_e2 |
| ifaddr | 0.2.0 | integrazione network |
| iglo | 1.2.7 | integrazione iglo |
| igloohome-api | 0.1.1 | integrazione igloohome |
| ihcsdk | 2.8.12 | integrazione ihc |
| imageio | 2.38.1 | dremel3dpy |
| imeon-inverter-api | 0.4.0 | integrazione imeon_inverter |
| imgw-pib | 2.5.3 | integrazione imgw_pib |
| importlib-resources | 6.5.2 | heatmiserv3 |
| incomfort-client | 0.7.1 | integrazione incomfort |
| indevolt-api | 1.8.8 | integrazione indevolt |
| inflect | 7.6.0 | decora-wifi |
| inflection | 0.5.1 | python-smarttub |
| influxdb | 5.3.2 | integrazione influxdb |
| influxdb-client | 1.50.0 | integrazione influxdb |
| infrared-protocols | 10.5.0 | integrazione infrared |
| inkbird-ble | 1.7.1 | integrazione inkbird |
| insteon-frontend-home-assistant | 0.7.0 | integrazione insteon |
| intelhex | 2.3.0 | pymysensors |
| intellifire4py | 4.5.6 | integrazione intellifire |
| iometer | 1.0.2 | integrazione iometer |
| iottycloud | 0.3.0 | integrazione iotty |
| irm-kmi-api | 1.1.1 | integrazione irm_kmi |
| iseo-argo-ble | 0.9.10 | integrazione iseo_argo_ble |
| ismartgate | 5.0.2 | integrazione gogogate2 |
| iso4217 | 1.16.20260101 | aioraven |
| iso8601 | 2.1.0 | clx-sdk-xms |
| isodate | 0.7.2 | cookidoo-api |
| israel-rail-api | 0.1.5 | integrazione israel_rail |
| itsdangerous | 2.2.0 | flask |
| jaraco-abode | 6.4.0 | integrazione abode |
| jaraco-classes | 3.4.0 | jaraco-abode |
| jaraco-collections | 5.2.1 | jaraco-abode |
| jaraco-context | 6.1.2 | jaraco-abode |
| jaraco-email | 3.1.1 | jaraco-net |
| jaraco-functools | 4.6.0 | jaraco-abode |
| jaraco-itertools | 6.4.3 | jaraco-abode |
| jaraco-logging | 3.4.0 | jaraco-net |
| jaraco-net | 10.2.3 | jaraco-abode |
| jaraco-text | 4.3.0 | jaraco-collections |
| jellyfin-apiclient-python | 1.19.0 | integrazione jellyfin |
| jh2 | 5.0.15 | urllib3-future |
| jinja2 | 3.1.6 | flask |
| jmespath | 1.1.0 | pysma |
| josepy | 2.3.0 | hass-nabucasa |
| joserfc | 1.8.0 | authlib |
| jsonpath-python | 1.1.6 | integrazione rest |
| jsonpickle | 4.1.4 | pysmarlaapi |
| jsonschema | 4.26.0 | mcp |
| jsonschema-specifications | 2025.9.1 | jsonschema |
| justbackoff | 0.6.0 | nessclient |
| justnimbus | 0.7.4 | integrazione justnimbus |
| kaco-modbus | 1.1.0 | integrazione kaco_modbus |
| kaco-serial | 1.0.3 | integrazione kaco_rs485 |
| kaiterra-async-client | 1.1.0 | integrazione kaiterra |
| keba-kecontact | 1.3.0 | integrazione keba |
| kegtron-ble | 1.0.2 | integrazione kegtron |
| keyring | 25.7.0 | pyicloud |
| keyrings-alt | 5.0.2 | pyicloud |
| kiosker-python-api | 1.2.9 | integrazione kiosker |
| kiwiki-client | 0.1.2 | integrazione kiwi |
| knocki | 0.4.2 | integrazione knocki |
| knx-frontend | 2026.11.8.11926 | integrazione knx |
| knx-telegram-store | 0.15.2 | integrazione knx |
| lacrosse-view | 1.1.2 | integrazione lacrosse_view |
| lakeside | 0.13 | integrazione eufy |
| langcodes | 3.5.1 | aioamazondevices |
| laundrify-aio | 1.2.2 | integrazione laundrify |
| lcn-frontend | 0.2.9 | integrazione lcn |
| ld2410-ble | 0.1.1 | integrazione ld2410_ble |
| leaone-ble | 0.3.0 | integrazione leaone |
| led-ble | 1.1.11 | integrazione led_ble |
| lektricowifi | 0.1 | integrazione lektrico |
| letpot | 0.7.0 | integrazione letpot |
| lg-rs232-tv | 1.3.1 | integrazione lg_tv_rs232 |
| libpyfoscamcgi | 0.0.9 | integrazione foscam |
| libpyvivotek | 0.6.1 | integrazione vivotek |
| librehardwaremonitor-api | 1.12.0 | integrazione libre_hardware_monitor |
| librouteros | 4.2.2 | integrazione mikrotik |
| libsoundtouch | 0.8.0 | integrazione soundtouch |
| lifx-async | 7.13.1 | integrazione lifx |
| lighthouse-ble | 0.2.0 | integrazione steamvr_base_station |
| lightify | 1.0.7.3 | integrazione osramlightify |
| lightwave | 0.24 | integrazione lightwave |
| livisi | 1.0.2 | integrazione livisi |
| logfury | 1.0.1 | b2sdk |
| loguru | 0.8.0 | pyosoenergyapi |
| lojack-api | 0.7.2 | integrazione lojack |
| lomond | 0.3.4 | jaraco-abode |
| london-tube-status | 0.7 | integrazione london_underground |
| loqedapi | 2.1.16 | integrazione loqed |
| lorawan-connection | 1.3.2 | integrazione thethingsnetwork_lorawan |
| luftdaten | 0.8.0 | integrazione luftdaten |
| lunatone-rest-api-client | 0.11.2 | integrazione lunatone |
| lupupy | 0.3.2 | integrazione lupusec |
| lyngdorf | 2.2.1 | integrazione lyngdorf |
| mac-vendor-lookup | 0.1.15 | ibeacon-ble |
| marantz-rs232 | 2.1.1 | integrazione marantz_rs232 |
| markdown | 3.11.3 | apprise |
| markdown-it-py | 4.3.0 | rich |
| marshmallow | 4.4.0 | weatherflow4py |
| marshmallow-dataclass | 8.7.1 | renault-api |
| mashumaro | 3.23 | pyzonneplan |
| mastodon-py | 2.3.3 | integrazione mastodon |
| matrix-nio | 0.26.0 | integrazione matrix |
| matter-ble-proxy | 0.7.1 | integrazione matter |
| matter-python-client | 1.5.5 | integrazione matter |
| maxcube-api | 0.4.3 | integrazione maxcube |
| mbddns | 0.1.2 | integrazione mythicbeastsdns |
| mcp | 2.3.0 | integrazione mcp_server |
| mcp-types | 2.3.0 | mcp |
| mcstatus | 14.2.0 | integrazione minecraft_server |
| mdurl | 0.1.2 | markdown-it-py |
| meater-python | 0.0.8 | integrazione meater |
| mechanize | 0.4.10 | jaraco-net |
| medcom-ble | 0.1.1 | integrazione medcom_ble |
| melnor-bluetooth | 0.0.25 | integrazione melnor |
| metar | 2.0.2 | pynws |
| meteo-lt-pkg | 0.7.3 | integrazione meteo_lt |
| meteoalertapi | 0.3.1 | integrazione meteoalarm |
| meteocalc | 1.1.0 | aioecowitt |
| meteofrance-api | 1.5.0 | integrazione meteo_france |
| mficlient | 0.5.0 | integrazione mfi |
| midea-local | 15.0.0 | integrazione midea |
| mill-local | 0.5.0 | integrazione mill |
| millheater | 0.14.1 | integrazione mill |
| minio | 7.1.12 | integrazione minio |
| mitsubishi-comfort | 0.6.0 | integrazione mitsubishi_comfort |
| moat-ble | 0.1.1 | integrazione moat |
| modbus-connection | 4.12.7 | integrazione modbus |
| moehlenhoff-alpha2 | 1.4.0 | integrazione moehlenhoff_alpha2 |
| momonga | 0.7.0 | integrazione route_b_smart_meter |
| monarchmoneycommunity | 1.6.0 | integrazione monarch_money |
| monzopy | 1.8.0 | integrazione monzo |
| mopeka-iot-ble | 0.8.0 | integrazione mopeka |
| more-itertools | 11.2.1 | jaraco-abode |
| motionblinds | 0.6.31 | integrazione motion_blinds |
| motioneye-client | 0.4.1 | integrazione motioneye |
| mozart-api | 6.2.0.44.0 | integrazione bang_olufsen |
| msal | 1.39.0 | azure-kusto-data |
| msal-extensions | 1.3.1 | azure-identity |
| mt-940 | 5.1.3 | fints |
| mullvad-api | 1.0.0 | integrazione mullvad |
| multidict | 7.2.1 | simplisafe-python |
| munch | 4.0.0 | plugwise |
| music-assistant-client | 1.6.0 | integrazione music_assistant |
| music-assistant-models | 1.1.214 | music-assistant-client |
| mutagen | 1.48.1 | integrazione tts |
| mutesync | 0.0.1 | integrazione mutesync |
| mvg | 1.6.0 | integrazione mvglive |
| my-pv | 0.0.17 | integrazione my_pv |
| mypy-extensions | 1.2.0 | typing-inspect |
| myuplink | 0.7.0 | integrazione myuplink |
| nad-receiver | 0.4.0 | integrazione nad |
| neopool-modbus | 4.13.0 | integrazione neopool |
| nessclient | 1.3.1 | integrazione ness_alarm |
| netdata | 1.3.0 | integrazione netdata |
| netio | 2.0.1 | integrazione netio |
| netmap | 0.7.0.2 | integrazione nmap_tracker |
| nettigo-air-monitor | 5.1.0 | integrazione nam |
| nexblue-api | 0.1.3 | integrazione nexblue |
| nexia | 2.13.0 | integrazione nexia |
| nextcloudmonitor | 1.5.1 | integrazione nextcloud |
| nextcord | 3.1.0 | integrazione discord |
| nextdns | 5.2.0 | integrazione nextdns |
| nhc | 0.8.1 | integrazione niko_home_control |
| nibe | 2.25.0 | integrazione nibe_heatpump |
| nice-go | 1.0.3 | integrazione nice_go |
| niluclient | 0.1.2 | integrazione nilu |
| niquests | 3.21.3 | caldav |
| noaa-coops | 0.4.0 | integrazione noaa_tides |
| nodeenv | 1.12.0 | pre-commit |
| noiseprotocol | 0.3.1 | aioesphomeapi |
| notifications-android-tv | 0.1.5 | integrazione nfandroidtv |
| notify-events | 1.0.4 | integrazione notify_events |
| nrgkick-api | 1.8.1 | integrazione nrgkick |
| nsapi | 3.1.3 | integrazione nederlandse_spoorwegen |
| nuheat | 1.0.1 | integrazione nuheat |
| num2words | 0.5.14 | hdate |
| numato-gpio | 0.13.0 | integrazione numato |
| nyt-games | 1.0.1 | integrazione nyt_games |
| oasatelematics | 0.4 | integrazione oasa_telematics |
| oathtool | 2.4.0 | monarchmoneycommunity |
| oauth2client | 4.1.3 | integrazione google |
| oauthlib | 3.3.1 | ring-doorbell |
| objgraph | 3.5.0 | integrazione profiler |
| odp-amsterdam | 6.1.2 | integrazione garages_amsterdam |
| oemthermostat | 1.1.1 | integrazione oem |
| ohme | 1.9.1 | integrazione ohme |
| okokscale | 0.0.5 | integrazione okok_scale |
| ollama | 0.6.2 | integrazione ollama |
| ondilo | 0.5.0 | integrazione ondilo_ico |
| onedrive-personal-sdk | 0.2.0 | integrazione onedrive_for_business |
| onvif-parsers | 2.5.0 | integrazione onvif |
| onvif-zeep-async | 4.3.0 | integrazione onvif |
| open-garage | 0.3.1 | integrazione opengarage |
| open-meteo | 1.0.1 | integrazione open_meteo |
| openai | 3.26.0 | integrazione cloud |
| openerz-api | 0.3.0 | integrazione openerz |
| openhomedevice | 2.5 | integrazione openhome |
| openrgb-python | 0.3.8 | integrazione openrgb |
| opensensemap-api | 0.4.1 | integrazione opensensemap |
| opentelemetry-api | 1.45.2 | mcp |
| opentelemetry-sdk | 1.45.2 | google-cloud-pubsub |
| opentelemetry-semantic-conventions | 0.66b2 | opentelemetry-sdk |
| openwebifpy | 4.3.1 | integrazione enigma2 |
| openwrt-luci-rpc | 1.1.17 | integrazione luci |
| opower | 0.23.1 | integrazione opower |
| opuslib-next | 1.1.6 | voip-utils |
| oralb-ble | 1.1.3 | integrazione oralb |
| orderedmultidict | 1.0.2 | furl |
| ouman-eh-800-api | 1.1.0 | integrazione ouman_eh_800 |
| ourgroceries | 1.6.0 | integrazione ourgroceries |
| ovoenergy | 3.0.2 | integrazione ovo_energy |
| oyaml | 1.0 | fortiosapi |
| p1monitor | 3.2.0 | integrazione p1_monitor |
| packaging | 26.3 | ha-silabs-firmware-client |
| paho-mqtt | 2.1.0 | integrazione mqtt |
| pajgps-api | 0.4.2 | integrazione paj_gps |
| panacotta | 0.2 | integrazione panasonic_bluray |
| panasonic-viera | 0.4.6 | integrazione panasonic_viera |
| paramiko | 3.5.0 | unifi-ap |
| path | 17.1.1 | oathtool |
| pathvalidate | 3.3.1 | jaraco-net |
| pdunehd | 1.3.3 | integrazione dunehd |
| peblar | 2.1.0 | integrazione peblar |
| peco | 0.1.2 | integrazione peco |
| pencompy | 0.0.4 | integrazione pencom |
| pexpect | 4.9.0 | integrazione cisco_ios |
| phone-modem | 0.1.1 | integrazione modem_callerid |
| pigpio | 1.78 | integrazione remote_rpi_gpio |
| pint | 0.26.2 | pyweatherflowudp |
| pkce | 1.0.3 | wolf-comm |
| platformdirs | 4.14.5 | uiprotect |
| plexapi | 4.18.2 | integrazione plex |
| plexauth | 0.0.6 | integrazione plex |
| plexwebsocket | 0.0.14 | integrazione plex |
| plugwise | 1.14.8 | integrazione plugwise |
| poolsense | 0.1.0 | integrazione poolsense |
| powerfox | 2.1.3 | integrazione powerfox_local |
| prana-api-client | 0.12.0 | integrazione prana |
| praw | 7.5.0 | integrazione reddit |
| prawcore | 2.4.0 | praw |
| prayer-times-calculator-offline | 1.0.3 | integrazione islamic_prayer_times |
| pre-commit | 4.7.0 | pyhive-integration |
| prettytable | 3.18.0 | pynx584 |
| probatio | 0.13.0 | ritualsgenie |
| progettihwsw | 0.1.4 | integrazione progettihwsw |
| prometheus-client | 0.21.0 | integrazione prometheus |
| propcache | 0.5.4 | yalexs |
| proto-plus | 1.29.0 | google-cloud-pubsub |
| protobuf | 7.36.2 | tesla-fleet-api |
| prowlpy | 1.1.5 | integrazione prowl |
| proxmoxer | 2.3.0 | integrazione proxmoxve |
| psnawp | 3.0.3 | integrazione playstation_network |
| psutil-home-assistant | 0.0.1 | integrazione recorder |
| ptyprocess | 0.7.0 | pexpect |
| pulsectl | 23.5.2 | integrazione pulseaudio_loopback |
| pushbullet-py | 0.11.0 | integrazione pushbullet |
| pushover-complete | 1.2.0 | integrazione pushover |
| pvo | 4.0.0 | integrazione pvoutput |
| py-aosmith | 1.0.18 | integrazione aosmith |
| py-canary | 0.5.4 | integrazione canary |
| py-ccm15 | 1.1.2 | integrazione ccm15 |
| py-cpuinfo | 9.0.0 | integrazione cpuspeed |
| py-dactyl | 2.1.3 | integrazione pterodactyl |
| py-dormakaba-dkey | 1.0.6 | integrazione dormakaba_dkey |
| py-improv-ble-client | 2.0.1 | integrazione improv_ble |
| py-madvr2 | 1.9.1 | integrazione madvr |
| py-melissa-climate | 3.0.3 | integrazione melissa |
| py-nextbusnext | 2.3.0 | integrazione nextbus |
| py-nymta | 0.4.1 | integrazione mta |
| py-opendisplay | 7.18.1 | integrazione opendisplay |
| py-sucks | 0.9.11 | integrazione ecovacs |
| py-synologydsm-api | 2.11.1 | integrazione synology_dsm |
| py-unifi-access | 1.6.2 | integrazione unifi_access |
| py-vapid | 1.9.4 | integrazione html5 |
| pyaehw4a1 | 0.3.9 | integrazione hisense_aehw4a1 |
| pyaftership | 21.11.0 | integrazione aftership |
| pyairnow | 1.4.0 | integrazione airnow |
| pyairobotrest | 0.4.0 | integrazione airobot |
| pyairvisual | 2023.8.1 | integrazione airvisual_pro |
| pyanglianwater | 3.3.3 | integrazione anglian_water |
| pyaprilaire | 0.9.1 | integrazione aprilaire |
| pyaqvify | 0.0.12 | integrazione aqvify |
| pyasn1 | 0.6.5 | slixmpp |
| pyasn1-modules | 0.4.2 | slixmpp |
| pyasyncialarm | 3.1.1 | integrazione ialarm |
| pyatag | 0.3.7.1 | integrazione atag |
| pyatmo | 9.9.0 | integrazione netatmo |
| pyatv | 0.18.0 | integrazione apple_tv |
| pyaussiebb | 0.1.5 | integrazione aussie_broadband |
| pyaxencoapi | 1.0.7.post1 | integrazione myneomitis |
| pybalboa | 1.1.3 | integrazione balboa |
| pyblackbird | 0.10 | integrazione blackbird |
| pyblu | 2.2.0 | integrazione bluesound |
| pybotvac | 0.0.29 | integrazione neato |
| pybraendstofpriser | 2.2.0 | integrazione fuelprices_dk |
| pybravia | 0.5.3 | integrazione braviatv |
| pycarwings2 | 2.14 | integrazione nissan_leaf |
| pycasperglow | 1.2.0 | integrazione casper_glow |
| pycfdns | 3.0.0 | integrazione cloudflare |
| pychannels | 1.2.3 | integrazione channels |
| pychromecast | 14.1.1 | integrazione cast |
| pycketcasts | 1.0.1 | integrazione pocketcasts |
| pycognito | 2024.5.1 | hass-nabucasa |
| pycomfoconnect | 0.5.1 | integrazione comfoconnect |
| pycontrol4 | 2.0.2 | integrazione control4 |
| pycoolmasternet-async | 0.2.6 | integrazione coolmaster |
| pycountry | 24.6.1 | psnawp |
| pycparser | 3.12 | cffi |
| pycsspeechtts | 1.0.8 | integrazione microsoft |
| pycync | 0.6.1 | integrazione cync |
| pydaikin | 2.22.1 | integrazione daikin |
| pydanfossair | 1.0.0 | integrazione danfoss_air |
| pydantic | 2.14.3 | zwave-js-server-python |
| pydantic-settings | 2.16.3 | pyteleinfo |
| pydantic-xml | 2.22.0 | pyrejseplan |
| pydeako | 0.6.0 | integrazione deako |
| pydeconz | 120 | integrazione deconz |
| pydelijn | 1.1.0 | integrazione delijn |
| pydexcom | 0.5.1 | integrazione dexcom |
| pydiscovergy | 3.0.2 | integrazione discovergy |
| pydoods | 1.0.2 | integrazione doods |
| pydrawise | 2026.10.1 | integrazione hydrawise |
| pydroid-ipcam | 3.0.0 | integrazione android_ip_webcam |
| pydroplet | 2.5.0 | integrazione droplet |
| pyduotecno | 2024.10.1 | integrazione duotecno |
| pyecoforest | 0.4.0 | integrazione ecoforest |
| pyeconet | 0.3.1 | integrazione econet |
| pyecotrend-ista | 3.4.0 | integrazione ista_ecotrend |
| pyedimax | 0.2.1 | integrazione edimax |
| pyefergy | 22.5.0 | integrazione efergy |
| pyegps | 0.2.5 | integrazione energenie_power_sockets |
| pyelectra | 1.2.10 | integrazione electrasmart |
| pyelftools | 0.33 | pygbl |
| pyemoncms | 0.1.3 | integrazione emoncms_history |
| pyenphase | 4.4.0 | integrazione enphase_envoy |
| pyenvertechevt800 | 0.2.4 | integrazione envertech_evt800 |
| pyenvisalink | 4.9 | integrazione envisalink |
| pyephember2 | 0.4.12 | integrazione ephember |
| pyeverlights | 0.1.0 | integrazione everlights |
| pyevilgenius | 2.0.0 | integrazione evil_genius_labs |
| pyezvizapi | 1.0.5.0 | integrazione ezviz |
| pyfibaro | 0.8.4 | integrazione fibaro |
| pyfido | 2.1.2 | integrazione fido |
| pyfirefly | 0.1.17 | integrazione firefly_iii |
| pyfireservicerota | 0.0.49 | integrazione fireservicerota |
| pyflic | 2.0.4 | integrazione flic |
| pyflic-ble | 0.2.6 | integrazione flic_button |
| pyflume | 0.6.5 | integrazione flume |
| pyfnip | 0.2 | integrazione futurenow |
| pyforeca | 0.2.2 | integrazione foreca |
| pyforked-daapd | 0.1.14 | integrazione forked_daapd |
| pyfreedompro | 1.1.0 | integrazione freedompro |
| pyfreshr | 1.2.0 | integrazione freshr |
| pyfritzhome | 0.6.21 | integrazione fritzbox |
| pygbl | 1.2.0 | integrazione homeassistant_hardware |
| pygeosphere-warnings | 0.1.1 | integrazione geosphere_austria_warnings |
| pygments | 2.21.0 | pyairvisual |
| pygtfs | 0.1.11 | integrazione gtfs |
| pygti | 1.1.1 | integrazione hvv_departures |
| pyhaseiq | 0.2.1 | integrazione hase_iq |
| pyhaversion | 22.8.0 | integrazione version |
| pyhelty | 0.2.0 | integrazione helty |
| pyheltycloud | 0.2.2 | integrazione helty_cloud |
| pyheos | 1.0.6 | integrazione heos |
| pyhik | 0.4.8 | integrazione hikvision |
| pyhive-integration | 1.0.10 | integrazione hive |
| pyhomee | 1.4.7 | integrazione homee |
| pyhomeworks | 1.1.2 | integrazione homeworks |
| pyicloud | 2026.10.1 | integrazione icloud |
| pyimouapi | 1.4.0 | integrazione imou |
| pyinsteon | 1.7.0 | integrazione insteon |
| pyintelliclima | 0.5.0 | integrazione intelliclima |
| pyintesishome | 2.5.0 | integrazione intesishome |
| pyipma | 3.0.10 | integrazione ipma |
| pyiqvia | 2022.4.0 | integrazione iqvia |
| pyirishrail | 0.0.2 | integrazione irish_rail_transport |
| pyiskra | 0.1.29 | integrazione iskra |
| pyisy | 3.8.0 | integrazione isy994 |
| pyituran | 0.1.6 | integrazione ituran |
| pyjvcprojector | 2.1.1 | integrazione jvc_projector |
| pyjwt | 2.15.1 | yalexs |
| pykaleidescape | 1.2.0 | integrazione kaleidescape |
| pykira | 0.1.1 | integrazione kira |
| pykodi | 0.2.7 | integrazione kodi |
| pykoplenti | 1.5.1 | integrazione kostal_plenticore |
| pykwb | 0.0.8 | integrazione kwb |
| pylamarzocco | 2.5.6 | integrazione lamarzocco |
| pylast | 5.1.0 | integrazione lastfm |
| pylaunches | 2.0.0 | integrazione launch_library |
| pylgnetcast | 0.3.9 | integrazione lg_netcast |
| pylibrespot-java | 0.1.1 | integrazione forked_daapd |
| pyliebherrhomeapi | 0.5.1 | integrazione liebherr |
| pylitejet | 0.6.3 | integrazione litejet |
| pylitterbot | 2025.7.0 | integrazione litterrobot |
| pyloadapi | 2.1.0 | integrazione pyload |
| pylondonair | 0.1.1 | integrazione london_air |
| pylutron | 0.4.4 | integrazione lutron |
| pylutron-caseta | 0.30.1 | integrazione lutron_caseta |
| pymata-express | 1.19 | integrazione firmata |
| pymeteireann | 2024.11.0 | integrazione met_eireann |
| pymeteoclimatic | 0.1.1 | integrazione meteoclimatic |
| pymetno | 0.13.0 | integrazione norway_air |
| pymicrobot | 0.0.23 | integrazione keymitt_ble |
| pymiele | 0.6.3 | integrazione miele |
| pymitv | 1.4.3 | integrazione xiaomi_tv |
| pymodbus | 3.13.1 | integrazione modbus |
| pymonoprice | 0.6.1 | integrazione monoprice |
| pymysensors | 0.26.0 | integrazione mysensors |
| pynecil | 4.2.1 | integrazione iron_os |
| pynetgear | 0.10.10 | integrazione netgear |
| pynina | 1.0.4 | integrazione nina |
| pynintendoauth | 1.0.3 | integrazione nintendo_parental_controls |
| pynintendoparental | 2.6.2 | integrazione nintendo_parental_controls |
| pynobo | 1.9.0 | integrazione nobo_hub |
| pynordpool | 0.4.0 | integrazione nordpool |
| pynuki | 1.6.3 | integrazione nuki |
| pynws | 1.8.2 | integrazione nws |
| pynx584 | 0.8.2 | integrazione nx584 |
| pyobihai | 1.4.2 | integrazione obihai |
| pyoctoprintapi | 0.1.14 | integrazione octoprint |
| pyombi | 0.1.10 | integrazione ombi |
| pyomie | 1.1.3 | integrazione omie |
| pyopenssl | 26.4.0 | pyswitchbot |
| pyopenuv | 2023.2.0 | integrazione openuv |
| pyopenweathermap | 0.3.0 | integrazione openweathermap |
| pyoppleio-legacy | 1.0.8 | integrazione opple |
| pyosoenergyapi | 1.2.4 | integrazione osoenergy |
| pyotgw | 2.2.3 | integrazione opentherm_gw |
| pyotp | 2.10.0 | integrazione otp |
| pyoverkiz | 2.3.2 | integrazione overkiz |
| pypalazzetti | 0.1.21 | integrazione palazzetti |
| pypaperless | 5.2.3 | integrazione paperless_ngx |
| pyparsing | 3.3.3 | httplib2 |
| pypasser | 1.1.0 | tami4edgeapi |
| pypca | 0.0.7 | integrazione elv |
| pypck | 0.9.13 | integrazione lcn |
| pypglab | 0.0.5 | integrazione pglab |
| pypjlink2 | 1.2.1 | integrazione pjlink |
| pyplaato | 0.0.19 | integrazione plaato |
| pypoint | 3.0.0 | integrazione point |
| pyportainer | 1.0.50 | integrazione portainer |
| pyprobeplus | 2.1.0 | integrazione probe_plus |
| pyprusalink | 3.0.0 | integrazione prusalink |
| pyps4-2ndscreen | 1.3.1 | integrazione ps4 |
| pypubsub | 4.0.7 | pyinsteon |
| pyquery | 2.1.0 | pyhive-integration |
| pyqvrpro | 0.52 | integrazione qvr_pro |
| pyrail | 0.6.1 | integrazione nmbs |
| pyrainbird | 6.6.0 | integrazione rainbird |
| pyrate-limiter | 4.5.1 | integrazione playstation_network |
| pyrecswitch | 1.0.2 | integrazione recswitch |
| pyrejseplan | 1.0.9 | integrazione rejseplanen |
| pyrepetierng | 0.1.0 | integrazione repetier |
| pyrfc3339 | 2.1.1 | acme |
| pyrfxtrx | 0.34.0 | integrazione rfxtrx |
| pyrisco | 0.8.1 | integrazione risco |
| pyrmvtransport | 0.3.3 | integrazione rmvtransport |
| pyroute2 | 0.9.6 | integrazione thread |
| pyrympro | 0.0.9 | integrazione rympro |
| pysabnzbd | 1.1.1 | integrazione sabnzbd |
| pysaunum | 0.7.0 | integrazione saunum |
| pyschlage | 2026.9.0 | integrazione schlage |
| pyscorpiontrack | 0.1.2 | integrazione scorpiontrack |
| pysdcp | 1 | integrazione sony_projector |
| pysensibo | 1.2.1 | integrazione sensibo |
| pysenz | 1.1.2 | integrazione senz |
| pyserial | 3.5 | pyw800rf32 |
| pyserial-asyncio-fast | 0.16 | pyteleinfo |
| pysesame2 | 1.0.2 | integrazione sesame |
| pyseventeentrack | 1.2.0 | integrazione seventeentrack |
| pysiaalarm | 3.2.2 | integrazione sia |
| pysignalclirestapi | 0.3.25 | integrazione signal_messenger |
| pysignalr | 1.3.3 | pysmarlaapi |
| pysillaprism | 0.2.1 | integrazione silla_prism |
| pyskyqhub | 0.1.4 | integrazione sky_hub |
| pysma | 1.1.9 | integrazione sma |
| pysmappee | 0.2.29 | integrazione smappee |
| pysmarlaapi | 1.0.2 | integrazione smarla |
| pysmartthings | 4.0.3 | integrazione smartthings |
| pysmarty2 | 0.10.3 | integrazione smarty |
| pysmartyplants | 0.2.1 | integrazione smartyplants |
| pysmb | 1.2.15 | pyairvisual |
| pysmhi | 2.1.0 | integrazione smhi |
| pysml | 0.1.8 | integrazione edl21 |
| pysmlight | 0.5.3 | integrazione smlight |
| pysnmp | 7.1.27 | integrazione snmp |
| pysnooz | 0.10.1 | integrazione snooz |
| pysomfymylink | 1.0.0 | integrazione somfy_mylink |
| pyspcwebgw | 0.7.0 | integrazione spc |
| pysqueezebox | 0.14.0 | integrazione squeezebox |
| pysrdaligateway | 0.21.0 | integrazione sunricher_dali |
| pystiebeleltron | 0.10.1 | integrazione stiebel_eltron |
| pysuezv2 | 2.0.8 | integrazione suez_water |
| pyswitchbee | 1.8.3 | integrazione switchbee |
| pyswitchbot | 3.1.0 | integrazione switchbot |
| pyswitchmate | 0.5.1 | integrazione switchmate |
| pytautulli | 23.1.1 | integrazione tautulli |
| pyteleinfo | 0.4.0 | integrazione teleinfo |
| python-aidot | 0.3.59 | integrazione aidot |
| python-awair | 0.2.5 | integrazione awair |
| python-backoff | 3.1.0 | wled |
| python-bsblan | 6.2.1 | integrazione bsblan |
| python-citybikes | 0.3.3 | integrazione citybikes |
| python-dateutil | 4.0.0 | yalexs |
| python-decouple | 3.8 | homelink-integration-api |
| python-didl-lite | 1.5.2 | async-upnp-client |
| python-discovery | 1.9.5 | virtualenv |
| python-dotenv | 1.2.4 | pydantic-settings |
| python-dropbox-api | 0.1.6 | integrazione dropbox |
| python-duco-connectivity | 0.18.0 | integrazione duco |
| python-ecobee-api | 0.4.2 | integrazione ecobee |
| python-engineio | 4.15.1 | aioambient |
| python-engineio-v3 | 3.14.2 | sisyphus-control |
| python-fullykiosk | 0.0.15 | integrazione fully_kiosk |
| python-gc100 | 1.0.3a0 | integrazione gc100 |
| python-google-drive-api | 0.2.1 | integrazione google_drive |
| python-google-maps-routing-api | 0.0.4 | integrazione google_travel_time |
| python-google-weather-api | 0.0.10 | integrazione google_weather |
| python-homeassistant-analytics | 0.9.0 | integrazione analytics_insights |
| python-homewizard-energy | 10.2.0 | integrazione homewizard |
| python-hotspring | 3.1.0 | integrazione hotspring |
| python-hpilo | 4.4.3 | integrazione hp_ilo |
| python-http-client | 3.3.7 | sendgrid |
| python-izone | 1.3.12 | integrazione izone |
| python-join-api | 0.1.1 | integrazione joaoapps_join |
| python-kasa | 0.12.0 | integrazione tplink |
| python-linkplay | 0.2.14 | integrazione linkplay |
| python-magic | 0.4.27 | pushbullet-py |
| python-melcloud | 0.1.3 | integrazione melcloud |
| python-miio | 0.5.12 | integrazione xiaomi_miio |
| python-motionmount | 2.3.0 | integrazione motionmount |
| python-mpd2 | 3.1.1 | integrazione mpd |
| python-multipart | 0.0.33 | mcp |
| python-mystrom | 2.5.0 | integrazione mystrom |
| python-open-router | 0.5.0 | integrazione open_router |
| python-openevse-http | 1.8.0 | integrazione openevse |
| python-opensky | 1.0.1 | integrazione opensky |
| python-otbr-api | 3.1.0 | integrazione thread |
| python-overseerr | 0.9.0 | integrazione overseerr |
| python-picnic-api2 | 2.0.1 | integrazione picnic |
| python-pooldose | 0.9.10 | integrazione pooldose |
| python-qube-heatpump | 1.17.0 | integrazione hr_energy_qube |
| python-rabbitair | 0.0.8 | integrazione rabbitair |
| python-roborock | 7.12.0 | integrazione roborock |
| python-slugify | 9.1.3 | blinkpy |
| python-smarttub | 0.0.47 | integrazione smarttub |
| python-sn2 | 0.4.0 | integrazione systemnexa2 |
| python-snoo | 0.8.3 | integrazione snoo |
| python-socketio | 5.18.1 | yalexs |
| python-socketio-v4 | 4.6.1 | sisyphus-control |
| python-socks | 3.2.1 | aiohttp-socks |
| python-songpal | 0.16.2 | integrazione songpal |
| python-swisscom-internet-box | 0.2.0 | integrazione swisscom |
| python-tado | 0.18.16 | integrazione tado |
| python-technove | 2.1.3 | integrazione technove |
| python-telegram-bot | 22.7 | integrazione telegram_bot |
| python-xbox | 0.4.0 | integrazione xbox |
| pythonkuma | 0.5.3 | integrazione uptime_kuma |
| pytibber | 0.39.1 | integrazione tibber |
| pytile | 2024.12.0 | integrazione tile |
| pytomorrowio | 0.3.6 | integrazione tomorrowio |
| pytouchline-extended | 0.4.5 | integrazione touchline |
| pytouchlinesl | 0.6.0 | integrazione touchline_sl |
| pytraccar | 3.0.0 | integrazione traccar_server |
| pytradfri | 14.0.0 | integrazione tradfri |
| pytrafikverket | 2.0.0 | integrazione trafikverket_weatherstation |
| pytransportnsw | 0.1.1 | integrazione transport_nsw |
| pytrydan | 1.0.5 | integrazione v2c |
| pytz | 2026.6 | python-miio |
| pyuptimerobot | 25.0.0 | integrazione uptimerobot |
| pyusb | 1.3.1 | pyegps |
| pyvera | 0.3.16 | integrazione vera |
| pyvesync | 3.5.1 | integrazione vesync |
| pyvicare | 2.63.0 | integrazione vicare |
| pyvlx | 0.2.36 | integrazione velux |
| pyvolumio | 0.1.5 | integrazione volumio |
| pyw215 | 0.8.0 | integrazione dlink |
| pyw800rf32 | 0.4 | integrazione w800rf32 |
| pywaze | 1.2.4 | integrazione waze_travel_time |
| pyweatherflowudp | 1.7.0 | integrazione weatherflow |
| pywebpush | 2.3.0 | integrazione html5 |
| pywemo | 1.4.0 | integrazione wemo |
| pywilight | 0.0.74 | integrazione wilight |
| pywillow | 0.1.2 | integrazione willow |
| pywizlight | 0.6.3 | integrazione wiz |
| pywmspro | 0.4.2 | integrazione wmspro |
| pyws66i | 1.1 | integrazione ws66i |
| pyxeoma | 1.4.2 | integrazione xeoma |
| pyxiaomigateway | 0.14.3 | integrazione xiaomi_aqara |
| pyyardian | 1.4.2 | integrazione yardian |
| pyzbar | 0.1.9 | integrazione qrcode |
| pyzipper | 0.4.0 | xknxproject |
| pyzonneplan | 0.3.0 | integrazione zonneplan |
| qbittorrent-api | 2026.5.1 | integrazione qbittorrent |
| qbusmqttapi | 1.5.1 | integrazione qbus |
| qingping-ble | 1.1.6 | integrazione qingping |
| qnapstats | 0.6.2 | integrazione qnap |
| quantum-gateway | 0.0.8 | integrazione quantum_gateway |
| rachiopy | 1.1.0 | integrazione rachio |
| radios | 2.0.1 | integrazione radio_browser |
| rainbowminer-api-client | 0.2.3 | integrazione rainbowminer |
| rapt-ble | 2.0.0 | integrazione rapt_ble |
| raspyrfm-client | 1.2.9 | integrazione raspyrfm |
| reactivex | 6.1.0 | influxdb-client |
| recurring-ical-events | 3.8.2 | caldav |
| redgtech-api | 0.1.38 | integrazione redgtech |
| referencing | 0.37.0 | jsonschema |
| refoss-ha | 1.2.6 | integrazione refoss |
| regenmaschine | 2024.3.0 | integrazione rainmachine |
| renault-api | 0.6.3 | integrazione renault |
| renson-endura-delta | 1.7.2 | integrazione renson |
| reolink-aio | 0.22.0 | integrazione reolink |
| requests | 2.34.2 | zwave-me-ws |
| requests-file | 3.0.1 | zeep |
| requests-oauthlib | 2.0.0 | total-connect-client |
| requests-toolbelt | 1.0.0 | webexpythonsdk |
| restrictedpython | 8.5 | integrazione python_script |
| retry2 | 0.9.5 | zeversolar |
| reverse-geocode | 1.6.5 | geocachingapi |
| rf-protocols | 4.3.0 | integrazione radio_frequency |
| rfk101py | 0.0.1 | integrazione idteck_prox |
| rflink | 0.0.68 | integrazione rflink |
| rich | 15.0.0 | pytrydan |
| ring-doorbell | 0.9.14 | integrazione ring |
| ritualsgenie | 0.1.0 | integrazione rituals_perfume_genie |
| rocketchat-api | 0.6.1 | integrazione rocketchat |
| rokuecp | 0.19.5 | integrazione roku |
| romy | 0.0.10 | integrazione romy |
| roombapy | 2.0.2 | integrazione roomba |
| roonapi | 0.1.6 | integrazione roon |
| rpi-bad-power | 0.1.0 | integrazione rpi_power |
| rsa | 4.9.1 | oauth2client |
| ruuvitag-ble | 0.4.0 | integrazione ruuvitag_ble |
| rxv | 0.7.0 | integrazione yamaha |
| s3transfer | 0.16.1 | boto3 |
| samsung-exlink | 1.2.1 | integrazione samsung_exlink |
| samsungtvws | 3.0.7 | integrazione samsungtv |
| sanix | 1.0.6 | integrazione sanix |
| satel-integra | 1.6.0 | integrazione satel_integra |
| scapy | 2.8.0 | aiodhcpwatcher |
| schedule | 1.2.2 | pysmappee |
| screenlogicpy | 0.10.2 | integrazione screenlogic |
| securetar | 2026.10.0 | integrazione backup |
| segno | 1.6.6 | aiovodafone |
| semver | 3.1.1 | vilfo-api-client |
| sendgrid | 6.8.2 | integrazione sendgrid |
| sense-energy | 0.15.0 | integrazione sense |
| sensecap-lorawan | 1.0.0 | integrazione sensecap_lorawan |
| sensirion-ble | 0.1.1 | integrazione sensirion_ble |
| sensor-state-data | 2.20.0 | xiaomi-ble |
| sensorpro-ble | 0.7.1 | integrazione sensorpro |
| sensorpush-api | 2.1.3 | integrazione sensorpush_cloud |
| sensorpush-ble | 1.9.0 | integrazione sensorpush |
| sensorpush-ha | 1.3.2 | integrazione sensorpush_cloud |
| sensoterra | 2.0.1 | integrazione sensoterra |
| sentence-stream | 1.3.0 | integrazione elevenlabs |
| sentry-sdk | 2.48.0 | integrazione sentry |
| sepaxml | 2.7.0 | fints |
| serialx | 1.15.1 | integrazione usb |
| setuptools | 84.2.1 | colorzero |
| sfrbox-api | 0.2.1 | integrazione sfr_box |
| sharkiq | 1.5.0 | integrazione sharkiq |
| shellingham | 1.5.4 | typer |
| shortuuid | 1.0.13 | wolf-comm |
| simple-websocket | 1.1.0 | python-engineio |
| simpleeval | 1.0.8 | pyimouapi |
| simplefin4py | 0.0.18 | integrazione simplefin |
| simplehound | 0.3 | integrazione sighthound |
| simplepush | 3.8.0 | integrazione simplepush |
| simplisafe-python | 2026.6.0 | integrazione simplisafe |
| siobrultech-protocols | 0.5.0 | greeneye-monitor |
| sisyphus-control | 3.1.4 | integrazione sisyphus |
| six | 1.17.0 | pysignalclirestapi |
| skoda-public-api | 1.4.0 | integrazione skoda |
| skyboxremote | 0.0.6 | integrazione sky_remote |
| slack-sdk | 3.33.4 | integrazione slack |
| sleekxmppfs | 1.4.1 | py-sucks |
| smart-meter-texas | 0.5.6 | integrazione smart_meter_texas |
| sniffio | 1.3.1 | openai |
| snitun | 0.47.0 | hass-nabucasa |
| soco | 0.32.1 | integrazione sonos |
| sofar-modbus | 0.25.3 | integrazione sofar |
| solaredge-web | 0.5.0 | integrazione solaredge |
| solaredged | 0.5.0 | integrazione solaredge_modbus |
| solarlog-cli | 0.7.1 | integrazione solarlog |
| solarman-opendata | 0.0.3 | integrazione solarman |
| solax | 3.2.4 | integrazione solax |
| solyx-energy-api-client | 0.2.0 | integrazione solyx_energy_cloud |
| sonos-websocket | 0.2.0 | integrazione sonos |
| sortedcontainers | 2.4.0 | blinkpy |
| soupsieve | 4.1 | beautifulsoup4 |
| speak2mary | 1.4.0 | integrazione marytts |
| specialized-turbo | 0.8.4 | integrazione specialized_turbo |
| speedtest-cli | 2.1.3 | integrazione speedtestdotnet |
| spinev-ble | 0.5.0 | integrazione exicom_spin_ble |
| spotifyaio | 2.1.0 | integrazione spotify |
| sqlalchemy | 2.0.54 | integrazione recorder |
| sqlparse | 0.6.0 | integrazione sql |
| srp | 1.0.22 | pyicloud |
| srpenergy | 1.3.8 | integrazione srp_energy |
| srptools | 2.0.0 | pyatv |
| sse-starlette | 3.7.0 | mcp |
| starkbank-ecdsa | 2.3.1 | sendgrid |
| starlette | 1.10.0 | mcp |
| starlingbank | 3.2 | integrazione starlingbank |
| starlink-grpc-core | 1.2.5 | integrazione starlink |
| statsd | 3.2.1 | integrazione statsd |
| steamloop | 1.2.1 | integrazione trane |
| steamodd | 5.0 | integrazione steam_online |
| stevedore | 5.10.0 | pynx584 |
| stookwijzer | 1.6.1 | integrazione stookwijzer |
| striprtf | 0.0.35 | xknxproject |
| subarulink | 0.7.21 | integrazione subaru |
| sunsynk-api-client | 1.4.0 | integrazione sunsynk |
| surepy | 0.10.0 | integrazione surepetcare |
| svgwrite | 1.4.3 | irm-kmi-api |
| swisshydrodata | 0.1.0 | integrazione swiss_hydrological_data |
| switchbot-api | 2.13.0 | integrazione switchbot_cloud |
| synology-srm | 0.2.0 | integrazione synology_srm |
| systembridgeconnector | 5.18.0 | integrazione system_bridge |
| tabulate | 0.10.0 | vtjp |
| tailscale | 1.0.0 | integrazione tailscale |
| tami4edgeapi | 3.0 | integrazione tami4 |
| tank-utility | 1.5.1 | integrazione tank_utility |
| telnetlib3 | 5.0.2 | nad-receiver |
| teltasync | 0.4.1 | integrazione teltonika |
| tempora | 5.13.0 | jaraco-logging |
| tenacity | 9.2.1 | pyzonneplan |
| tesla-fleet-api | 1.19.0 | integrazione tessie |
| tesla-protocol | 6.3.0 | tesla-fleet-api |
| tesla-wall-connector | 1.2.0 | integrazione tesla_wall_connector |
| teslemetry-stream | 1.3.0 | integrazione teslemetry |
| tessie-api | 0.1.3 | integrazione tessie |
| text-unidecode | 1.3 | sepaxml |
| theben-conexa-smgw | 0.4.3 | integrazione theben_conexa |
| thermobeacon-ble | 0.10.0 | integrazione thermobeacon |
| thermopro-ble | 1.1.4 | integrazione thermopro |
| thingspeak | 1.0.0 | integrazione thingspeak |
| thinqconnect | 1.0.14 | integrazione lg_thinq |
| tilt-ble | 1.0.1 | integrazione tilt_ble |
| tilt-pi | 0.2.1 | integrazione tilt_pi |
| tinyhtml | 1.3.0 | pyicloud |
| tinytag | 2.3.2 | pyatv |
| tld | 0.13.2 | whoisdomain |
| tmb | 0.0.4 | integrazione tmb |
| tmodbus | 1.1.0 | integrazione modbus |
| todoist-api-python | 3.1.0 | integrazione todoist |
| togrill-bluetooth | 0.9.0 | integrazione togrill |
| tololib | 1.2.2 | integrazione tolo |
| tomli | 2.5.0 | axis |
| tomli-w | 1.2.0 | axis |
| tonewinner-rs232 | 1.3.0 | integrazione tonewinner |
| toonapi | 0.3.0 | integrazione toon |
| total-connect-client | 2026.9 | integrazione totalconnect |
| tplink-omada-client | 1.5.13 | integrazione tplink_omada |
| tqdm | 4.70.1 | universal-silabs-flasher |
| transitions | 0.8.11 | pysnooz |
| transmission-rpc | 7.0.3 | integrazione transmission |
| triggercmd | 0.0.36 | integrazione triggercmd |
| trmnl | 0.1.1 | integrazione trmnl |
| truststore | 0.10.5 | httpx2 |
| ttls | 1.11.1 | integrazione twinkly |
| ttn-client | 1.3.0 | integrazione thethingsnetwork |
| tuya-device-handlers | 0.0.34 | integrazione tuya |
| tuya-device-sharing-sdk | 0.2.15 | integrazione tuya |
| twentemilieu | 3.0.0 | integrazione twentemilieu |
| twitchapi | 4.2.1 | integrazione twitch |
| typeguard | 4.6.0 | marshmallow-dataclass |
| typer | 0.29.1 | pytrydan |
| typer-slim | 0.24.0 | jaraco-text |
| types-pytz | 2026.6.0.20261031 | pyefergy |
| typing-extensions | 4.16.0 | serialx |
| typing-inspect | 0.9.0 | dataclasses-json |
| typing-inspection | 0.4.5 | mcp |
| tzdata | 2026.6 | ical |
| tzlocal | 5.4.5 | melnor-bluetooth |
| uart-devices | 0.1.1 | bluetooth-adapters |
| uasiren | 0.0.1 | integrazione ukraine_alarm |
| uhooapi | 1.2.8 | integrazione uhoo |
| uiprotect | 25.0.0 | integrazione unifiprotect |
| ultraheat-api | 0.6.1 | integrazione landisgyr_heat_meter |
| umodbus | 1.0.4 | async-modbus |
| unicode-rbnf | 2.4.1 | gazetteer-matcher |
| unifi-ap | 0.0.2 | integrazione unifi_direct |
| unifi-discovery | 1.5.0 | integrazione unifi_discovery |
| universal-silabs-flasher | 1.3.0 | integrazione homeassistant_hardware |
| unpaddedbase64 | 2.1.0 | matrix-nio |
| untangle | 1.3.0 | pyqvrpro |
| upb-lib | 0.7.2 | integrazione upb |
| upcloud-api | 2.9.0 | integrazione upcloud |
| update-checker | 1.0.1 | praw |
| uritemplate | 4.2.0 | google-api-python-client |
| url-normalize | 3.0.1 | integrazione zwave_me |
| urllib3 | 2.8.0 | xs1-api-client |
| urllib3-future | 2.25.906 | niquests |
| usb-devices | 0.5.1 | bluetooth-adapters |
| uvcclient | 0.12.1 | integrazione uvc |
| uvicorn | 0.54.0 | mcp |
| vacuum-map-parser-base | 0.1.5 | vacuum-map-parser-roborock |
| vacuum-map-parser-roborock | 0.1.5 | integrazione roborock |
| validators | 0.36.0 | dremel3dpy |
| vallox-websocket-api | 6.1.0 | integrazione vallox |
| vegehub | 0.1.26 | integrazione vegehub |
| vehicle | 3.0.0 | integrazione rdw |
| velbus-aio | 2026.11.0 | integrazione velbus |
| venstarcolortouch | 0.22 | integrazione venstar |
| viaggiatreno-ha | 0.2.4 | integrazione viaggiatreno |
| victron-ble | 0.9.3 | victron-ble-ha-parser |
| victron-ble-ha-parser | 0.8.0 | integrazione victron_ble |
| victron-mqtt | 2026.10.3 | integrazione victron_gx |
| victron-vrm | 0.1.14 | integrazione victron_remote_monitoring |
| vilfo-api-client | 0.5.0 | integrazione vilfo |
| virtualenv | 21.15.6 | pre-commit |
| visionpluspython | 1.1.0 | integrazione watts |
| vizaio | 0.7.0 | integrazione vizio |
| vobject | 0.9.9 | integrazione caldav |
| voip-utils | 0.5.1 | integrazione voip |
| voluptuous | 0.16.0 | async-upnp-client |
| volvocarsapi | 0.5.0 | integrazione volvo |
| vtjp | 0.2.1 | integrazione vasttrafik |
| wakeonlan | 3.3.0 | integrazione wake_on_lan |
| wallbox | 0.9.0 | integrazione wallbox |
| wassima | 2.1.5 | niquests |
| waterfurnace | 1.10.0 | integrazione waterfurnace |
| watergate-local-api | 2026.2.2 | integrazione watergate |
| wcwidth | 0.9.4 | prettytable |
| weatherflow4py | 1.5.12 | integrazione weatherflow_cloud |
| webcolors | 25.10.0 | flux-led |
| webencodings | 0.6.1 | bleach |
| webexpythonsdk | 2.0.6 | integrazione cisco_webex_teams |
| webmin-xmlrpc | 0.0.2 | integrazione webmin |
| webrtc-models | 0.3.0 | hass-nabucasa |
| websocket-client | 1.9.2 | zwave-me-ws |
| websockets | 17.4 | whirlpool-sixth-sense |
| weheat | 2026.9.3 | integrazione weheat |
| werkzeug | 3.1.9 | flask |
| whirlpool-sixth-sense | 2.1.0 | integrazione whirlpool |
| whodap | 0.2.1 | whoisdomain |
| whoisdomain | 2.20260806.3 | integrazione whois |
| wiffi | 1.1.2 | integrazione wiffi |
| wiim | 0.4.0 | integrazione wiim |
| wirelesstagpy | 0.8.1 | integrazione wirelesstag |
| wled | 1.1.1 | integrazione wled |
| wolf-comm | 0.0.52 | integrazione wolflink |
| wrapt | 2.5.2 | aiobotocore |
| wsdot | 0.0.1 | integrazione wsdot |
| wsproto | 1.3.2 | httpx-ws |
| wyoming | 1.10.2 | integrazione wyoming |
| x-wr-timezone | 2.0.1 | recurring-ical-events |
| xiaomi-ble | 1.21.2 | integrazione xiaomi_ble |
| xiaomi-weather | 0.1.0 | integrazione xiaomi_weather |
| xknx | 4.1.1 | integrazione knx |
| xknxproject | 3.10.0 | integrazione knx |
| xmlschema | 4.3.2 | sepaxml |
| xmltodict | 1.0.4 | integrazione ted5000 |
| xs1-api-client | 3.0.0 | integrazione xs1 |
| yagrc | 1.1.3 | starlink-grpc-core |
| yalesmartalarmclient | 0.4.3 | integrazione yale_smart_alarm |
| yalexs | 9.2.13 | integrazione yale |
| yalexs-ble | 4.1.2 | integrazione yalexs_ble |
| yarl | 1.25.1 | pyzonneplan |
| yeelight | 0.7.16 | integrazione yeelight |
| yolink-api | 0.6.5 | integrazione yolink |
| yoto-api | 4.5.1 | integrazione yoto |
| youless-api | 2.2.0 | integrazione youless |
| youtubeaio | 2.1.2 | integrazione youtube |
| yt-dlp | 2026.7.4 | integrazione media_extractor |
| zabbix-utils | 2.0.4 | integrazione zabbix |
| zamg | 0.4.1 | integrazione zamg |
| zcc-helper | 3.8 | integrazione zimi |
| zeep | 4.3.3 | onvif-zeep-async |
| zeversolar | 0.3.2 | integrazione zeversolar |
| zha | 2.4.3 | integrazione zha |
| zha-quirks | 2.4.1 | integrazione zha |
| zhong-hong-hvac | 1.0.21 | integrazione zhong_hong |
| ziggo-mediabox-xl | 1.1.0 | integrazione ziggo_mediabox_xl |
| zigpy | 2.4.1 | zha |
| zigpy-deconz | 1.1.1 | zha |
| zigpy-xbee | 0.22.0 | zha |
| zigpy-zigate | 0.14.0 | zha |
| zigpy-ziggurat | 1.2.1 | zha |
| zigpy-znp | 1.1.1 | zha |
| zinvolt | 1.1.1 | integrazione zinvolt |
| zm-py | 0.5.12 | integrazione zoneminder |
| zwave-js-server-python | 0.74.0 | integrazione zwave_js |
| zwave-me-ws | 0.4.3 | integrazione zwave_me |

