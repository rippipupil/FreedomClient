// Interfaz del launcher. Habla con Rust por los comandos de Tauri (invoke) y los eventos (listen).
(() => {
  const tauri = window.__TAURI__;
  const invoke = tauri.core.invoke;
  const listen = tauri.event.listen;
  const $ = (id) => document.getElementById(id);

  const COLORS = ["#F2C94C", "#FF8C42", "#D7263D", "#E86A92", "#9B6BFF", "#5DADE2", "#4CC38A", "#F5F1E8"];
  let state = null;
  let editing = null; // perfil abierto en la página Perfiles
  let busy = false;

  // ---------- utilidades ----------
  function toast(text, error = false) {
    const el = document.createElement("div");
    el.className = "toast" + (error ? " error" : "");
    el.textContent = text;
    $("toasts").appendChild(el);
    setTimeout(() => el.remove(), error ? 7000 : 3500);
  }

  async function call(cmd, args) {
    try {
      return await invoke(cmd, args);
    } catch (e) {
      toast(String(e), true);
      throw e;
    }
  }

  const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
  const mb = (n) => (n >= 1024 ? (n / 1024).toFixed(n % 1024 === 0 ? 0 : 1) + " GB" : n + " MB");
  const head = (account) => (account ? `https://mc-heads.net/avatar/${account.kind === "microsoft" ? account.uuid : account.name}/32` : "img/icon.png");

  function ago(seconds) {
    if (!seconds) return "Never played";
    const diff = Date.now() / 1000 - seconds;
    if (diff < 3600) return "Played " + Math.max(1, Math.round(diff / 60)) + " min ago";
    if (diff < 86400) return "Played " + Math.round(diff / 3600) + " h ago";
    return "Played " + Math.round(diff / 86400) + " days ago";
  }

  function selectedProfile() {
    return state.profiles.find((p) => p.id === state.settings.selected_profile) || state.profiles[0];
  }

  function selectedAccount() {
    return state.accounts.find((a) => a.uuid === state.settings.selected_account) || state.accounts[0];
  }

  async function reload() {
    state = await invoke("load_state");
    render();
  }

  // ---------- navegación ----------
  document.querySelectorAll(".nav").forEach((button) =>
    button.addEventListener("click", () => showPage(button.dataset.page))
  );

  function showPage(page) {
    document.querySelectorAll(".nav").forEach((b) => b.classList.toggle("active", b.dataset.page === page));
    document.querySelectorAll(".page").forEach((p) => p.classList.toggle("active", p.id === "page-" + page));
    if (page === "profiles") openProfile(editing ? editing.id : selectedProfile().id);
  }

  // ---------- ventana ----------
  const win = tauri.window.getCurrentWindow();
  $("minimize").addEventListener("click", () => win.minimize());
  $("close").addEventListener("click", () => win.close());

  // ---------- render general ----------
  function render() {
    $("launcher-version").textContent = "v" + state.launcher_version;
    $("mc-version").textContent = state.minecraft_version;
    const account = selectedAccount();
    $("account-name").textContent = account ? account.name : "Add account";
    $("account-head").src = head(account);
    // Con el launcher oficial el login lo hace Mojang: aquí no hacen falta cuentas.
    $("account-button").classList.toggle("hidden", officialMode());
    const profile = selectedProfile();
    $("picker-name").textContent = profile.name;
    $("picker-swatch").style.background = profile.color;
    const commit = state.client_commit ? state.client_commit.slice(0, 7) : "not installed yet";
    $("client-version").textContent = "FreedomClient " + commit;
    $("memory-info").textContent = "RAM: " + mb(memoryFor(profile)) + (state.total_memory_mb ? " of " + mb(Math.round(state.total_memory_mb / 1024) * 1024) : "");
    renderLaunch();
    renderSettings();
    renderProfiles();
  }

  function officialMode() {
    return state.settings.launch_with === "official";
  }

  function memoryFor(profile) {
    return profile.memory_mb || state.settings.memory_mb || state.auto_memory_mb;
  }

  // ---------- Jugar ----------
  function renderLaunch() {
    const button = $("launch");
    const running = state.running && !busy;
    button.disabled = busy || running;
    button.classList.toggle("busy", busy || running);
    if (!busy) {
      $("launch-label").textContent = running ? "PLAYING" : "LAUNCH";
      $("launch-progress").style.width = "0";
    }
  }

  $("launch").addEventListener("click", async () => {
    if (busy) return;
    if (!officialMode() && !state.accounts.length) {
      openAccounts();
      toast("Add an account to play");
      return;
    }
    busy = true;
    renderLaunch();
    $("launch-label").textContent = "Preparing…";
    try {
      await invoke("launch", { id: selectedProfile().id });
      if (officialMode()) {
        busy = false;
        renderLaunch();
        $("launch-status").textContent = 'Press PLAY in the Minecraft Launcher ("FreedomClient" is already selected)';
        return;
      }
      $("launch-label").textContent = "PLAYING";
      $("launch-status").textContent = "Have fun!";
      $("launch-progress").style.width = "100%";
    } catch (e) {
      toast(String(e), true);
      $("launch-status").textContent = "";
      busy = false;
      renderLaunch();
      return;
    }
    busy = false;
    state.running = selectedProfile().id;
    renderLaunch();
  });

  listen("progress", ({ payload }) => {
    if (!busy) return;
    const { stage, done, total } = payload;
    if (total > 0) {
      const percent = Math.min(100, Math.floor((done / total) * 100));
      $("launch-label").textContent = percent + "%";
      $("launch-progress").style.width = percent + "%";
      $("launch-status").textContent = `${stage} · ${mb(Math.round(done / 1048576))} / ${mb(Math.round(total / 1048576))}`;
    } else {
      $("launch-label").textContent = "Preparing…";
      $("launch-status").textContent = stage + "…";
    }
  });

  listen("game-exit", ({ payload }) => {
    state.running = null;
    $("launch-status").textContent = payload && payload !== 0 ? "The game closed with an error (code " + payload + "). Check the log." : "";
    renderLaunch();
    reload();
  });

  // Selector de perfil debajo del botón.
  $("profile-picker").addEventListener("click", (e) => {
    e.stopPropagation();
    const menu = $("profile-menu");
    menu.innerHTML = "";
    state.profiles.forEach((p) => {
      const item = document.createElement("button");
      item.className = p.id === selectedProfile().id ? "selected" : "";
      item.innerHTML = `<i class="swatch" style="background:${escapeHtml(p.color)}"></i><span>${escapeHtml(p.name)}</span>`;
      item.addEventListener("click", async () => {
        state.settings.selected_profile = p.id;
        menu.classList.add("hidden");
        render();
        await call("select_profile", { id: p.id });
      });
      menu.appendChild(item);
    });
    menu.classList.toggle("hidden");
  });
  document.addEventListener("click", () => $("profile-menu").classList.add("hidden"));

  // Botones de carpetas y log (en Jugar usan el perfil seleccionado; en Perfiles, el que se edita).
  document.querySelectorAll("[data-open]").forEach((button) =>
    button.addEventListener("click", () => {
      const inProfiles = button.closest("#page-profiles");
      const id = inProfiles && editing ? editing.id : selectedProfile().id;
      call("open_path", { id, target: button.dataset.open });
    })
  );

  // ---------- Perfiles ----------
  function renderProfiles() {
    const list = $("profiles");
    list.innerHTML = "";
    state.profiles.forEach((p) => {
      const card = document.createElement("button");
      card.className = "profile-card" + (editing && editing.id === p.id ? " selected" : "");
      card.innerHTML = `<i class="swatch" style="background:${escapeHtml(p.color)}"></i><div><span>${escapeHtml(p.name)}</span><small>${ago(p.last_played)}</small></div>`;
      card.addEventListener("click", () => openProfile(p.id));
      list.appendChild(card);
    });
  }

  function openProfile(id) {
    const profile = state.profiles.find((p) => p.id === id) || state.profiles[0];
    editing = { ...profile };
    $("p-name").value = editing.name;
    $("p-server").value = editing.server;
    $("p-jvm").value = editing.extra_jvm_args;
    $("p-memory").max = Math.max(4096, Math.floor((state.total_memory_mb || 8192) * 0.75 / 512) * 512);
    $("p-memory").value = editing.memory_mb;
    updateProfileMemoryLabel();
    const colors = $("p-colors");
    colors.innerHTML = "";
    COLORS.forEach((c) => {
      const b = document.createElement("button");
      b.style.background = c;
      b.className = c.toLowerCase() === editing.color.toLowerCase() ? "selected" : "";
      b.addEventListener("click", () => {
        editing.color = c;
        colors.querySelectorAll("button").forEach((x) => x.classList.toggle("selected", x === b));
        saveProfile();
      });
      colors.appendChild(b);
    });
    $("delete-profile").disabled = state.profiles.length <= 1;
    renderProfiles();
    loadMods();
  }

  function updateProfileMemoryLabel() {
    const value = Number($("p-memory").value);
    $("p-memory-label").textContent = value ? mb(value) : "Settings default (" + mb(state.settings.memory_mb || state.auto_memory_mb) + ")";
  }

  let profileTimer = null;
  function saveProfile() {
    clearTimeout(profileTimer);
    profileTimer = setTimeout(async () => {
      editing.name = $("p-name").value;
      editing.server = $("p-server").value.trim();
      editing.extra_jvm_args = $("p-jvm").value;
      editing.memory_mb = Number($("p-memory").value);
      if (!editing.name.trim()) return;
      const saved = await call("save_profile", { profile: editing });
      const index = state.profiles.findIndex((p) => p.id === saved.id);
      if (index >= 0) state.profiles[index] = saved;
      render();
    }, 350);
  }

  ["p-name", "p-server", "p-jvm"].forEach((id) => $(id).addEventListener("input", saveProfile));
  $("p-memory").addEventListener("input", () => {
    updateProfileMemoryLabel();
    saveProfile();
  });

  $("new-profile").addEventListener("click", async () => {
    const color = COLORS[state.profiles.length % COLORS.length];
    const saved = await call("save_profile", {
      profile: { id: "", name: "Profile " + (state.profiles.length + 1), color, memory_mb: 0, extra_jvm_args: "", server: "", created: 0, last_played: 0 },
    });
    state.profiles.push(saved);
    openProfile(saved.id);
    $("p-name").focus();
    $("p-name").select();
  });

  $("delete-profile").addEventListener("click", async () => {
    if (!editing || state.profiles.length <= 1) return;
    const sure = await tauri.dialog.ask(`Delete "${editing.name}"? Its worlds, mods and settings will be deleted too.`, {
      title: "Delete profile",
      kind: "warning",
    });
    if (!sure) return;
    await call("delete_profile", { id: editing.id, deleteFiles: true });
    editing = null;
    await reload();
    openProfile(selectedProfile().id);
  });

  // Mods del perfil
  async function loadMods() {
    if (!editing) return;
    const mods = await call("list_mods", { id: editing.id });
    const box = $("mods");
    box.innerHTML = "";
    if (!mods.length) {
      box.innerHTML = '<div class="empty">Drop .jar files here or use "Add mods".</div>';
      return;
    }
    mods.forEach((mod) => {
      const row = document.createElement("div");
      row.className = "mod" + (mod.enabled ? "" : " disabled");
      row.innerHTML = `
        <label class="toggle" title="${mod.enabled ? "Disable" : "Enable"}"><input type="checkbox" ${mod.enabled ? "checked" : ""}><span></span></label>
        <div class="info"><span>${escapeHtml(mod.name)} <small>${escapeHtml(mod.version)}</small></span><small>${escapeHtml(mod.description || mod.file)}</small></div>
        <button class="btn small remove" title="Remove">✕</button>`;
      row.querySelector("input").addEventListener("change", async (e) => {
        await call("toggle_mod", { id: editing.id, file: mod.file, enabled: e.target.checked });
        loadMods();
      });
      row.querySelector(".remove").addEventListener("click", async () => {
        await call("remove_mod", { id: editing.id, file: mod.file });
        loadMods();
      });
      box.appendChild(row);
    });
  }

  async function addMods(files) {
    const jars = files.filter((f) => f.toLowerCase().endsWith(".jar"));
    if (!jars.length || !editing) return;
    const added = await call("add_mods", { id: editing.id, files: jars });
    toast(added === 1 ? "Mod added" : added + " mods added");
    loadMods();
  }

  $("add-mods").addEventListener("click", async () => {
    const picked = await tauri.dialog.open({ multiple: true, title: "Add mods", filters: [{ name: "Fabric mods", extensions: ["jar"] }] });
    if (picked) addMods(Array.isArray(picked) ? picked : [picked]);
  });

  // Arrastrar y soltar mods en la ventana (solo en la página Perfiles).
  tauri.webview.getCurrentWebview().onDragDropEvent((event) => {
    const onProfiles = $("page-profiles").classList.contains("active");
    const type = event.payload.type;
    $("mods").classList.toggle("drop", onProfiles && (type === "enter" || type === "over"));
    if (type === "drop" && onProfiles) addMods(event.payload.paths || []);
  });

  // ---------- Configuración ----------
  function renderChips(id, value, onPick) {
    const box = $(id);
    box.innerHTML = "";
    box.dataset.options.split("|").forEach((option) => {
      const [key, label] = option.split(":");
      const b = document.createElement("button");
      b.textContent = label;
      b.className = String(value) === key ? "selected" : "";
      b.addEventListener("click", () => onPick(key));
      box.appendChild(b);
    });
  }

  function renderSettings() {
    const s = state.settings;
    const auto = s.memory_mb === 0;
    $("s-memory-auto").checked = auto;
    $("s-memory").disabled = auto;
    $("s-memory").max = Math.max(4096, Math.floor((state.total_memory_mb || 8192) * 0.75 / 256) * 256);
    $("s-memory").value = auto ? state.auto_memory_mb : s.memory_mb;
    $("s-memory-value").textContent = mb(auto ? state.auto_memory_mb : s.memory_mb);
    $("memory-hint").textContent = state.total_memory_mb
      ? `Your PC has ${mb(Math.round(state.total_memory_mb / 1024) * 1024)}. Auto uses ${mb(state.auto_memory_mb)}; more is not faster.`
      : "Auto picks the best amount for your PC.";
    renderChips("s-launch-with", s.launch_with, (v) => update({ launch_with: v }));
    $("launch-with-hint").textContent =
      s.launch_with === "official"
        ? "Launch opens the official launcher with FreedomClient ready: just press Play. It uses its Microsoft login."
        : "Starts the game from here with your own Microsoft login (needs the Azure app ID).";
    renderChips("s-gc", s.gc, (v) => update({ gc: v }));
    renderChips("s-after", s.after_launch, (v) => update({ after_launch: v }));
    renderChips("s-downloads", s.concurrent_downloads, (v) => update({ concurrent_downloads: Number(v) }));
    $("s-width").value = s.width;
    $("s-height").value = s.height;
    $("s-fullscreen").checked = s.fullscreen;
    $("s-jvm").value = s.extra_jvm_args;
    $("s-java").value = s.java_path;
    $("s-autoupdate").checked = s.auto_update_client;
    $("s-discord").checked = s.discord_rpc;
    $("s-discord-id").value = s.discord_app_id;
    $("s-ms-id").value = s.microsoft_client_id;
    $("data-dir").textContent = state.data_dir;
  }

  let settingsTimer = null;
  function update(changes, rerender = true) {
    Object.assign(state.settings, changes);
    if (rerender) render();
    clearTimeout(settingsTimer);
    settingsTimer = setTimeout(() => call("save_settings", { settings: state.settings }), 300);
  }

  $("s-memory-auto").addEventListener("change", (e) => update({ memory_mb: e.target.checked ? 0 : state.auto_memory_mb }));
  $("s-memory").addEventListener("input", (e) => {
    $("s-memory-value").textContent = mb(Number(e.target.value));
    update({ memory_mb: Number(e.target.value) }, false);
  });
  $("s-memory").addEventListener("change", () => render());
  $("s-width").addEventListener("change", (e) => update({ width: Math.max(640, Number(e.target.value) || 1280) }));
  $("s-height").addEventListener("change", (e) => update({ height: Math.max(480, Number(e.target.value) || 720) }));
  $("s-fullscreen").addEventListener("change", (e) => update({ fullscreen: e.target.checked }));
  $("s-jvm").addEventListener("input", (e) => update({ extra_jvm_args: e.target.value }, false));
  $("s-java").addEventListener("input", (e) => update({ java_path: e.target.value.trim() }, false));
  $("s-autoupdate").addEventListener("change", (e) => update({ auto_update_client: e.target.checked }));
  $("s-discord").addEventListener("change", (e) => update({ discord_rpc: e.target.checked }));
  $("s-discord-id").addEventListener("input", (e) => update({ discord_app_id: e.target.value.trim() }, false));
  $("s-ms-id").addEventListener("input", (e) => update({ microsoft_client_id: e.target.value.trim() }, false));

  // ---------- Cuentas ----------
  function openAccounts() {
    renderAccounts();
    $("accounts").classList.remove("hidden");
  }

  function renderAccounts() {
    const list = $("account-list");
    list.innerHTML = state.accounts.length ? "" : '<p class="hint">No accounts yet.</p>';
    state.accounts.forEach((a) => {
      const row = document.createElement("div");
      row.className = "account-row" + (selectedAccount() && selectedAccount().uuid === a.uuid ? " selected" : "");
      row.innerHTML = `<img src="${head(a)}" alt=""><span>${escapeHtml(a.name)}</span><small>${a.kind === "microsoft" ? "Microsoft" : "Offline"}</small><button class="btn small" title="Remove">✕</button>`;
      row.addEventListener("click", async () => {
        state.settings.selected_account = a.uuid;
        render();
        renderAccounts();
        await call("select_account", { uuid: a.uuid });
      });
      row.querySelector("button").addEventListener("click", async (e) => {
        e.stopPropagation();
        await call("remove_account", { uuid: a.uuid });
        await reload();
        renderAccounts();
      });
      list.appendChild(row);
    });
    $("offline-row").classList.toggle("hidden", !state.owns_game);
    $("offline-hint").classList.toggle("hidden", state.owns_game);
  }

  $("account-button").addEventListener("click", openAccounts);
  document.querySelectorAll("[data-close]").forEach((b) =>
    b.addEventListener("click", () => {
      b.closest(".overlay").classList.add("hidden");
      loginCode = null;
    })
  );

  $("add-offline").addEventListener("click", async () => {
    await call("add_offline", { name: $("offline-name").value });
    $("offline-name").value = "";
    await reload();
    renderAccounts();
  });

  let loginCode = null;
  $("add-microsoft").addEventListener("click", async () => {
    const code = await call("login_start");
    loginCode = code;
    $("login-code").textContent = code.user_code;
    $("login-status").textContent = "Waiting for you to log in…";
    $("login-open").onclick = () => call("open_url", { url: code.verification_uri });
    $("login-copy").onclick = () => navigator.clipboard.writeText(code.user_code).then(() => toast("Code copied"));
    $("login").classList.remove("hidden");
    const expires = Date.now() + code.expires_in * 1000;
    while (loginCode === code && Date.now() < expires) {
      await new Promise((r) => setTimeout(r, Math.max(2, code.interval) * 1000));
      if (loginCode !== code) return;
      try {
        const account = await invoke("login_poll", { deviceCode: code.device_code });
        if (account) {
          loginCode = null;
          $("login").classList.add("hidden");
          toast("Logged in as " + account.name);
          await reload();
          renderAccounts();
          return;
        }
      } catch (e) {
        $("login-status").textContent = String(e);
        loginCode = null;
        return;
      }
    }
  });

  // ---------- inicio ----------
  reload().then(() => {
    if (!officialMode() && !state.accounts.length) openAccounts();
  });
})();
