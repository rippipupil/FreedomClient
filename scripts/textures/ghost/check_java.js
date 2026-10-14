// Compara GhostMotion (Java) con el diseño (ghost.html) número a número:
//   node check_java.js ghost.html java.json
// java.json es la salida de CheckGhost.java (las mismas muestras, calculadas en Java).
const fs = require('fs'), vm = require('vm');
const html = fs.readFileSync(process.argv[2], 'utf8');
const design = html.slice(html.indexOf('// DISEÑO'), html.indexOf('(function themes() {')).replace('%GHOST%', '{"sprites":{}}');
const c = {}; vm.createContext(c);
vm.runInContext('const GHOST_JSON = 0;' + design.replace(/^\/\/ DISEÑO.*$/m, '') + ';Object.assign(this,{petOrbit,petAnchor,petHurt,petFace,petLimbs,auraGhost,SPARKLES,sparkle,flowerState,petalState,PET,AURA});', c);
const J = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));
const FACES = ['feliz', 'contento', 'owo', 'uwu', 'guino', 'sorpresa', 'dormido', 'triste'];
const MOODS = ['idle', 'wave', 'celebrate', 'sleep', 'hide', 'hurt', 'fly'], EMO = ['none', 'happy', 'blush', 'hearts'];
const S = { poses: [], faces: [], limbs: [], aura: [], sparkles: [], flowers: [], petals: [] };
const P = o => [o.x, o.y, o.z, o.yaw, o.pitch, o.roll];
// La pose como en el bucle de la vista previa: vuelta, golpe, sitio del estado y la mezcla desde "from".
function pose(mood, t, s, from) {
  let p = c.petOrbit(t); const a = c.petAnchor(mood, t, s);
  if (mood === 'hurt') p = c.petHurt(p, s);
  if (a) p = a;
  if (from && s < c.PET.blend) { const k = (u => { u = Math.max(0, Math.min(1, u)); return u * u * (3 - 2 * u); })(s / c.PET.blend);
    p = { x: from.x + (p.x - from.x) * k, y: from.y + (p.y - from.y) * k, z: from.z + (p.z - from.z) * k, yaw: from.yaw + Math.atan2(Math.sin(p.yaw - from.yaw), Math.cos(p.yaw - from.yaw)) * k, pitch: from.pitch + (p.pitch - from.pitch) * k, roll: from.roll + (p.roll - from.roll) * k }; }
  return p;
}
const from = { x: 3, y: 20, z: -4, yaw: 2, pitch: 0.1, roll: -0.3 };
for (const m of MOODS) for (let t = 0; t < 30; t += 0.43) for (const s of [0, 0.3, 0.79, 1.3, 2.7]) {
  S.poses.push(P(pose(m, t, s, null)), P(pose(m, t, s, from)));
  for (const e of EMO) {
    S.faces.push(FACES.indexOf(c.petFace(m === 'hurt' && s > c.PET.hurt ? 'idle' : m, e, t)));
    const l = c.petLimbs(m, e, t, s); S.limbs.push([l.left, l.right, l.hop]);
  }
}
for (let i = 0; i < 6; i++) for (let t = 0; t < 60; t += 0.37) { const q = c.auraGhost(i, t); S.aura.push([q.x, q.y, q.z, q.yaw, q.roll, q.s, FACES.indexOf(q.face), q.white ? 1 : 0]); }
for (const p of c.SPARKLES) for (let t = 0; t < 40; t += 0.61) { const q = c.sparkle(p, t); S.sparkles.push([q.x, q.y, q.z, q.s, q.rot]); }
for (let a = -0.2; a < 3.3; a += 0.07) for (const seed of [0, 0.37, 0.9]) { const q = c.flowerState(a, seed, 3); S.flowers.push(q ? [q.s, q.sway, q.close] : []); }
for (let a = -0.1; a < 1.3; a += 0.05) { const q = c.petalState(a, 2.5, -1.5, 4.2); S.petals.push(q ? [q.x, q.y, q.z, q.rot, q.s] : []); }
let worst = 0, where = '';
const cmp = (a, b, path) => {
  if (Array.isArray(a)) { if (!Array.isArray(b) || a.length !== b.length) { worst = Infinity; where = path + ' (longitud)'; return; } a.forEach((v, i) => cmp(v, b[i], path + '[' + i + ']')); return; }
  const d = Math.abs(a - b); if (!(d <= worst)) { worst = d; where = path; }
};
for (const k in S) cmp(S[k], J[k], k);
console.log('Diferencia máxima entre el diseño y Java:', worst, where);
process.exit(worst > 1e-9 ? 1 : 0);
