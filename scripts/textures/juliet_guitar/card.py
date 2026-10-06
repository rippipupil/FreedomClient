"""Tarjeta de Juliet: un campo de flores rojas y blancas en un vacío blanco, con una flor morada en el medio, todo
plantado en un terreno blanco que se pierde a lo lejos. Borde de raíces blancas con espinas, una flor morada arriba a
la derecha y una roja abajo a la izquierda.

Capas del fondo (320x64, en bucle a lo ancho): lejos (cielo, suelo y flores lejanas y medias), neblina (se desplaza
despacio) y cerca (flores grandes). Aparte: la flor morada del centro, el rótulo JULIET, las raíces de los cuatro
lados y las dos flores de las esquinas.
    python3 card.py <carpeta de texturas>
"""
import math
import random
import sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
W, H = 320, 64
HORIZON = 27

# Colores del diseño de la guitarra.
RED, RED_D, RED_L = (177, 16, 28), (138, 13, 23), (224, 86, 106)
WHITE, WHITE_S, WHITE_D = (247, 247, 249), (205, 206, 214), (185, 186, 194)
LEAF, LEAF_L = (63, 125, 44), (140, 180, 60)
P_DARK, P_MID, P_BASE, P_LIGHT, P_GLOW = (53, 32, 110), (75, 49, 150), (90, 62, 168), (109, 80, 196), (138, 111, 216)
VOID = (250, 250, 252)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def put(img, x, y, c, wrap=True):
    w, h = img.size
    x, y = int(round(x)), int(round(y))
    if wrap: x %= w
    if 0 <= x < w and 0 <= y < h: img.load()[x, y] = c if len(c) == 4 else rgba(c)


def depth(y):
    """0 en el horizonte, 1 abajo del todo."""
    return max(0.0, min(1.0, (y - HORIZON) / (H - HORIZON)))


def fade(c, y):
    """Las cosas lejanas se pierden en el blanco del vacío."""
    return mix(c, VOID, (1 - depth(y)) ** 1.6 * 0.82)


# ---------- Lejos: vacío blanco, suelo blanco con perspectiva y el campo hasta media distancia ----------
far = Image.new('RGBA', (W, H))
p = far.load()
for y in range(H):
    for x in range(W):
        if y < HORIZON:
            # Vacío: blanco arriba, un velo lila muy suave junto al horizonte.
            t = (y / HORIZON) ** 3
            c = mix(VOID, (236, 233, 243), t * 0.8)
        else:
            d = depth(y)
            c = mix((238, 236, 243), (246, 245, 248), d)
        p[x, y] = rgba(c)
# Surcos del terreno: líneas que se separan al acercarse (perspectiva).
for k in range(1, 12):
    y = HORIZON + round((k / 11) ** 2 * (H - HORIZON - 1))
    for x in range(W):
        if (x + k * 7) % 5 != 0: put(far, x, y, mix(p[x % W, y][:3], (226, 223, 233), 0.35))
rnd = random.Random(21)
# Flores lejanas y medias: más pequeñas y más pálidas cuanto más lejos.
for i in range(900):
    y = HORIZON + 1 + (rnd.random() ** 1.7) * (H - HORIZON - 14)
    d = depth(y)
    x = rnd.random() * W
    red = rnd.random() < 0.55
    petal = RED if red else WHITE
    edge = RED_D if red else WHITE_D
    if d < 0.18:
        put(far, x, y, fade(RED if red else WHITE_D, y))
    elif d < 0.4:
        put(far, x, y + 1, fade(LEAF, y))
        put(far, x, y, fade(petal, y)); put(far, x + 1, y, fade(edge, y))
    else:
        # Florecita en cruz con tallo.
        for k in range(1, 3): put(far, x, y + k, fade(LEAF if k == 1 else LEAF_L, y))
        put(far, x, y - 1, fade(petal, y)); put(far, x - 1, y, fade(petal, y)); put(far, x + 1, y, fade(edge, y))
        put(far, x, y + 0.6, fade(edge, y)); put(far, x, y, fade(RED_D if not red else (90, 10, 18), y))
far.save(f'{OUT}/juliet_card_far.png')

# ---------- Neblina: un velo blanco a ras del horizonte que pasa despacio ----------
haze = Image.new('RGBA', (W, H))
for x in range(W):
    t = x / W * 2 * math.pi
    mid = HORIZON + 2 + 2 * math.sin(t * 2 + 1) + 1.5 * math.sin(t * 5)
    thick = 7 + 2 * math.sin(t * 3 + 2)
    for y in range(H):
        a = max(0.0, 1 - abs(y - mid) / thick)
        a = round(a * 5) / 5
        if a > 0: put(haze, x, y, (252, 252, 254, int(a * 150)))
