"""Tarjeta "Pilar" de Void Keys (versión 3 de card_versions.html): texturas de lo que no cambia, con el mismo dibujo
que la propuesta. El juego anima encima las esquirlas y el destello del borde.

    python3 card.py <carpeta de texturas>

- void_keys_card_glow.png   resplandor del haz sobre el azul noche (140 de ancho, centrado en el haz)
- void_keys_card_beam.png   el haz de luz (12x48), sin vaivén: el juego lo dibuja en franjas desplazadas
- void_keys_card_orb.png    la esfera y sus halos (52x34, el centro de la esfera en (26, 26))
- void_keys_card_spikes.png las púas de abajo meciéndose: 16 fotogramas de 330x26 uno debajo de otro
- void_keys_card_title.png  el rótulo VOID KEYS con su brillo y su sombra
- void_keys_card_edge.png   las púas del borde (blancas, el juego las tiñe): arriba en las filas 0-5, abajo en 6-11
"""
import math
import sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
W = 178  # ancho de la tarjeta de la propuesta
H = 58


def rng(seed):
    s = [seed]
    def r():
        s[0] = (s[0] * 16807) % 2147483647
        return s[0] / 2147483647
    return r


def rgba(c, a=1.0):
    return (c >> 16 & 255, c >> 8 & 255, c & 255, round(255 * a))


def put(img, x, y, c):
    w, h = img.size
    if 0 <= x < w and 0 <= y < h:
        img.putpixel((x, y), c)


def js_round(v):
    return math.floor(v + 0.5)


def fill_rect(img, x, y, w, h, c):
    x, y, w, h = js_round(x), js_round(y), js_round(w), js_round(h)
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(img, xx, yy, c)


def tri(img, ax, ay, bx, by, cx, cy, c, dx=0, dy=0):
    """El mismo relleno por filas que tri() de la propuesta."""
    min_y = math.floor(min(ay, by, cy))
    max_y = math.ceil(max(ay, by, cy))
    for y in range(min_y, max_y + 1):
        xs = []
        for x0, y0, x1, y1 in ((ax, ay, bx, by), (bx, by, cx, cy), (cx, cy, ax, ay)):
            if min(y0, y1) <= y < max(y0, y1) and y0 != y1:
                xs.append(x0 + (y - y0) * (x1 - x0) / (y1 - y0))
        if len(xs) >= 2:
            l, r = js_round(min(xs)), js_round(max(xs))
            for x in range(l, r):
                put(img, x + dx, y + dy, c)


# Púas: las de la propuesta (semilla 9, de x = 46 hasta W + 6) y, para tarjetas más anchas, más con otra semilla.
P = rng(9)
spikes = []
x = 46.0
while x < W + 6:
    spikes.append((x, 8 + P() * 16, 2 + P() * 3, P() * 6))
    x += 3 + P() * 4
# (los números siguientes de la semilla 9 son las esquirlas: el juego los saca igual)
extra = rng(19)
while x < 330:
    spikes.append((x, 8 + extra() * 16, 2 + extra() * 3, extra() * 6))
    x += 3 + extra() * 4

FRAMES, SW, SH = 16, 330, 26
sheet = Image.new('RGBA', (SW, SH * FRAMES))
for f in range(FRAMES):
    t = f / FRAMES * (2 * math.pi / 1.3)
    for sx, h, w, p in spikes:
        lean = math.sin(t * 1.3 + p) * 1.2
        color = rgba(0x1b2034 if sx % 2 != 0 else 0x141829)
        # En la tarjeta la base está en y = H; aquí, en la fila SH de cada fotograma.
        tri(sheet, sx - w, H, sx + lean, H - h, sx + w, H, color, 0, f * SH + SH - H)
sheet.save(f'{OUT}/void_keys_card_spikes.png')

# Resplandor del haz: mezcla con 0x3a4468 de k² · 0,8, k = 1 - |dx| / 70.
glow = Image.new('RGBA', (140, 64))
for xx in range(140):
    k = max(0.0, 1 - abs(xx + 0.5 - 70) / 70)
    for yy in range(64):
        glow.putpixel((xx, yy), rgba(0x3a4468, k * k * 0.8))
