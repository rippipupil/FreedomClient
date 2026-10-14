"""Fantasmita de FreedomPlayer (tablero Ghost: SVG de 64x64) pasado a pixel art y a vóxeles.

    python3 ghost_art.py salida.json [vista.png]

Saca, del mismo dibujo:
- La silueta del cuerpo a 1 px del modelo (4 unidades del SVG): cajas con el cuerpo más grueso en el centro y más
  fino en los bordes (redondeado), y los bracitos aparte para poder moverlos.
- Las 8 caras (feliz, contento, owo, uwu, guino, sorpresa, dormido, triste) a 0,5 px (2 unidades del SVG), pegadas a
  la cara delantera del cuerpo, y el brillo de la cabeza.
- El sprite 2D para el menú a 0,5 px (28x26) con contorno, una por cara.
Coordenadas de las cajas: píxeles del modelo, y hacia arriba, los pies (las colas) en y = 0, mirando a +z.
"""
import json
import math
import sys

BODY_PATH = ("M32 6 C18 6 12 17 12 29 L12 33 C8 34 5 37 4 40 C8 40 11 39 12 38 L12 50 C15 54 18 56 21 52 "
             "C24 57 28 58 32 53 C36 58 40 57 43 52 C46 56 49 54 52 50 L52 38 C53 39 56 40 60 40 "
             "C59 37 56 34 52 33 L52 29 C52 17 46 6 32 6 Z")


def flatten(path, steps=24):
    import re
    tok = re.findall(r'[MLCZ]|-?[0-9.]+', path)
    pts, i, cur = [], 0, (0, 0)
    while i < len(tok):
        c = tok[i]
        if c == 'M' or c == 'L':
            cur = (float(tok[i + 1]), float(tok[i + 2])); pts.append(cur); i += 3
        elif c == 'C':
            p1 = (float(tok[i + 1]), float(tok[i + 2])); p2 = (float(tok[i + 3]), float(tok[i + 4])); p3 = (float(tok[i + 5]), float(tok[i + 6]))
            for s in range(1, steps + 1):
                t = s / steps; u = 1 - t
                pts.append((u ** 3 * cur[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
                            u ** 3 * cur[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]))
            cur = p3; i += 7
        elif c == 'Z':
            i += 1
        else:
            raise ValueError(c)
    return pts


def inside(poly, x, y):
    n, c = len(poly), False
    for k in range(n):
        x1, y1 = poly[k]; x2, y2 = poly[(k + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c


BODY = flatten(BODY_PATH)


def coverage(test, x0, y0, size, n=6):
    hit = 0
    for a in range(n):
        for b in range(n):
            if test(x0 + (a + 0.5) * size / n, y0 + (b + 0.5) * size / n):
                hit += 1
    return hit / (n * n)


# ---- Primitivas de las caras (unidades del SVG) ----
def quad(p0, p1, p2, steps=40):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
            for t in (s / steps for s in range(steps + 1))]


def seg_dist(px, py, a, b):
    vx, vy = b[0] - a[0], b[1] - a[1]
    L = vx * vx + vy * vy
    t = 0 if L == 0 else max(0, min(1, ((px - a[0]) * vx + (py - a[1]) * vy) / L))
    return math.hypot(px - a[0] - t * vx, py - a[1] - t * vy)


def stroke(points, width):
    def test(x, y):
        return any(seg_dist(x, y, points[k], points[k + 1]) <= width / 2 for k in range(len(points) - 1))
    return test


def ellipse(cx, cy, rx, ry, rot=0.0):
    c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))

    def test(x, y):
        dx, dy = x - cx, y - cy
        u, v = dx * c + dy * s, -dx * s + dy * c
        return (u / rx) ** 2 + (v / ry) ** 2 <= 1
    return test


def ring(cx, cy, r, width):
    return lambda x, y: abs(math.hypot(x - cx, y - cy) - r) <= width / 2


def fill_poly(points):
    return lambda x, y: inside(points, x, y)


