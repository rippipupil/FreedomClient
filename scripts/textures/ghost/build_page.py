"""Mete el fantasma (salida de ghost_art.py) en la página del diseño: python3 build_page.py ghost.html ghost.json salida.html"""
import sys

page, data, out = sys.argv[1:4]
html = open(page).read().replace('%GHOST%', open(data).read())
open(out, 'w').write(html)
print(len(html), 'bytes')
