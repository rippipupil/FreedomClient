"""Genera las capas de FreedomClient (Cape): estilos Angel Devil y Neon, solo con el emblema sin letras.

Uso: python3 tools/make_capes.py   (requiere Pillow)
Texturas de capa en alta resolución (256x128, la distribución de una capa de 64x32 multiplicada por 4): la cara
de la capa ocupa 40x64 píxeles, con el emblema (tools/fc_logo.py EMBLEM) y el halo encima.
"""
import math
from pathlib import Path
from PIL import Image

import fc_logo

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient/textures/cosmetic"
S = 4  # escala respecto a una capa de 64x32


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def gradient(stops, t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            return lerp(c0, c1, (t - t0) / (t1 - t0) if t1 > t0 else 0)
    return stops[-1][1]


STYLES = {
    "angel": {
        "sky": [(0.0, (26, 11, 46)), (0.45, (92, 26, 53)), (0.8, (178, 47, 42)), (1.0, (238, 122, 52))],
        "border": [(0.0, (242, 201, 76)), (1.0, (201, 143, 30))],
        "star": (245, 241, 232),
        "edge": (58, 12, 20),
    },
    "neon": {
        "sky": [(0.0, (4, 6, 26)), (0.5, (14, 24, 66)), (1.0, (31, 48, 111))],
        "border": [(0.0, (255, 225, 74)), (0.3, (198, 242, 90)), (0.55, (94, 240, 200)), (0.8, (63, 215, 255)), (1.0, (58, 123, 255))],
        "star": (191, 239, 255),
        "edge": (6, 9, 32),
    },
}


def face(style):
    """Cara de la capa (40x64): cielo en franjas con tramado, estrellas, borde, halo y emblema."""
    p = STYLES[style]
    image = Image.new("RGBA", (40, 64), (0, 0, 0, 255))
    bands = 16
    for y in range(64):
        band = y * bands // 64
        color = gradient(p["sky"], band / (bands - 1))
        nxt = gradient(p["sky"], min(bands - 1, band + 1) / (bands - 1))
        for x in range(40):
            # Tramado pixel entre franjas.
            last_row = (y + 1) * bands // 64 != band
            image.putpixel((x, y), (nxt if last_row and (x + y) % 2 else color) + (255,))
    for i in range(18):
        x = (i * 13 + 5) % 36 + 2
        y = (i * 29 + 7) % 30 + 3
        image.putpixel((x, y), p["star"] + (255,))
    # Borde de 2 px (en Neon con el degradado de su energía).
    for y in range(64):
        for x in range(40):
            if x < 2 or x > 37 or y > 61:
                t = y / 63 if x < 2 or x > 37 else x / 39
                image.putpixel((x, y), gradient(p["border"], t) + (255,))
    # Halo sobre el emblema.
    palette = fc_logo.PALETTES["neon" if style == "neon" else "angel"]
    light, dark = palette["halo"]
    cx, cy, rx, ry = 20, 20, 8, 2
    for x in range(-rx, rx + 1):
        t = x / rx
        dy = round(math.sqrt(max(0.0, 1 - t * t)) * ry)
        image.putpixel((cx + x, cy - dy - 1), light + (255,))
        image.putpixel((cx + x, cy + dy), dark + (255,))
    # Emblema con contorno oscuro, centrado.
    cells = fc_logo.cells(fc_logo.EMBLEM)
    ox, oy = 9, 26
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in cells:
                image.putpixel((ox + x + dx, oy + y + dy), p["edge"] + (255,))
    for (x, y), kind in cells.items():
        image.putpixel((ox + x, oy + y), fc_logo.color(kind, y, "neon" if style == "neon" else "angel") + (255,))
    return image


def cape(style):
    """Capa completa: la caja de 10x16x1 de vanilla a escala 4, con la misma cara por dentro y por fuera."""
    image = Image.new("RGBA", (64 * S, 32 * S), (0, 0, 0, 0))
    p = STYLES[style]
    edge = gradient(p["border"], 0.5) + (255,)
    # Arriba, abajo y los cantos.
    for x in range(0, 22 * S):
        for y in range(0, 17 * S):
            image.putpixel((x, y), edge)
    f = face(style)
    for face_x in (1, 12):
        image.paste(f, (face_x * S, 1 * S))
    return image


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for style in STYLES:
        cape(style).save(OUT / f"cape_{style}.png")
    print("capes written to", OUT)


if __name__ == "__main__":
    main()
