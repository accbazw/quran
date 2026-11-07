#!/usr/bin/env python3
"""
Validate Tanzil Quran text + metadata BEFORE building the app database.

Checks (all mandatory; any failure => exit code 1):
  1. Exactly 114 suras in both the text file and quran-data.xml, indices 1..114, no gaps/dups.
  2. Total ayas == 6236.
  3. Per-sura aya count in text == "ayas" attribute in metadata.
  4. Aya numbers per sura are consecutive: 1..N.
  5. No empty (or whitespace-only) aya text.
  6. No duplicate (sura, aya) pairs.
  7. Metadata sanity: cumulative "start" indices, unique revelation "order" 1..114,
     "type" in {Meccan, Medinan}, rukus > 0, sura names present.
  8. Bismillah structure (display-layer contract, stored text stays untouched):
       - for every sura except 1 and 9, aya 1 begins with a 4-word bismillah prefix
         (first word = "بسم" after removing diacritics; the data contains the standard
         form and the shadda form used in the Madina mushaf for suras 95 and 97)
       - sura 1 aya 1 is itself a bismillah
       - sura 9 aya 1 does NOT start with any found bismillah prefix
       - the exact distinct prefixes are printed for use as display-layer constants
  9. Partitioning: 30 juz entries; juz starts parse to valid (sura, aya).
 10. INFO (non-fatal): count of aya texts that appear more than once
     (expected: e.g. the refrain of sura ar-Rahman).

Usage:  python3 scripts/validate_quran.py
Exit:   0 = all checks passed, 1 = failures reported.
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXT_PATH = ROOT / "data/tanzil/quran-uthmani.txt"
META_PATH = ROOT / "data/tanzil/quran-data.xml"

EXPECTED_SURAS = 114
EXPECTED_TOTAL = 6236

errors: list[str] = []
warnings: list[str] = []
infos: list[str] = []


def fail(msg: str) -> None:
    errors.append(msg)


def parse_text() -> dict[int, dict[int, str]]:
    ayas: dict[int, dict[int, str]] = {}
    seen: set[tuple[int, int]] = set()
    with open(TEXT_PATH, encoding="utf-8") as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("|", 2)
            if len(parts) != 3:
                fail(f"line {lineno}: expected 'sura|aya|text', got: {line[:60]!r}")
                continue
            try:
                s, a = int(parts[0]), int(parts[1])
            except ValueError:
                fail(f"line {lineno}: non-numeric sura/aya: {parts[0]!r}|{parts[1]!r}")
                continue
            text = parts[2].strip()
            if not text:
                fail(f"{s}:{a} empty aya text")
            if (s, a) in seen:
                fail(f"{s}:{a} duplicate (sura,aya) pair")
            seen.add((s, a))
            ayas.setdefault(s, {})[a] = text
    return ayas


def parse_meta() -> dict[int, dict[str, str]]:
    root = ET.parse(META_PATH).getroot()
    suras = root.find("suras")
    meta: dict[int, dict[str, str]] = {}
    for el in suras.findall("sura"):
        idx = int(el.attrib["index"])
        if idx in meta:
            fail(f"metadata: duplicate sura index {idx}")
        meta[idx] = dict(el.attrib)
    return meta, root


def main() -> int:
    if not TEXT_PATH.exists():
        print(f"FATAL: {TEXT_PATH} not found"); return 1
    if not META_PATH.exists():
        print(f"FATAL: {META_PATH} not found"); return 1

    ayas = parse_text()
    meta, meta_root = parse_meta()

    # ---- 1. sura count / indices
    text_suras = sorted(ayas.keys())
    meta_suras = sorted(meta.keys())
    if text_suras != list(range(1, EXPECTED_SURAS + 1)):
        fail(f"text sura indices are not exactly 1..114 (got {len(text_suras)} suras, "
             f"min={text_suras[0] if text_suras else '-'}, max={text_suras[-1] if text_suras else '-'})")
    if meta_suras != list(range(1, EXPECTED_SURAS + 1)):
        fail(f"metadata sura indices are not exactly 1..114 (got {len(meta_suras)})")

    # ---- 2. total ayas
    total = sum(len(v) for v in ayas.values())
    if total != EXPECTED_TOTAL:
        fail(f"total ayas = {total}, expected {EXPECTED_TOTAL}")

    # ---- 3 + 4. per-sura count & consecutive numbering
    for s in range(1, EXPECTED_SURAS + 1):
        if s not in ayas:
            continue
        got = ayas[s]
        n = len(got)
        if s in meta and n != int(meta[s]["ayas"]):
            fail(f"sura {s}: text has {n} ayas, metadata says {meta[s]['ayas']}")
        expected_keys = set(range(1, n + 1))
        actual_keys = set(got.keys())
        if actual_keys != expected_keys:
            missing = sorted(expected_keys - actual_keys)[:5]
            extra = sorted(actual_keys - expected_keys)[:5]
            fail(f"sura {s}: aya numbers not consecutive 1..{n} "
                 f"(missing sample={missing}, extra sample={extra})")

    # ---- 7. metadata sanity
    prev_end = 0
    orders: list[int] = []
    for s in range(1, EXPECTED_SURAS + 1):
        if s not in meta:
            continue
        m = meta[s]
        start = int(m["start"])
        if start != prev_end:
            fail(f"sura {s}: metadata start={start}, expected cumulative {prev_end}")
        prev_end = start + int(m["ayas"])
        if m["type"] not in ("Meccan", "Medinan"):
            fail(f"sura {s}: bad type {m['type']!r}")
        if not m.get("name"):
            fail(f"sura {s}: missing Arabic name")
        if not m.get("tname") or not m.get("ename"):
            fail(f"sura {s}: missing transliterated/English name")
        if int(m["rukus"]) <= 0:
            fail(f"sura {s}: bad rukus {m['rukus']}")
        orders.append(int(m["order"]))
    if sorted(orders) != list(range(1, EXPECTED_SURAS + 1)):
        fail("revelation order values are not a permutation of 1..114")

    # ---- 9. juz partitioning (<juz index="1" sura="1" aya="1"/>)
    juzs_el = meta_root.find(".//juzs")
    if juzs_el is None:
        warnings.append("metadata: no <juzs> section found")
    else:
        juz_list = juzs_el.findall("juz")
        if len(juz_list) != 30:
            warnings.append(f"metadata: expected 30 juz, found {len(juz_list)}")
        for j in juz_list:
            s, a = j.attrib.get("sura"), j.attrib.get("aya")
            if not (s and a and s.isdigit() and a.isdigit()
                    and 1 <= int(s) <= 114 and int(a) >= 1):
                fail(f"juz {j.attrib.get('index')}: bad start "
                     f"(sura={s!r}, aya={a!r})")

    # ---- 8. bismillah contract (all values taken from the data itself)
    # Strip diacritics ONLY as a structural check inside this validator
    # (stored/displayed text is never altered).
    def strip_marks(s: str) -> str:
        import unicodedata
        return "".join(c for c in s if unicodedata.category(c) not in ("Mn", "Me"))

    def first_word(s: str) -> str:
        return s.split(" ", 1)[0] if s else ""

    prefixes: dict[str, list[int]] = {}
    for s in range(2, EXPECTED_SURAS + 1):
        if s == 9:
            continue
        first = ayas.get(s, {}).get(1, "")
        words = first.split(" ")
        if len(words) < 4:
            fail(f"sura {s} aya 1 too short to contain a bismillah prefix")
            continue
        prefix = " ".join(words[:4])
        if strip_marks(first_word(prefix)) != "بسم":
            fail(f"sura {s} aya 1 does not start with a bismillah word (got {first_word(prefix)!r})")
            continue
        prefixes.setdefault(prefix, []).append(s)

    bis_forms = sorted(prefixes.keys())
    if not bis_forms:
        fail("no bismillah prefix found in the data")
    else:
        for prefix, sura_list in prefixes.items():
            infos.append(f"bismillah prefix form used by {len(sura_list)} suras "
                         f"(e.g. {sura_list[:3]}): {prefix!r}")
        # sura 1 aya 1 must itself be a bismillah
        bis1 = ayas.get(1, {}).get(1, "")
        if strip_marks(first_word(bis1)) != "بسم":
            fail("sura 1 aya 1 is not a bismillah")
        # sura 9 aya 1 must not start with any of the found prefixes
        first9 = ayas.get(9, {}).get(1, "")
        for prefix in bis_forms:
            if first9.startswith(prefix):
                fail("sura 9 aya 1 unexpectedly starts with bismillah")
                break
        # every sura except 1 and 9 must start with one of the found prefixes
        for s in range(2, EXPECTED_SURAS + 1):
            if s == 9:
                continue
            first = ayas.get(s, {}).get(1, "")
            if not any(first.startswith(p + " ") for p in bis_forms):
                fail(f"sura {s} aya 1 does not start with any known bismillah prefix")

    # ---- 10. INFO: repeated texts (legitimate, e.g. ar-Rahman refrain)
    text_counter = Counter(t for sura in ayas.values() for t in sura.values())
    repeated = {t: c for t, c in text_counter.items() if c > 1}
    if repeated:
        infos.append(f"{len(repeated)} distinct aya texts occur more than once "
                     f"(max repeat = {max(repeated.values())}) — legitimate (e.g. sura ar-Rahman refrain)")

    # ---------------- report ----------------
    print("=" * 62)
    print("Tanzil data validation report")
    print("=" * 62)
    print(f"file        : {TEXT_PATH.name}  ({TEXT_PATH.stat().st_size:,} bytes)")
    print(f"metadata    : {META_PATH.name}  ({META_PATH.stat().st_size:,} bytes)")
    print(f"suras       : {len(text_suras)} / 114")
    print(f"total ayas  : {total} / 6236")
    print(f"bismillah forms : {len(bis_forms)} distinct prefix(es)")
    for p in bis_forms:
        print(f"   -> {p!r}")
    print("-" * 62)
    for w in warnings:
        print(f"[WARN ] {w}")
    for i in infos:
        print(f"[INFO ] {i}")
    if errors:
        print(f"\nFAILED — {len(errors)} error(s):")
        for e in errors[:60]:
            print(f"  [FAIL] {e}")
        if len(errors) > 60:
            print(f"  ... and {len(errors) - 60} more")
        return 1
    print("\nALL CHECKS PASSED ✔")
    return 0


if __name__ == "__main__":
    sys.exit(main())
