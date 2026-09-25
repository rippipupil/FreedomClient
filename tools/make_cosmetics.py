"""Genera las texturas de los cosméticos (alas, halo y capa) de FreedomClient.

Uso: python3 tools/make_cosmetics.py   (requiere Pillow)
"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient/textures/cosmetic"

WHITE = (245, 241, 232, 255)
LIGHT = (214, 206, 196, 255)
SHADE = (170, 160, 150, 255)
OUTLINE = (120, 108, 100, 255)
GOLD = (242, 201, 76, 255)
GOLD_DARK = (201, 143, 30, 255)
GOLD_LIGHT = (255, 232, 150, 255)
RED = (215, 38, 61, 255)
RED_DARK = (142, 20, 38, 255)
RED_DEEP = (90, 12, 26, 255)

# Silueta del ala (20 de ancho x 16 de alto). El borde izquierdo es el que se une a la espalda.
WING = [
    "..........##########",
    "......##############",
    "....################",
    "..#################.",
    ".#################..",
    "#################...",
    "##############......",
    "#############.......",
    "############........",
    "###########.........",
    "##########..........",
    "#########...........",
    "########............",
    "#######.............",
    "######..............",
    "#####...............",
]


def wing_color(x, y):
    """Plumas: bandas diagonales más oscuras cada 4 px y borde con contorno."""
    feather = (x + y * 2) % 8
    if feather in (0, 1):
        return SHADE
    if feather == 2:
        return LIGHT
    return WHITE


def make_wings():
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y, row in enumerate(WING):
        for x, char in enumerate(row):
            if char != "#":
                continue
            edge = (
                x + 1 >= len(row) or row[x + 1] != "#"
                or y + 1 >= len(WING) or WING[y + 1][x] != "#"
                or y == 0 or WING[y - 1][x] != "#"
            )
            color = OUTLINE if edge else wing_color(x, y)
            # Cara delantera en (1,1) y trasera en (22,1) de una caja de 20x16x1; la trasera en espejo.
            image.putpixel((1 + x, 1 + y), color)
            image.putpixel((22 + (19 - x), 1 + y), color)
    image.save(OUT / "wings.png")


def make_halo():
    image = Image.new("RGBA", (16, 16), GOLD)
    for y in range(16):
        for x in range(16):
            if (x + y) % 5 == 0:
                image.putpixel((x, y), GOLD_LIGHT)
            elif (x * 3 + y) % 7 == 0:
                image.putpixel((x, y), GOLD_DARK)
    image.save(OUT / "halo.png")


FC = [
    "###.###",
    "#...#..",
    "##..#..",
    "#...#..",
    "#...###",
]


def make_cape():
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Caja de capa 10x16x1: arriba (1,0), abajo (11,0), lados (0,1) y (11,1), cara (1,1) y espalda (12,1).
    for x in range(22):
        for y in range(17):
            image.putpixel((x, y), RED_DARK)
    for face_x in (1, 12):
        for y in range(16):
            for x in range(10):
                border = x == 0 or x == 9 or y == 15
                color = GOLD if border else (RED if (x + y) % 6 else RED_DARK)
                image.putpixel((face_x + x, 1 + y), color)
        # Halo pequeño y las letras FC.
        for x in range(3, 7):
            image.putpixel((face_x + x, 3), GOLD_LIGHT)
        image.putpixel((face_x + 2, 4), GOLD_LIGHT)
        image.putpixel((face_x + 7, 4), GOLD_LIGHT)
        for x in range(3, 7):
            image.putpixel((face_x + x, 5), GOLD_DARK)
        for y, row in enumerate(FC):
            for x, char in enumerate(row):
                if char == "#":
                    image.putpixel((face_x + 1 + x, 8 + y), WHITE)
        for y in range(13, 15):
            for x in range(1, 9):
                if (x + y) % 2 == 0:
                    image.putpixel((face_x + x, 1 + y), RED_DEEP)
    image.save(OUT / "cape.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    make_wings()
    make_halo()
    make_cape()
    print("cosmetic textures written to", OUT)


if __name__ == "__main__":
    main()
