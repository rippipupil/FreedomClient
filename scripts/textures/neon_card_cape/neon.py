"""Capa Neon: tarjeta de agente con el puño de Neon cargado soltando rayos (animada)."""
from PIL import Image, ImageDraw
import math, random

W, H = 80, 128
N = 12

BG_TOP = (14, 30, 42)
BG_BOT = (20, 52, 60)
PANEL = (26, 62, 72)
PANEL2 = (34, 78, 86)
LINE = (120, 214, 206)
LINE_DIM = (52, 120, 124)
CYAN = (80, 236, 255)
CYAN_L = (190, 252, 255)
WHITE = (250, 255, 255)
YELLOW = (232, 255, 80)
YELLOW_D = (170, 210, 40)
GOLD = (236, 186, 70)
GOLD_D = (170, 120, 40)
GLOVE = (28, 34, 70)
GLOVE_L = (58, 66, 120)
PAD = (206, 214, 232)
PAD_D = (150, 158, 186)
SLEEVE = (60, 36, 98)
SLEEVE_L = (96, 62, 140)
SKIN = (128, 86, 64)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def bolt(rnd, x0, y0, x1, y1, rough, depth=5):
    pts = [(x0, y0), (x1, y1)]
    for _ in range(depth):
        new = [pts[0]]
        for (ax, ay), (bx, by) in zip(pts, pts[1:]):
            mx, my = (ax + bx) / 2, (ay + by) / 2
            dx, dy = bx - ax, by - ay
            ln = math.hypot(dx, dy) or 1
            off = (rnd.random() - 0.5) * rough * ln / 6
            new += [(mx - dy / ln * off, my + dx / ln * off), (bx, by)]
        pts = new
    return pts


def line_pixels(pts):
    out = []
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        steps = int(max(abs(bx - ax), abs(by - ay))) + 1
        for k in range(steps + 1):
            t = k / steps
            out.append((int(round(ax + (bx - ax) * t)), int(round(ay + (by - ay) * t))))
    return out


