// Guitarra "Blossom": la forma de una Jackson Kelly (pala puntiaguda con 6 clavijas en un lado, aletas de tiburón en
// el diapasón, dos humbuckers, puente Floyd Rose con palanca y un cuerpo de puntas: cuerno largo arriba a la derecha,
// cintura muy marcada y ala larga abajo a la izquierda) con la paleta Blossom (flor de cerezo): nácar y rosas en
// degradado, rama de cerezo con flores en relieve, pétalos cayendo, diapasón ciruela y herrajes de oro rosa.
//
// Mismo formato que la guitarra Juliet (scripts/textures/juliet_guitar): vóxeles de 1 en (x, y, z) con y hacia abajo
// (0 = punta de la pala), x = 0 en el centro del mástil y z = 3 la tapa; las cajas sueltas llevan tamaño (sx, sy, sz).
// En node imprime las cajas en JSON (node design.js > design.json); en la vista previa se usa gen() directamente.
const PAL_NAMES = ['pearl', 'blush', 'petal', 'rose', 'cherry', 'bark', 'barkHi', 'board', 'inlay', 'fret', 'gold', 'goldDk',
  'white', 'pole', 'string', 'back', 'neck'];
const PAL = [0xfff4f8, 0xffd6e6, 0xf7a8c8, 0xe784ae, 0xc2326f, 0x5a3440, 0x7a4a58, 0x3d2430, 0xffe4f0, 0xe8c4b8, 0xe2a48c, 0xb9786a,
  0xffffff, 0xff6fae, 0xf5f0f5, 0xf2c9da, 0xf6e6dc];
const C = Object.fromEntries(PAL_NAMES.map((n, i) => [n, i]));

// Contornos (de la foto, a 19 px por vóxel): el cuerpo empezando en la unión izquierda del mástil, y la pala.
const BODY = [[-2.2, 55.6], [-5, 56.6], [-9.2, 58.4], [-8.2, 61], [-7.2, 64.5], [-6.8, 68.4], [-7.6, 73], [-9.6, 78], [-13, 84], [-17, 89.6],
  [-20.1, 94], [15.2, 80], [13.5, 74], [11.3, 68], [10.1, 64], [10.2, 61], [11.5, 57.5], [13.6, 53], [15.9, 48.8], [13, 50.5],
  [10, 52.4], [7, 54.5], [4.5, 56.2], [2.2, 57.6]];
const HEAD = [[-2, 14], [-3.4, 12], [-4.2, 10], [-4.3, 8.2], [7.5, 0], [6.8, 2.5], [4.5, 7.5], [2.6, 12], [2, 14]];
// Ramas de cerezo (de punta a punta) y flores: [x, y, blanca].
// El cuerpo es estrecho (18-26 vóxeles por fila con los herrajes en medio): las flores grandes van en el ala de abajo
// y en el cuerno; las pequeñas, en los huecos de los lados. [x, y, blanca, grande].
// La rama cruza la parte de abajo del ala izquierda hasta debajo del puente y sube por la derecha entre los
// potenciómetros y el canto.
const BRANCHES = [[[-16, 89], [-12, 86], [-7, 83], [-2, 80.5], [3, 79]], [[3, 79], [7, 77.6], [10, 74], [10, 70], [8, 68]], [[8, 68], [7.5, 64], [7, 61]], [[-12, 86], [-14, 88]]];
const FLOWERS = [[-7, 83, 0, 1], [3, 79, 1, 1], [8, 68, 1, 1], [-14, 88, 1, 0], [7, 61, 0, 0], [10.5, 72.5, 1, 0]];
// Pastillas, puente, potenciómetros y selector.
const PICKUPS = [59, 65];
const BRIDGE = [71, 76];
const KNOBS = [[6, 72], [7.8, 75.5], [9.2, 79]];
const SWITCH = [-4, 88.5];
const NUT = 14, NECK_END = 58, FRETS = 24, SCALE_LEN = 57.3;

function inside(poly, x, y) {
  let c = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const [xi, yi] = poly[i], [xj, yj] = poly[j];
    if ((yi > y) !== (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c;
  }
  return c;
}
function segDist(px, py, a, b) {
  const vx = b[0] - a[0], vy = b[1] - a[1], L = vx * vx + vy * vy, t = Math.max(0, Math.min(1, ((px - a[0]) * vx + (py - a[1]) * vy) / L));
  return Math.hypot(px - a[0] - t * vx, py - a[1] - t * vy);
}
function edgeDist(poly, x, y) {
  let d = 1e9;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) d = Math.min(d, segDist(x, y, poly[j], poly[i]));
  return d;
}

