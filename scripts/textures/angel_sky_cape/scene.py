"""Capa Angel Devil: Angel Devil mirando al cielo estrellado, alas batiendo, estrellas parpadeando, plumas cayendo."""
from PIL import Image, ImageDraw
import math, random

W, H = 80, 128
SS = 6

# Etiquetas de material
SKY, STAR, HALO, HALOG, HAIR, HAIRS, HAIRH, SKIN, SKINS, EYE, LASH, MOUTH, SHIRT, SHIRTS, TIE, JACK, JACKS, JACKH, \
    WING, WINGS, WINGL, WINGO, KANJI, FEATHER, OUTLINE, BLUSH, STAR2 = range(27)

def rot(px, py, cx, cy, a):
    c, s = math.cos(a), math.sin(a)
    dx, dy = px - cx, py - cy
    return (cx + dx * c - dy * s, cy + dx * s + dy * c)

class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new('L', (w * SS, h * SS), SKY)
        self.d = ImageDraw.Draw(self.img)
    def P(self, pts):
        return [(x * SS, y * SS) for x, y in pts]
    def poly(self, pts, lab):
        self.d.polygon(self.P(pts), fill=lab)
    def ell(self, cx, cy, rx, ry, lab):
        self.d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=lab)
    def rect(self, x0, y0, x1, y1, lab):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS - 1, y1 * SS - 1], fill=lab)
    def down(self):
        # Mayoría por bloque
        out = [[SKY] * self.w for _ in range(self.h)]
        px = self.img.load()
        for y in range(self.h):
            for x in range(self.w):
                cnt = {}
                for sy in range(SS):
                    for sx in range(SS):
                        v = px[x * SS + sx, y * SS + sy]
                        cnt[v] = cnt.get(v, 0) + 1
                out[y][x] = max(cnt.items(), key=lambda kv: kv[1])[0]
        return out

def feather(cv, R, root, tip, width, lab, edge=1.0):
    """Pluma en forma de hoja con borde oscuro: primero el contorno y luego el relleno un poco más pequeño."""
    rx, ry = root; tx, ty = tip
    dx, dy = tx - rx, ty - ry
    ln = math.hypot(dx, dy); ux, uy = dx / ln, dy / ln; nx, ny = -uy, ux
    def leaf(w, grow):
        pts = []
        for k in range(9):
            f = k / 8.0
            half = w * math.sin(math.pi * min(1.0, f * 1.15)) ** 0.7 + 0.01
            px = rx + dx * f + ux * grow * 0.35 * (2 * f - 1)
            py = ry + dy * f + uy * grow * 0.35 * (2 * f - 1)
            pts.append((px + nx * (half + grow), py + ny * (half + grow)))
        for k in range(8, -1, -1):
            f = k / 8.0
            half = w * math.sin(math.pi * min(1.0, f * 1.15)) ** 0.7 + 0.01
            px = rx + dx * f + ux * grow * 0.35 * (2 * f - 1)
            py = ry + dy * f + uy * grow * 0.35 * (2 * f - 1)
            pts.append((px - nx * (half + grow), py - ny * (half + grow)))
        return pts
    cv.poly(R(leaf(width, edge)), WINGO)
    cv.poly(R(leaf(width, 0.0)), lab)


