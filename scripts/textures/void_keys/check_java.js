// Compara VoidKeysMotion (Java) con el diseño (void_keys.html) número a número:
//   node check_java.js void_keys.html flow_keys.txt java.json
// java.json es la salida de CheckMotion.java (las mismas muestras, calculadas en Java).
const fs = require('fs'), vm = require('vm');
const html = fs.readFileSync(process.argv[2], 'utf8');
const flow = fs.readFileSync(process.argv[3], 'utf8').split('\n').filter(l => l.trim() && !l.startsWith('#')).map(l => l.trim().split(/\s+/).map(Number));
const design = html.slice(html.indexOf('<script>\n') + 9, html.indexOf('</script>', html.indexOf('<script>\n'))).replace('%FLOW%', JSON.stringify(flow));
const c = {}; vm.createContext(c);
vm.runInContext(design + ';Object.assign(this,{RING,noteKey,whiteAngle,blackAngle,keyPose,particle,rockPieces,streaks,rocks,figurePose,playPose,chaseLight,ghostKey,ghostLight,sigilState,dropDelta,orbState,dropState,SIGIL});', c);
const J = JSON.parse(fs.readFileSync(process.argv[4], 'utf8'));
const S = { keys: [], noteKeys: [], particles: [], rocks: [], poses: [], light: [], chase: [], ghostKeys: [], ghost: [], sigil: [], orb: [] };
const keyAngle = k => k < c.RING.whites ? c.whiteAngle(k) : c.blackAngle(k - c.RING.whites);
for (let key = 0; key < 44; key++) for (const t of [0, 1.3, 17.77, 123.4]) { const p = c.keyPose(keyAngle(key), t); S.keys.push([p.angle, p.y, p.tilt]); }
for (let n = 0; n < 44; n++) { const k = c.noteKey(n); S.noteKeys.push(k.white !== undefined ? k.white : c.RING.whites + k.black); }
for (const set of [c.streaks, c.rocks]) for (const p of set) for (const t of [0, 2.5, 33.3, 400]) { const q = c.particle(p, t); S.particles.push([q.x, q.y, q.z, q.s, q.rot, q.life]); }
for (const p of c.rocks) for (let t = 0; t < 60; t += 0.7) S.rocks.push(c.rockPieces(p, t).map(q => [q.x, q.y, q.z, q.s, q.rot, q.shape]));
const pose = p => [p.lift, p.pitch, p.yaw, p.roll, p.head[0], p.head[1], p.headYaw, p.rArm[0], p.rArm[1], p.lArm[0], p.lArm[1], p.rLeg[0], p.rLeg[1], p.lLeg[0], p.lLeg[1]];
for (let t = 0; t < 40; t += 0.37) for (const k of [[1, 0, 0], [1, 0.6, 0], [1, 1.4, 0], [1, 0, 0.3], [1, 1, -0.2], [0, 1, 0], [0, 0, 0.1]])
  S.poses.push(pose(c.figurePose({ t, floating: k[0] === 1, playing: false, speed: k[1], vy: k[2] })));
for (let t = 0; t < 205; t += 0.53) for (const playIn of [0.4, 1]) {
  const q = c.playPose(t); S.poses.push(pose(c.figurePose({ t, floating: true, playing: true, speed: 0, vy: 0, play: q, playIn })));
  S.poses.push([q.facing, q.down[0], q.down[1], q.reach[0], q.reach[1], q.look]);
}
// Luz: la vista previa enciende una tecla al llegar su nota y la apaga con min(1, edad/0,08)·(1-edad/0,9)².
const hits = Array.from({ length: 44 }, () => []);
for (const [ms, note] of flow) { const k = c.noteKey(note); hits[k.white !== undefined ? k.white : 26 + k.black].push(ms / 1000); }
for (let key = 0; key < 44; key++) for (let t = 0; t < 205; t += 1.7) {
  const past = hits[key].filter(h => h <= t), age = past.length ? t - past[past.length - 1] : 9;
  S.light.push(age < 0.9 ? Math.min(1, age / 0.08) * Math.pow(1 - age / 0.9, 2) : 0);
}
// Detalles: ola de luz, teclas fantasma, círculo del suelo y orbe / pilar en los drops.
for (let key = 0; key < 44; key++) for (let t = 0; t < 40; t += 0.13) S.chase.push(c.chaseLight(keyAngle(key), t));
for (let i = 0; i < 5000; i++) S.ghostKeys.push(c.ghostKey(i));
for (let key = 0; key < 44; key++) for (let t = 0; t < 60; t += 0.11) S.ghost.push(c.ghostLight(key, t));
for (let t = 0; t < 80; t += 0.29) { const q = c.sigilState(t); S.sigil.push([q.outer, q.inner, q.pulse, ...q.spikes.flatMap(p => [p.a, p.h])]); }
for (let songT = 0; songT < 205; songT += 0.047) {
  const d = c.dropDelta(songT), t = songT * 1.37 + 3, o = c.orbState(t, d), b = c.dropState(d, o.y);
  S.orb.push([d === null ? 0 : d, o.y, o.scale, o.rot, ...(b ? [b.pillarH, b.pillarW, b.waveR, b.waveS] : [])]);
  S.orb.push(b ? b.shards.map(q => [q.x, q.y, q.z, q.s, q.rot, q.shape]) : []);
}
let worst = 0, where = '';
const cmp = (a, b, path) => {
  if (Array.isArray(a)) { if (a.length !== b.length) { worst = Infinity; where = path + ' (longitud ' + a.length + ' vs ' + b.length + ')'; return; } a.forEach((v, i) => cmp(v, b[i], path + '[' + i + ']')); return; }
  const d = Math.abs(a - b); if (d > worst) { worst = d; where = path; }
};
for (const k in S) cmp(S[k], J[k], k);
console.log('Diferencia máxima entre el diseño y Java:', worst, where);
process.exit(worst > 1e-6 ? 1 : 0);
