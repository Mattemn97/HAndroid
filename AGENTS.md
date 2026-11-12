# Nome del progetto

<!-- Compila le parti tra parentesi e cancella questi commenti. Un modello completo è in esempi/ del kit. -->

## Stack

- **Linguaggio**: (es. JavaScript, Node.js)
- **Framework**: (es. Vue 3)
- **Dipendenze chiave**: (es. Firebase, Tailwind)

## Comandi

```bash
# Installazione
# Avvio in locale
# Test
# Build
# Deploy
```

## Regole del progetto

<!-- Solo quello che non si capisce leggendo il codice. Ogni regola dice cosa fare e cosa si rompe altrimenti.
Di solito valgono: cosa va aggiornato insieme, dove sta una certa logica, cosa non va toccato, cosa viene controllato da un test.
Esempio: "Un campo nuovo del profilo va aggiunto anche alle regole del database, altrimenti il salvataggio viene rifiutato." -->

## Specifiche

Stanno in `docs/specs/NNNN-titolo.md`: requisiti, dati, sicurezza, decisioni e conseguenze, in poche righe.

## Come lavorare

- **Leggi poco e bene.** Cerca prima di aprire, poi leggi solo le parti che servono. Non rileggere quello che hai già in conversazione.
- **Raggruppa.** Le letture indipendenti vanno in parallelo, i controlli piccoli in un solo comando. Filtra l'output di test e build: errori e totali, non tutto il log.
- **Niente subagenti** se non li chiedo: ripartono da zero e costano molto.
- **Decidi le cose piccole.** Se una scelta ha un default ragionevole, prendilo e segnalalo nel report. Chiedi solo se la risposta cambia davvero il lavoro.
- **Finisci il lavoro.** Non fermarti a metà per proporre: implementa, verifica, poi riferisci.
- **Resta nel perimetro.** Fai quello che ho chiesto; le idee in più scrivile in una riga, senza realizzarle.
- **Segui il codice che c'è.** Stessi nomi, stesso stile, stessa densità di commenti. Riusa quello che esiste.
- **Di' come stanno le cose.** Un test fallito o un passaggio saltato si dichiarano. Afferma solo ciò che hai controllato.
- **Commit, push, merge e deploy solo se te lo chiedo.** Un permesso vale per quella volta.
- **Scrivimi semplice.** Prima il risultato, poi il resto, senza gergo inutile. Durante il lavoro bastano aggiornamenti di una riga.
- **Skill di processo solo a richiesta.** `/scope`, `/architect`, `/develop`, `/check`, `/test`, `/debug`, `/document`, `/sync` e `/audit` partono quando le scrivo: sono più complete, ma consumano di più. Per il lavoro normale bastano `implementa`, `pubblica` e `proponi`.
- **Tieni vivo questo file.** Quando scopri una regola non ovvia, aggiungila in una riga nella sezione giusta.