CHEEKS = [('c', ellipse(17.5, 33, 3.4, 1.8)), ('c', ellipse(46.5, 33, 3.4, 1.8))]
FACES = {
    'feliz': [('k', stroke([(20, 23), (26, 27), (20, 31)], 2.6)), ('k', stroke([(44, 23), (38, 27), (44, 31)], 2.6)),
              ('k', stroke(quad((28, 33), (32, 37), (36, 33)), 2.6))],
    'contento': [('k', stroke([(19.5, 28.5), (23, 24.5), (26.5, 28.5)], 2.6)), ('k', stroke([(37.5, 28.5), (41, 24.5), (44.5, 28.5)], 2.6)),
                 ('k', stroke(quad((28, 33), (32, 37.5), (36, 33)), 2.6))],
    # Los aros de OwO, algo más finos y anchos que en el SVG: a 0,5 px el hueco del centro se cerraba.
    'owo': CHEEKS + [('k', ring(23, 27, 4.0, 1.6)), ('k', ring(41, 27, 4.0, 1.6)),
                     ('k', stroke(quad((27, 33), (29.5, 37), (32, 33)) + quad((32, 33), (34.5, 37), (37, 33)), 2.4))],
    'uwu': CHEEKS + [('k', stroke(quad((19.5, 25), (23, 31), (26.5, 25)), 2.6)), ('k', stroke(quad((37.5, 25), (41, 31), (44.5, 25)), 2.6)),
                     ('k', stroke(quad((27, 33.5), (29.5, 37.5), (32, 33.5)) + quad((32, 33.5), (34.5, 37.5), (37, 33.5)), 2.4))],
    'guino': CHEEKS + [('k', stroke([(19.5, 28.5), (23, 24.5), (26.5, 28.5)], 2.6)),
                       ('k', stroke(quad((37.5, 27.5), (39.5, 24.5), (41, 27)) + quad((41, 27), (42.5, 29.5), (44.5, 26.5)), 2.6)),
                       ('k', stroke(quad((28, 33), (32, 37.5), (36, 33)), 2.6)),
                       ('t', fill_poly(quad((33.5, 35.2), (35, 38.5), (36.6, 35.6))))],
    'sorpresa': [('k', ellipse(23.5, 26.5, 3.2, 3.2)), ('k', ellipse(40.5, 26.5, 3.2, 3.2)), ('k', ellipse(32, 36, 3, 4))],
    'dormido': [('k', stroke(quad((19.5, 27), (23, 29.5), (26.5, 27)), 2.4)), ('k', stroke(quad((37.5, 27), (41, 29.5), (44.5, 27)), 2.4)),
                ('k', ellipse(32, 35, 1.8, 2.2))],
    'triste': [('k', ellipse(24, 27, 4.6, 2.6, -22)), ('k', ellipse(40, 27, 4.6, 2.6, 22)), ('k', ellipse(32, 37, 2.2, 3))],
}
ORDER = ['feliz', 'contento', 'owo', 'uwu', 'guino', 'sorpresa', 'dormido', 'triste']
SHINE = ellipse(23, 16, 7, 4)

# Rejilla fina: 0,5 px del modelo = 2 unidades del SVG. Columnas desde x = 4 del SVG, filas desde y = 6.
FX0, FY0, F = 4.0, 6.0, 2.0
FW, FH = 28, 27  # 56 x 54 unidades: del bracito izquierdo al derecho y de la cabeza a las colas


def fine_grid(test, thr):
    return [[coverage(test, FX0 + c * F, FY0 + r * F, F) >= thr for c in range(FW)] for r in range(FH)]