def frame(f, height=H):
    oy = height - H
    img = Image.new('RGB', (W, height))
    px = img.load()
    # Fondo: degradado azul petróleo con paneles diagonales más claros.
    for y in range(height):
        for x in range(W):
            c = mix(BG_TOP, BG_BOT, y / height)
            # Paneles en diagonal (como los cristales del fondo de la tarjeta).
            band = (x * 0.8 + y) % 46
            if 30 <= band < 38 and y < 88 + oy:
                c = mix(c, PANEL, 0.7)
            if 36 <= band < 38 and y < 88 + oy:
                c = mix(c, PANEL2, 0.8)
            px[x, y] = c
    d = ImageDraw.Draw(img)
    rnd = random.Random(f * 7919 + 3)
    t = f / N
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * t * 2)
    fx, fy = 52, 28 + oy  # puño
    # Resplandor del puño (anillos escalonados, estilo pixel).
    R = 20 + pulse * 4
    for y in range(height):
        for x in range(W):
            dd = math.hypot(x - fx, (y - fy) * 1.05)
            if dd < R:
                k = 1 - dd / R
                k = round(k * 4) / 4
                px[x, y] = mix(px[x, y], CYAN, k * 0.55)
    # Mechones eléctricos de Neon asomando por la izquierda (azules con las puntas cargadas).
    for k, (bx, by, tx, ty) in enumerate(((0, 70, 12, 50), (0, 64, 16, 58), (0, 76, 18, 66), (2, 82, 20, 74))):
        flick = rnd.randint(-2, 2)
        d.polygon([(bx, by + oy), (tx + flick, ty + oy), (bx + 3, by + 5 + oy)], fill=(40, 70, 170))
        d.line([(tx + flick, ty + oy), (tx + flick - 3, ty + 4 + oy)], fill=YELLOW if (k + f) % 2 else CYAN_L)
    # Brazo de Neon: manga morada desde abajo a la izquierda, pulsera dorada y guante oscuro con protecciones.
    d.polygon([(0, 112 + oy), (0, 80 + oy), (20, 62 + oy), (32, 56 + oy), (40, 66 + oy), (24, 84 + oy), (10, 112 + oy)], fill=SLEEVE)
    d.polygon([(0, 84 + oy), (20, 64 + oy), (28, 60 + oy), (22, 66 + oy), (2, 88 + oy)], fill=SLEEVE_L)
    # Guantelete del antebrazo con su franja clara.
    d.polygon([(24, 60 + oy), (34, 48 + oy), (44, 56 + oy), (34, 68 + oy)], fill=GLOVE)
    d.polygon([(27, 59 + oy), (35, 50 + oy), (37, 52 + oy), (29, 61 + oy)], fill=GLOVE_L)
    # Pulsera dorada gruesa en la muñeca.
    d.polygon([(32, 48 + oy), (38, 42 + oy), (48, 50 + oy), (42, 57 + oy)], fill=GOLD_D)
    d.polygon([(33, 47 + oy), (38, 43 + oy), (41, 45 + oy), (36, 50 + oy)], fill=GOLD)
    # Puño: guante oscuro redondeado con el pulgar y cuatro placas en los nudillos.
    d.polygon([(38, 42 + oy), (40, 32 + oy), (46, 25 + oy), (56, 27 + oy), (60, 34 + oy), (56, 44 + oy), (48, 50 + oy)], fill=GLOVE)
    d.polygon([(40, 40 + oy), (41, 33 + oy), (46, 27 + oy), (49, 28 + oy), (44, 34 + oy), (43, 41 + oy)], fill=GLOVE_L)
    d.polygon([(46, 44 + oy), (54, 40 + oy), (56, 44 + oy), (50, 48 + oy)], fill=GLOVE_L)
    for k, (x, y) in enumerate(((48, 26), (53, 27), (57, 31), (58, 36))):
        d.rectangle([x - 2, y - 2 + oy, x + 1, y + 1 + oy], fill=PAD_D)
        d.rectangle([x - 2, y - 2 + oy, x, y + oy], fill=PAD)
    # Rayos: varios saliendo del puño hacia arriba, nuevos en cada fotograma.
    targets = [(78, 2), (70, -2), (60, 0), (30, 4), (78, 20), (14, 14)]
    rnd.shuffle(targets)
    for k, (tx, ty) in enumerate(targets[:4]):
        tx += rnd.randint(-6, 6); ty += rnd.randint(-4, 6)
        pts = bolt(rnd, fx + 2, fy - 2, tx, ty + oy, 2.2)
        main = k < 2
        colour = CYAN_L if main else YELLOW
        halo = CYAN if main else YELLOW_D
        pix = line_pixels(pts)
        for x, y in pix:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < W and 0 <= y + dy < height: px[x + dx, y + dy] = mix(px[x + dx, y + dy], halo, 0.75)
        for x, y in pix:
            if 0 <= x < W and 0 <= y < height: px[x, y] = colour
        # Ramitas.
        for _ in range(2):
            i = rnd.randrange(len(pts) // 3, len(pts) - 1)
            bx, by = pts[i]
            br = bolt(rnd, bx, by, bx + rnd.randint(-12, 12), by + rnd.randint(-12, 4), 2.5, 3)
            for x, y in line_pixels(br):
                if 0 <= x < W and 0 <= y < height: px[x, y] = halo
    # Núcleo blanco del puño.
    for y in range(height):
        for x in range(W):
            dd = math.hypot(x - fx - 1, y - fy + 1)
            if dd < 4 + pulse * 1.5:
                px[x, y] = WHITE if dd < 2.5 + pulse else CYAN_L
    # Chispas que salen volando.
    for k in range(10):
        ang = rnd.random() * math.tau
        dist = 8 + ((t * 3 + k / 10) % 1.0) * 26
        x = int(fx + math.cos(ang) * dist); y = int(fy + math.sin(ang) * dist * 0.9)
        if 0 <= x < W and 0 <= y < height:
            px[x, y] = YELLOW if k % 2 else CYAN_L
    font = {'N': ["#..#", "##.#", "#.##", "#..#", "#..#"], 'E': ["####", "#...", "###.", "#...", "####"],
            'O': [".##.", "#..#", "#..#", "#..#", ".##."]}
    glow = 0.6 + 0.4 * pulse
    for i, ch in enumerate("NEON"):
        for yy, row in enumerate(font[ch]):
            for xx, c in enumerate(row):
                if c == '#': px[30 + i * 5 + xx, 82 + oy + yy] = mix(CYAN, CYAN_L, glow)
    # Parte de abajo: línea blanca, panel oscuro con una barra y los dos chevrones amarillos.
    sep = 90 + oy
    for x in range(2, W - 2): px[x, sep] = (236, 246, 246)
    for y in range(sep + 1, height):
        for x in range(W):
            px[x, y] = mix((16, 34, 46), (26, 56, 66), (y - sep) / (height - sep))
    # Rayo de Neon tenue en el centro del panel.
    d.polygon([(43, sep + 14), (34, sep + 26), (40, sep + 26), (36, sep + 36), (46, sep + 22), (40, sep + 22), (44, sep + 14)], fill=(34, 70, 84))
    d.rectangle([22, sep + 6, 57, sep + 9], fill=(120, 132, 140))
    # Dos chevrones amarillos grandes e inclinados; un brillo los recorre de arriba abajo.
    shine = (f % N) / N
    for side in (-1, 1):
        for i in range(14):
            y0 = sep + 16 + i
            lit = abs(i / 14 - shine) < 0.12
            col = (255, 240, 160) if lit else GOLD
            for w in range(5):
                x = 40 + side * (24 - i // 2 + w)
                if 0 <= x < W: px[x, y0] = col
    # Borde inferior en V y marco fino de la tarjeta.
    for y in range(height - 12, height):
        k = y - (height - 12)
        for x in range(W):
            if abs(x - 40) < k * 3.2 - 10:
                px[x, y] = mix(px[x, y], (60, 110, 120), 0.5)
    for x in range(W):
        px[x, 0] = LINE_DIM; px[x, height - 1] = LINE_DIM
    for y in range(height):
        px[0, y] = LINE_DIM; px[W - 1, y] = LINE_DIM
        px[2, y] = LINE if y < sep else LINE_DIM
        px[W - 3, y] = LINE if y < sep else LINE_DIM
    # Pestaña de arriba con un rayito.
    d.polygon([(32, 0), (48, 0), (45, 5), (35, 5)], fill=(30, 50, 60))
    d.line([(32, 0), (35, 5), (45, 5), (48, 0)], fill=LINE)
    for x, y in ((41, 1), (40, 2), (39, 3), (40, 3), (41, 3), (40, 4)):
        px[x, y] = YELLOW
    return img


if __name__ == '__main__':
    fr = [frame(i) for i in (0, 3, 6, 9)]
    sheet = Image.new('RGB', (W * 4 + 12, H))
    for i, im in enumerate(fr): sheet.paste(im, (i * (W + 4), 0))
    sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save('preview.png')