haze.save(f'{OUT}/juliet_card_haze.png')

# ---------- Cerca: flores grandes rojas y blancas, con hojas ----------
near = Image.new('RGBA', (W, H))
rnd = random.Random(8)
RED_FLOWER = ['.rr.rr.', 'rRRrRRr', 'rRRcRRr', '.rRRRr.', '..rrr..']
WHITE_FLOWER = ['.ww.ww.', 'wWWwWWw', 'wWWcWWw', '.wWWWw.', '..www..']


def flower(img, x, y, rows, colors, stem):
    # Tallo y una hoja.
    for k in range(len(rows), len(rows) + stem):
        put(img, x + 3, y + k, LEAF)
    put(img, x + 4, y + len(rows) + stem // 2, LEAF_L); put(img, x + 5, y + len(rows) + stem // 2 - 1, LEAF_L)
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch != '.': put(img, x + c, y + r, colors[ch])


placed = []
for i in range(60):
    x = rnd.randrange(W)
    y = rnd.randint(H - 17, H - 6)
    red = rnd.random() < 0.5
    if red:
        flower(near, x, y, RED_FLOWER, {'r': RED_D, 'R': RED, 'c': P_DARK}, H - y)
    else:
        flower(near, x, y, WHITE_FLOWER, {'w': WHITE_D, 'W': WHITE, 'c': RED_L}, H - y)
# Hierba baja blanca y verde pálido a ras del suelo.
for x in range(W):
    for k in range(rnd.randint(0, 2)):
        put(near, x, H - 1 - k, mix(LEAF_L, VOID, 0.55) if (x * 7) % 3 else LEAF)
near.save(f'{OUT}/juliet_card_near.png')

# ---------- La flor morada del centro (como la de la guitarra), con su tallo ----------
FW, FH = 17, 26
fl = Image.new('RGBA', (FW, FH))
cx, cy = 8, 7
for y in range(15):
    for x in range(FW):
        fx, fy = x - cx, y - cy
        d = math.hypot(fx, fy); th = math.atan2(fy, fx)
        c = None
        if d <= 7.4 * 0.95 + 0.9 * math.cos(8 * th): c = P_MID
        if d <= 6.2 * 0.95 + 0.8 * math.cos(8 * th + 1.2): c = P_BASE
        if d <= 4.8 * 0.95 + 0.7 * math.cos(8 * th + 2.4): c = P_LIGHT
        if d <= 3.3 + 0.5 * math.cos(6 * th): c = P_BASE
        if d <= 2 + 0.3 * math.cos(6 * th + 1): c = P_MID
        if d <= 1: c = P_DARK
        if c: put(fl, x, y, c, False)
# Brillo arriba a la izquierda de los pétalos.
for x, y in [(4, 3), (5, 2), (3, 5)]: put(fl, x, y, P_GLOW, False)
for y in range(15, FH):
    put(fl, cx, y, LEAF, False)
for x, y in [(9, 18), (10, 17), (11, 17), (12, 16), (7, 21), (6, 20), (5, 20), (4, 19)]:
    put(fl, x, y, LEAF_L if y < 18 or x < 6 else LEAF, False)
fl.save(f'{OUT}/juliet_card_flower.png')

# ---------- Rótulo JULIET: letras pixel moradas con una gota roja ----------
L = {
    'J': ["..####", "....#.", "....#.", "....#.", "....#.", "#...#.", "#...#.", ".###..", "......"],
    'U': ["##..##", ".#..#.", ".#..#.", ".#..#.", ".#..#.", ".#..#.", ".#..#.", "..##..", "......"],
    'L': ["###...", ".#....", ".#....", ".#....", ".#....", ".#....", ".#...#", "######", "......"],
    'I': ["###", ".#.", ".#.", ".#.", ".#.", ".#.", ".#.", "###", "..."],
    'E': ["######", ".#...#", ".#....", ".####.", ".#....", ".#....", ".#...#", "######", "......"],
    'T': ["######", "#.##.#", "..##..", "..##..", "..##..", "..##..", "..##..", ".####.", "......"],
}
word = "JULIET"; gap = 2
TW = sum(len(L[c][0]) for c in word) + gap * (len(word) - 1) + 2
TH = 9 + 3
title = Image.new('RGBA', (TW, TH)); tp = title.load()
cells = set(); ox = 0
for ch in word:
    for y, row in enumerate(L[ch]):
        for x, c in enumerate(row):
            if c == '#': cells.add((ox + x, y))
    ox += len(L[ch][0]) + gap
for (x, y) in cells:
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        X, Y = x + dx, y + dy
        if 0 <= X < TW and 0 <= Y < TH and (X, Y) not in cells and tp[X, Y][3] == 0: tp[X, Y] = (255, 255, 255, 200)
for (x, y) in cells:
    if (x + 1, y + 1) not in cells and x + 1 < TW and y + 1 < TH: tp[x + 1, y + 1] = (200, 190, 228, 255)
for (x, y) in cells: tp[x, y] = rgba(P_BASE if y < 4 else P_MID)
# Una gota roja que cae de la J.
for x, y in [(4, 8), (4, 9), (4, 10)]: tp[x, y] = rgba(RED if y < 10 else RED_D)
title.save(f'{OUT}/juliet_title.png')
print('title', title.size)

# ---------- Raíces espinosas blancas para los bordes ----------
def roots(length, seed):
    """Tira de 6 px de grueso en bucle: dos raíces blancas que se enroscan una en la otra (una vuelta cada 16 px), con
    contorno gris para que se vean sobre el blanco, espinas oscuras que salen torcidas y alguna raicilla que cuelga
    hacia dentro (hacia y = 5)."""
    img = Image.new('RGBA', (length, 6))
    r = random.Random(seed)
    turns = (length // 20, length // 32)
    GREY, DARK, THORN = (150, 150, 162), (120, 120, 134), (98, 98, 112)
    def strand(x, phase):
        # Ondas de periodo entero sobre el largo (para el bucle), distintas para cada raíz: se cruzan sin regla fija.
        t = x / length * 2 * math.pi
        n = turns[0] if phase == 0.0 else turns[1]
        return 2.5 + 1.5 * math.sin(t * n + phase + 0.9 * math.sin(t * 3 + phase))
    for phase, front in ((math.pi, False), (0.0, True)):
        for x in range(length):
            y = int(round(strand(x, phase)))
            # La de delante tapa a la de detrás donde se cruzan.
            if not front and abs(y - int(round(strand(x, 0.0)))) <= 0: continue
            put(img, x, y - 1, WHITE_S if front else WHITE_D)
            put(img, x, y, WHITE if front else WHITE_S)
            put(img, x, y + 1, GREY if front else DARK)
    x = r.randrange(5)
    while x < length:
        phase = r.choice((0.0, math.pi))
        y = int(round(strand(x, phase)))
        if r.random() < 0.8:
            # Espina: sale de la raíz en diagonal, gris en la base y oscura en la punta.
            d = -1 if y <= 2 else 1
            side = r.choice((-1, 1))
            put(img, x, y + 2 * d if d > 0 else y - 1, GREY)
            put(img, x + side, y + 2 * d + d if d > 0 else y - 2, THORN)
        else:
            # Raicilla que cuelga hacia dentro.
            put(img, x, y + 2, WHITE_S); put(img, x + 1, y + 3, GREY); put(img, x + 1, y + 4, DARK); put(img, x + 2, y + 5, THORN)
        x += r.randint(4, 8)
    return img


top = roots(W, 3)
top.save(f'{OUT}/juliet_root_top.png')
roots(W, 5).transpose(Image.FLIP_TOP_BOTTOM).save(f'{OUT}/juliet_root_bottom.png')
roots(H, 7).transpose(Image.ROTATE_90).transpose(Image.FLIP_LEFT_RIGHT).save(f'{OUT}/juliet_root_left.png')
roots(H, 9).transpose(Image.ROTATE_90).save(f'{OUT}/juliet_root_right.png')


# ---------- Flores de las esquinas ----------
def corner(colors, name):
    img = Image.new('RGBA', (11, 11))
    for y in range(11):
        for x in range(11):
            fx, fy = x - 5, y - 5
            d = math.hypot(fx, fy); th = math.atan2(fy, fx)
            c = None
            if d <= 4.6 + 0.9 * math.cos(5 * th + 0.3): c = colors[0]
            if d <= 3.4 + 0.6 * math.cos(5 * th + 1.0): c = colors[1]
            if d <= 2.0 + 0.3 * math.cos(5 * th + 2): c = colors[2]
            if d <= 0.8: c = colors[3]
            if c: put(img, x, y, c, False)
    # Contorno gris claro para que se despegue del fondo blanco.
    p = img.load(); out = img.copy(); q = out.load()
    for y in range(11):
        for x in range(11):
            if p[x, y][3] == 0 and any(0 <= x + dx < 11 and 0 <= y + dy < 11 and p[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                q[x, y] = (255, 255, 255, 220)
    out.save(f'{OUT}/{name}.png')


corner([P_MID, P_BASE, P_LIGHT, P_DARK], 'juliet_corner_purple')
corner([RED_D, RED, RED_L, P_DARK], 'juliet_corner_red')
