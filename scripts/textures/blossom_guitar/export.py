"""Exporta la guitarra Blossom a blossom.vox a partir de las cajas que genera su diseño (mismo formato que Juliet).

    node design.js > design.json
    python3 export.py design.json ../../../src/main/resources/assets/freedomclient/vox/blossom.vox

Usa el exportador de Juliet (scripts/textures/juliet_guitar/export.py) con la paleta de Blossom: cada índice de PAL del
diseño pasa a un carácter de la paleta de BlossomGuitarCosmetic.
"""
import importlib.util
import json
import sys
from pathlib import Path

spec = importlib.util.spec_from_file_location('juliet_export', Path(__file__).resolve().parent.parent / 'juliet_guitar/export.py')
juliet = importlib.util.module_from_spec(spec)
spec.loader.exec_module(juliet)
# PAL_NAMES del diseño: pearl blush petal rose cherry bark barkHi board inlay fret gold goldDk white pole string back neck
juliet.CH = ['a', 'b', 'p', 'r', 'c', 'k', 'K', 'd', 'i', 'f', 'g', 'G', 'w', 'o', 's', 'B', 'n']


def main(src, dst):
    design = json.load(open(src))
    boxes = juliet.boxes_of(design)
    base = max(b[2] + b[5] for b in boxes)
    with open(dst, 'w') as f:
        f.write('# Blossom: cajas "color x y z ancho alto fondo" en píxeles del modelo (y hacia abajo, base en y = 0, la cara hacia +z)\n')
        f.write('# Generado con scripts/textures/blossom_guitar/export.py desde design.js. Base del diseño: y = %r\n' % base)
        for b in boxes:
            f.write('%s %r %r %r %r %r %r\n' % (b[0], b[1], b[2] - base, b[3], b[4], b[5], b[6]))
    print('design boxes', len(design), '-> vox boxes', len(boxes), 'base', base)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
