---
name: implementa
description: Porta una richiesta dall'idea alla funzione finita e verificata, spendendo pochi token. Usala ogni volta che l'utente chiede di aggiungere, modificare o sistemare una funzione del progetto, anche se la richiesta è breve o informale ("fai in modo che...", "aggiungi...", "non funziona...", "procedi"), e quando chiede di fare più funzioni insieme.
---

# Implementa

Il costo di una sessione sta soprattutto in quello che si legge e si rilegge. L'obiettivo è capire il minimo necessario, fare tutto il lavoro in pochi passaggi larghi e dimostrare che funziona.

## 1. Capisci in un colpo solo

Leggi le regole del progetto che hai già in contesto: spesso dicono cosa va aggiornato insieme (permessi, test finti, elenchi, documentazione). Poi cerca con un conteggio dove vive la funzione e apri solo quei file, tutti nello stesso turno. Per i file grandi leggi le righe che servono.

Prima di progettare controlla tre cose, perché sono quelle che rompono il lavoro dopo:

- la forma reale dei dati, guardando un esempio vero invece di immaginarla;
- chi altro usa il codice che stai per cambiare;
- cosa succede ai dati già esistenti.

## 2. Decidi senza fermarti

Prendi le decisioni piccole da solo e annotale per il report. Fermati a chiedere solo se la scelta è costosa da cambiare e nessun default è ragionevole. Se l'utente ha chiesto più funzioni, progettale insieme: condividono letture e verifiche.

Le scelte con conseguenze vanno prese con prudenza. Se un dato sbagliato può fare danni (salute, legge, soldi), lascialo decidere a una persona invece di dedurlo.

## 3. Scrivi tutto in un passaggio

Crea un branch prima di modificare, se sei sul ramo principale. I file nuovi si scrivono direttamente. Quando le modifiche toccano molti file, usa un solo script di sostituzioni esatte:

```js
import fs from 'fs'
const edit = (file, pairs) => {
  let text = fs.readFileSync(file, 'utf8')
  const crlf = text.includes('\r\n')
  if (crlf) text = text.replace(/\r\n/g, '\n')
  for (const [from, to] of pairs) {
    if (!text.includes(from)) throw new Error(`${file}: non trovo ${from.slice(0, 50)}`)
    text = text.replace(from, () => to)
  }
  fs.writeFileSync(file, crlf ? text.replace(/\n/g, '\r\n') : text)
}
```

Si ferma appena un testo non combacia, quindi non lascia modifiche silenziosamente saltate, e rispetta gli a capo del file. Salvalo con lo strumento di scrittura dei file, fuori dalla repo: gli script lunghi passati nella shell possono troncarsi.

Scrivi i test insieme al codice e aggiorna regole e documentazione del progetto nello stesso giro. Per la documentazione bastano poche righe: il perché delle scelte, non la descrizione del codice.

## 4. Verifica davvero

Esegui lint, test e build in un comando solo, filtrando l'output su errori e totali. Se qualcosa fallisce, guarda solo quel caso, correggi e rilancia. Alla fine passa tutta la suite una volta.

I test verdi non bastano sempre:

- per una logica nuova, prova esempi che sai giudicare e guarda se il risultato ha senso;
- per una pagina, guarda uno screenshot nel formato più scomodo;
- per dati o permessi, prova anche i casi che devono fallire.

Togli sempre i file e le modifiche temporanee usati per controllare.

## 5. Riferisci

Prima frase: il risultato, compreso se è online o no. Poi, senza gergo:

- cosa fa ora l'utente finale;
- le scelte prese da te, da controllare;
- cosa non hai verificato o è rimasto fuori;
- il prossimo comando o la decisione che serve.

Non fare commit, push o merge se non sono stati chiesti.
