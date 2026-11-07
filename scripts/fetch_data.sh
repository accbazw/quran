#!/usr/bin/env bash
# Re-download the Tanzil data files (reproducible data pipeline, step 1/3).
# Usage:  bash scripts/fetch_data.sh
# After this, ALWAYS run:
#   python3 scripts/validate_quran.py   (must pass)
#   python3 scripts/build_db.py         (must pass)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/data/tanzil"
mkdir -p "$DEST"

UA="Mozilla/5.0 (X11; Linux x86_64)"

# Quran text — Uthmani, format "sura|aya|text" (outType=txt-2), with pause marks,
# sajdah signs and tatweel-below-superscript-alef (the form's defaults).
curl -L --max-time 120 -A "$UA" \
  "https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&marks=true&sajdah=true&tatweel=true&agree=true" \
  -o "$DEST/quran-uthmani.txt"

# Metadata (juz / manzil / page / sajda partitioning + sura info).
curl -L --max-time 120 -A "$UA" \
  "http://tanzil.net/res/text/metadata/quran-data.xml" \
  -o "$DEST/quran-data.xml"

echo "--- files:"
wc -c "$DEST/quran-uthmani.txt" "$DEST/quran-data.xml"
echo "--- hashes:"
md5sum "$DEST/quran-uthmani.txt" "$DEST/quran-data.xml"
echo
echo "Now run:  python3 scripts/validate_quran.py && python3 scripts/build_db.py"
