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


# Paleta de la mascota Angel Devil: una franja de 4 px de alto por color, en este orden
# (AngelDevilPetRenderer usa el índice de cada letra).
PET_COLORS = [
    GOLD,                   # g halo
    GOLD_DARK,              # G halo oscuro
    RED,                    # r pelo
    RED_DARK,               # R pelo oscuro
    (250, 214, 186, 255),   # s piel
    (40, 18, 24, 255),      # e ojos
    (226, 120, 130, 255),   # p boca
    WHITE,                  # h camisa
    (28, 22, 30, 255),      # k negro (corbata, pantalón)
    (70, 62, 74, 255),      # K gris oscuro (zapatos)
    (255, 255, 255, 255),   # w alas
    LIGHT,                  # W alas sombra
]


def make_pet():
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for index, color in enumerate(PET_COLORS):
        for y in range(index * 4, index * 4 + 4):
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
