"""Mete el código del diseño en la vista previa: python3 build_page.py preview.html design.js salida.html"""
import sys

page, design, out = sys.argv[1:4]
html = open(page).read().replace('%DESIGN%', open(design).read())
open(out, 'w').write(html)
print(len(html), 'bytes')