glow.save(f'{OUT}/void_keys_card_glow.png')

# El haz: núcleo blanco (0,9) y halo (0,25) que se ensanchan hacia abajo, centrado en x = 6.
beam = Image.new('RGBA', (12, 48))
for yy in range(48):
    w = 4 + yy * 0.06
    for xx in range(js_round(6 - w / 2 - 2), js_round(6 - w / 2 - 2) + js_round(w + 4)):
        put(beam, xx, yy, rgba(0xdce4ff, 0.25))
for yy in range(48):
    w = 4 + yy * 0.06
    for xx in range(js_round(6 - w / 2), js_round(6 - w / 2) + js_round(w)):
        put(beam, xx, yy, rgba(0xffffff, 0.92))
beam.save(f'{OUT}/void_keys_card_beam.png')

# La esfera: halos (discos de 0,07 de radio 26, 23, 20 y 17) y media esfera blanca de radio 14 encima.
orb = Image.new('RGBA', (52, 34))
for yy in range(34):
    for xx in range(52):
        d = math.hypot(xx + 0.5 - 26, yy + 0.5 - 26)
        layers = sum(1 for r in (26, 23, 20, 17) if d <= r)
        a = 1 - (1 - 0.07) ** layers
        if a > 0:
            orb.putpixel((xx, yy), rgba(0xdce4ff, a))
        if d <= 14 and yy + 0.5 <= 26:
            orb.putpixel((xx, yy), rgba(0xf4f6ff))
orb.save(f'{OUT}/void_keys_card_orb.png')

# Rótulo: letras 5x7 con 2 de separación, brillo 3x3 blanco a 0,06, sombra 0x1b2034 abajo a la derecha.
FONT = {
    'V': ['10001', '10001', '10001', '10001', '01010', '01010', '00100'], 'O': ['01110', '10001', '10001', '10001', '10001', '10001', '01110'],
    'I': ['111', '010', '010', '010', '010', '010', '111'], 'D': ['11110', '10001', '10001', '10001', '10001', '10001', '11110'],
    'K': ['10001', '10010', '10100', '11000', '10100', '10010', '10001'], 'E': ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
    'Y': ['10001', '10001', '01010', '00100', '00100', '00100', '00100'], 'S': ['01111', '10000', '10000', '01110', '00001', '00001', '11110'],
    ' ': ['000', '000', '000', '000', '000', '000', '000'],
}
cells = []
cx = 1
for ch in 'VOID KEYS':
    rows = FONT[ch]
    for j, row in enumerate(rows):
        for i, b in enumerate(row):
            if b == '1':
                cells.append((cx + i, 1 + j))
    cx += len(rows[0]) + 2
title = Image.new('RGBA', (cx + 1, 10))
alpha = {}
for px, py in cells:
    for ddx in (-1, 0, 1):
        for ddy in (-1, 0, 1):
            alpha[(px + ddx, py + ddy)] = 1 - (1 - alpha.get((px + ddx, py + ddy), 0)) * (1 - 0.06)
for (px, py), a in alpha.items():
    put(title, px, py, rgba(0xffffff, a))
for px, py in cells:
    put(title, px + 1, py + 1, rgba(0x1b2034))
for px, py in cells:
    put(title, px, py, rgba(0xffffff))
title.save(f'{OUT}/void_keys_card_title.png')
print('title', title.size)

# Púas del borde cada 9 px (arriba entran desde y = 0, abajo desde y = H), en blanco para teñirlas.
edge = Image.new('RGBA', (330, 12))
white = rgba(0xffffff)
for x0 in range(6, 330, 9):
    h = 2 + (x0 * 7 % 5) * 0.6
    tri(edge, x0 - 1.5, 0, x0, h + 1, x0 + 1.5, 0, white)
    tri(edge, x0 + 3, H, x0 + 4.5, H - h - 1, x0 + 6, H, white, 0, 12 - H)
edge.save(f'{OUT}/void_keys_card_edge.png')
