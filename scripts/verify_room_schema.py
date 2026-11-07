#!/usr/bin/env python3
"""
Verify that the prepackaged database matches EXACTLY what Room will expect
from the entities in Entities.kt at runtime (Room validates this on first
open of a createFromAsset database and CRASHES on mismatch).

Expected schema here mirrors:
  SurahEntity      -> surahs
  AyahEntity       -> ayahs  (+ index index_ayahs_text_simple)
  TranslationEntity-> translations
  JuzEntity        -> juzs
  MetaEntity       -> meta
(bookmarks is user data: Room creates it itself at runtime -> must NOT be in the file)

Usage: python3 scripts/verify_room_schema.py   (exit 1 on any mismatch)
"""
from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / "app/src/main/assets/databases/quran.db"

# (table, {column: (affinity, notNull, pk_position)}, {index_name: (unique, [cols])})
EXPECTED = {
    "surahs": (
        {
            "number": ("INTEGER", True, 1),
            "name_arabic": ("TEXT", True, 0),
            "name_persian": ("TEXT", True, 0),
            "ayah_count": ("INTEGER", True, 0),
            "revelation_type": ("TEXT", True, 0),
            "revelation_order": ("INTEGER", True, 0),
        },
        {},
    ),
    "ayahs": (
        {
            "surah": ("INTEGER", True, 1),
            "ayah": ("INTEGER", True, 2),
            "text": ("TEXT", True, 0),
            "text_simple": ("TEXT", True, 0),
        },
        {"index_ayahs_text_simple": (False, ["text_simple"])},
    ),
    "translations": (
        {
            "translator": ("TEXT", True, 1),
            "surah": ("INTEGER", True, 2),
            "ayah": ("INTEGER", True, 3),
            "text": ("TEXT", True, 0),
        },
        {},
    ),
    "juzs": (
        {
            "number": ("INTEGER", True, 1),
            "surah": ("INTEGER", True, 0),
            "ayah": ("INTEGER", True, 0),
        },
        {},
    ),
    "meta": (
        {
            "key": ("TEXT", True, 1),
            "value": ("TEXT", True, 0),
        },
        {},
    ),
}

errors: list[str] = []
if not DB.exists():
    print(f"FATAL: {DB} missing — run scripts/build_db.py first")
    sys.exit(1)

con = sqlite3.connect(DB)
cur = con.cursor()

actual_tables = {
    r[0] for r in cur.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'"
    )
}

for table, (cols, idxs) in EXPECTED.items():
    if table not in actual_tables:
        errors.append(f"missing table {table}")
        continue
    info = list(cur.execute(f"PRAGMA table_info('{table}')"))
    actual_cols = {r[1]: (r[2].upper(), bool(r[3]), r[5]) for r in info}
    for name, (aff, nn, pk) in cols.items():
        if name not in actual_cols:
            errors.append(f"{table}.{name}: column missing")
            continue
        a_aff, a_nn, a_pk = actual_cols[name]
        if a_aff != aff:
            errors.append(f"{table}.{name}: affinity {a_aff} != expected {aff}")
        if a_nn != nn:
            errors.append(f"{table}.{name}: notNull {a_nn} != expected {nn}")
        if a_pk != pk:
            errors.append(f"{table}.{name}: pk position {a_pk} != expected {pk}")
    extra = set(actual_cols) - set(cols)
    if extra:
        errors.append(f"{table}: unexpected extra columns {sorted(extra)}")
    # indices
    db_idxs = {
        r[1]: r[2]
        for r in cur.execute(f"PRAGMA index_list('{table}')")
    }
    for iname, (uniq, icols) in idxs.items():
        if iname not in db_idxs:
            errors.append(f"{table}: missing index {iname} (Room will fail validation)")
            continue
        actual_unique = bool(db_idxs[iname])  # index_list: [seq, name, unique, ...]
        actual_icols = [r[2] for r in cur.execute(f"PRAGMA index_info('{iname}')")]
        if actual_unique != uniq or actual_icols != icols:
            errors.append(
                f"{table}.{iname}: (unique={actual_unique}, cols={actual_icols}) "
                f"!= expected (unique={uniq}, cols={icols})"
            )

# bookmarks must NOT exist yet (Room creates it; a stale copy would break future migrations)
if "bookmarks" in actual_tables:
    errors.append("'bookmarks' exists in the prepackaged file — remove it (Room creates user tables itself)")

con.close()

print("=" * 62)
print("Room schema vs prebuilt DB verification")
print("=" * 62)
for t, (cols, idxs) in EXPECTED.items():
    n_err = sum(1 for e in errors if e.startswith(t + ".") or e == f"missing table {t}")
    print(f"  [{'PASS' if n_err == 0 else 'FAIL'}] {t}  ({len(cols)} cols, {len(idxs)} index(es))")
print(f"  [{'PASS' if 'bookmarks' not in actual_tables else 'FAIL'}] bookmarks absent (created by Room)")
if errors:
    print(f"\nFAILED — {len(errors)} mismatch(es):")
    for e in errors:
        print(f"  [FAIL] {e}")
    sys.exit(1)
print("\nALL SCHEMA CHECKS PASSED ✔")
