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


def feather_image(size=16):
    """Pluma pixel en diagonal (cañón dorado del abajo-izquierda al arriba-derecha), con barbas sombreadas y contorno."""
    import math
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    x0, y0, x1, y1 = 2.0, size - 3.0, size - 3.0, 2.0
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    filled = {}
    for y in range(size):
        for x in range(size):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            along = (px * ux + py * uy) / length
            side = px * -uy + py * ux
            if along < -0.02 or along > 1.02:
                continue
            width = 3.4 * math.sin(math.pi * min(1.0, max(0.0, (along - 0.12) / 0.9))) ** 0.7
            if abs(side) < 0.6 and along < 0.95:
                filled[(x, y)] = GOLD_DARK if along < 0.2 else GOLD
            elif along > 0.12 and abs(side) <= width:
                # Barbas: rayas diagonales cada 3 píxeles; un lado más claro que el otro.
                stripe = int(along * length * 1.1 + abs(side) * 0.6) % 3 == 0
                if side > 0:
                    filled[(x, y)] = WHITE
                else:
                    filled[(x, y)] = LIGHT if stripe else WHITE
    for (x, y), color in filled.items():
        image.putpixel((x, y), color)
    for (x, y) in list(filled):
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + ox, y + oy)
            if p not in filled and 0 <= p[0] < size and 0 <= p[1] < size:
                image.putpixel(p, OUTLINE)
    return image


# Sprites de partículas para las auras y TotemPop (letra -> color; '.' transparente).
PARTICLE_COLORS = {
    "g": GOLD, "G": GOLD_DARK, "y": GOLD_LIGHT, "w": (255, 255, 255, 255), "W": (220, 230, 255, 255),
    "r": RED, "R": RED_DARK, "o": (255, 140, 66, 255), "O": (255, 196, 110, 255), "p": (255, 120, 170, 255),
    "P": (200, 60, 110, 255), "b": (93, 173, 226, 255), "B": (46, 110, 168, 255), "k": (58, 5, 8, 255),
}
PARTICLES = {
    # Chispa dorada de ángel.
    "spark": ["...y...", "...g...", "..ygy..", "ygggggy", "..ygy..", "...g...", "...y..."],
    # Brasa de demonio.
    "ember": [".rr..", "roOr.", "rOOor", ".roor", "..rr."],
    # Estrella blanca que parpadea.
    "star": ["...w...", "...w...", "..wWw..", "wwWWWww", "..wWw..", "...w...", "...w..."],
    # Corazón rosa.
    "heart": [".PP.PP.", "PppPppP", "PpppppP", "PpppppP", ".PpppP.", "..PpP..", "...P..."],
    # Chispa azul cielo.
    "sky_spark": ["..b..", ".bWb.", "bWWWb", ".bWb.", "..b.."],
    # Nota musical.
    "note": ["...kkk", "...kgk", "...kgk", "...k.k", "kkkk..", "kggk..", "kkkk.."],
}


