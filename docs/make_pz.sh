#!/bin/bash
# Полная сборка пояснительной записки: текст -> docx -> содержание и счётчики -> ведомость -> PDF.
# Ведомость документов содержит общее число страниц ПЗ, поэтому при его изменении сборка повторяется.
set -e
cd "$(dirname "$0")"
export LANG=C.UTF-8
OUT_DOCX="ПЗ_КП_ПСП_Балащенко.docx"

python3 content.py
python3 autosplit.py content.json raw.docx
N=$(cat .pages 2>/dev/null || echo 99)
for attempt in 1 2 3; do
  python3 ../tools/make_graphics.py "$N" ved
  node build.js content.json raw.docx
  python3 postprocess.py raw.docx "$OUT_DOCX"
  NEW=$(cat .pages)
  if [ "$NEW" = "$N" ]; then break; fi
  echo "число страниц изменилось: $N -> $NEW, пересборка"
  N=$NEW
done
rm -f raw.docx
soffice --headless --convert-to pdf "$OUT_DOCX" >/dev/null 2>&1
echo "готово: $OUT_DOCX, $(pdfinfo "${OUT_DOCX%.docx}.pdf" | grep Pages)"
