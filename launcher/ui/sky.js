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
    for (let i = 0; i < 70; i++) {
      const sx = ((i * 7919) % w) + Math.round(px * 0.3);
      const sy = ((i * 104729) % Math.max(1, Math.floor(horizon / 3))) + Math.round(py * 0.3);
      if ((Math.floor(t * 2.5) + i * 13) % 7 === 0) continue;
      ctx.fillStyle = hex(theme.star, i % 3 === 0 ? 0.9 : 0.45);
      ctx.fillRect(sx, sy, 1, 1);
    }

    // Sol que se pone (solo en el atardecer).
    if (theme.sun) {
      const sunX = Math.floor(w * 0.68 + px * 0.5);
      const sunY = (theme === THEMES.mint ? Math.floor(horizon / 4) : horizon - 2) + Math.round(py * 0.5);
      disc(sunX, sunY, 17, hex(0xffd27a, 0.3));
      disc(sunX, sunY, 13, hex(0xffe08a));
      disc(sunX, sunY, 9, hex(0xfff1c2));
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