def main():
    out_json = sys.argv[1]
    body_fine = fine_grid(lambda x, y: inside(BODY, x, y), 0.5)
    # ---- Cuerpo en vóxeles de 0,5 px ----
    # Visto de frente es la silueta del SVG; de lado, cada fila es una elipse: grosor D en el centro que baja hacia los
    # bordes (redondo por los lados y, como las filas de arriba son más estrechas, también por arriba). El grosor va en
    # pasos de 0,5 px y cada fila se hace con cajas metidas unas en otras (una por paso): pocas cajas.
    W, H = FW * 0.5, FH * 0.5  # 14 x 13,5 px
    D, LEVELS = 3.25, [0.75, 1.25, 1.75, 2.25, 2.75, 3.25]

    def is_arm(c):
        sx = FX0 + (c + 0.5) * F
        return sx < 12 or sx > 52

    def px_x(c): return c * 0.5 - W / 2
    def px_y(r): return H - (r + 1) * 0.5

    def half_width(r):
        cols = [c for c in range(FW) if body_fine[r][c] and not is_arm(c)]
        return max(abs(px_x(min(cols))), abs(px_x(max(cols)) + 0.5)) if cols else 0

    def half_depth(r, c):
        if is_arm(c): return 0.75
        hw = half_width(r)
        u = min(1.0, abs(px_x(c) + 0.25) / hw) if hw else 1.0
        d = D * math.sqrt(max(0.0, 1 - u * u))
        return max([l for l in LEVELS if l <= d + 1e-9] or [LEVELS[0]])

    def role(r):
        return 'top' if r <= 4 else 'shade' if r >= FH - 5 else 'body'
    boxes = {'body': [], 'armL': [], 'armR': []}
    for r in range(FH):
        for part, test in (('body', lambda c: not is_arm(c)), ('armL', lambda c: is_arm(c) and c < FW // 2), ('armR', lambda c: is_arm(c) and c >= FW // 2)):
            for level in LEVELS:
                c = 0
                while c < FW:
                    if not (body_fine[r][c] and test(c) and half_depth(r, c) >= level):
                        c += 1; continue
                    c1 = c
                    while c1 < FW and body_fine[r][c1] and test(c1) and half_depth(r, c1) >= level: c1 += 1
                    boxes[part].append([px_x(c), px_y(r), -level, px_x(c1), px_y(r) + 0.5, level, role(r)])
                    c = c1
    # Profundidad de la cara delantera en cada punto (para pegar las caras y el brillo).
    def front(x, y):
        c, r = int(math.floor((x + W / 2) / 0.5)), int(math.floor((H - y) / 0.5))
        if 0 <= r < FH and 0 <= c < FW and body_fine[r][c]: return half_depth(r, c)
        return None

    # ---- Caras a 0,5 px pegadas delante ----
    def fine_boxes(prims, extra=None):
        res = []
        for color, test in prims:
            g = fine_grid(test, 0.4)
            for r in range(FH):
                for c in range(FW):
                    if not g[r][c] or not body_fine[r][c]: continue
                    x, y = px_x(c), px_y(r)
                    z = front(x + 0.25, y + 0.25)
                    if z is None: continue
                    res.append([x, y, z, x + 0.5, y + 0.5, z + 0.12, color])
        return res
    faces = {name: fine_boxes(FACES[name]) for name in ORDER}
    shine = fine_boxes([('w', SHINE)])
    # Quitar del brillo lo que tape una cara no hace falta: el brillo está en la frente, encima de los ojos.

    # ---- Sprite 2D (menú) a 0,5 px, con contorno ----
    sprites = {}
    for name in ORDER:
        grid = [['.' if not body_fine[r][c] else 'b' for c in range(FW)] for r in range(FH)]
        for r in range(FH):
            for c in range(FW):
                if grid[r][c] == 'b' and r >= FH - 6: grid[r][c] = 's'
        sg = fine_grid(SHINE, 0.4)
        for r in range(FH):
            for c in range(FW):
                if sg[r][c] and grid[r][c] != '.': grid[r][c] = 'w'
        for color, test in FACES[name]:
            g = fine_grid(test, 0.4)
            for r in range(FH):
                for c in range(FW):
                    if g[r][c] and grid[r][c] != '.': grid[r][c] = color
        # Contorno de 1 celda alrededor.
        padded = [['.'] * (FW + 2) for _ in range(FH + 2)]
        for r in range(FH):
            for c in range(FW): padded[r + 1][c + 1] = grid[r][c]
        for r in range(FH + 2):
            for c in range(FW + 2):
                if padded[r][c] == '.' and any(0 <= r + a < FH + 2 and 0 <= c + b < FW + 2 and padded[r + a][c + b] not in '.o'
                                                for a in (-1, 0, 1) for b in (-1, 0, 1) if abs(a) + abs(b) == 1):
                    padded[r][c] = 'o'
        sprites[name] = [''.join(row) for row in padded]

    data = {'order': ORDER, 'body': boxes['body'], 'armL': boxes['armL'], 'armR': boxes['armR'], 'shine': shine, 'faces': faces,
            'sprites': sprites, 'size': [W, H]}
    json.dump(data, open(out_json, 'w'), separators=(',', ':'))
    print('body', len(boxes['body']), 'arms', len(boxes['armL']), len(boxes['armR']), 'shine', len(shine),
          'faces', {k: len(v) for k, v in faces.items()})
    if len(sys.argv) > 2:
        from PIL import Image
        cols = {'.': (40, 30, 50), 'o': (60, 20, 50), 'b': (244, 166, 203), 's': (229, 141, 184), 'w': (255, 230, 242),
                'k': (194, 50, 111), 'c': (255, 111, 174), 't': (255, 111, 174)}
        S = 8
        img = Image.new('RGB', ((FW + 3) * len(ORDER) * S, (FH + 2) * S), (40, 30, 50))
        for i, name in enumerate(ORDER):
            for r, row in enumerate(sprites[name]):
                for c, ch in enumerate(row):
                    for a in range(S):
                        for b in range(S):
                            img.putpixel(((i * (FW + 3) + c) * S + b, r * S + a), cols[ch])
        img.save(sys.argv[2])


main()
