// Exporta las piezas del diseño (void_keys.html) para el juego: node export_shapes.js void_keys.html salida.vox
// Líneas "palette nombre RRGGBB brilla(0/1) opacidad" y "pieza nombre x0 y0 z0 ancho alto fondo" en píxeles del modelo,
// y hacia arriba, igual que en el diseño. Piezas: white, black, rock0..rock2, streak, dust.
const fs = require('fs'), vm = require('vm');
const html = fs.readFileSync(process.argv[2], 'utf8');
const design = html.slice(html.indexOf('<script>\n') + 9, html.indexOf('</script>', html.indexOf('<script>\n'))).replace('%FLOW%', '[]');
const c = {}; vm.createContext(c);
vm.runInContext(design + ';Object.assign(this,{PAL,GLOWING,ALPHA,WHITE_KEY,BLACK_KEY,ROCKS,STREAK,DUST});', c);
const out = ['# Void Keys: piezas del diseño (scripts/textures/void_keys/export_shapes.js). y hacia arriba, píxeles del modelo.'];
for (const k in c.PAL) out.push(`palette ${k} ${c.PAL[k].toString(16).padStart(6, '0')} ${c.GLOWING.includes(k) ? 1 : 0} ${c.ALPHA[k] || 1}`);
const put = (name, boxes) => { for (const b of boxes) out.push(`${name} ${b[6]} ${b[0]} ${b[1]} ${b[2]} ${b[3] - b[0]} ${b[4] - b[1]} ${b[5] - b[2]}`); };
put('white', c.WHITE_KEY); put('black', c.BLACK_KEY); c.ROCKS.forEach((r, i) => put('rock' + i, r)); put('streak', c.STREAK); put('dust', c.DUST);
fs.writeFileSync(process.argv[3], out.join('\n') + '\n');
console.log(out.length - 1, 'lines');
