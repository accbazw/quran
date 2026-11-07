#!/usr/bin/env python3
"""
Build the prepackaged SQLite database (assets/databases/quran.db) for Room.

The schema MUST stay byte-for-byte compatible with the Room entities in
app/src/main/java/com/uthmani/quran/data/db/Entities.kt:

  surahs(number PK, name_arabic, name_persian, ayah_count, revelation_type, revelation_order)
  ayahs(surah, ayah, text, text_simple)  PK(surah, ayah) + index_ayahs_text_simple
  translations(translator, surah, ayah, text) PK(translator, surah, ayah)   [empty, future layer]
  juzs(number PK, surah, ayah)

Rules (documented in SOURCES.md):
  - Quran text is copied VERBATIM from data/tanzil/quran-uthmani.txt. Never modified.
  - text_simple is an ADDITIONAL column used only for diacritic-insensitive search.
    It must stay in sync with QuranTextNormalizer.kt (same rules below).
  - name_persian is DETERMINISTICALLY derived from the Tanzil Arabic name:
      1) strip the leading definite article "ال" (unless the name starts with "آل")
      2) map: أ إ آ ٱ -> ا ، ة -> ه ، ي ى -> ی ، ك -> ک
  - Run scripts/validate_quran.py first; this script re-checks the essentials.

Usage:  python3 scripts/build_db.py
"""
from __future__ import annotations

import shutil
import sqlite3
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXT_PATH = ROOT / "data/tanzil/quran-uthmani.txt"
META_PATH = ROOT / "data/tanzil/quran-data.xml"
OUT_PATH = ROOT / "app/src/main/assets/databases/quran.db"

EXPECTED_SURAS, EXPECTED_TOTAL = 114, 6236

# ----- search normalization (MUST mirror QuranTextNormalizer.kt) -----
REMOVE_RANGES = [
    (0x0610, 0x061A),  # Arabic honorific signs
    (0x0640, 0x0640),  # tatweel
    (0x064B, 0x065F),  # tashkeel (harakat)
    (0x0670, 0x0670),  # superscript alef
    (0x06D6, 0x06ED),  # Quranic annotation marks (pause marks, small waw, ...)
    (0x08D3, 0x08FF),  # extended Arabic marks
]
REPLACE = {
    "\u0622": "\u0627",  # آ -> ا
    "\u0623": "\u0627",  # أ -> ا
    "\u0625": "\u0627",  # إ -> ا
    "\u0671": "\u0627",  # ٱ -> ا
    "\u0629": "\u0647",  # ة -> ه
    "\u0649": "\u064A",  # ى -> ي
    "\u06CC": "\u064A",  # ی -> ي
    "\u06A9": "\u0643",  # ک -> ك
    "\u0626": "\u064A",  # ئ -> ي
    "\u0624": "\u0648",  # ؤ -> و
}


def normalize(text: str) -> str:
    out = []
    for ch in text:
        code = ord(ch)
        if any(lo <= code <= hi for lo, hi in REMOVE_RANGES):
            continue
        out.append(REPLACE.get(ch, ch))
    return "".join(out)


def strip_marks(s: str) -> str:
    return "".join(c for c in s if unicodedata.category(c) not in ("Mn", "Me"))


def persian_name(arabic_name: str) -> str:
    s = arabic_name.strip()
    if s.startswith("ال") and not s.startswith("آل") and len(s) > 3:
        s = s[2:]
    table = str.maketrans({
        "\u0623": "\u0627", "\u0625": "\u0627", "\u0622": "\u0627", "\u0671": "\u0627",
        "\u0629": "\u0647",
        "\u064A": "\u06CC", "\u0649": "\u06CC",
        "\u06A9": "\u06A9", "\u0643": "\u06A9",
    })
    return s.translate(table)


SCHEMA = """
CREATE TABLE surahs (
    number INTEGER NOT NULL PRIMARY KEY,
    name_arabic TEXT NOT NULL,
    name_persian TEXT NOT NULL,
    ayah_count INTEGER NOT NULL,
    revelation_type TEXT NOT NULL,
    revelation_order INTEGER NOT NULL
);
CREATE TABLE ayahs (
    surah INTEGER NOT NULL,
    ayah INTEGER NOT NULL,
    text TEXT NOT NULL,
    text_simple TEXT NOT NULL,
    PRIMARY KEY(surah, ayah)
);
CREATE INDEX index_ayahs_text_simple ON ayahs (text_simple);
CREATE TABLE translations (
    translator TEXT NOT NULL,
    surah INTEGER NOT NULL,
    ayah INTEGER NOT NULL,
    text TEXT NOT NULL,
    PRIMARY KEY(translator, surah, ayah)
);
CREATE TABLE juzs (
    number INTEGER NOT NULL PRIMARY KEY,
    surah INTEGER NOT NULL,
    ayah INTEGER NOT NULL
);
CREATE TABLE meta (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
"""


