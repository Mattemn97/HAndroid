# Usare Home Assistant da fuori casa

Passo passo per aprire HAndroid quando non sei sulla rete di casa, senza aprire porte sul router. Si usa [Tailscale](https://tailscale.com): una rete privata tra i tuoi dispositivi, gratuita per uso personale (fino a 100 dispositivi).

In alternativa c'è il cloud di Nabu Casa (a pagamento, si attiva da Impostazioni → Home Assistant Cloud): dalla 0.5.0 HAndroid include grpcio, che gli serve.

## Cosa serve

- Il telefono server con HAndroid acceso e collegato al Wi-Fi di casa.
- Il telefono (o PC) da cui vuoi usare HA da fuori.
- Un account Google, Microsoft, Apple o GitHub per entrare in Tailscale. Lo stesso account su tutti i dispositivi.

## 1. Tailscale sul telefono server

1. Installa Tailscale: dal Play Store, oppure scarica l'APK da [tailscale.com/download/android](https://tailscale.com/download/android) se il telefono non ha account Google.
2. Apri Tailscale, tocca **Accedi** ed entra con il tuo account.
3. Accetta la richiesta di Android di creare una **connessione VPN**.
4. Impostazioni Android → **VPN** → ingranaggio accanto a Tailscale → attiva **VPN sempre attiva**. Così si ricollega da solo dopo un riavvio, come HA.
5. Su MIUI: Impostazioni → App → Tailscale → **Avvio automatico** attivo e **Risparmio batteria: nessuna restrizione**.
6. In Tailscale, sotto il nome del telefono, c'è il suo indirizzo: `100.x.y.z`. Annotalo.

Scorciatoia: in HAndroid tocca **Usa da fuori casa**. Se Tailscale manca ti porta al download, se è spento lo apre, se è acceso ti fa condividere l'indirizzo (es. su WhatsApp verso il telefono personale). La schermata dell'app mostra sempre l'indirizzo alla riga "Da fuori casa".

## 2. Tailscale sul telefono personale

1. Installa Tailscale dal Play Store (o dall'App Store) ed entra con **lo stesso account**.
2. Nell'elenco dei dispositivi compare il telefono server con l'indirizzo del punto 1.6.
3. Prova: con il Wi-Fi spento (solo dati mobili) e Tailscale acceso, apri `http://100.x.y.z:8123` nel browser. Deve comparire HA.

## 3. Indirizzi in Home Assistant

Dalla 0.5.0 non serve: se l'URL Internet è vuoto, HAndroid ci scrive da solo l'indirizzo Tailscale al successivo avvio di HA (riavvialo dopo aver acceso Tailscale). Un URL scritto da te non viene toccato. A mano:

1. In HA: Impostazioni → Sistema → **Rete**.
2. **URL Internet**: scrivi `http://100.x.y.z:8123` (togli la spunta "Usa Home Assistant Cloud" se c'è).
3. **URL rete locale**: lascia quello di casa, es. `http://192.168.1.36:8123`.
4. Salva.

Se i campi sono grigi, HAndroid è precedente alla 0.3.1: aggiorna l'app.

## 4. App Companion

1. Sul telefono personale installa **Home Assistant** (app Companion) e aggiungi il server con l'indirizzo di casa.
2. Impostazioni dell'app → Companion app → il tuo server:
   - **URL Home Assistant** (esterno): `http://100.x.y.z:8123`
   - **URL interno**: quello di casa, e aggiungi il nome del Wi-Fi di casa in **SSID della rete domestica**.

L'app usa da sola l'indirizzo di casa quando sei sul tuo Wi-Fi e quello di Tailscale quando sei fuori.

## 5. Nome comodo (facoltativo)

Nella console di Tailscale ([login.tailscale.com](https://login.tailscale.com/admin/dns)) attiva **MagicDNS**: il server diventa raggiungibile anche con il suo nome, es. `http://xiaomi-12t:8123`, al posto dell'indirizzo numerico.

## Se non funziona

| Problema | Cosa fare |
| --- | --- |
| Da fuori la pagina non si apre | Tailscale acceso su entrambi i dispositivi? Nell'app il server deve risultare **Connected**. |
| Funziona per un po', poi smette | Batteria e avvio automatico di Tailscale sul server (punto 1.5), e **VPN sempre attiva** (punto 1.4). |
| Si apre solo a casa | L'URL Internet in HA o nell'app Companion è ancora quello locale (`192.168.…`). |
| Integrazioni con login (es. Xiaomi Home) | Il login va fatto dal PC di casa: il ritorno su `homeassistant.local` funziona solo sulla rete locale (vedi la guida, "Hardware del telefono"). |

Tailscale cifra il traffico tra i dispositivi: HA non è esposto su Internet, lo vedono solo i dispositivi del tuo account.
