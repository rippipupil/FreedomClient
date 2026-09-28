"""Genera las partículas "pixel 3D" de Hit Particles: plumas, copos de nieve y calabazas hechos de vóxeles.

Uso: python3 tools/make_voxel_particles.py   (requiere Pillow)
Cada modelo se dibuja girando sobre sí mismo en FRAMES posiciones (sprites hit_<nombre>_<n>.png de 16x16); la
partícula va pasando por ellos, así parece un objeto 3D pixelado que da vueltas. Se rasteriza sin suavizado.
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freedomclient/textures/particle"
SIZE = 16
FRAMES = 8
TILT = math.radians(24)
_l = (-0.45, 0.75, -0.5)
LIGHT = tuple(c / math.sqrt(sum(v * v for v in _l)) for c in _l)

# Cada modelo: capas (de delante a atrás) de filas; cada carácter es un color de la paleta del modelo.
FEATHER = {
    "colors": {"w": (250, 248, 244), "W": (214, 210, 222), "s": (170, 150, 128), "t": (255, 214, 120)},
    "layers": [[
        "....ww.",
        "...wwW.",
        "..wwWW.",
        "..wwWs.",
        ".wwWsW.",
        ".wwsWW.",
        ".wWsW..",
        "wwsW...",
        "wWs....",
        ".s.....",
        "t......",
    ]],
}

SNOWFLAKE = {
    "colors": {"w": (255, 255, 255), "c": (196, 238, 255), "b": (120, 196, 255)},
    "layers": [[
        "....c....",
        ".c..w..c.",
        "..c.w.c..",
        "...cwc...",
        "cwwwbwwwc",
        "...cwc...",
        "..c.w.c..",
        ".c..w..c.",
        "....c....",
    ]],
}


def pumpkin_layers():
    """Calabaza de 7x5x7 con gajos verticales, cara tallada delante y rabito verde arriba."""
    face = ["..y.y..", ".......", ".yyyyy.", "..y.y.."]
    layers = []
    for z in range(7):
        rows = []
        # Rabito: dos filas encima del cuerpo, solo en el centro.
        rows.append("...g..." if z == 3 else ".......")
        rows.append("...G..." if z in (3, 4) else ".......")
        for y in range(5):
            row = ""
            for x in range(7):
                corner = (x in (0, 6) and z in (0, 6)) or (y in (0, 4) and (x in (0, 6) or z in (0, 6)))
                if corner:
                    row += "."
                elif z == 0 and 0 < y < 5 and face[y - 1][x] == "y":
                    row += "y"
                else:
                    row += "O" if (x + z) % 2 else "o"
            rows.append(row)
        layers.append(rows)
    return layers


PUMPKIN = {
    "colors": {"o": (244, 142, 38), "O": (206, 104, 22), "g": (90, 160, 56), "G": (58, 112, 36), "y": (255, 226, 96)},
    "layers": pumpkin_layers(),
}

MODELS = {"feather": (FEATHER, 1.35), "snow": (SNOWFLAKE, 1.35), "pumpkin": (PUMPKIN, 1.45)}


def voxels(model):
    layers = model["layers"]
    depth = len(layers)
    height = len(layers[0])
    width = len(layers[0][0])
    out = []
    for z, layer in enumerate(layers):
        for y, row in enumerate(layer):
            for x, char in enumerate(row):
                if char != ".":
                    # Centrado en el origen; y hacia arriba.
                    out.append((x - (width - 1) / 2, (height - 1) / 2 - y, z - (depth - 1) / 2, model["colors"][char]))
    return out


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color) + (255,)


def rotate(point, yaw):
    x, y, z = point
    cos, sin = math.cos(yaw), math.sin(yaw)
    x, z = x * cos + z * sin, -x * sin + z * cos
    # Inclinación fija hacia la cámara para que se vea también la cara de arriba.
    cos, sin = math.cos(TILT), math.sin(TILT)
    y, z = y * cos - z * sin, y * sin + z * cos
    return x, y, z


FACES = [
    # (normal, esquinas del cubo unidad centrado, brillo)
    ((0, 0, -1), [(-1, -1, -1), (1, -1, -1), (1, 1, -1), (-1, 1, -1)], 1.0),
    ((0, 0, 1), [(-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1)], 0.62),
    ((0, 1, 0), [(-1, 1, -1), (1, 1, -1), (1, 1, 1), (-1, 1, 1)], 1.12),
    ((0, -1, 0), [(-1, -1, -1), (1, -1, -1), (1, -1, 1), (-1, -1, 1)], 0.55),
    ((1, 0, 0), [(1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)], 0.78),
    ((-1, 0, 0), [(-1, -1, -1), (-1, 1, -1), (-1, 1, 1), (-1, -1, 1)], 0.84),
]


def render(model, scale, yaw):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    quads = []
    for vx, vy, vz, color in voxels(model):
        for normal, corners, light in FACES:
            n = rotate(normal, yaw)
            if n[2] >= 0:  # mira hacia atrás: no se ve (la cámara está en -z)
                continue
            points = [rotate((vx + cx * 0.5, vy + cy * 0.5, vz + cz * 0.5), yaw) for cx, cy, cz in corners]
            depth = sum(p[2] for p in points) / 4
            # Luz fija desde arriba a la izquierda y delante: al girar, cada cara se ilumina según hacia dónde mira.
            dot = n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2]
            factor = 0.58 + 0.52 * max(0.0, dot)
            quads.append((depth, [(SIZE / 2 + p[0] * scale, SIZE / 2 - p[1] * scale) for p in points], shade(color, factor)))
    for _, polygon, fill in sorted(quads, key=lambda q: -q[0]):
        draw.polygon(polygon, fill=fill)
    # Contorno oscuro de 1 px alrededor para que se lea sobre cualquier fondo.
    alpha = image.getchannel("A")
    outline = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y in range(SIZE):
        for x in range(SIZE):
            if alpha.getpixel((x, y)):
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < SIZE and 0 <= ny < SIZE and alpha.getpixel((nx, ny)):
                    outline.putpixel((x, y), (30, 20, 30, 170))
                    break
    return Image.alpha_composite(outline, image)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (model, scale) in MODELS.items():
        for frame in range(FRAMES):
            render(model, scale, math.tau * frame / FRAMES).save(OUT / f"hit_{name}_{frame}.png")
    print("voxel particles written to", OUT)


if __name__ == "__main__":
    main()
