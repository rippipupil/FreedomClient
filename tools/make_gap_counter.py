"""Genera Gap Counter: manzanas de oro (gaps) y de Notch con el número de la pila dibujado encima.

Uso: python3 tools/make_gap_counter.py   (requiere Pillow)
- textures/item/gapcounter/{gap,notch}_N.png (32x32): manzana estilo vanilla (dibujada aquí, no copiada) con el
  contorno del color de la cantidad (claro = muchas, oscuro = pocas) y el número grande abajo a la derecha.
- textures/item/gapcounter/badge_{gap,notch}_N.png: solo el número sobre una placa, para usar la manzana del
  paquete de texturas del jugador.
- textures/item/gapcounter/pumpkin_{gap,notch}[_N].png: estilo calabaza (dorada para las gaps y tallada para las
  de Notch), con el mismo contorno por cantidad y el número.
- Modelos y los paquetes integrados (resourcepacks/gapcounter_vanilla, gapcounter_pumpkin y gapcounter_pack) que cambian
  items/golden_apple.json y items/enchanted_golden_apple.json con "range_dispatch" según la cantidad. En el
  inventario se ve la manzana normal (el número ya lo pone el juego).
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
TEXTURES = ROOT / "assets/freedomclient/textures/item/gapcounter"
MODELS = ROOT / "assets/freedomclient/models/item/gapcounter"
PACKS = ROOT / "resourcepacks"
MAX = 64

APPLE = [
    "................",
    "........kk......",
    ".......kbk.kk...",
    "........kbkggk..",
    ".....kkkkbkGk...",
    "....kYYyykyyOk..",
    "...kYWYyyyyyyOk.",
    "...kYYyyyyyyyOk.",
    "...kyyyyyyyyOOk.",
    "...kyyyyyyyyOOk.",
    "...kOyyyyyyyOOk.",
    "....kOyyyyyOOk..",
    "....kOOyyyOOOk..",
    ".....kkOOOOkk...",
    ".......kkkk.....",
    "................",
]
# Calabaza dorada (gaps) y calabaza dorada tallada (Notch) para el estilo "Pumpkin".
PUMPKIN = [
    "................",
    "........gl......",
    ".......gGll.....",
    "....kkkgkkkk....",
    "...kYyYyyYyyk...",
    "..kYWyYyyyYyOk..",
    "..kYyyYyyyYyOk..",
    ".kyYyyYyyyYyOOk.",
    ".kyYyyYyyyYyOOk.",
    ".kyYyyYyyyYyOOk.",
    ".kyyyyYyyyYyOOk.",
    "..kOyyOyyyOyOk..",
    "..kOOyOyyyOOOk..",
    "...kkOOOOOOkk...",
    ".....kkkkkk.....",
    "................",
]
CARVED_PUMPKIN = [
    "................",
    "........gl......",
    ".......gGll.....",
    "....kkkgkkkk....",
    "...kYyYyyYyyk...",
    "..kYWyYyyyYyOk..",
    "..kYydyyyydyOk..",
    ".kyYdFdyydFdOOk.",
    ".kyYyyYddyYyOOk.",
    ".kyYyyYyyyYyOOk.",
    ".kydyyYyyyYydOk.",
    "..kdFdFdFdFdOk..",
    "..kOdFdFdFdOOk..",
    "...kkOOOOOOkk...",
    ".....kkkkkk.....",
    "................",
]
APPLE_COLORS = {
    "k": (92, 58, 10), "b": (107, 74, 32), "g": (84, 184, 74), "G": (46, 122, 46),
    "Y": (255, 243, 160), "W": (255, 255, 255), "y": (242, 201, 76), "O": (201, 143, 30),
    "l": (110, 190, 70), "d": (96, 34, 4), "F": (255, 150, 30),
}
# Colores del contorno según la cantidad: de oscuro (pocas) a claro (muchas).
GAP_STOPS = [(0.0, (74, 14, 20)), (0.12, (168, 24, 46)), (0.3, (255, 106, 42)), (0.55, (242, 201, 76)),
             (0.8, (255, 230, 128)), (1.0, (255, 251, 224))]
NOTCH_STOPS = [(0.0, (58, 10, 74)), (0.15, (122, 31, 168)), (0.35, (192, 80, 224)), (0.6, (255, 122, 217)),
               (0.85, (255, 194, 238)), (1.0, (255, 240, 250))]
DIGITS = {
    "0": [".##.", "#..#", "#..#", "#..#", "#..#", ".##."],
    "1": [".#..", "##..", ".#..", ".#..", ".#..", "###."],
    "2": [".##.", "#..#", "..#.", ".#..", "#...", "####"],
    "3": ["###.", "...#", ".##.", "...#", "...#", "###."],
    "4": ["#..#", "#..#", "####", "...#", "...#", "...#"],
    "5": ["####", "#...", "###.", "...#", "...#", "###."],
    "6": [".##.", "#...", "###.", "#..#", "#..#", ".##."],
    "7": ["####", "...#", "..#.", ".#..", ".#..", ".#.."],
    "8": [".##.", "#..#", ".##.", "#..#", "#..#", ".##."],
    "9": [".##.", "#..#", "#..#", ".###", "...#", ".##."],
}


def gradient(stops, t):
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1))
    return stops[-1][1]


def apple(border=None, sprite=None):
    sprite = sprite or APPLE
    assert all(len(row) == 16 for row in sprite) and len(sprite) == 16
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(sprite):
        for x, c in enumerate(row):
            if c != ".":
                color = border if (c == "k" and border) else APPLE_COLORS[c]
                image.putpixel((x, y), color + (255,))
    return image


def number_mask(n):
    """Píxeles del número a 32x32: cifras de 4x6 dobladas a 8x12, separadas 2 px."""
    text = str(n)
    width = len(text) * 8 + (len(text) - 1) * 2
    pixels = set()
    for i, ch in enumerate(text):
        ox = i * 10
        for y, row in enumerate(DIGITS[ch]):
            for x, c in enumerate(row):
                if c == "#":
                    for dx in range(2):
                        for dy in range(2):
                            pixels.add((ox + x * 2 + dx, y * 2 + dy))
    return pixels, width, 12


def draw_number(image, n, right=31, bottom=31):
    pixels, width, height = number_mask(n)
    ox, oy = right - width, bottom - height
    outline = set()
    for x, y in pixels:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in pixels:
                    outline.add((x + dx, y + dy))
    for x, y in outline:
        if 0 <= ox + x < 32 and 0 <= oy + y < 32:
            image.putpixel((ox + x, oy + y), (20, 8, 10, 255))
    for x, y in pixels:
        image.putpixel((ox + x, oy + y), (255, 255, 255, 255))
    return ox, oy, width, height


def numbered(n, stops, sprite=None):
    color = gradient(stops, (n - 1) / (MAX - 1))
    base = apple(color, sprite).resize((32, 32), Image.NEAREST)
    # Contorno de 1 px más por fuera, del mismo color un poco más oscuro, para que se vea de lejos.
    alpha = base.getchannel("A")
    ring = tuple(max(0, int(c * 0.7)) for c in color) + (255,)
    for y in range(32):
        for x in range(32):
            if alpha.getpixel((x, y)):
                continue
            if any(0 <= x + dx < 32 and 0 <= y + dy < 32 and alpha.getpixel((x + dx, y + dy))
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                base.putpixel((x, y), ring)
    draw_number(base, n)
    return base


def badge(n, stops):
    color = gradient(stops, (n - 1) / (MAX - 1))
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    pixels, width, height = number_mask(n)
    # Placa del color de la cantidad detrás del número, con borde oscuro.
    x0, y0, x1, y1 = 31 - width - 3, 31 - height - 3, 31, 31
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            corner = (x in (x0, x1)) and (y in (y0, y1))
            if corner:
                continue
            image.putpixel((x, y), (20, 8, 10, 255) if edge else tuple(int(c * 0.8) for c in color) + (255,))
    draw_number(image, n, right=29, bottom=29)
    return image


def model(path, layers):
    textures = {f"layer{i}": texture for i, texture in enumerate(layers)}
    path.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": textures}, indent=1))


def item_definition(kind, plain_model, numbered_prefix):
    entries = [{"threshold": n, "model": {"type": "minecraft:model", "model": f"{numbered_prefix}_{n}"}} for n in range(1, MAX + 1)]
    return {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [{"when": "gui", "model": {"type": "minecraft:model", "model": plain_model}}],
            "fallback": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:count",
                "normalize": False,
                "entries": entries,
                "fallback": {"type": "minecraft:model", "model": f"{numbered_prefix}_1"},
            },
        }
    }


def write_pack(name, description, definitions):
    pack = PACKS / name
    items = pack / "assets/minecraft/items"
    items.mkdir(parents=True, exist_ok=True)
    (pack / "pack.mcmeta").write_text(json.dumps({"pack": {"description": description, "pack_format": 75,
                                                            "min_format": 75, "max_format": 75}}, indent=1))
    for item, definition in definitions.items():
        (items / f"{item}.json").write_text(json.dumps(definition, indent=1))


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    apple().save(TEXTURES / "apple.png")
    model(MODELS / "apple.json", ["freedomclient:item/gapcounter/apple"])
    sprites = {"gap": PUMPKIN, "notch": CARVED_PUMPKIN}
    for kind, sprite in sprites.items():
        apple(sprite=sprite).save(TEXTURES / f"pumpkin_{kind}.png")
        model(MODELS / f"pumpkin_{kind}.json", [f"freedomclient:item/gapcounter/pumpkin_{kind}"])
    for kind, stops in (("gap", GAP_STOPS), ("notch", NOTCH_STOPS)):
        for n in range(1, MAX + 1):
            numbered(n, stops).save(TEXTURES / f"{kind}_{n}.png")
            badge(n, stops).save(TEXTURES / f"badge_{kind}_{n}.png")
            numbered(n, stops, sprites[kind]).save(TEXTURES / f"pumpkin_{kind}_{n}.png")
            model(MODELS / f"pumpkin_{kind}_{n}.json", [f"freedomclient:item/gapcounter/pumpkin_{kind}_{n}"])
            model(MODELS / f"{kind}_{n}.json", [f"freedomclient:item/gapcounter/{kind}_{n}"])
            vanilla = "minecraft:item/golden_apple" if kind == "gap" else "minecraft:item/enchanted_golden_apple"
            model(MODELS / f"pack_{kind}_{n}.json", [vanilla.replace("item/enchanted_golden_apple", "item/golden_apple"),
                                                     f"freedomclient:item/gapcounter/badge_{kind}_{n}"])
    write_pack("gapcounter_vanilla", "FreedomClient Gap Counter: numbered golden apples", {
        "golden_apple": item_definition("gap", "freedomclient:item/gapcounter/apple", "freedomclient:item/gapcounter/gap"),
        "enchanted_golden_apple": item_definition("notch", "freedomclient:item/gapcounter/apple", "freedomclient:item/gapcounter/notch"),
    })
    write_pack("gapcounter_pumpkin", "FreedomClient Gap Counter: numbered golden pumpkins", {
        "golden_apple": item_definition("gap", "freedomclient:item/gapcounter/pumpkin_gap", "freedomclient:item/gapcounter/pumpkin_gap"),
        "enchanted_golden_apple": item_definition("notch", "freedomclient:item/gapcounter/pumpkin_notch", "freedomclient:item/gapcounter/pumpkin_notch"),
    })
    write_pack("gapcounter_pack", "FreedomClient Gap Counter: numbers over your pack's golden apples", {
        "golden_apple": item_definition("gap", "minecraft:item/golden_apple", "freedomclient:item/gapcounter/pack_gap"),
        "enchanted_golden_apple": item_definition("notch", "minecraft:item/enchanted_golden_apple", "freedomclient:item/gapcounter/pack_notch"),
    })
    print("gap counter written")


if __name__ == "__main__":
    main()
