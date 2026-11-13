# 0002 · M1 Server affidabile

**Obiettivo:** HAndroid fa da server senza interventi: riparte da solo, installa le integrazioni che aggiungi, si aggiorna e salva i backup. Gate: 7 giorni di fila senza interventi manuali sul 12T.

## Requisiti

- Le integrazioni si aggiungono dall'interfaccia di HA anche se non sono nell'APK.
- HA riparte dopo l'accensione del telefono, dopo un aggiornamento dell'app, dopo "Riavvia" dall'interfaccia, dopo un crash e se smette di rispondere.
- Un errore che si ripete non manda il telefono in un ciclo di riavvii.
- I backup si possono portare fuori dalla memoria privata dell'app.

## Come funziona

1. **Pacchetti a runtime.** `handroid_boot.py` sostituisce `homeassistant.util.package.install_package`: al posto di uv usa pip, lanciato con `libhpython.so`, un eseguibile `python` compilato dall'NDK e messo tra le librerie native dell'APK (l'unica cartella da cui Android lascia eseguire programmi). I pacchetti vanno in `ha-config/deps` (cartella utente di Python, `--user`), in testa a `sys.path`.
2. **Wheel nativi.** pip sul telefono non compila codice C. I pacchetti nativi delle integrazioni più comuni (`scripts/integrazioni-indice.txt` → `docs/dipendenze-indice.md`) si compilano in WSL e vanno nell'APK, in `python/wheels`; pip li cerca lì e poi nell'indice online (pubblicato con i rilasci, M4).
3. **Supervisor** (`Supervisor.kt`, `HaService.kt`):
   - codice di uscita 100 ("Riavvia" dall'interfaccia): riparte dopo 1 secondo;
   - codice 0 ("Ferma"): resta fermo e l'avvio automatico si spegne finché non lo riavvii dall'app;
   - altro codice, o 3 controlli falliti di fila sul watchdog (uno ogni 30 secondi): crash, riparte dopo 10 secondi;
   - processo ucciso di colpo: nessuno chiama il riavvio e MIUI ignora `START_STICKY`, quindi il watchdog tiene una sveglia 2 minuti avanti; se scatta, il servizio riparte e il file `in-corso` rimasto conta un crash;
   - 3 crash in 15 minuti: riparte con `--recovery-mode`; il conteggio si azzera dopo 10 minuti di funzionamento regolare.
4. **Riavvio del processo.** HA gira nel processo `:ha`, che muore a ogni giro (Python non si reinizializza). Il riavvio passa da una sveglia esatta di `AlarmManager`; da Android 12 un servizio in primo piano parte da dietro le quinte solo così, o se l'app è esclusa dall'ottimizzazione batteria (l'app lo chiede).
5. **Avvio automatico** (`BootReceiver`): `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED`.
6. **Rete:** WifiLock ad alte prestazioni e MulticastLock (mDNS) finché HA gira.
7. **Device owner** (`AdminReceiver`): permessi concessi da soli, aggiornamenti di sistema solo tra le 2 e le 5, niente schermata di blocco.
8. **Backup:** li crea Home Assistant (integrazione `backup`); l'app copia l'ultimo in `Download/HAndroid`.
9. **Aggiornamenti:** l'APK contiene anche HA. L'app confronta la sua versione con l'ultimo rilascio su GitHub e apre il download.

## Sicurezza

- pip scarica da PyPI e dal nostro indice su HTTPS; i pacchetti girano con i permessi dell'app, come quelli dell'APK.
- `ha-config/deps` sta nella memoria privata dell'app.

## Decisioni

- **pip e non uv.** uv è un programma Rust che dovremmo compilare e firmare dentro l'APK; pip è già Python.
- **Wheel dentro l'APK prima dell'indice online.** Funziona anche senza rete e senza aver pubblicato niente; pesa circa 15 MB.
- **`.pyc` precompilati.** Il primo avvio non compila migliaia di file: l'APK cresce, ma HA parte in meno della metà del tempo. `unchecked-hash` perché l'estrazione cambia le date dei sorgenti.
- **Stato condiviso via file** (`avvio-automatico`, `crash.txt`, `stato.txt`): i due processi dell'app non condividono le SharedPreferences.
- **Bluetooth senza adattatori.** BlueZ non esiste su Android: `handroid_boot.py` dà a `bluetooth_adapters` un elenco vuoto, così `bluetooth` parte e le integrazioni che ne dipendono (es. Shelly) funzionano via rete. Il Bluetooth vero è M2.

## Conseguenze

- Il primo uso di un'integrazione richiede rete e qualche secondo (sull'emulatore minuti) per pip.
- Un pacchetto nativo che non è nell'indice fa fallire l'installazione dell'integrazione, con un errore nei log `[handroid]`.
- Il gate dei 7 giorni va verificato sul telefono: il codice non lo dimostra da solo.
