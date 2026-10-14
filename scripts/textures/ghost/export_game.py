"""Pasa el fantasmita (salida de ghost_art.py) al juego.

    python3 ghost_art.py ghost.json && python3 export_game.py ghost.json

- assets/freedomclient/vox/ghost.vox: las cajas del modelo 3D, "pieza papel x0 y0 z0 ancho alto fondo" en píxeles del
  modelo, y hacia arriba, las colas en y = 0, mirando a +z (como en el diseño). Piezas: body, armL, armR, shine y una
  por cara (face_feliz...).
- ui/GhostArt.java: los sprites 2D del menú (30x29 a medio píxel de interfaz, con contorno), entre las marcas.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
data = json.load(open(sys.argv[1]))
lines = ['# Fantasmita: generado con scripts/textures/ghost/export_game.py desde ghost_art.py (FreedomPlayer). y hacia arriba.']
for part in ('body', 'armL', 'armR', 'shine'):
    for b in data[part]:
        lines.append('%s %s %r %r %r %r %r %r' % (part, b[6], b[0], b[1], b[2], b[3] - b[0], b[4] - b[1], b[5] - b[2]))
for face in data['order']:
    for b in data['faces'][face]:
        lines.append('face_%s %s %r %r %r %r %r %r' % (face, b[6], b[0], b[1], b[2], b[3] - b[0], b[4] - b[1], b[5] - b[2]))
vox = ROOT / 'src/main/resources/assets/freedomclient/vox/ghost.vox'
vox.write_text('\n'.join(lines) + '\n')
print(vox, len(lines) - 1, 'boxes')

java = ROOT / 'src/main/java/com/freedomclient/ui/GhostArt.java'
src = java.read_text()
body = '\t// --- generado: sprites (scripts/textures/ghost/export_game.py) ---\n\tprivate static final String[][] SPRITES = {\n'
for face in data['order']:
    body += '\t\t\t{ // %s\n' % face
    body += ''.join('\t\t\t\t\t"%s",\n' % row for row in data['sprites'][face])
    body += '\t\t\t},\n'
body += '\t};\n\t// --- fin de lo generado ---\n'
src = re.sub(r'\t// --- generado: sprites.*?// --- fin de lo generado ---\n', lambda m: body, src, flags=re.S)
java.write_text(src)
print(java)
