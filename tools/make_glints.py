"""Genera los paquetes integrados de color del brillo de encantamiento (mod Visuals).

Uso: python3 tools/make_glints.py   (requiere Pillow)
Cada paquete reemplaza enchanted_glint_item.png y enchanted_glint_armor.png por un brillo propio del color elegido.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image

BASE = Path(__file__).resolve().parent.parent / "src/main/resources/resourcepacks"
COLORS = {
    "red": (255, 70, 90),
    "gold": (255, 200, 70),
    "sky": (90, 180, 255),
    "pink": (255, 120, 210),
    "white": (240, 240, 255),
    "purple": (170, 90, 255),
    "green": (90, 255, 120),
}
SIZE = 64


def brightness(x, y, noise):
    """Rayas diagonales suaves que se repiten sin cortes cada 64 píxeles."""
    a = math.sin((x + y) * 2 * math.pi / 32 + 1.2 * math.sin(y * 2 * math.pi / 64))
    b = math.sin((x - 2 * y) * 2 * math.pi / 64 + 0.8 * math.sin(x * 2 * math.pi / 32))
    v = 0.5 + 0.3 * a + 0.2 * b
    v = v * 0.85 + 0.15 * noise[y][x]
    return max(0.0, min(1.0, v)) ** 1.6


def main():
    random.seed(7)
    noise = [[random.random() for _ in range(SIZE)] for _ in range(SIZE)]
    for name, (r, g, b) in COLORS.items():
        root = BASE / f"glint_{name}"
        textures = root / "assets/minecraft/textures/misc"
        textures.mkdir(parents=True, exist_ok=True)
        image = Image.new("RGBA", (SIZE, SIZE))
        for y in range(SIZE):
            for x in range(SIZE):
                v = brightness(x, y, noise)
                image.putpixel((x, y), (int(r * v), int(g * v), int(b * v), 255))
        image.save(textures / "enchanted_glint_item.png")
        image.save(textures / "enchanted_glint_armor.png")
        meta = {"pack": {"description": f"FreedomClient glint color: {name}", "pack_format": 75, "min_format": 75, "max_format": 75}}
        (root / "pack.mcmeta").write_text(json.dumps(meta, indent="\t") + "\n")
    print(f"{len(COLORS)} glint packs written to {BASE}")


if __name__ == "__main__":
    main()