def feather_wing(cv, ax, ay, side, ang, scale=1.0):
    """Ala abierta y levantada: pivote en el hombro (ax, ay); side -1 = ala de la izquierda. Coordenadas locales con
    x hacia fuera e y hacia abajo. Se dibuja de atrás adelante: primarias, secundarias, cobertoras y el borde."""
    def R(pts):
        return [rot(ax + side * px * scale, ay + py * scale, ax, ay, ang * side) for px, py in pts]
    # Primarias: largas, salen de la mano del ala (arriba fuera) y caen hacia fuera.
    for i in range(5):
        t = i / 4.0
        root = (24 - t * 6, -30 + t * 6)
        tip = (41 - t * 6, -13 + t * 19)
        feather(cv, R, root, tip, 3.4, WINGS if i % 2 else WING)
    # Secundarias: del antebrazo hacia abajo.
    for i in range(5):
        t = i / 4.0
        root = (18 - t * 14, -24 + t * 18)
        tip = (28 - t * 16, 7 + t * 9)
        feather(cv, R, root, tip, 3.6, WING if i % 2 else WINGS)
    # Cobertoras: plumas cortas y redondas en dos filas junto al brazo.
    for row, (off, ln, w) in enumerate(((7, 10, 3.0), (2, 7, 2.6))):
        for i in range(5):
            t = i / 4.0
            root = (27 - t * 24 - row * 1.5, -30 + t * 26 + off * 0.3)
            tip = (root[0] + 3 + (1 - t) * 3, root[1] + ln)
            feather(cv, R, root, tip, w, WINGL if (i + row) % 2 == 0 else WING)
    # Hueso del ala: borde de arriba blanco que sube del hombro hasta la muñeca.
    cv.poly(R([(-1, 2), (6, -12), (14, -24), (22, -32), (27, -33), (29, -30), (24, -27), (16, -20), (8, -8), (2, 4)]), WINGO)
    cv.poly(R([(0, 1), (7, -12), (15, -24), (22, -31), (26, -31.5), (27.5, -30), (23, -27.5), (16, -21), (8, -9), (1.5, 3)]), WINGL)

def scene(frame, nframes, height=H, seed=7):
    cv = Canvas(W, height)
    oy = height - H
    t = frame / nframes
    flap = math.sin(2 * math.pi * t) * math.radians(9)
    # Alas detrás del cuerpo.
    feather_wing(cv, 31, 84 + oy, -1, flap, 0.82)
    feather_wing(cv, 49, 84 + oy, 1, flap, 0.82)
    # Cuerpo: americana negra con hombros redondeados.
    cv.poly([(25, 84 + oy), (32, 79 + oy), (48, 79 + oy), (55, 84 + oy), (60, 96 + oy), (63, 128 + oy), (17, 128 + oy), (20, 96 + oy)], JACK)
    cv.ell(26, 88 + oy, 6, 6, JACK)
    cv.ell(54, 88 + oy, 6, 6, JACK)
    # Mangas: costura oscura entre los brazos y el pecho.
    cv.poly([(29.4, 90 + oy), (30.4, 90 + oy), (30.2, 128 + oy), (29.0, 128 + oy)], JACKS)
    cv.poly([(49.6, 90 + oy), (50.6, 90 + oy), (51.0, 128 + oy), (49.8, 128 + oy)], JACKS)
    # Sombra en el lado derecho de la americana.
    cv.poly([(50, 82 + oy), (55, 84 + oy), (60, 96 + oy), (63, 128 + oy), (54, 128 + oy), (52, 100 + oy)], JACKS)
    # Camisa blanca en V con solapas.
    cv.poly([(34, 78 + oy), (46, 78 + oy), (44, 92 + oy), (40, 108 + oy), (36, 92 + oy)], SHIRT)
    cv.poly([(42, 78 + oy), (46, 78 + oy), (44, 92 + oy), (41, 104 + oy)], SHIRTS)
    # Solapas (líneas claras de la americana).
    cv.poly([(33, 79 + oy), (35, 79 + oy), (39.5, 108 + oy), (38.5, 108 + oy)], JACKH)
    cv.poly([(45, 79 + oy), (47, 79 + oy), (41.5, 108 + oy), (40.5, 108 + oy)], JACKH)
    # Botón de la americana.
    cv.ell(40.5, 113 + oy, 1.0, 1.0, JACKH)
    # Corbata.
    cv.poly([(38.6, 79 + oy), (41.4, 79 + oy), (41, 82 + oy), (39, 82 + oy)], TIE)
    cv.poly([(39.2, 82 + oy), (40.8, 82 + oy), (41.8, 100 + oy), (40, 103 + oy), (38.2, 100 + oy)], TIE)
    # Cuello.
    cv.poly([(36, 68 + oy), (44, 68 + oy), (44.5, 79 + oy), (35.5, 79 + oy)], SKIN)
    cv.poly([(36, 68 + oy), (44, 68 + oy), (44, 73 + oy), (36, 72 + oy)], SKINS)
    # Pelo de detrás (melena que cae hasta los hombros).
    cv.poly([(27, 52 + oy), (53, 52 + oy), (56, 64 + oy), (57, 76 + oy), (55, 82 + oy), (53, 78 + oy), (50, 84 + oy),
             (47, 74 + oy), (33, 74 + oy), (30, 84 + oy), (27, 78 + oy), (25, 82 + oy), (23, 76 + oy), (24, 64 + oy)], HAIRS)
    # Cara mirando hacia arriba (barbilla un poco levantada).
    cv.ell(40, 59 + oy, 8.6, 9.5, SKIN)
    cv.poly([(31.6, 60 + oy), (48.4, 60 + oy), (46, 67 + oy), (41, 71 + oy), (39, 71 + oy), (34, 67 + oy)], SKIN)
    # Pelo de arriba y flequillo largo despeinado que cae sobre los ojos.
    cv.ell(40, 49 + oy, 13, 10.5, HAIR)
    bangs = [(29, 50, 30, 64), (31, 50, 33.5, 61), (34, 50, 36, 59), (36.5, 50, 38.5, 60.5), (39, 50, 41, 58.5),
             (41.5, 50, 43, 60.5), (44, 50, 46, 59), (46.5, 50, 48.5, 62), (49, 50, 51, 65)]
    for i, (x0, y0, x1, y1) in enumerate(bangs):
        cv.poly([(x0 - 1.4, y0 + oy), (x0 + 2.4, y0 + oy), (x1, y1 + oy)], HAIR if i % 3 else HAIRS)
    # Mechones sueltos a los lados que caen hasta la barbilla.
    cv.poly([(27, 52 + oy), (31, 52 + oy), (31.8, 70 + oy), (30, 80 + oy), (28.5, 72 + oy), (26.5, 78 + oy), (25.5, 66 + oy), (25.5, 58 + oy)], HAIR)
    cv.poly([(49, 52 + oy), (53, 52 + oy), (54.5, 58 + oy), (54.5, 66 + oy), (53.5, 78 + oy), (51.5, 72 + oy), (50, 80 + oy), (48.2, 70 + oy)], HAIR)
    # Brillos del pelo.
    cv.poly([(33, 43 + oy), (40, 40 + oy), (46, 42 + oy), (44, 44 + oy), (40, 42.5 + oy), (35, 45 + oy)], HAIRH)
    cv.poly([(28.5, 47 + oy), (31, 44 + oy), (30.5, 49 + oy)], HAIRH)
    # Mechones que salen hacia arriba (despeinado).
    cv.poly([(29, 47 + oy), (25.5, 44 + oy), (30, 44 + oy)], HAIR)
    cv.poly([(50, 46 + oy), (55, 44 + oy), (51, 43 + oy)], HAIR)
    cv.poly([(38, 39.5 + oy), (40, 35.5 + oy), (42.5, 39.5 + oy)], HAIR)
    lab = cv.down()
    return lab, oy, t

