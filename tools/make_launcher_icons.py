"""Genera los iconos del launcher (Tauri) y el logo de la interfaz.

Uso: python3 tools/make_launcher_icons.py   (requiere Pillow)
El icono es un cuadrado redondeado con el cielo de atardecer en franjas pixel, las iniciales FC
con degradado y contorno, y el halo dorado encima. Se dibuja a 32x32 y se escala sin suavizar.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "launcher"
ICONS = ROOT / "app/icons"
UI = ROOT / "ui/img"

FC = [
    "#######...######",
    "#######..#######",
    "##.......##.....",
    "##.......##.....",
    "######...##.....",
    "######...##.....",
    "##.......##.....",
    "##.......##.....",
    "##.......#######",
    "##........######",
]
FC_ROWS = [0xFFFFFF, 0xF5F1E8, 0xF5F1E8, 0xF5F1E8, 0xF7E6C8, 0xF7E6C8, 0xF5D98A, 0xF2C94C, 0xE8A93A, 0xD7263D]
SKY_ALL = [0x1A0B2E, 0x241035, 0x2E1339, 0x3B1639, 0x4A1838, 0x5C1A35, 0x701C31, 0x86202E,
       0x9C262C, 0xB22F2A, 0xC63D29, 0xD74F2A, 0xE4642E, 0xEE7A34, 0xF4913C, 0xF7A845]
SKY = SKY_ALL[1::2]
OUTLINE = (26, 5, 8, 255)
GOLD = (242, 201, 76, 255)
GOLD_DARK = (201, 143, 30, 255)


def rgb(value, alpha=255):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, alpha)


def logo(image, ox, oy):
    """FC con sombra, contorno y degradado por filas en (ox, oy)."""
    cells = {(x, y) for y, row in enumerate(FC) for x, c in enumerate(row) if c == "#"}
    for (x, y) in cells:
        p = (ox + x + 1, oy + y + 1)
        if p[0] < image.width and p[1] < image.height:
            image.putpixel(p, (26, 5, 8, 150))
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in cells:
                image.putpixel((ox + x + dx, oy + y + dy), OUTLINE)
    for (x, y) in cells:
        image.putpixel((ox + x, oy + y), rgb(FC_ROWS[y]))


def halo(image, cx, cy, rx, ry=2):
    """Anillo aplanado como el del menú: borde de arriba dorado y el de abajo más oscuro."""
    for x in range(-rx, rx + 1):
        t = x / rx
        dy = round((max(0.0, 1 - t * t)) ** 0.5 * ry)
        image.putpixel((cx + x, cy - dy - 1), GOLD)
        image.putpixel((cx + x, cy + dy), GOLD_DARK)


def icon():
    size = 32
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    radius = 5
    for y in range(size):
        band = SKY[min(len(SKY) - 1, y * len(SKY) // size)]
        for x in range(size):
            # Esquinas redondeadas en pixel.
            cx = min(x, size - 1 - x)
            cy = min(y, size - 1 - y)
            if cx < radius and cy < radius and (radius - cx - 0.5) ** 2 + (radius - cy - 0.5) ** 2 > radius ** 2:
                continue
            image.putpixel((x, y), rgb(band))
    # Tramado entre franjas para que parezca pixel art.
    for y in range(1, size):
        above = SKY[min(len(SKY) - 1, (y - 1) * len(SKY) // size)]
        here = SKY[min(len(SKY) - 1, y * len(SKY) // size)]
        if above != here:
            for x in range(y % 2, size, 2):
                if image.getpixel((x, y - 1))[3]:
                    image.putpixel((x, y - 1), rgb(here))
    # Sol que asoma abajo.
    for x in range(size):
        for y in range(size):
            if (x - 16) ** 2 + (y - 33) ** 2 <= 49 and image.getpixel((x, y))[3]:
                image.putpixel((x, y), rgb(0xFFE08A))
    halo(image, 16, 7, 7)
    logo(image, 8, 12)
    return image


def ui_logo():
    """Logo sin fondo para la cabecera del launcher: halo encima de las iniciales."""
    image = Image.new("RGBA", (20, 18), (0, 0, 0, 0))
    halo(image, 9, 3, 7)
    logo(image, 1, 7)
    return image


def main():
    ICONS.mkdir(parents=True, exist_ok=True)
    UI.mkdir(parents=True, exist_ok=True)
    base = icon()
    scale = lambda img, s: img.resize((s, s), Image.NEAREST)
    scale(base, 32).save(ICONS / "32x32.png")
    scale(base, 128).save(ICONS / "128x128.png")
    scale(base, 256).save(ICONS / "128x128@2x.png")
    scale(base, 512).save(ICONS / "icon.png")
    scale(base, 256).save(ICONS / "icon.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    scale(base, 64).save(UI / "icon.png")
    small = ui_logo()
    small.resize((small.width * 4, small.height * 4), Image.NEAREST).save(UI / "logo.png")
    print("icons written to", ICONS, "and", UI)


if __name__ == "__main__":
    main()
