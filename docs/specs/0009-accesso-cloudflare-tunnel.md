# 0009 · Accesso da fuori casa con Cloudflare Tunnel

18 novembre 2026. Alternativa gratuita a Tailscale ([accesso-remoto.md](../accesso-remoto.md)) e a Nabu Casa.

## Requisiti

- Da qualsiasi browser o dall'app Companion, senza app sul telefono personale e senza porte aperte: `https://<nome>.<dominio>` porta a HA.
- Configurazione dall'app: si incolla il token del tunnel (o il comando intero di Cloudflare); campo vuoto = tunnel spento.
- Il tunnel si ricollega da solo (attesa da 10 s a 5 min) e vive quanto il processo `:ha`.
- La schermata dell'app mostra lo stato: spento, in collegamento, collegato, token rifiutato.

## Dati

- `files/cloudflare-token.txt`: il token, nella memoria privata dell'app. `files/tunnel-stato.txt`: ultimo stato.
- Il dominio e il nome host si scelgono nella dashboard di Cloudflare, non nell'app.

## Sicurezza

- HA diventa raggiungibile da Internet: la protezione è il login di HA (con 2FA facoltativa) più HTTPS di Cloudflare.
- Il token è un segreto: passa a cloudflared nella variabile `TUNNEL_TOKEN`, non negli argomenti (visibili in `ps`).
- HA si fida di `X-Forwarded-For` solo da 127.0.0.1 e ::1, e solo con un token salvato.

## Decisioni

| # | Decisione | Perché | Conseguenze |
| --- | --- | --- | --- |
| T1 | **cloudflared compilato per `GOOS=android` con cgo** (`prepara-wsl.sh`), nell'APK come `libcloudflared.so`. | Quello ufficiale per Linux cerca i DNS in `/etc/resolv.conf`, che Android non ha. Android esegue programmi solo da jniLibs. | Circa 12 MB in più nell'APK. Nuova versione: cambia `CLOUDFLARED` nello script e cancella `~/android-deps/.../bin/cloudflared`. |
| T2 | **Tunnel gestito dalla dashboard (token)**, niente login né file di configurazione. | Un solo campo nell'app; i nomi host si cambiano senza toccare il telefono. | Serve un dominio su Cloudflare: i tunnel rapidi senza dominio cambiano indirizzo a ogni avvio. |
| T3 | **Proxy fidati impostati da `handroid_boot.py`** se `configuration.yaml` non ne parla. | Senza, HA risponde 400 alle richieste inoltrate. | Una sezione `http:` con `use_x_forwarded_for` scritta dall'utente vince. |

## Conseguenze

- Da provare sul 12T: collegamento, ricollegamento dopo un cambio di rete, app Companion via tunnel.
