# 0007 · Rilascio (M4)

13 novembre 2026. Stato: primo rilascio pubblicato il 14 novembre, [v0.3.0-m3](https://github.com/Mattemn97/HAndroid/releases/tag/v0.3.0-m3).

## Requisiti

- Un APK firmato per ogni versione, scaricabile da GitHub, che l'app riconosce come aggiornamento ("Cerca aggiornamenti" confronta `versionName` con il tag dell'ultimo rilascio).
- Installazione dal PC in un comando, anche su MIUI senza SIM: `scripts/installa-handroid.sh`.
- Elenco delle integrazioni provate davvero sul telefono: [integrazioni-testate.md](../integrazioni-testate.md).

## Come si fa

1. `scripts/prepara-python-android.sh` (WSL) per `python.zip`.
2. `versionName` e `versionCode` in `app/build.gradle.kts`, voce nel registro, commit.
3. `scripts/rilascio.sh`: test, APK firmato in `build/rilascio/`, SHA-256, note dalla voce più recente del registro. Con `PUBBLICA=1` crea il tag `v<versione>` e il rilascio su GitHub, segnato come ultimo.
4. `PUBBLICA=1 scripts/pubblica-indice.sh` se sono cambiati wheel, add-on o pagina.

## Sicurezza

- Chiave di firma in `%USERPROFILE%\.handroid\handroid-rilascio.jks`, password in `keystore.properties` (fuori da git, copia accanto alla chiave). Va salvata anche altrove: senza la stessa chiave nessun APK aggiorna quello installato.
- L'APK di debug e quello di rilascio hanno firme diverse: per passare dall'uno all'altro serve disinstallare, cioè perdere la configurazione di HA. Prima si fa un backup da HA e lo si copia fuori ("Copia l'ultimo backup in Download").

## Decisioni

| # | Decisione | Perché | Conseguenza |
|---|---|---|---|
| R1 | **Rilascio costruito sul PC, non in CI.** | `python.zip` richiede i wheel compilati in WSL con cibuildwheel (ore la prima volta) e la CI di GitHub dovrebbe rifare tutto a ogni rilascio. | Lo script controlla che non ci siano modifiche non salvate e che il tag non esista. |
| R2 | **Niente F-Droid ufficiale per ora.** | F-Droid compila dal sorgente sui suoi server e non accetta binari già pronti: qui dentro ci sono CPython, 140 MB di pacchetti Python e wheel nativi. Andrebbe ricostruito tutto nella loro ricetta. | Distribuzione da GitHub Releases, aggiornabile con l'app stessa o con Obtainium. IzzyOnDroid (accetta APK già firmati) è il passo successivo, dopo qualche rilascio stabile. |
| R3 | **Versioni `0.x.0-mN` fino alla M4 compresa, pubblicate come rilascio normale.** | L'app e l'installer leggono `/releases/latest`, che salta le pre-release; il suffisso dice già che è una versione di prova. | `1.0.0` quando il 12T regge una settimana senza interventi. Il vecchio `v0.1.2` della sessione notturna (app scheletro) resta su GitHub, non più come ultimo. |
