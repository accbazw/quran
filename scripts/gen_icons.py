#!/usr/bin/env python3
"""Generate launcher icons deterministically (no AI, no network).

Outputs legacy PNGs (mdpi..xxxhdpi) for ic_launcher and ic_launcher_round.
API 26+ uses res/mipmap-anydpi-v26 adaptive icons (vector foreground),
so these PNGs only serve minSdk 24/25 devices.
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
S = 1024
BG = (36, 92, 64, 255)       # #245C40
FG = (255, 255, 255, 255)
STAR = (233, 196, 106, 255)  # #E9C46A

SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def star_points(cx: float, cy: float, r_out: float, r_in: float) -> list[tuple[float, float]]:
    pts = []
    for i in range(10):
        r = r_out if i % 2 == 0 else r_in
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def content(canvas: Image.Image) -> None:
    d = ImageDraw.Draw(canvas)
    # crescent (white disc minus offset bg disc)
    d.ellipse([512 - 300, 540 - 300, 512 + 300, 540 + 300], fill=FG)
    d.ellipse([640 - 265, 470 - 265, 640 + 265, 470 + 265], fill=BG)
    d.polygon(star_points(770, 285, 100, 42), fill=STAR)


def square_bg() -> Image.Image:
    img = Image.new("RGBA", (S, S), BG)
    content(img)
    return img


def with_mask(img: Image.Image, mask: Image.Image) -> Image.Image:
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


rounded_mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(rounded_mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=210, fill=255)
circle_mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(circle_mask).ellipse([0, 0, S - 1, S - 1], fill=255)

base = square_bg()
launcher = with_mask(base, rounded_mask)
round_launcher = with_mask(base, circle_mask)

for density, size in SIZES.items():
    folder = RES / f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    launcher.resize((size, size), Image.LANCZOS).save(folder / "ic_launcher.png")
    round_launcher.resize((size, size), Image.LANCZOS).save(folder / "ic_launcher_round.png")
    print(f"mipmap-{density}: {size}x{size} ✔")
print("done")
