"""Genera la insignia FC que va junto a tu nombre (glifo de la fuente freedomclient:icons).

Uso: python3 tools/make_badge.py   (requiere Pillow)
Las iniciales FC sin fondo, con degradado blanco-dorado, contorno fino y un halo encima.
Se dibuja al doble de resolución (media unidad de interfaz por píxel) para que quede más fino.
"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient/textures/font/fc_icon.png"

F = ["#####", "#####", "##...", "####.", "####.", "##...", "##..."]
C = [".####", "#####", "##...", "##...", "##...", "#####", ".####"]
# Degradado de las letras por fila (a doble resolución): blanco arriba, crema, dorado y el borde rojo abajo.
ROW_COLORS = [
    (255, 255, 255), (255, 255, 255), (250, 246, 236), (245, 241, 232), (247, 234, 208), (247, 230, 200),
    (246, 218, 160), (245, 208, 130), (243, 200, 100), (242, 201, 76), (236, 180, 62), (232, 169, 58),
    (215, 38, 61), (190, 30, 52),
]
OUTLINE = (58, 5, 8, 255)
GOLD = (242, 201, 76, 255)
GOLD_DARK = (201, 143, 30, 255)
WIDTH, HEIGHT = 27, 22
LETTERS_TOP = 6


def main():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    letters = set()
    for offset, glyph in ((1, F), (15, C)):
        for y, row in enumerate(glyph):
            for x, char in enumerate(row):
                if char == "#":
                    for dy in range(2):
                        for dx in range(2):
                            letters.add((offset + x * 2 + dx, LETTERS_TOP + y * 2 + dy))
    # Contorno fino alrededor de las letras (solo donde no hay letra).
    for (x, y) in letters:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            p = (x + dx, y + dy)
            if p not in letters and 0 <= p[0] < WIDTH and 0 <= p[1] < HEIGHT:
                image.putpixel(p, OUTLINE)
    for (x, y) in letters:
        image.putpixel((x, y), ROW_COLORS[y - LETTERS_TOP] + (255,))
    # Halo: un anillo aplanado encima de las iniciales, dorado arriba y más oscuro abajo.
    cx, cy, rx, ry = 13.0, 2.5, 8.5, 2.0
    for x in range(WIDTH):
        for y in range(0, LETTERS_TOP - 1):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if 0.55 <= d <= 1.25:
                image.putpixel((x, y), GOLD if y <= cy else GOLD_DARK)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print("badge written to", OUT)


if __name__ == "__main__":
    main()
