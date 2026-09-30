// Tema del launcher (Angel Devil, Neon, Morado o Menta). Se aplica antes de pintar nada para que no parpadee al abrir;
// app.js lo cambia cuando cargan los ajustes o el usuario elige otro.
(() => {
  // El logo (el icono con las letras FC) es el mismo en todos los temas.
  const LOGOS = { angel: "img/logo.png", neon: "img/logo.png", purple: "img/logo.png", mint: "img/logo.png" };

  function apply(name) {
    const theme = LOGOS[name] ? name : "angel";
    document.documentElement.dataset.theme = theme;
    document.querySelectorAll("img[data-logo]").forEach((img) => {
      if (!img.src.endsWith(LOGOS[theme])) img.src = LOGOS[theme];
    });
    if (window.fcSky) window.fcSky.setTheme(theme);
    try {
      localStorage.setItem("fc-theme", theme);
    } catch (e) {}
  }

  let saved = "angel";
  try {
    saved = localStorage.getItem("fc-theme") || "angel";
  } catch (e) {}
  document.documentElement.dataset.theme = saved;
  document.addEventListener("DOMContentLoaded", () => apply(saved));
  window.fcTheme = { apply };
})();