def halo_ring_image():
    """Anillo del halo visto un poco desde arriba (para el brillo del tótem)."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - 8) / 7.0) ** 2 + ((y + 0.5 - 8) / 3.2) ** 2
            if 0.55 <= d <= 1.0:
                image.putpixel((x, y), GOLD_LIGHT if y < 8 else GOLD)
    return image


def make_feather():
    particle_dir = OUT.parent / "particle"
    particle_dir.mkdir(parents=True, exist_ok=True)
    feather_image().save(particle_dir / "feather.png")
    halo_ring_image().save(particle_dir / "halo_ring.png")
    for name, rows in PARTICLES.items():
        image = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, char in enumerate(row):
                if char in PARTICLE_COLORS:
                    image.putpixel((x, y), PARTICLE_COLORS[char])
        # Los sprites del atlas deben ser cuadrados como mucho del mismo tamaño; se centran en un lienzo de 8x8.
        canvas = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        canvas.paste(image, ((8 - image.width) // 2, (8 - image.height) // 2))
        canvas.save(particle_dir / f"{name}.png")


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


def make_pet(sleeping=False):
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # Cabeza 8x8x8 en (0, 0).
    hair_side = ["rrrrrrrr", "rRrrrrLr", "rrrRrrrr", "rrrrrRrr", "rRrrrrrr", "rrrrrrrs", "rrRrrrrs", "rrrrrrss"]
    paint_box(image, 0, 0, 8, 8, 8,
              top=["rrrLrrrr", "rrrLrrrr", "rrrRrrrr", "rrrRrrrr", "rrrRrrrr", "rrrrrrrr", "rrrrrrrr", "rrrrrrrr"],
              bottom=["rsssssr ".replace(" ", "r"), "rssssssr", "rssssssr", "rssssssr", "rssssssr", "rssssssr", "rrrrrrrr", "rrrrrrrr"],
              right=[row[::-1] for row in hair_side],
              front=["rrrLrrrr", "rrRrrRrr", "rrsRRsrr", "rssssssr",
                     # Dormida: ojos cerrados (una línea) y la boca pequeña.
                     "rssssssr" if sleeping else "rsewsewr",
                     "rseeseer",
                     "rbssssbr",
                     "rsssmssr" if sleeping else "rssmmssr"],
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
    image.save(OUT / ("pet_sleep.png" if sleeping else "pet.png"))


def make_halo_styles():
    """Franjas de color para los estilos de halo (anillo roto, corona y cuernos): dorado, dorado oscuro, rojo y rojo oscuro."""
    image = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    for index, color in enumerate((GOLD, GOLD_DARK, RED, RED_DEEP)):
        for y in range(index * 4, index * 4 + 4):
            for x in range(32):
                image.putpixel((x, y), color)
    image.save(OUT / "halo_styles.png")


# Nube con alas (AngelCloudPetRenderer): cuerpo y bultos con UV de caja, y franjas de color para alas y halo.
CLOUD = {
    "w": (255, 255, 255, 255),   # blanco
    "s": (230, 236, 248, 255),   # sombra azulada
    "S": (200, 210, 230, 255),   # sombra de abajo
    "e": (40, 30, 50, 255),      # ojos y boca
    "b": (255, 170, 190, 255),   # rubor
}
# Franjas desde v = 40: alas, alas claras, alas sombra, contorno de las alas y halo.
CLOUD_STRIPS = [(255, 255, 255, 255), LIGHT, SHADE, OUTLINE, GOLD, GOLD_DARK]


def paint_cloud_face(image, u, v, rows):
    for y, row in enumerate(rows):
        for x, char in enumerate(row):
            if char in CLOUD:
                image.putpixel((u + x, v + y), CLOUD[char])


def paint_cloud_box(image, u, v, w, h, d, front=None):
    """Caja de nube: blanca, con sombra azulada en los lados y la base más oscura."""
    side = ["w" * d] * (h - 1) + ["s" * d]
    paint_cloud_face(image, u + d, v, ["w" * w] * d)            # arriba
    paint_cloud_face(image, u + d + w, v, ["S" * w] * d)        # abajo
    paint_cloud_face(image, u, v + d, side)                     # lado
    paint_cloud_face(image, u + d, v + d, front or (["w" * w] * (h - 1) + ["s" * w]))
    paint_cloud_face(image, u + d + w, v + d, side)             # otro lado
    paint_cloud_face(image, u + d + w + d, v + d, ["w" * w] * (h - 1) + ["s" * w])


def make_cloud_pet(sleeping=False):
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    if sleeping:
        face = ["wwwwwwwwww", "wwwwwwwwww", "wbeewweebw", "wwwweewwww", "ssssssssss"]
    else:
        face = ["wwwwwwwwww", "wwewwwweww", "wbewwwwebw", "wwwweewwww", "ssssssssss"]
    paint_cloud_box(image, 0, 0, 10, 5, 6, front=face)   # cuerpo 10x5x6
    paint_cloud_box(image, 0, 16, 4, 3, 4)               # bulto de arriba izquierda
    paint_cloud_box(image, 16, 16, 5, 4, 4)              # bulto de arriba derecha
    paint_cloud_box(image, 36, 0, 3, 3, 4)               # bulto lateral
    paint_cloud_box(image, 36, 8, 3, 3, 4)               # el otro bulto lateral
    for index, color in enumerate(CLOUD_STRIPS):
        for y in range(40 + index * 4, 44 + index * 4):
            for x in range(64):
                image.putpixel((x, y), color)
    image.save(OUT / ("cloud_pet_sleep.png" if sleeping else "cloud_pet.png"))


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
    make_pet(sleeping=True)
    make_cloud_pet()
    make_cloud_pet(sleeping=True)
    make_halo()
    make_halo_styles()
    make_cape()
    print("cosmetic textures written to", OUT)


if __name__ == "__main__":
    main()