# Pixel art de la cara y detalles a resolución final
def paint_face(lab, oy):
    def put(x, y, v):
        if 0 <= y < len(lab) and 0 <= x < W: lab[y][x] = v
    ey = 60 + oy
    for cx, outer in ((34, -1), (43, 1)):
        # Párpado de arriba (medio cerrado, mirada cansada) con la pestaña hacia fuera.
        for x in range(cx, cx + 4): put(x, ey, LASH)
        put(cx + (4 if outer > 0 else -1), ey + (0), LASH)
        put(cx + (4 if outer > 0 else -1), ey - 1, LASH)
        # Iris granate pegado al párpado (mira hacia arriba) con un brillo, y el blanco a los lados.
        put(cx, ey + 1, SHIRT); put(cx + 3, ey + 1, SHIRT)
        put(cx + 1, ey + 1, EYE); put(cx + 2, ey + 1, EYE)
        put(cx + 1, ey + 1, STAR) if outer < 0 else put(cx + 2, ey + 1, STAR)
        put(cx + 1, ey + 2, EYE); put(cx + 2, ey + 2, LASH)
        # Ojera suave debajo.
        for x in range(cx, cx + 4): put(x, ey + 3, SKINS)
    put(40, 64 + oy, SKINS)
    put(39, 67 + oy, MOUTH); put(40, 67 + oy, MOUTH); put(41, 67 + oy, SKINS)
    for x in (32, 33, 47, 48): put(x, 63 + oy, BLUSH)

