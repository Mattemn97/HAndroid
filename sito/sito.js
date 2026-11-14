// Pagina di HAndroid: elenco dei wheel da wheels/indice.json (generato da scripts/pubblica-indice.sh)
(function () {
  document.getElementById('anno').textContent = new Date().getFullYear();
  document.getElementById('urlIndice').textContent = new URL('wheels/', location.href).href;

  var righe = document.getElementById('righe');
  var q = document.getElementById('q');
  var wheels = [];

  function mb(bytes) {
    return bytes < 1048576 ? Math.max(1, Math.round(bytes / 1024)) + ' kB' : (bytes / 1048576).toFixed(1) + ' MB';
  }

  function cella(testo, classe) {
    var td = document.createElement('td');
    td.textContent = testo;
    if (classe) td.className = classe;
    return td;
  }

  function mostra() {
    var filtro = q.value.trim().toLowerCase();
    var scelti = wheels.filter(function (w) { return w.nome.toLowerCase().indexOf(filtro) !== -1; });
    righe.textContent = '';
    if (!scelti.length) {
      var tr = document.createElement('tr');
      var td = cella('Nessun pacchetto trovato.', 'muted');
      td.colSpan = 4;
      tr.appendChild(td);
      righe.appendChild(tr);
      return;
    }
    scelti.forEach(function (w) {
      var tr = document.createElement('tr');
      var nome = document.createElement('td');
      var a = document.createElement('a');
      a.href = 'wheels/' + encodeURIComponent(w.file);
      a.textContent = w.nome;
      nome.appendChild(a);
      tr.appendChild(nome);
      tr.appendChild(cella(w.versione, 'mono'));
      tr.appendChild(cella(w.tipo === 'android' ? 'nativo arm64' : 'solo Python', 'tipo-' + w.tipo));
      tr.appendChild(cella(mb(w.byte)));
      righe.appendChild(tr);
    });
  }

  fetch('wheels/indice.json')
    .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
    .then(function (dati) {
      wheels = dati.wheel;
      var nativi = wheels.filter(function (w) { return w.tipo === 'android'; }).length;
      document.getElementById('conteggio').textContent =
        wheels.length + ' pacchetti · ' + nativi + ' nativi · aggiornato il ' + dati.aggiornato;
      mostra();
    })
    .catch(function () {
      righe.innerHTML = '<tr><td colspan="4" class="muted">Indice non disponibile.</td></tr>';
    });

  q.addEventListener('input', mostra);
})();
