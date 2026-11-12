---
name: pubblica
description: Chiude il lavoro con commit, push, pull request ed eventuale merge, in un solo comando, e passa il deploy all'utente con i controlli da fare. Usala quando l'utente dice "fai il commit", "apri la PR", "mergia", "chiudi", "pubblica", anche in una frase sola o come risposta breve a una domanda precedente.
---

# Pubblica

Fai esattamente quello che l'utente ha autorizzato: "fai il commit" non comprende il merge, e un permesso dato prima non vale per il lavoro nuovo.

## Prima

Se dall'ultima verifica il codice è cambiato, rilancia i controlli. Se l'ultima verifica è ancora valida, non ripeterla.

Aggiungi i file per nome o per cartella e guarda cosa resta fuori: log, file temporanei e segreti non vanno nel commit. Non saltare mai i controlli automatici del commit. Se ne fallisce uno, sistema la causa.

## Il comando

Unisci commit, push, creazione della PR, merge e allineamento del ramo principale in un solo comando, includendo solo i passaggi autorizzati. Controlla lo stato finale nello stesso comando: stato della PR, ultimo commit, ramo pulito.

Scrivi messaggi di commit e descrizione della PR nello stile dei commit recenti della repo. Il messaggio spiega il perché. La PR elenca cosa cambia e una lista di verifiche, con le voci non fatte lasciate non spuntate.

Se la PR è aperta sullo stesso argomento e non è ancora mergiata, aggiungi lì il lavoro collegato e aggiorna titolo e descrizione.

## Il deploy

Il deploy lo lancia l'utente, a meno che non chieda esplicitamente il contrario. Digli il comando esatto e dove lanciarlo. Se codice e regole del database devono andare online insieme, scrivilo.

Aggiungi una lista breve di controlli da fare online, partendo da quello che non hai potuto verificare.

## Il report

Poche righe: link alla PR, stato del ramo principale, commit di merge, cosa resta da fare. Se il lavoro non è ancora online, dillo nella prima frase.