WINGSET = {WING, WINGS, WINGL, WINGO}


def despur(lab):
    """Quita los pelillos sueltos de las puntas de las plumas (píxeles de ala con 3 o más vecinos de cielo)."""
    h = len(lab); w = len(lab[0])
    for _ in range(2):
        kill = []
        for y in range(h):
            for x in range(w):
                if lab[y][x] not in WINGSET: continue
                sky = 0
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < w and 0 <= ny < h) or lab[ny][nx] == SKY: sky += 1
                if sky >= 3: kill.append((x, y))
        for x, y in kill: lab[y][x] = SKY


def outline(lab):
    h = len(lab); w = len(lab[0])
    bg = {SKY, STAR, STAR2, KANJI, HALO, HALOG, FEATHER}
    despur(lab)
    out = [row[:] for row in lab]
    for y in range(h):
        for x in range(w):
            if lab[y][x] in bg: continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and lab[ny][nx] in bg:
                    # Las alas llevan un borde suave; el resto, la línea oscura.
                    out[y][x] = WINGO if lab[y][x] in WINGSET else OUTLINE
                    break
    return out

KANJI_TEN = ["...........", ".#########.", ".....#.....", ".....#.....", "###########", ".....#.....",
             "....#.#....", "...#...#...", "..#.....#..", ".#.......#.", "#.........#"]
KANJI_SHI = ["..#....#...", "..#.#######", ".#.....#...", ".#..#######", "##..#..#..#", ".#..#######",
             ".#.....#...", ".#....#....", ".#...#.#...", ".#..#...#..", ".#.#.....##"]

def sky_decor(lab, frame, nframes, height, seed=7):
    rnd = random.Random(seed)
    oy = height - H
    def free(x, y):
        return 0 <= x < W and 0 <= y < height and lab[y][x] == SKY
    # Kanji 天使 arriba a la derecha.
    for k, glyph in enumerate((KANJI_TEN, KANJI_SHI)):
        gx, gy = 52 + k * 13 - (1 if k else 0), 6
        gx = 54 if k == 0 else 54
        gy = 5 + k * 13
        for yy, row in enumerate(glyph):
            for xx, ch in enumerate(row):
                if ch == '#' and free(gx + xx, gy + yy): lab[gy + yy][gx + xx] = KANJI
    # Estrellas: puntos, cruces y destellos de 4 puntas que parpadean.
    stars = []
    for i in range(46):
        x = rnd.randrange(2, W - 2); y = rnd.randrange(2, 40 + oy + 20)
        kind = rnd.choice([0, 0, 0, 1, 1, 2])
        stars.append((x, y, kind, rnd.random()))
    for x, y, kind, ph in stars:
        if 52 <= x <= 67 and 4 <= y <= 30: continue
        b = 0.5 + 0.5 * math.sin(2 * math.pi * (frame / nframes * 2 + ph))
        if kind == 0:
            if b > 0.25 and free(x, y): lab[y][x] = STAR if b > 0.6 else STAR2
        elif kind == 1:
            if free(x, y): lab[y][x] = STAR
            if b > 0.5:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if free(x + dx, y + dy): lab[y + dy][x + dx] = STAR2
        else:
            r = 1 + int(b * 2.5)
            if free(x, y): lab[y][x] = STAR
            for k in range(1, r + 1):
                for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
                    if free(x + dx, y + dy): lab[y + dy][x + dx] = STAR if k == 1 else STAR2

