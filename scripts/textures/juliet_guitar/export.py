"""Exporta la guitarra Juliet a juliet.vox a partir de las cajas que genera el código original del diseño.

    node design.js > design.json
    python3 export.py design.json ../../../src/main/resources/assets/freedomclient/vox/juliet.vox

En el diseño cada caja tiene su centro en (x + 0.5, 60 - y, z - 1.5) con y hacia arriba (three.js) y tamaño
(sx, sy, sz). En el .vox van como "color x y z ancho alto fondo" en píxeles del modelo, con y hacia abajo, la base de
la guitarra en y = 0 y la cara hacia +z: la misma caja es x0 = x + 0.5 - sx / 2, y0 = y - sy / 2, z0 = z - 1.5 - sz / 2
(y luego se sube todo para que la base quede en 0). Los vóxeles enteros del mismo color se juntan en cajas grandes.
"""
import json
import math
import sys

# Índice de color del diseño (PAL) -> carácter de la paleta de JulietGuitarCosmetic.
CH = ['w', 'd', 'g', 'l', 'b', 'r', 'p', 'v', 'u', 'm', 'n', 'e', 'y', 'c', 's', 'h', 'k']
# Cada caja: ancho + fondo y alto + fondo como mucho 32 (el tamaño de la franja de color de la textura).
MAX = 31


def is_unit(v):
    return v['sx'] == 1 and v['sy'] == 1 and v['sz'] == 1 and all(float(v[k]).is_integer() for k in 'xyz')


def boxes_of(design):
    grid = {}
    extras = []
    for v in design:
        if is_unit(v):
            key = (int(v['x']), int(v['y']), int(v['z']))
            assert key not in grid, key
            grid[key] = CH[v['c']]
        else:
            extras.append(v)
    cols = {}
    for (x, y, z), c in grid.items(): cols.setdefault((x, y), {})[z] = c
    zr = []
    for (x, y), zs in cols.items():
        zl = sorted(zs); i = 0
        while i < len(zl):
            z0 = zl[i]; c = zs[z0]; j = i
            while j + 1 < len(zl) and zl[j + 1] == zl[j] + 1 and zs[zl[j + 1]] == c: j += 1
            zr.append((x, y, z0, zl[j] + 1, c)); i = j + 1
    byrow = {}
    for x, y, z0, z1, c in zr: byrow.setdefault((y, z0, z1, c), []).append(x)
    xr = []
    for (y, z0, z1, c), xs in byrow.items():
        xs.sort(); i = 0
        while i < len(xs):
            j = i
            while j + 1 < len(xs) and xs[j + 1] == xs[j] + 1 and (xs[j + 1] - xs[i] + 1) + (z1 - z0) <= MAX: j += 1
            xr.append((xs[i], xs[j] + 1, y, z0, z1, c)); i = j + 1
    bycol = {}
    for x0, x1, y, z0, z1, c in xr: bycol.setdefault((x0, x1, z0, z1, c), []).append(y)
    boxes = []
    for (x0, x1, z0, z1, c), ys in bycol.items():
        ys.sort(); i = 0
        while i < len(ys):
            j = i
            while j + 1 < len(ys) and ys[j + 1] == ys[j] + 1 and (ys[j + 1] - ys[i] + 1) + (z1 - z0) <= MAX: j += 1
            boxes.append([c, x0, ys[i] - 0.5, z0 - 2, x1 - x0, ys[j] - ys[i] + 1, z1 - z0]); i = j + 1
    for v in extras:
        sx, sy, sz = v['sx'], v['sy'], v['sz']
        n = max(1, math.ceil((sy + sz) / MAX))
        for k in range(n):
            boxes.append([CH[v['c']], v['x'] + 0.5 - sx / 2, v['y'] - sy / 2 + sy * k / n, v['z'] - 1.5 - sz / 2, sx, sy / n, sz])
    return boxes


def main(src, dst):
    design = json.load(open(src))
    boxes = boxes_of(design)
    base = max(b[2] + b[5] for b in boxes)
    with open(dst, 'w') as f:
        f.write('# Juliet: cajas "color x y z ancho alto fondo" en píxeles del modelo (y hacia abajo, base en y = 0, la cara hacia +z)\n')
        f.write('# Generado con scripts/textures/juliet_guitar/export.py desde el código del diseño. Base del diseño: y = %r\n' % base)
        for b in boxes:
            f.write('%s %r %r %r %r %r %r\n' % (b[0], b[1], b[2] - base, b[3], b[4], b[5], b[6]))
    print('design boxes', len(design), '-> vox boxes', len(boxes), 'base', base)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
