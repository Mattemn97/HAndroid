# 0006 · Supervisore: server affidabile (M1)

12 novembre 2026. Stato: Proposta. Si appoggia su [0002](0002-home-assistant-core.md) (M0b).

## Perimetro

- Dentro: backup notturno con rotazione e backup manuale; aggiornamento di HA dall'app con rollback; avviso di nuovo APK; schermata nativa di stato; guida all'ottimizzazione batteria e all'avvio automatico Xiaomi.
- Fuori: ripristino dall'app (si fa dall'interfaccia di HA), installazione silenziosa dell'APK, backup cifrati.
- Fatto quando (gate M1): 7 giorni di funzionamento senza interventi, con 7 backup notturni in `config/backups`.

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| F1 | **Backup in formato HA**: `.tar` con `backup.json` e `homeassistant.tar.gz` (config sotto `data/`), scritto in `config/backups` da un tar minimo in Kotlin (`Backup.kt`). Esclusi `backups`, `tts`, `deps`, `-wal`/`-shm`. | HA vede i backup nella sua pagina e li può ripristinare; niente librerie. | Il database SQLite si copia a caldo: in rari casi l'ultima scrittura può mancare. Formato non ancora provato con il ripristino di HA. |
| F2 | **Pianificazione**: `AlarmManager.setAndAllowWhileIdle` alle 3:30, riarmato a ogni esecuzione e a ogni avvio di `HaService`; il lavoro gira in `SupervisoreReceiver` nel processo `:ha`, tenuto vivo dal servizio in primo piano. | Niente WorkManager né permesso di allarmi esatti; il ritardo di Doze (minuti) va bene. | Il backup può partire qualche minuto dopo le 3:30. |
| F3 | **Rotazione**: si tengono gli ultimi 7 `handroid_*.tar` (notturni, manuali e pre-aggiornamento insieme); i backup creati da HA non si toccano. | Semplice e prevedibile. | Molti backup manuali in un giorno spingono fuori i notturni. |
| F4 | **Copia esterna opzionale** in una cartella scelta con `ACTION_OPEN_DOCUMENT_TREE` (permesso persistente). | La cartella dati non è leggibile dall'utente. | Se la copia fallisce resta il backup interno e una riga nel registro. |
| F5 | **Aggiornamento di HA**: controllo su PyPI; l'app fa un backup, scrive la versione in `aggiorna_ha` e riavvia. All'avvio `handroid_main.py` installa `homeassistant==V` con pip in `ha.nuovo`, poi scambia: `ha` → `ha.prev`, `ha.nuovo` → `ha`, e crea `ha.prova`. `ha` sta in testa a `sys.path` (vince su site-packages e site.zip). | Lo scambio di cartelle rende il rollback istantaneo; se pip fallisce non cambia niente. | Serve spazio per due copie di HA durante la prova. Le dipendenze native devono esistere nell'indice `wheels`. |
| F6 | **Rollback**: `Regole.decidiAvvio` — con `ha.prova` presente e 3 avvii falliti di fila si ripristina `ha.prev` invece della modalità provvisoria. Quando il watchdog vede HA rispondere, `ha.prova` e `ha.prev` si cancellano. | Riusa contatore e watchdog esistenti. | Il rollback arriva dopo al massimo 3 cicli del watchdog (≈ 3 × 6,5 min). |
| F7 | **APK**: controllo dell'ultima release GitHub e link alla pagina. L'installazione silenziosa da device owner (`PackageInstaller` + `setRequireUserAction`/sessione da owner) è possibile ma **non realizzata**: richiede scaricare l'APK, verificarne la firma e gestire il riavvio del processo durante l'installazione. | Rischio alto per il gate M1, guadagno basso (aggiornamenti rari). | Valutare in M2. |
| F8 | **Schermata di stato** `StatoActivity` (View native): stato, IP e URL `:8123`, versioni, ultimo backup, pulsanti backup/aggiorna/riavvia/cartella, registro `supervisore.log` + logcat. Si apre toccando la notifica del server o tenendo premuto Indietro nella WebView, che resta la schermata principale. | Niente AndroidX; nessuna interferenza con l'interfaccia di HA. | — |
| F9 | **Batteria**: se non device owner, pulsanti per `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` e per l'avvio automatico MIUI (`com.miui.securitycenter/...AutoStartManagementActivity`, ripiego sui dettagli dell'app). | Su HyperOS senza questi passi il servizio viene chiuso. | — |
| F10 | **Stato tra processi in file** nella cartella base (`supervisore.log`, `versione_ha`, `cartella_esterna`, `aggiorna_ha`, `ha.prova`). | Le SharedPreferences sono in cache per processo. | — |

## Da verificare

- Ripristino di un backup HAndroid dalla pagina Backup di HA.
- Aggiornamento reale di HA con pip in-process sul telefono (tempo, spazio, dipendenze native).
- Allarme alle 3:30 in Doze profondo su HyperOS.