function gen() {
  const M = new Map(), X = [];
  const set = (x, y, z, c) => M.set(x + ',' + y + ',' + z, { x, y, z, c, sx: 1, sy: 1, sz: 1 });
  const get = (x, y, z) => M.get(x + ',' + y + ',' + z);
  const box = (x, y, z, c, sx, sy, sz) => X.push({ x, y, z, c, sx, sy, sz });
  const hardware = (x, y) => (x >= -5 && x < 5 && y >= 57 && y < 78) || Math.hypot(x + 0.5 - SWITCH[0], y - SWITCH[1]) < 1.6
    || KNOBS.some(k => Math.hypot(x + 0.5 - k[0], y - k[1]) < 1.6);

  // Cuerpo: 4 capas; la tapa (z = 3) rosa pétalo, más clara en el centro y más intensa hacia los bordes, con un filete
  // blanco continuo por todo el canto.
  for (let y = 46; y <= 95; y++) for (let x = -22; x <= 17; x++) {
    const cx = x + 0.5, cy = y;
    if (!inside(BODY, cx, cy)) continue;
    const e = edgeDist(BODY, cx, cy);
    for (let z = 0; z <= 2; z++) set(x, y, z, C.back);
    set(x, y, 3, e < 1 ? C.white : e < 3 ? C.rose : e < 8 ? C.petal : C.blush);
  }
  // Pala: rosa con filete blanco, las 6 clavijas de oro rosa en el canto de arriba y una flor en lugar del logo.
  for (let y = -1; y <= 14; y++) for (let x = -6; x <= 8; x++) {
    const cx = x + 0.5, cy = y;
    if (!inside(HEAD, cx, cy)) continue;
    const e = edgeDist(HEAD, cx, cy);
    set(x, y, 2, C.rose);
    set(x, y, 3, e < 0.8 ? C.white : C.petal);
  }
  for (let k = 0; k < 6; k++) {
    const t = (k + 0.5) / 6, ex = -4.25 + (7.5 + 4.25) * t, ey = 8.2 + (0 - 8.2) * t; // canto de arriba
    const nx = -8.2, ny = -11.75, nl = Math.hypot(nx, ny), ox = nx / nl, oy = ny / nl; // normal hacia fuera
    box(ex - ox * 1.3 - 0.5, ey - oy * 1.3, 3.6, C.gold, 0.9, 0.9, 0.8); // poste
    box(ex + ox * 0.9 - 0.5, ey + oy * 0.9, 2.5, C.goldDk, 1.2, 1.2, 1); // llave
    box(ex + ox * 1.9 - 0.5, ey + oy * 1.9, 2.5, C.gold, 1.4, 0.6, 1.4);
  }
  sakura(set, 3, 4, 4, 1);
  // Mástil (madera clara detrás) y diapasón ciruela con trastes de oro rosa y aletas de tiburón nácar.
  for (let y = NUT; y <= NECK_END; y++) for (let x = -2; x <= 1; x++) {
    set(x, y, 3, C.neck); set(x, y, 2, C.neck); if (x === -1 || x === 0) set(x, y, 1, C.neck);
    set(x, y, 4, y === NUT ? C.white : C.board);
  }
  const fretY = n => NUT + SCALE_LEN * (1 - Math.pow(2, -n / 12));
  for (let n = 1; n <= FRETS; n++) box(-0.5, fretY(n), 4.55, C.fret, 4, 0.22, 0.12);
  for (const n of [3, 5, 7, 9, 12, 15, 17, 19, 21, 24]) {
    // Aleta: del borde de un lado al otro, ancha junto al traste de abajo y en punta junto al de arriba.
    const y0 = fretY(n - 1) + 0.25, y1 = fretY(n) - 0.25, steps = Math.max(2, Math.round((y1 - y0) / 0.25));
    for (let s = 0; s < steps; s++) {
      const u = (s + 0.5) / steps, w = 3.4 * u, yy = y0 + (y1 - y0) * u;
      box(-2 + 0.3 + w / 2 - 0.5, yy, 4.52, C.inlay, w, (y1 - y0) / steps, 0.06);
    }
  }
  // Pastillas: marco de oro rosa, tapa blanca y dos filas de 6 polos rosas.
  for (const p of PICKUPS) {
    for (let y = p; y < p + 5; y++) for (let x = -4; x <= 3; x++) {
      const ring = y === p || y === p + 4 || x === -4 || x === 3;
      set(x, y, 4, ring ? C.gold : C.white);
    }
    for (const row of [p + 1.5, p + 3.1]) for (let i = 0; i < 6; i++) box(-3.25 + i * 1.1, row, 4.55, C.pole, 0.55, 0.55, 0.12);
  }
  // Puente Floyd Rose: placa de oro rosa, 6 selletas, afinadores finos y la palanca que baja hacia la derecha.
  for (let y = BRIDGE[0]; y <= BRIDGE[1]; y++) for (let x = -4; x <= 3; x++) set(x, y, 4, y === BRIDGE[1] ? C.goldDk : C.gold);
  for (let i = 0; i < 6; i++) { box(-3.25 + i * 1.1, 72.5, 5, C.goldDk, 0.8, 1.6, 0.6); box(-3.25 + i * 1.1, 75.5, 4.9, C.gold, 0.55, 0.55, 0.8); }
  const arm = [[3.6, 74], [5.6, 75.5], [6.2, 78], [6.8, 81], [7.6, 84], [8.4, 87], [9, 89.5]];
  for (let i = 1; i < arm.length; i++) { const [x0, y0] = arm[i - 1], [x1, y1] = arm[i]; box((x0 + x1) / 2 - 0.5, (y0 + y1) / 2, 5.2, C.gold, 0.5, Math.hypot(x1 - x0, y1 - y0) + 0.3, 0.5); }
  box(arm[arm.length - 1][0] - 0.5, arm[arm.length - 1][1] + 0.6, 5.2, C.pearl, 0.8, 1.4, 0.8);
  // Potenciómetros y selector.
  for (const [kx, ky] of KNOBS) { box(kx - 0.5, ky, 4, C.gold, 1.6, 1.6, 1); box(kx - 0.5, ky, 4.6, C.pearl, 1, 1, 0.3); }
  box(SWITCH[0] - 0.5, SWITCH[1], 4, C.gold, 1.4, 1.4, 0.8); box(SWITCH[0] - 0.5, SWITCH[1], 4.7, C.cherry, 0.7, 0.7, 0.6);
  // Ramas de cerezo en relieve (oscuras, con la cara de arriba algo más clara) y sus flores.
  for (const br of BRANCHES) for (let i = 1; i < br.length; i++) {
    const [x0, y0] = br[i - 1], [x1, y1] = br[i], n = Math.ceil(Math.hypot(x1 - x0, y1 - y0) * 2);
    for (let s = 0; s <= n; s++) {
      const x = Math.floor(x0 + (x1 - x0) * s / n), y = Math.floor(y0 + (y1 - y0) * s / n);
      if (get(x, y, 3) && !hardware(x, y)) set(x, y, 4, (x + y) % 3 === 0 ? C.barkHi : C.bark);
    }
  }
  for (const [fx, fy, white, big] of FLOWERS) sakura((x, y, z, c) => { if (get(x, y, 3) && get(x, y, 3).c !== C.white) set(x, y, z, c); }, Math.floor(fx), Math.floor(fy), 4, white, big);
  // Pétalos sueltos posados en la tapa (los mismos siempre: semilla 11).
  let seed = 11; const rnd = () => (seed = seed * 16807 % 2147483647) / 2147483647;
  for (let k = 0; k < 18; k++) {
    const x = -18 + rnd() * 33, y = 50 + rnd() * 44, rot = rnd();
    if (!inside(BODY, x, y) || edgeDist(BODY, x, y) < 1.6 || hardware(Math.floor(x), Math.floor(y)) || get(Math.floor(x), Math.floor(y), 4)) continue;
    box(x - 0.5, y, 3.58, rot < 0.5 ? C.petal : C.blush, rot < 0.5 ? 0.9 : 0.6, rot < 0.5 ? 0.6 : 0.9, 0.16);
  }
  // Cuerdas: de la cejuela a las selletas.
  for (let i = 0; i < 6; i++) { const x = -0.5 + (i - 2.5) * 0.6, y0 = NUT, y1 = 73; X.push({ x, y: (y0 + y1) / 2, z: 4.85, c: C.string, sx: 0.12, sy: y1 - y0, sz: 0.12, str: 1 }); }
  return [...M.values(), ...X];
}

// Flor de cerezo en relieve (grande de 5x5 o pequeña de 3x3): pétalos (rosa o blanca), el interior más claro y el
// centro cereza encima.
const SAKURA = ['.P.P.', 'PPpPP', '.pcp.', 'PPpPP', '..P..'];
const SAKURA_SMALL = ['.P.', 'PcP', '.P.'];
function sakura(set, x, y, z, white, big = 1) {
  const art = big ? SAKURA : SAKURA_SMALL, h = art.length >> 1;
  art.forEach((row, r) => { for (let c = 0; c < row.length; c++) {
    const ch = row[c]; if (ch === '.') continue;
    const xx = x + c - h, yy = y + r - h;
    if (ch === 'c') { set(xx, yy, z, C.blush); set(xx, yy, z + 1, C.cherry); }
    else set(xx, yy, z, ch === 'P' ? (white ? C.white : C.rose) : (white ? C.blush : C.pearl));
  } });
}

if (typeof module !== 'undefined' && require.main === module) {
  const V = gen();
  console.log(JSON.stringify(V.map(v => ({ x: v.x, y: v.y, z: v.z, c: v.c, sx: v.sx, sy: v.sy, sz: v.sz, str: v.str ? 1 : 0, hex: PAL[v.c] }))));
}
