# 0010 · Backup fuori dal telefono e avviso se il server si ferma

19 novembre 2026. Versione 0.6.0 ([piano-versioni.md](../piano-versioni.md)).

## Requisiti

- I backup di HA escono dal telefono da soli: ogni ora l'ultimo `.tar` non ancora copiato va nella cartella scelta dall'utente; nella cartella restano gli ultimi 7.
- "Ripristina un backup da file": un `.tar` scelto dal telefono entra tra i backup di HA, HA riparte e lo si ripristina da Impostazioni → Sistema → Backup (o dall'onboarding su un telefono nuovo).
- Avviso se il server si ferma: finché HA risponde, ogni 5 minuti una chiamata a un URL di controllo (healthchecks.io o simili). Se le chiamate smettono, il servizio esterno avvisa.

## Dati

- `files/backup-cartella.txt`: URI della cartella (selettore di Android, permesso persistente). `files/backup-copiato.txt`: ultimo backup copiato.
- `files/controllo-url.txt`: URL di controllo.

## Sicurezza

- I backup di HA contengono password e token: la cartella la sceglie l'utente; per Drive vale la sicurezza del suo account. Si può attivare la cifratura dei backup in HA (chiave di backup).
- L'URL di controllo non porta dati: solo il fatto che il server è vivo.

## Decisioni

| # | Decisione | Perché | Conseguenze |
| --- | --- | --- | --- |
| K1 | **Selettore di Android (SAF)** invece di protocolli propri (SMB, Drive API). | Un solo codice per Drive, cartelle di rete (con un'app che le espone) e chiavette; niente credenziali nell'app. | Dipende dall'app che offre la cartella: con alcune la scrittura in background si interrompe. |
| K2 | **Copia dal watchdog del processo `:ha`**, una volta all'ora. | È già sveglio ogni 30 s; niente WorkManager né nuove dipendenze. | Con HA fermo non si copia (ma senza HA non nascono backup nuovi). |
| K3 | **Avviso con un servizio esterno a "ping"** invece di un nostro server. | Se il telefono è spento non può avvisare da solo: serve qualcuno fuori. Gratis e già pronto. | Lo stesso servizio diventerà nostro con l'abbonamento (roadmap, 1.0.0). |
