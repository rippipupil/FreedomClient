// Comprueba que ninguna pose del jugador mete las manos, los pies, la cabeza o el cuerpo en el aro de
// teclas: carga el diseño (void_keys.html), recorre las poses en el tiempo y mira las esquinas de cada
// parte. Uso: node check_poses.js void_keys.html three.min.js
const fs = require('fs'), vm = require('vm');
const THREE = require(require('path').resolve(process.argv[3]));
const html = fs.readFileSync(process.argv[2], 'utf8');
const design = html.slice(html.indexOf('<script>\n') + 9, html.indexOf('</script>', html.indexOf('<script>\n'))).replace('%FLOW%', '[]');
const ctx = {}; vm.createContext(ctx); vm.runInContext(design + '\n;this.RING=RING;this.figurePose=figurePose;', ctx);
const { RING, figurePose } = ctx;

// El mismo esqueleto que la vista previa.
const acro = new THREE.Group(), fig = new THREE.Group(); fig.position.y = -16; acro.add(fig);
const parts = {};
function part(name, w, h, d, px, py, ox, oy) {
  const pivot = new THREE.Group(); pivot.position.set(px, py, 0); fig.add(pivot);
  parts[name] = { pivot, w, h, d, ox, oy };
}
part('head', 8, 8, 8, 0, 24, 0, 4); part('body', 8, 12, 4, 0, 18, 0, 0);
part('rArm', 4, 12, 4, -6, 22, 0, -4); part('lArm', 4, 12, 4, 6, 22, 0, -4);
part('rLeg', 4, 12, 4, -2, 12, 0, -6); part('lLeg', 4, 12, 4, 2, 12, 0, -6);

const top = RING.height + RING.amp;            // tapas en lo más alto de la ola
const band = [RING.height - RING.amp - 3.75 - 0.3, RING.height + RING.amp + 2.75 + 0.3];
let worst = { gap: 1e9 };
function check(mode, o, jumpY = 0) {
  const p = figurePose(o);
  acro.position.y = 16 + p.lift + jumpY; acro.rotation.set(p.pitch, p.yaw, p.roll, 'YXZ');
  for (const n of ['rArm', 'lArm', 'rLeg', 'lLeg']) parts[n].pivot.rotation.set(p[n][0], 0, p[n][1]);
  parts.head.pivot.rotation.set(p.head[0], 0, p.head[1]);
  acro.updateMatrixWorld(true);
  for (const n in parts) {
    const q = parts[n];
    for (const sx of [-1, 1]) for (const sy of [-1, 1]) for (const sz of [-1, 1]) {
      const v = new THREE.Vector3(q.ox + sx * q.w / 2, q.oy + sy * q.h / 2, sz * q.d / 2).applyMatrix4(q.pivot.matrixWorld);
      const r = Math.hypot(v.x, v.z);
      if (v.y < band[0] || v.y > band[1]) continue;
      // Tocando, las manos pueden posarse encima de las teclas (por encima de las tapas).
      if (o.playing && (n === 'rArm' || n === 'lArm') && v.y > top + 0.2) continue;
      const gap = RING.inner - r;
      if (gap < worst.gap) worst = { gap, mode, part: n, t: o.t, y: v.y.toFixed(2), r: r.toFixed(2) };
    }
  }
}
for (let t = 0; t < 36; t += 0.05) check('quieto', { t, floating: true, playing: false, speed: 0, vy: 0, hand: 0, handHit: 0 });
for (const speed of [0.25, 0.5, 1, 1.4]) for (let t = 0; t < 6.4; t += 0.05) check('avanzando ' + speed, { t, floating: true, playing: false, speed, vy: 0, hand: 0, handHit: 0 });
for (let j = 0; j < 1; j += 0.01) for (const speed of [0, 1, 1.4]) {
  const vy = 0.35 * Math.cos(Math.PI * j), t = j * 3.2;
  check('saltando ' + speed, { t, floating: true, playing: false, speed, vy, hand: 0, handHit: 0 }, 9 * Math.sin(Math.PI * j));
}
for (let t = 0; t < 6.4; t += 0.05) for (const hand of [0, 1]) for (const handHit of [0, 0.5, 1])
  check('tocando', { t, floating: true, playing: true, speed: 0, vy: 0, hand, handHit });
for (let t = 0; t < 3; t += 0.05) check('sin flotar', { t, floating: false, playing: false, speed: 1, vy: 0, hand: 0, handHit: 0 });
console.log('Hueco mínimo hasta las teclas (px; negativo = las atraviesa):', worst.gap.toFixed(2), worst);
// Con armadura los brazos y las piernas engordan 1 px por lado: hace falta al menos 1,5 px de hueco.
process.exit(worst.gap < 1.5 ? 1 : 0);
