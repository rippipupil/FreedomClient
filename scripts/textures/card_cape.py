from PIL import Image
import math, sys

SUITS = ['club', 'diamond', 'heart', 'spade']

def inside(kind, x, y):
    def circ(cx, cy, r): return (x-cx)**2 + (y-cy)**2 <= r*r
    def tri(a, b, c):
        def s(p, q, r): return (p[0]-r[0])*(q[1]-r[1]) - (q[0]-r[0])*(p[1]-r[1])
        d1, d2, d3 = s((x,y),a,b), s((x,y),b,c), s((x,y),c,a)
        neg = d1 < 0 or d2 < 0 or d3 < 0; pos = d1 > 0 or d2 > 0 or d3 > 0
        return not (neg and pos)
    if kind == 'diamond':
        return abs(x)/0.72 + abs(y) <= 1.0
    if kind == 'heart':
        return circ(-0.47, -0.42, 0.5) or circ(0.47, -0.42, 0.5) or tri((-0.95, -0.25), (0.95, -0.25), (0, 0.98))
    if kind == 'spade':
        return (circ(-0.46, 0.18, 0.44) or circ(0.46, 0.18, 0.44) or tri((-0.9, 0.05), (0.9, 0.05), (0, -1.0)) or circ(0, 0.15, 0.32)
                or tri((0, 0.2), (-0.38, 0.98), (0.38, 0.98)))
    if kind == 'club':
        return (circ(0, -0.52, 0.42) or circ(-0.5, 0.1, 0.42) or circ(0.5, 0.1, 0.42) or circ(0, 0.0, 0.3)
                or tri((0, -0.1), (-0.36, 0.98), (0.36, 0.98)))

def mask(kind, w, h, ss=6):
    m = [[0.0]*w for _ in range(h)]
    for py in range(h):
        for px in range(w):
            c = 0
            for sy in range(ss):
                for sx in range(ss):
                    x = ((px + (sx+0.5)/ss) / w) * 2 - 1
                    y = ((py + (sy+0.5)/ss) / h) * 2 - 1
                    c += inside(kind, x, y)
            m[py][px] = c / (ss*ss)
    return m

SMALL = {}
# Palos pequeños a mano (5x6) para que se lean bien en las esquinas.
SMALL['club'] = ["..#..", ".###.", "#.#.#", "#####", "#.#.#", ".###."]
SMALL['club'] = [".###.", ".###.", "#####", "#####", "..#..", ".###."]
SMALL['diamond'] = ["..#..", ".###.", "#####", "#####", ".###.", "..#.."]
SMALL['heart'] = ["##.##", "#####", "#####", ".###.", "..#..", "....."]
SMALL['spade'] = ["..#..", ".###.", "#####", "#####", "..#..", ".###."]
TINY = {'club': ["###", "###", ".#."], 'diamond': [".#.", "###", ".#."], 'heart': ["#.#", "###", ".#."], 'spade': [".#.", "###", ".#."]}

BG = (12, 12, 14)
PAT = (30, 30, 36)
PAT2 = (22, 22, 26)
LINE = (236, 236, 240)
WHITE = (246, 246, 248)
SHADE = (178, 180, 188)
EDGE = (20, 20, 24)

BIG = {k: mask(k, 18, 18) for k in SUITS}

def mix(a, b, t): return tuple(int(a[i] + (b[i]-a[i])*t) for i in range(3))

def face(w, h, suit_a, suit_b, t):
    """Cara de la carta de w x h: fondo negro con palos grises, marco blanco, palos en las esquinas y el palo
    grande en el centro pasando de suit_a a suit_b (t de 0 a 1)."""
    px = [[BG]*w for _ in range(h)]
    # Patrón de palos oscuros en diagonal.
    for gy in range(-1, h//7 + 2):
        for gx in range(-1, w//7 + 2):
            ox = gx*7 + (3 if gy % 2 else 0); oy = gy*7
            kind = SUITS[(gx + gy*3) % 4]
            for yy, row in enumerate(TINY[kind]):
                for xx, ch in enumerate(row):
                    X, Y = ox+xx, oy+yy
                    if ch == '#' and 0 <= X < w and 0 <= Y < h:
                        px[Y][X] = PAT if (gx+gy) % 2 else PAT2
    # Marco blanco fino, con hueco donde van los palos de las esquinas.
    inset = 4
    for x in range(inset, w-inset):
        for y in (inset, h-1-inset):
            px[y][x] = LINE
    for y in range(inset, h-inset):
        for x in (inset, w-1-inset):
            px[y][x] = LINE
    corner_kind = suit_a if t < 0.5 else suit_b
    small = SMALL[corner_kind]
    def stamp(ox, oy, flip):
        for yy in range(-1, 7):
            for xx in range(-1, 6):
                X, Y = ox+xx, oy+yy
                if 0 <= X < w and 0 <= Y < h: px[Y][X] = BG
        for yy, row in enumerate(small):
            for xx, ch in enumerate(row):
                sy, sx = (5-yy, 4-xx) if flip else (yy, xx)
                if ch == '#': px[oy+sy][ox+sx] = WHITE
    stamp(2, 2, False)
    stamp(w-7, h-8, True)
    # Palo grande del centro, con sombra abajo a la derecha; en el cambio uno se funde en el otro.
    S = 18
    cx, cy = (w - S)//2, (h - S)//2
    def cover(kind, x, y):
        if 0 <= x < S and 0 <= y < S: return BIG[kind][y][x]
        return 0.0
    for y in range(-1, S+2):
        for x in range(-1, S+2):
            ca = cover(suit_a, x, y) * (1-t) + cover(suit_b, x, y) * t
            sh = cover(suit_a, x-1, y-1) * (1-t) + cover(suit_b, x-1, y-1) * t
            X, Y = cx+x, cy+y
            if not (0 <= X < w and 0 <= Y < h): continue
            base = px[Y][X]
            if sh > 0.5 and ca <= 0.5: base = mix(base, SHADE, 0.35*sh)
            if ca > 0.02:
                px[Y][X] = mix(base, WHITE, min(1.0, ca*1.15))
    return px

def paste(img, ox, oy, f, mirror=False):
    h = len(f); w = len(f[0])
    for y in range(h):
        for x in range(w):
            img.putpixel((ox + (w-1-x if mirror else x), oy+y), f[y][x] + (255,))

def texture(suit_a, suit_b, t):
    img = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
    for y in range(0, 68):
        for x in range(0, 88): img.putpixel((x, y), EDGE + (255,))
    for y in range(0, 88):
        for x in range(88, 184):
            if y < 8 and not (96 <= x < 176): continue
            img.putpixel((x, y), EDGE + (255,))
    cape = face(40, 64, suit_a, suit_b, t)
    paste(img, 4, 4, cape)
    paste(img, 48, 4, cape, mirror=True)
    wing = face(40, 80, suit_a, suit_b, t)
    paste(img, 96, 8, wing)
    paste(img, 144, 8, wing, mirror=True)
    return img

if __name__ == '__main__':
    out = sys.argv[1]
    frame = 0
    for i, s in enumerate(SUITS):
        n = SUITS[(i+1) % 4]
        for t in (0.0, 0.25, 0.5, 0.75):
            texture(s, n, t).save(f'{out}/cape_card_{frame}.png'); frame += 1
