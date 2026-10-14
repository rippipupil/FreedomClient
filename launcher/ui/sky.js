// Cielo en pixel art (el mismo del menú del cliente), dibujado a baja resolución y escalado sin suavizar.
// Tema Angel Devil: atardecer. Tema Neon: noche con tormenta y rayos que mezclan amarillo, lima, cian y azul.
// Se anima a pocos FPS y se para cuando la ventana no se ve.
(() => {
  const SUNSET = [
    0x1a0b2e, 0x241035, 0x2e1339, 0x3b1639, 0x4a1838, 0x5c1a35, 0x701c31, 0x86202e,
    0x9c262c, 0xb22f2a, 0xc63d29, 0xd74f2a, 0xe4642e, 0xee7a34, 0xf4913c, 0xf7a845,
    0xf6bd52, 0xf2c94c,
  ];
  const STORM = [
    0x04061a, 0x060a24, 0x080e2e, 0x0b1338, 0x0e1842, 0x121d4d, 0x162358, 0x1a2963,
    0x1f306f, 0x25387b, 0x2b4086, 0x31488f, 0x2f5aa0, 0x2d6fb0, 0x2c86bf,
  ];
  const PURPLE = [
    0x120726, 0x1a0a33, 0x230d40, 0x2d114d, 0x38155a, 0x441a66, 0x511f72, 0x5f257d,
    0x6e2c87, 0x7e3490, 0x8f3e98, 0xa04a9e, 0xb158a3, 0xc168a6, 0xd07ba8,
  ];
  const MINT = [
    0x2e8c8a, 0x359692, 0x3da09a, 0x46aaa2, 0x50b4aa, 0x5bbdb2, 0x67c6ba, 0x74cec2,
    0x82d6ca, 0x91ddd1, 0xa1e4d9, 0xb2eae0, 0xc4f0e8,
  ];
  const PINK = [
    0xff7fbc, 0xff88c0, 0xff91c5, 0xff9aca, 0xffa3cf, 0xffacd4, 0xffb5d9, 0xffbede,
    0xffc7e3, 0xffd0e8, 0xffd9ed, 0xffe2f1, 0xffebf5, 0xfff3f9,
  ];
  const ALLEY = [
    0x0b0714, 0x0e0918, 0x110b1d, 0x140d22, 0x170f28, 0x1b112e, 0x1f1334, 0x24153a,
    0x2a1740, 0x311945, 0x3a1b4a, 0x441d4e, 0x501f51, 0x5c2153,
  ];
  // Fantasmita de FreedomPlayer en pequeño (para el cielo rosa): b cuerpo, k ojos y boca.
  const GHOST = [
    "...bbbb...",
    "..bbbbbb..",
    ".bbbbbbbb.",
    ".bbkbbkbb.",
    "bbbbkkbbbb",
    ".bbbbbbbb.",
    ".bbbbbbbb.",
    ".b.bb.bb.b",
  ];
  // Colores de los rayos de arriba abajo: se mezclan como la energía de Neon.
  const BOLT = [0xffe14a, 0xe6f055, 0xc6f25a, 0x8ef07a, 0x5ef0c8, 0x3fd7ff, 0x3fa8ff, 0x3a7bff, 0x6b5bff];
  const THEMES = {
    angel: {
      bands: SUNSET, star: 0xf5f1e8, sun: true,
      clouds: [0xe89a7a, 0xb8606a, 0xffc9a0, 0xd9776a], hills: [0x5c1a2a, 0x3a0f1a], rim: 0,
    },
    neon: {
      bands: STORM, star: 0xbfefff, sun: false,
      clouds: [0x26307a, 0x121840, 0x1c2458, 0x1a2160], hills: [0x0b1236, 0x060920], rim: 0x2f6bff,
    },
    purple: {
      bands: PURPLE, star: 0xf3e6ff, sun: false,
      clouds: [0x9a63b8, 0x7a4a9e, 0x7a4a9e, 0x5e3680], hills: [0x4a2270, 0x1e0b33], rim: 0,
    },
    mint: {
      bands: MINT, star: 0xffffff, sun: true,
      clouds: [0xffffff, 0xd4f2ea, 0xf2fffb, 0xcdefe6], hills: [0x5fbf8c, 0x2f7a5e], rim: 0,
    },
    pink_ghost: {
      bands: PINK, star: 0xffffff, sun: true, noStars: true, ghosts: true,
      clouds: [0xffffff, 0xffc4df, 0xffe6f2, 0xffc4df], hills: [0xfbc6de, 0xf49cc4], rim: 0,
    },
    neon_alley: {
      bands: ALLEY, star: 0xf4a6cb, sun: false, city: true,
      clouds: [0x34204a, 0x2a1a3e, 0x2a1a3e, 0x22142f], hills: [0x1c1430, 0x150f21], rim: 0xff3fa4,
    },
  };
  const CLOUD = [
    "......####..........",
    "...##########.......",
    "..##############....",
    ".##################.",
    "####################",
    ".##################.",
  ];
  const CLOUD_SMALL = ["...###....", ".#######..", "##########", ".########."];
  const P = 4; // píxeles de pantalla por píxel del cielo
  const FPS = 12;

  const canvas = document.getElementById("sky");
  const ctx = canvas.getContext("2d", { alpha: false });
  let w = 0;
  let h = 0;
  let mouseX = 0;
  let mouseY = 0;
  let timer = null;
  let theme = THEMES.angel;
  let storm = false;

  const hex = (c, a = 1) => `rgba(${(c >> 16) & 255},${(c >> 8) & 255},${c & 255},${a})`;

  function resize() {
    w = Math.ceil(window.innerWidth / P);
    h = Math.ceil(window.innerHeight / P);
    canvas.width = w;
    canvas.height = h;
  }

  function shape(rows, x, y, color) {
    ctx.fillStyle = color;
    rows.forEach((row, ry) => {
      for (let rx = 0; rx < row.length; rx++) {
        if (row[rx] === "#") ctx.fillRect(Math.round(x) + rx, Math.round(y) + ry, 1, 1);
      }
    });
  }

  function disc(cx, cy, r, color) {
    ctx.fillStyle = color;
    for (let y = -r; y <= r; y++) {
      const half = Math.floor(Math.sqrt(Math.max(0, r * r - y * y)));
      ctx.fillRect(cx - half, cy + y, half * 2, 1);
    }
  }

  // Números pseudoaleatorios repetibles: el mismo rayo se dibuja igual en todos sus fotogramas.
  function seeded(seed) {
    let a = seed >>> 0;
    return () => {
      a = (a + 0x6d2b79f5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function bolt(x, top, bottom, random, alpha, branches) {
    let y = top;
    while (y < bottom) {
      const direction = random() < 0.5 ? 1 : -1;
      const segment = 2 + Math.floor(random() * 4);
      for (let k = 0; k < segment && y < bottom; k++) {
        const f = (y - top) / Math.max(1, bottom - top);
        const rgb = BOLT[Math.min(BOLT.length - 1, Math.floor(f * BOLT.length))];
        ctx.fillStyle = hex(rgb, alpha * 0.25);
        ctx.fillRect(x - 1, y, 1, 1);
        ctx.fillRect(x + 1, y, 1, 1);
        ctx.fillStyle = hex(rgb, alpha);
        ctx.fillRect(x, y, 1, 1);
        y++;
        if (k % 2 === 0) x += direction;
      }
      if (branches && random() < 0.18) {
        bolt(x + direction, y, Math.min(bottom, y + Math.floor((bottom - top) / 4)), random, alpha * 0.6, false);
      }
    }
  }

  // Dos "carriles" de rayos con periodos distintos: caen de vez en cuando, con un destello suave del cielo.
  function lightning(now, horizon, px) {
    let active = false;
    for (let lane = 0; lane < 2; lane++) {
      const period = 3400 + lane * 1500;
      const t = now + lane * 1900;
      const phase = t % period;
      const duration = 700;
      if (phase > duration) continue;
      active = true;
      const life = phase / duration;
      const strength = life < 0.08 ? life / 0.08 : (1 - life) * (1 - life);
      const random = seeded(Math.floor(t / period) * 7919 + lane * 31);
      ctx.fillStyle = hex(0x3fd7ff, strength * 0.07);
      ctx.fillRect(0, 0, w, horizon);
      const x = Math.floor(w * (0.12 + random() * 0.76) + px * 0.6);
      const top = Math.floor(horizon / 5 + random() * (horizon / 8));
      const bottom = Math.floor(horizon - 2 - random() * (horizon / 3));
      bolt(x, top, bottom, random, strength, true);
    }
    return active;
  }

  function draw() {
    const now = performance.now();
    const t = now / 1000;
    const BANDS = theme.bands;
    const horizon = Math.floor(h * 0.74);
    const band = Math.max(1, Math.floor(horizon / BANDS.length));
    const px = mouseX * 3;
    const py = mouseY * 2;

    for (let i = 0; i < BANDS.length; i++) {
      const y1 = i * band;
      const y2 = i === BANDS.length - 1 ? h : y1 + band;
      ctx.fillStyle = hex(BANDS[i]);
      ctx.fillRect(0, y1, w, y2 - y1);
      if (i + 1 < BANDS.length) {
        ctx.fillStyle = hex(BANDS[i + 1]);
        for (let x = i % 2; x < w; x += 2) ctx.fillRect(x, y2 - 1, 1, 1);
      }
    }

    // Estrellas que parpadean en la parte alta (la capa más lejana).
    for (let i = 0; i < (theme.noStars ? 0 : 70); i++) {
      const sx = ((i * 7919) % w) + Math.round(px * 0.3);
      const sy = ((i * 104729) % Math.max(1, Math.floor(horizon / 3))) + Math.round(py * 0.3);
      if ((Math.floor(t * 2.5) + i * 13) % 7 === 0) continue;
      ctx.fillStyle = hex(theme.star, i % 3 === 0 ? 0.9 : 0.45);
      ctx.fillRect(sx, sy, 1, 1);
    }

    // Sol que se pone (solo en el atardecer).
    if (theme.sun) {
      const sunX = Math.floor(w * 0.68 + px * 0.5);
      const high = theme === THEMES.mint || theme === THEMES.pink_ghost;
      const sunY = (high ? Math.floor(horizon / 4) : horizon - 2) + Math.round(py * 0.5);
      // Pink Ghost: sol blanco con halo rosa.
      const pink = theme === THEMES.pink_ghost;
      disc(sunX, sunY, 17, hex(pink ? 0xffffff : 0xffd27a, pink ? 0.35 : 0.3));
      disc(sunX, sunY, 13, hex(pink ? 0xffffff : 0xffe08a));
      disc(sunX, sunY, 9, hex(pink ? 0xfff6fa : 0xfff1c2));
    }

    // Luna en el tema morado.
    if (theme === THEMES.purple) {
      const moonX = Math.floor(w * 0.68 + px * 0.5);
      const moonY = Math.floor(horizon / 4) + Math.round(py * 0.5);
      disc(moonX, moonY, 12, hex(0xe6b8ff, 0.18));
      disc(moonX, moonY, 8, hex(0xf6efff));
      disc(moonX - 2, moonY - 2, 2, hex(0xd8c8ee));
      disc(moonX + 3, moonY + 2, 1, hex(0xd8c8ee));
    }

    // Nubes que se mueven despacio.
    const drift = (t * 1.6) % (w + 60);
    shape(CLOUD, ((w * 0.1 + drift) % (w + 40)) - 30 + px, h * 0.18 + py, hex(theme.clouds[0]));
    shape(CLOUD, ((w * 0.1 + drift) % (w + 40)) - 30 + px, h * 0.18 + py + 4, hex(theme.clouds[1]));
    shape(CLOUD_SMALL, ((w * 0.62 + drift * 0.7) % (w + 30)) - 15 + px, h * 0.3 + py, hex(theme.clouds[2]));
    shape(CLOUD_SMALL, ((w * 0.35 + drift * 1.2) % (w + 30)) - 15 + px, h * 0.46 + py, hex(theme.clouds[3]));
    storm = theme === THEMES.neon && lightning(now, horizon, px);

    // Pink Ghost: fantasmitas rosas y blancos paseando entre las nubes.
    if (theme.ghosts) {
      for (let i = 0; i < 4; i++) {
        const gx = ((w * 0.2 * i + t * (4 + (i % 2) * 2)) % (w + 20)) - 10 + px;
        const gy = horizon * (0.2 + 0.15 * i) + Math.sin(t * 1.6 + i * 2) * 2 + py;
        GHOST.forEach((row, ry) => {
          for (let rx = 0; rx < row.length; rx++) {
            if (row[rx] === ".") continue;
            ctx.fillStyle = hex(row[rx] === "k" ? (i % 2 ? 0x1a1420 : 0xc2326f) : i % 2 ? 0xf3eef4 : 0xf4a6cb);
            ctx.fillRect(Math.round(gx) + rx, Math.round(gy) + ry, 1, 1);
          }
        });
      }
    }

    // Neon Alley: la ciudad con ventanas de neón que se encienden y apagan, en lugar de las colinas.
    if (theme.city) {
      let s = 7;
      const r = () => (s = (s * 16807) % 2147483647) / 2147483647;
      for (let x = 0, i = 0; x < w; i++) {
        const bw = 6 + Math.floor(r() * 12);
        const bh = Math.floor(h * (0.12 + r() * 0.22));
        const top = h - bh;
        ctx.fillStyle = hex([0x1a1328, 0x160f22, 0x1c1430][i % 3]);
        ctx.fillRect(x + Math.round(px * 0.8), top, bw, bh);
        for (let k = 0; k < 6; k++) {
          const wx = x + 1 + Math.floor(r() * (bw - 2)), wy = top + 2 + Math.floor(r() * (bh - 4));
          const on = (Math.floor(t / (2 + (k % 3))) + i * 7 + k) % 5 !== 0;
          if (on && r() < 0.7) {
            ctx.fillStyle = hex([0xff3fa4, 0x5b57d9, 0x2f9c82, 0xf4a6cb][(i + k) % 4], 0.6);
            ctx.fillRect(wx + Math.round(px * 0.8), wy, 1, 1);
          }
        }
        x += bw;
      }
      ctx.fillStyle = hex(0x0d0915);
      ctx.fillRect(0, h - 2, w, 2);
      return;
    }

    // Colinas en primer plano (la capa más cercana es la que más se mueve).
    // En el tema Neon las cimas tienen un borde de luz azul que brilla más con cada rayo.
    const hills = (base, amp, freq, color, shift, rim) => {
      for (let x = 0; x < w; x++) {
        const hy = Math.round(base + Math.sin((x + shift) * freq) * amp + Math.sin((x + shift) * freq * 2.7) * amp * 0.4);
        ctx.fillStyle = color;
        ctx.fillRect(x, hy, 1, h - hy);
        if (rim) {
          ctx.fillStyle = rim;
          ctx.fillRect(x, hy, 1, 1);
        }
      }
    };
    const rim = theme.rim ? hex(theme.rim, storm ? 0.85 : 0.45) : null;
    hills(horizon + 4 + py, 5, 0.045, hex(theme.hills[0]), Math.round(px * 1.4), rim);
    hills(horizon + 14 + py * 1.5, 6, 0.03, hex(theme.hills[1]), 90 + Math.round(px * 2.2), rim && hex(theme.rim, storm ? 0.6 : 0.3));
  }

  function start() {
    if (timer) return;
    draw();
    timer = setInterval(draw, 1000 / FPS);
  }

  function stop() {
    clearInterval(timer);
    timer = null;
  }

  window.addEventListener("resize", () => {
    resize();
    draw();
  });
  window.addEventListener("mousemove", (e) => {
    mouseX = e.clientX / window.innerWidth - 0.5;
    mouseY = e.clientY / window.innerHeight - 0.5;
  });
  document.addEventListener("visibilitychange", () => (document.hidden ? stop() : start()));
  window.fcSky = {
    setTheme(name) {
      theme = THEMES[name] || THEMES.angel;
      draw();
    },
  };
  // El tema guardado se aplica antes de que carguen los ajustes para que no parpadee.
  try {
    theme = THEMES[localStorage.getItem("fc-theme")] || THEMES.angel;
  } catch (e) {}
  resize();
  start();
})();
