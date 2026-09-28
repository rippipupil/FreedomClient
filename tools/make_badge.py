"""Genera la insignia de FreedomClient que va junto a tu nombre (glifo de la fuente freedomclient:icons).

Uso: python3 tools/make_badge.py   (requiere Pillow)
El emblema sin letras (tools/fc_logo.py) sin fondo, con contorno fino y el halo encima.
La textura mide 22 px de alto para un glifo de 11 de alto: medio píxel de interfaz por píxel, más fino.
"""
import math
from pathlib import Path
from PIL import Image

import fc_logo

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient/textures/font/fc_icon.png"
WIDTH, HEIGHT = 25, 22
LOGO_X, LOGO_Y = 1, 7


def main():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    palette = fc_logo.PALETTES["angel"]
    cells = fc_logo.cells(fc_logo.EMBLEM)
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            p = (LOGO_X + x + dx, LOGO_Y + y + dy)
            if (x + dx, y + dy) not in cells and 0 <= p[0] < WIDTH and 0 <= p[1] < HEIGHT:
                image.putpixel(p, palette["outline"] + (255,))
    for (x, y), kind in cells.items():
        image.putpixel((LOGO_X + x, LOGO_Y + y), fc_logo.color(kind, y, "angel") + (255,))
    # Halo: anillo aplanado encima del emblema, claro arriba y más oscuro abajo.
    light, dark = palette["halo"]
    cx, cy, rx, ry = 12, 3, 9, 2
    for x in range(-rx, rx + 1):
        t = x / rx
        dy = round(math.sqrt(max(0.0, 1 - t * t)) * ry)
        image.putpixel((cx + x, cy - dy - 1), light + (255,))
        image.putpixel((cx + x, cy + dy), dark + (255,))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print("badge written to", OUT)


if __name__ == "__main__":
    main()