def halo(lab, frame, nframes, oy):
    bob = 1 if (frame // (nframes // 4)) % 2 else 0
    cx, cy = 40.5, 33 + oy - bob
    rx, ry = 10.5, 3.0
    for y in range(len(lab)):
        for x in range(W):
            # Anillo inclinado
            dx = (x + 0.5 - cx); dy = (y + 0.5 - cy) + dx * 0.12
            d = (dx / rx) ** 2 + (dy / ry) ** 2
            if 0.62 <= d <= 1.25 and lab[y][x] in (SKY, STAR, STAR2):
                lab[y][x] = HALO
            elif 1.25 < d <= 1.75 and lab[y][x] in (SKY, STAR2):
                lab[y][x] = HALOG

def feathers(lab, frame, nframes, height):
    t = frame / nframes
    for k, (x0, phase) in enumerate(((12, 0.0), (68, 0.5))):
        p = (t + phase) % 1.0
        y = int(48 + p * (height - 60))
        x = int(x0 + math.sin(p * 2 * math.pi * 1.5) * 4)
        shape = [(0, 0), (1, 0), (2, 1), (1, 1)] if int(p * 8) % 2 else [(0, 1), (1, 1), (1, 0), (2, 0)]
        for dx, dy in shape:
            xx, yy = x + dx, y + dy
            if 0 <= xx < W and 0 <= yy < height and lab[yy][xx] in (SKY, STAR, STAR2): lab[yy][xx] = FEATHER

def render(frame, nframes, height=H):
    lab, oy, t = scene(frame, nframes, height)
    paint_face(lab, oy)
    lab = outline(lab)
    halo(lab, frame, nframes, oy)
    sky_decor(lab, frame, nframes, height)
    feathers(lab, frame, nframes, height)
    return lab

COLOR = {
    STAR: (255, 250, 240), STAR2: (178, 150, 232), HALO: (255, 214, 92), HALOG: (150, 110, 150),
    HAIR: (210, 101, 58), HAIRS: (150, 62, 40), HAIRH: (240, 146, 88), SKIN: (246, 222, 206), SKINS: (226, 190, 172),
    EYE: (150, 34, 52), LASH: (48, 20, 26), MOUTH: (196, 122, 120), BLUSH: (240, 180, 170),
    SHIRT: (244, 242, 238), SHIRTS: (200, 194, 214), TIE: (16, 16, 22), JACK: (32, 30, 42), JACKS: (20, 18, 28),
    JACKH: (70, 64, 90), WING: (248, 246, 252), WINGS: (214, 204, 236), WINGL: (255, 255, 255), WINGO: (150, 140, 170),
    KANJI: (255, 246, 230), FEATHER: (240, 236, 255), OUTLINE: (22, 12, 34),
}
MONO = {
    STAR: (255, 255, 255), STAR2: (120, 120, 120), HALO: (236, 236, 236), HALOG: (70, 70, 70),
    HAIR: (64, 64, 66), HAIRS: (34, 34, 36), HAIRH: (128, 128, 130), SKIN: (236, 236, 236), SKINS: (176, 176, 176),
    EYE: (40, 40, 40), LASH: (8, 8, 8), MOUTH: (130, 130, 130), BLUSH: (206, 206, 206),
    SHIRT: (240, 240, 240), SHIRTS: (180, 180, 180), TIE: (6, 6, 6), JACK: (34, 34, 38), JACKS: (20, 20, 22),
    JACKH: (110, 110, 114), WING: (226, 226, 226), WINGS: (168, 168, 168), WINGL: (250, 250, 250), WINGO: (100, 100, 100),
    KANJI: (255, 255, 255), FEATHER: (220, 220, 220), OUTLINE: (84, 84, 88),
}

def sky_color(style, y, height):
    if style == 'mono':
        return (6, 6, 8)
    # Cielo morado: índigo arriba, violeta y un brillo magenta abajo.
    stops = [(0.0, (14, 8, 36)), (0.45, (46, 22, 92)), (0.8, (96, 44, 140)), (1.0, (150, 70, 160))]
    f = y / max(1, height - 1)
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if a <= f <= b:
            k = (f - a) / (b - a)
            # Bandas de 4 px para que se vea pixel art
            k = round(k * 6) / 6
            return tuple(int(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return stops[-1][1]

def colorize(lab, style):
    pal = MONO if style == 'mono' else COLOR
    h = len(lab)
    img = Image.new('RGB', (W, h))
    for y in range(h):
        for x in range(W):
            v = lab[y][x]
            img.putpixel((x, y), sky_color(style, y, h) if v == SKY else pal[v])
    return img

if __name__ == '__main__':
    import sys
    n = 16
    for style in ('color', 'mono'):
        frames = [colorize(render(f, n), style) for f in (0, 4, 8, 12)]
        sheet = Image.new('RGB', (W * 4 + 12, H))
        for i, fr in enumerate(frames): sheet.paste(fr, (i * (W + 4), 0))
        sheet.resize((sheet.width * 4, sheet.height * 4), Image.NEAREST).save(f'preview_{style}.png')
