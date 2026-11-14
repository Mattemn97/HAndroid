#!/usr/bin/env bash
# =============================================================================
#  pubblica-indice.sh
#  Pubblica su GitHub Pages la pagina del progetto (sito/, stesso tema di mattemn97.github.io) e
#  i wheel Android di wheels/ come indice per pip: https://mattemn97.github.io/HAndroid/wheels/
#  (handroid_boot.py lo passa a pip con --find-links). wheels/index.html è la pagina semplice che
#  legge pip; wheels/indice.json alimenta la tabella della pagina del progetto.
#
#  Ogni pubblicazione è un commit normale sul ramo gh-pages (niente push forzati): git salva ogni
#  wheel una volta sola, quindi la storia cresce solo dei wheel nuovi. Pages è già attivo su gh-pages.
#
#  Uso: scripts/pubblica-indice.sh            (prepara build/pages e mostra cosa pubblicherebbe)
#       PUBBLICA=1 scripts/pubblica-indice.sh  (fa anche il commit e il push su gh-pages)
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

out=build/pages
rm -rf "$out" && mkdir -p "$out/wheels"
cp sito/* "$out/"
# solo i wheel Android e quelli puri compilati da noi (gli altri pip li trova su PyPI)
cp wheels/*android*arm64*.whl wheels/*-none-any.whl "$out/wheels/" 2>/dev/null || true
touch "$out/.nojekyll"
{
  echo '<!doctype html><meta charset="utf-8"><title>Wheel Android di HAndroid</title>'
  for f in "$out"/wheels/*.whl; do
    n=$(basename "$f")
    echo "<a href=\"$n\">$n</a><br>"
  done
} > "$out/wheels/index.html"
PYTHON="${PYTHON:-$(python3 -c '' 2>/dev/null && echo python3 || echo python)}"
"$PYTHON" - "$out/wheels" <<'PY'
import datetime, json, pathlib, sys
d = pathlib.Path(sys.argv[1])
righe = []
for f in sorted(d.glob("*.whl"), key=lambda f: f.name.lower()):
    nome, versione = f.name.split("-")[:2]
    righe.append({"nome": nome.replace("_", "-"), "versione": versione, "file": f.name, "byte": f.stat().st_size,
                  "tipo": "puro" if f.name.endswith("-none-any.whl") else "android"})
(d / "indice.json").write_text(json.dumps({"aggiornato": datetime.date.today().strftime("%d/%m/%Y"), "wheel": righe}))
PY

echo "$(ls "$out"/wheels/*.whl | wc -l) wheel, $(du -sh "$out" | cut -f1)"
[ -n "${PUBBLICA:-}" ] || { echo "Prova: niente pubblicato (PUBBLICA=1 per il push)"; exit 0; }

url=$(git remote get-url origin)
name=$(git config user.name) email=$(git config user.email)
# copia del ramo con solo l'ultimo commit, contenuto sostituito da quello di $out
rm -rf build/gh-pages
git clone -q --depth 1 --branch gh-pages "$url" build/gh-pages
find build/gh-pages -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
cp -r "$out"/. build/gh-pages/
cd build/gh-pages
git add -A
if git diff --cached --quiet; then echo "Niente di nuovo da pubblicare"; exit 0; fi
git -c user.name="$name" -c user.email="$email" commit -qm "Indice dei wheel Android: $(ls wheels/*.whl | wc -l) pacchetti"
git push -q origin gh-pages
echo "Pubblicato: https://mattemn97.github.io/HAndroid/wheels/"
