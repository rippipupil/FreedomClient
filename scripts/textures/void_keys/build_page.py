"""Mete la partitura de Flow en la página del diseño: python3 build_page.py void_keys.html flow_keys.txt salida.html"""
import json
import sys

page, keys, out = sys.argv[1:4]
notes = [[int(v) for v in l.split()] for l in open(keys) if l.strip() and not l.startswith('#')]
html = open(page).read().replace('%FLOW%', json.dumps(notes, separators=(',', ':')))
open(out, 'w').write(html)
print(len(notes), 'notes,', len(html), 'bytes')