def extract_copyright_notice() -> str:
    """The trailing '#'-comment block of the downloaded Tanzil file, verbatim
    (with the comment markers stripped). Stored inside the database so every
    copy of the db carries the required copyright notice (license condition 3)."""
    lines = []
    with open(TEXT_PATH, encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            if line.startswith("#"):
                lines.append(line[1:].lstrip(" ") if line[1:2] == " " else line[1:])
            elif lines:
                break  # block ended
    return "\n".join(lines).strip()


def main() -> int:
    # essentials (full gate = scripts/validate_quran.py)
    ayas: dict[int, dict[int, str]] = {}
    with open(TEXT_PATH, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            s, a, t = line.split("|", 2)
            ayas.setdefault(int(s), {})[int(a)] = t.strip()
    if len(ayas) != EXPECTED_SURAS or sum(len(v) for v in ayas.values()) != EXPECTED_TOTAL:
        print("FATAL: input failed essential counts — run scripts/validate_quran.py")
        return 1

    root = ET.parse(META_PATH).getroot()
    suras_meta = {int(e.attrib["index"]): e.attrib for e in root.find("suras").findall("sura")}
    juz_rows = [(int(j.attrib["index"]), int(j.attrib["sura"]), int(j.attrib["aya"]))
                for j in root.find(".//juzs").findall("juz")]

    bismillah_forms = sorted({
        " ".join(ayas[s][1].split(" ")[:4])
        for s in range(2, EXPECTED_SURAS + 1) if s != 9
    })

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    if OUT_PATH.exists():
        OUT_PATH.unlink()
    con = sqlite3.connect(OUT_PATH)
    con.executescript(SCHEMA)
    with con:
        for s in range(1, EXPECTED_SURAS + 1):
            m = suras_meta[s]
            con.execute(
                "INSERT INTO surahs VALUES (?,?,?,?,?,?)",
                (s, m["name"], persian_name(m["name"]), int(m["ayas"]), m["type"], int(m["order"])),
            )
            for a, text in sorted(ayas[s].items()):
                con.execute(
                    "INSERT INTO ayahs VALUES (?,?,?,?)",
                    (s, a, text, normalize(text)),
                )
        con.executemany("INSERT INTO juzs VALUES (?,?,?)", juz_rows)
        con.execute("INSERT INTO meta VALUES (?,?)", ("tanzil_copyright_notice", extract_copyright_notice()))
    con.commit()

    # ---- verification pass (reopen fresh) ----
    con2 = sqlite3.connect(OUT_PATH)
    cur = con2.cursor()
    checks = {
        "surahs == 114": cur.execute("SELECT COUNT(*) FROM surahs").fetchone()[0] == 114,
        "ayahs == 6236": cur.execute("SELECT COUNT(*) FROM ayahs").fetchone()[0] == 6236,
        "juzs == 30": cur.execute("SELECT COUNT(*) FROM juzs").fetchone()[0] == 30,
        "copyright notice in db": cur.execute(
            "SELECT COUNT(*) FROM meta WHERE key='tanzil_copyright_notice' AND LENGTH(value) > 100"
        ).fetchone()[0] == 1,
        "no empty text": cur.execute("SELECT COUNT(*) FROM ayahs WHERE TRIM(text)=''").fetchone()[0] == 0,
        "no empty text_simple": cur.execute("SELECT COUNT(*) FROM ayahs WHERE TRIM(text_simple)=''").fetchone()[0] == 0,
        "integrity": cur.execute("PRAGMA integrity_check").fetchone()[0] == "ok",
    }
    # text_simple must contain no combining marks and equal normalize(text) for every row
    mism = 0
    for s, a, text, simple in cur.execute("SELECT surah, ayah, text, text_simple FROM ayahs"):
        if normalize(text) != simple or any(unicodedata.category(c) in ("Mn", "Me") for c in simple):
            mism += 1
    checks["text_simple == normalize(text) for all rows"] = mism == 0
    con2.close()

    con.execute("VACUUM")
    con.close()

    # ---- keep the Kotlin display constants in sync with the data ----
    kotlin_path = ROOT / "app/src/main/java/com/uthmani/quran/data/QuranText.kt"
    try:
        import re
        consts = re.findall(
            r'const val BISMILLAH_FORM_\d = "([^"]+)"',
            kotlin_path.read_text(encoding="utf-8"),
        )
        checks["QuranText.kt bismillah constants == data"] = consts == bismillah_forms
    except OSError:
        checks["QuranText.kt bismillah constants == data"] = False

    print("=" * 62)
    print("Prebuilt database report")
    print("=" * 62)
    for k, ok in checks.items():
        print(f"  [{'PASS' if ok else 'FAIL'}] {k}")
    if not all(checks.values()):
        return 1
    print(f"  size        : {OUT_PATH.stat().st_size:,} bytes")
    print(f"  output      : {OUT_PATH.relative_to(ROOT)}")
    print("\n// Exact bismillah display constants (from the data — for QuranConstants.kt):")
    for i, form in enumerate(bismillah_forms, 1):
        cps = ", ".join(f"\\u{ord(c):04X}" for c in form)
        print(f"  // form {i}: {form}")
        print(f'  const val BISMILLAH_FORM_{i} = "{form}"')
    return 0


if __name__ == "__main__":
    sys.exit(main())
