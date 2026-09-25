// Cielo de atardecer en pixel art (el mismo del menú del cliente), dibujado a baja resolución
// y escalado sin suavizar. Se anima a pocos FPS y se para cuando la ventana no se ve.
(() => {
  const BANDS = [
    0x1a0b2e, 0x241035, 0x2e1339, 0x3b1639, 0x4a1838, 0x5c1a35, 0x701c31, 0x86202e,
    0x9c262c, 0xb22f2a, 0xc63d29, 0xd74f2a, 0xe4642e, 0xee7a34, 0xf4913c, 0xf7a845,
    0xf6bd52, 0xf2c94c,
  ];
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

  function draw() {
    const t = performance.now() / 1000;
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
      ctx.fillStyle = hex(0xf5f1e8, i % 3 === 0 ? 0.9 : 0.45);
      ctx.fillRect(sx, sy, 1, 1);
    }

    // Sol que se pone.
    const sunX = Math.floor(w * 0.68 + px * 0.5);
    const sunY = horizon - 2 + Math.round(py * 0.5);
    disc(sunX, sunY, 17, hex(0xffd27a, 0.3));
    disc(sunX, sunY, 13, hex(0xffe08a));
    disc(sunX, sunY, 9, hex(0xfff1c2));

    // Nubes que se mueven despacio.
    const drift = (t * 1.6) % (w + 60);
    shape(CLOUD, ((w * 0.1 + drift) % (w + 40)) - 30 + px, h * 0.18 + py, hex(0xe89a7a));
    shape(CLOUD, ((w * 0.1 + drift) % (w + 40)) - 30 + px, h * 0.18 + py + 4, hex(0xb8606a));
    shape(CLOUD_SMALL, ((w * 0.62 + drift * 0.7) % (w + 30)) - 15 + px, h * 0.3 + py, hex(0xffc9a0));
    shape(CLOUD_SMALL, ((w * 0.35 + drift * 1.2) % (w + 30)) - 15 + px, h * 0.46 + py, hex(0xd9776a));

    // Colinas en primer plano (la capa más cercana es la que más se mueve).
    const hills = (base, amp, freq, color, shift) => {
      ctx.fillStyle = color;
      for (let x = 0; x < w; x++) {
        const hy = Math.round(base + Math.sin((x + shift) * freq) * amp + Math.sin((x + shift) * freq * 2.7) * amp * 0.4);
        ctx.fillRect(x, hy, 1, h - hy);
      }
    };
    hills(horizon + 4 + py, 5, 0.045, hex(0x5c1a2a), Math.round(px * 1.4));
    hills(horizon + 14 + py * 1.5, 6, 0.03, hex(0x3a0f1a), 90 + Math.round(px * 2.2));
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
  resize();
  start();
})();
