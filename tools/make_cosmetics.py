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

BONE = (255, 252, 244, 255)


def make_wings():
    """Franjas sólidas de 4 px de alto, una por color. Las alas son voxel (AngelCosmeticsLayer) y cada cubo
    usa una franja: contorno, blanco, claro, sombra y hueso."""
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for index, color in enumerate((OUTLINE, WHITE, LIGHT, SHADE, BONE)):
        for y in range(index * 4, index * 4 + 4):
            for x in range(64):
                image.putpixel((x, y), color)
    image.save(OUT / "wings.png")


# Pluma pixel de 8x8 para Hit Particles: o = contorno, w = blanco, l = claro, q = cañón (dorado).
FEATHER = [
    "......oo",
    ".....owo",
    "....owlo",
    "...owlo.",
    "..owlo..",
    ".owlo...",
    ".oqo....",
    "q.......",
]


def make_feather():
    colors = {"o": OUTLINE, "w": WHITE, "l": LIGHT, "q": GOLD_DARK}
    image = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(FEATHER):
        for x, char in enumerate(row):
            if char in colors:
                image.putpixel((x, y), colors[char])
    particle_dir = OUT.parent / "particle"
    particle_dir.mkdir(parents=True, exist_ok=True)
    image.save(particle_dir / "feather.png")


# Mascota Angel Devil: un mini jugador (cabeza, pelo largo, cuerpo, brazos y piernas) con UV de caja como las skins,
# más unas franjas de color para el halo y las alas (AngelDevilPetRenderer).
PET = {
    "r": RED,                    # pelo
    "R": RED_DARK,               # mechones oscuros
    "L": (240, 86, 100, 255),    # brillo del pelo
    "s": (250, 214, 186, 255),   # piel
    "S": (226, 184, 156, 255),   # piel en sombra
    "e": (40, 18, 24, 255),      # ojos
    "w": (255, 255, 255, 255),   # brillo de los ojos / camisa
    "b": (244, 150, 160, 255),   # rubor
    "m": (196, 90, 104, 255),    # boca
    "h": WHITE,                  # camisa
    "H": (200, 194, 186, 255),   # camisa en sombra
    "k": (28, 22, 30, 255),      # corbata y pantalón
    "K": (70, 62, 74, 255),      # zapatos
}
# Franjas de color (v = 40, 44, 48...): halo, halo oscuro, ala, ala clara, ala sombra, contorno del ala.
PET_STRIPS = [GOLD, GOLD_DARK, (255, 255, 255, 255), LIGHT, SHADE, OUTLINE]


def paint_face(image, u, v, rows):
    for y, row in enumerate(rows):
        for x, char in enumerate(row):
            if char in PET:
                image.putpixel((u + x, v + y), PET[char])


def fill(image, u, v, w, h, char):
    paint_face(image, u, v, [char * w] * h)


def paint_box(image, u, v, w, h, d, top, bottom, right, front, left, back):
    """UV de caja de Minecraft: arriba/abajo en la fila v, lados, frente y espalda en la fila v + d."""
    paint_face(image, u + d, v, top)
    paint_face(image, u + d + w, v, bottom)
    paint_face(image, u, v + d, right)
    paint_face(image, u + d, v + d, front)
    paint_face(image, u + d + w, v + d, left)
    paint_face(image, u + d + w + d, v + d, back)


def make_pet():
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # Cabeza 8x8x8 en (0, 0).
    hair_side = ["rrrrrrrr", "rRrrrrLr", "rrrRrrrr", "rrrrrRrr", "rRrrrrrr", "rrrrrrrs", "rrRrrrrs", "rrrrrrss"]
    paint_box(image, 0, 0, 8, 8, 8,
              top=["rrrLrrrr", "rrrLrrrr", "rrrRrrrr", "rrrRrrrr", "rrrRrrrr", "rrrrrrrr", "rrrrrrrr", "rrrrrrrr"],
              bottom=["rsssssr ".replace(" ", "r"), "rssssssr", "rssssssr", "rssssssr", "rssssssr", "rssssssr", "rrrrrrrr", "rrrrrrrr"],
              right=[row[::-1] for row in hair_side],
              front=["rrrLrrrr", "rrRrrRrr", "rrsRRsrr", "rssssssr", "rsewsewr", "rseeseer", "rbssssbr", "rssmmssr"],
              left=hair_side,
              back=["rrrrrrrr", "rrRrrLrr", "rrrrrrrr", "rRrrrRrr", "rrrrrrrr", "rrrRrrrr", "rRrrrrRr", "rrrrrrrr"])
    # Pelo largo por detrás, 8x4x2 en (32, 0).
    long_hair = ["rrrrrrrr", "rRrrRrrr", "rrrRrrRr", "RrrrrrrR"]
    paint_box(image, 32, 0, 8, 4, 2,
              top=["rrrrrrrr"] * 2, bottom=["RRRRRRRR"] * 2,
              right=["rr", "rR", "rr", "RR"], front=long_hair, left=["rr", "Rr", "rr", "RR"], back=long_hair)
    # Cuerpo 6x6x3 en (0, 16): camisa blanca con corbata negra y el cinturón del pantalón.
    paint_box(image, 0, 16, 6, 6, 3,
              top=["hhhhhh"] * 3, bottom=["kkkkkk"] * 3,
              right=["hhh", "hhh", "hHh", "hhh", "hhH", "kkk"],
              front=["Hhkkhh", "hhkkhh", "hhkkhh", "hhhkhH", "hhhhhh", "kkkkkk"],
              left=["hhh", "hhh", "hHh", "hhh", "Hhh", "kkk"],
              back=["hhhhhh", "hhhhhh", "hHhhHh", "hhhhhh", "hhhhhh", "kkkkkk"])
    # Brazos 2x6x2 en (18, 16) y (26, 16): mangas blancas y manos.
    for u in (18, 26):
        sleeve = ["hh", "hh", "hH", "hh", "Hh", "ss"]
        paint_box(image, u, 16, 2, 6, 2, top=["hh"] * 2, bottom=["ss"] * 2,
                  right=sleeve, front=sleeve, left=sleeve, back=sleeve)
    # Piernas 3x5x3 en (0, 26) y (12, 26): pantalón negro y zapatos.
    for u in (0, 12):
        leg = ["kkk", "kkk", "kkk", "kkk", "KKK"]
        paint_box(image, u, 26, 3, 5, 3, top=["kkk"] * 3, bottom=["KKK"] * 3,
                  right=leg, front=leg, left=leg, back=leg)
    # Franjas de color para el halo y las alas.
    for index, color in enumerate(PET_STRIPS):
        for y in range(40 + index * 4, 44 + index * 4):
            for x in range(64):
                image.putpixel((x, y), color)
    image.save(OUT / "pet.png")


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
    make_feather()
    make_pet()
    make_halo()
    make_cape()
    print("cosmetic textures written to", OUT)


if __name__ == "__main__":
    main()
