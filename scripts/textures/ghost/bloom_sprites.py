"""Sprites "pixel 3D" de la estela Ghost Bloom, renderizados desde las flores y pétalos de vóxeles del diseño.

    python3 bloom_sprites.py ghost.html

Saca las piezas del diseño (FLOWER_BOXES, FLOWER_PAL, PETAL_BOXES y FLOWER.size) con node y las dibuja como las otras
partículas pixel 3D (tools/make_voxel_particles.py): vistas un poco desde arriba, con la luz fija y un contorno.
- bloom_<tipo>_<n>.png (16x16): la flor de pie con la base abajo en el centro, inclinada FLOWER_TILTS[n] radianes
  (los 4 primeros, el vaivén; los 3 últimos, cerrándose al final de su vida). El sprite mide FLOWER_SPAN px del diseño.
- bloom_petal_<tipo>_<n>.png (8x8): el pétalo suelto girando (8 posiciones). Mide PETAL_SPAN px del diseño.
"""
import json
import math
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[3] / 'src/main/resources/assets/freedomclient/textures/particle'
FLOWER_TILTS = [-0.12, -0.04, 0.04, 0.12, 0.25, 0.38, 0.5]
FLOWER_SPAN, PETAL_SPAN = 4.0, 1.5
CAMERA_TILT = math.radians(24)
_l = (-0.45, 0.75, -0.5)
LIGHT = tuple(c / math.sqrt(sum(v * v for v in _l)) for c in _l)

JS = """
const fs = require('fs'), vm = require('vm'), html = fs.readFileSync(process.argv[1], 'utf8');
const design = html.slice(html.indexOf('// DISEÑO'), html.indexOf('(function themes() {')).replace('%GHOST%', '{"sprites":{}}');
const c = {}; vm.createContext(c);
vm.runInContext(design + ';this.out = {FLOWER_BOXES, FLOWER_PAL, PETAL_BOXES, FLOWER, FLOWER_KINDS};', c);
console.log(JSON.stringify(c.out));
"""


def rot_z(p, a):
    x, y, z = p
    return x * math.cos(a) - y * math.sin(a), x * math.sin(a) + y * math.cos(a), z


def rot_x(p, a):
    x, y, z = p
    return x, y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)


def rot_y(p, a):
    x, y, z = p
    return x * math.cos(a) + z * math.sin(a), y, -x * math.sin(a) + z * math.cos(a)


def render(boxes, colors, size, span, transform, center_y):
    """Dibuja cajas [x0, y0, z0, x1, y1, z1, papel] (píxeles del diseño, y arriba, la cámara mira a -z desde +z)."""
    k = size / span
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    quads = []
    for b in boxes:
        x0, y0, z0, x1, y1, z1, role = b
        corners = {(i, j, l): (x1 if i else x0, y1 if j else y0, z1 if l else z0) for i in (0, 1) for j in (0, 1) for l in (0, 1)}
        faces = [((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]), ((0, 0, -1), [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)]),
                 ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]), ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]),
                 ((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)]), ((-1, 0, 0), [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)])]
        for normal, idx in faces:
            n = transform(normal, True)
            if n[2] <= 0:  # mira hacia atrás (la cámara está en +z)
                continue
            pts = [transform(corners[i], False) for i in idx]
            depth = sum(p[2] for p in pts) / 4
            dot = -n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * -LIGHT[2]
            f = 0.58 + 0.52 * max(0.0, dot)
            col = colors[role]
            fill = tuple(max(0, min(255, int(((col >> s) & 255) * f))) for s in (16, 8, 0)) + (255,)
            quads.append((depth, [(size / 2 + p[0] * k, center_y - p[1] * k) for p in pts], fill))
    for _, poly, fill in sorted(quads, key=lambda q: q[0]):
        draw.polygon(poly, fill=fill)
    alpha = image.getchannel('A')
    outline = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            if alpha.getpixel((x, y)):
                continue
            if any(0 <= x + dx < size and 0 <= y + dy < size and alpha.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                outline.putpixel((x, y), (60, 20, 50, 150))
    return Image.alpha_composite(outline, image)


def main():
    d = json.loads(subprocess.check_output(['node', '-e', JS, sys.argv[1]]))
    scale = d['FLOWER']['size']
    pal = d['FLOWER_PAL']
    boxes = [[v * scale if i < 6 else v for i, v in enumerate(b)] for b in d['FLOWER_BOXES']]
    petal = [[v * scale if i < 6 else v for i, v in enumerate(b)] for b in d['PETAL_BOXES']]
    for kind in d['FLOWER_KINDS']:
        colors = {'stem': pal['stem'], 'leaf': pal['leaf'], **pal[kind]}
        for n, tilt in enumerate(FLOWER_TILTS):
            def tf(p, normal, tilt=tilt):
                q = rot_y(p, 0.6)  # un poco girada, así se ven dos pétalos de lado
                q = rot_z(q, tilt)
                return rot_x(q, CAMERA_TILT)
            # La base de la flor (y = 0) abajo del todo, a 1 px del borde.
            render(boxes, colors, 16, FLOWER_SPAN, tf, 15).save(OUT / f'bloom_{kind}_{n}.png')
        for n in range(8):
            a = math.tau * n / 8

            def tp(p, normal, a=a):
                return rot_x(rot_y(rot_x(p, a), a * 0.6), CAMERA_TILT)
            render(petal, {'petal': pal[kind]['petal']}, 8, PETAL_SPAN, tp, 4).save(OUT / f'bloom_petal_{kind}_{n}.png')
    print('bloom sprites written to', OUT)


if __name__ == '__main__':
    main()
