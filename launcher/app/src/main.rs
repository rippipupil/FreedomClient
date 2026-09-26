// En Windows no se abre una consola junto al launcher.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use fc_core::discord::{Activity, Presence};
use fc_core::launch::{LaunchOptions, build_command, log_file, spawn};
use fc_core::paths::Paths;
use fc_core::settings::{self, Account, Accounts, AfterLaunch, LaunchWith, Profile, Settings, now};
use fc_core::{auth, client, mods, official};
use serde::Serialize;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter, Manager, State};
use tauri_plugin_opener::OpenerExt;

struct AppState {
    paths: Paths,
    http: fc_core::reqwest::Client,
    settings: Mutex<Settings>,
    profiles: Mutex<Vec<Profile>>,
    accounts: Mutex<Accounts>,
    presence: Mutex<Option<Presence>>,
    /// Perfil que se está jugando (o preparando) ahora mismo.
    running: Mutex<Option<String>>,
    started: u64,
}

type CmdResult<T> = Result<T, String>;

fn err(e: impl std::fmt::Display) -> String {
    e.to_string()
}

#[derive(Serialize)]
struct AccountView {
    uuid: String,
    name: String,
    kind: &'static str,
}

impl From<&Account> for AccountView {
    fn from(account: &Account) -> Self {
        Self {
            uuid: account.uuid().to_string(),
            name: account.name().to_string(),
            kind: match account {
                Account::Microsoft { .. } => "microsoft",
                Account::Offline { .. } => "offline",
            },
        }
    }
}

#[derive(Serialize)]
struct StateView {
    settings: Settings,
    profiles: Vec<Profile>,
    accounts: Vec<AccountView>,
    owns_game: bool,
    total_memory_mb: u64,
    auto_memory_mb: u32,
    client_commit: Option<String>,
    launcher_version: &'static str,
    minecraft_version: &'static str,
    running: Option<String>,
    data_dir: String,
}

#[derive(Clone, Serialize)]
struct ProgressEvent {
    stage: String,
    done: u64,
    total: u64,
}

impl AppState {
    fn save_settings(&self) -> CmdResult<()> {
        settings::save(&settings::settings_path(&self.paths), &*self.settings.lock().unwrap()).map_err(err)
    }
    fn save_profiles(&self) -> CmdResult<()> {
        settings::save(&settings::profiles_path(&self.paths), &*self.profiles.lock().unwrap()).map_err(err)
    }
    fn save_accounts(&self) -> CmdResult<()> {
        settings::save(&settings::accounts_path(&self.paths), &*self.accounts.lock().unwrap()).map_err(err)
    }
    fn profile(&self, id: &str) -> CmdResult<Profile> {
        self.profiles.lock().unwrap().iter().find(|p| p.id == id).cloned().ok_or_else(|| "Profile not found".to_string())
    }
    fn game_dir(&self, id: &str) -> CmdResult<PathBuf> {
        self.profile(id).map(|p| self.paths.instance(&p.id))
    }

    /// Estado de Discord: se reinicia si cambia el ID o se desactiva.
    fn restart_presence(&self) {
        let settings = self.settings.lock().unwrap().clone();
        let mut presence = self.presence.lock().unwrap();
        *presence = if settings.discord_rpc { Presence::start(settings.discord_app_id.clone()) } else { None };
        drop(presence);
        self.set_presence("In the launcher", "");
    }

    fn set_presence(&self, details: &str, state: &str) {
        if let Some(presence) = self.presence.lock().unwrap().as_ref() {
            presence.set(Some(Activity { details: details.into(), state: state.into(), start: self.started }));
        }
    }
}

#[tauri::command]
fn load_state(state: State<'_, AppState>) -> StateView {
    let total = fc_core::system::total_memory_mb();
    let accounts = state.accounts.lock().unwrap();
    StateView {
        settings: state.settings.lock().unwrap().clone(),
        profiles: state.profiles.lock().unwrap().clone(),
        accounts: accounts.accounts.iter().map(AccountView::from).collect(),
        owns_game: accounts.owns_game,
        total_memory_mb: total,
        auto_memory_mb: settings::auto_memory_mb(total),
        client_commit: client::installed_commit(&state.paths),
        launcher_version: env!("CARGO_PKG_VERSION"),
        minecraft_version: fc_core::MINECRAFT_VERSION,
        running: state.running.lock().unwrap().clone(),
        data_dir: state.paths.root.to_string_lossy().into_owned(),
    }
}

#[tauri::command]
fn save_settings(state: State<'_, AppState>, settings: Settings) -> CmdResult<()> {
    let restart = {
        let mut current = state.settings.lock().unwrap();
        let restart = current.discord_rpc != settings.discord_rpc || current.discord_app_id != settings.discord_app_id;
        *current = settings;
        restart
    };
    state.save_settings()?;
    if restart {
        state.restart_presence();
    }
    Ok(())
}

#[tauri::command]
fn select_profile(state: State<'_, AppState>, id: String) -> CmdResult<()> {
    state.settings.lock().unwrap().selected_profile = id;
    state.save_settings()
}

#[tauri::command]
fn save_profile(state: State<'_, AppState>, mut profile: Profile) -> CmdResult<Profile> {
    profile.name = profile.name.trim().chars().take(32).collect();
    if profile.name.is_empty() {
        return Err("The profile needs a name".into());
    }
    {
        let mut profiles = state.profiles.lock().unwrap();
        if profile.id.is_empty() {
            profile.id = settings::new_profile_id();
            profile.created = now();
            profiles.push(profile.clone());
        } else if let Some(existing) = profiles.iter_mut().find(|p| p.id == profile.id) {
            profile.created = existing.created;
            profile.last_played = existing.last_played;
            *existing = profile.clone();
        } else {
            return Err("Profile not found".into());
        }
    }
    std::fs::create_dir_all(mods::mods_dir(&state.paths.instance(&profile.id))).map_err(err)?;
    state.save_profiles()?;
    Ok(profile)
}

#[tauri::command]
fn delete_profile(state: State<'_, AppState>, id: String, delete_files: bool) -> CmdResult<()> {
    if state.running.lock().unwrap().as_deref() == Some(id.as_str()) {
        return Err("This profile is running".into());
    }
    {
        let mut profiles = state.profiles.lock().unwrap();
        if profiles.len() <= 1 {
            return Err("You need at least one profile".into());
        }
        profiles.retain(|p| p.id != id);
    }
    state.save_profiles()?;
    if delete_files {
        let dir = state.paths.instance(&id);
        if dir.starts_with(state.paths.instances()) && dir != state.paths.instances() {
            let _ = std::fs::remove_dir_all(dir);
        }
    }
    Ok(())
}

#[tauri::command]
fn list_mods(state: State<'_, AppState>, id: String) -> CmdResult<Vec<mods::ModInfo>> {
    Ok(mods::list(&state.game_dir(&id)?))
}

#[tauri::command]
fn add_mods(state: State<'_, AppState>, id: String, files: Vec<String>) -> CmdResult<usize> {
    let files: Vec<PathBuf> = files.into_iter().map(PathBuf::from).collect();
    mods::add(&state.game_dir(&id)?, &files).map_err(|e| format!("{e:#}"))
}

#[tauri::command]
fn toggle_mod(state: State<'_, AppState>, id: String, file: String, enabled: bool) -> CmdResult<()> {
    mods::set_enabled(&state.game_dir(&id)?, &file, enabled).map_err(err)
}

#[tauri::command]
fn remove_mod(state: State<'_, AppState>, id: String, file: String) -> CmdResult<()> {
    mods::remove(&state.game_dir(&id)?, &file).map_err(err)
}

/// Abre carpetas o el log con la aplicación del sistema.
#[tauri::command]
fn open_path(app: AppHandle, state: State<'_, AppState>, id: String, target: String) -> CmdResult<()> {
    let path = match target.as_str() {
        "launcher" => state.paths.root.clone(),
        other => {
            let dir = state.game_dir(&id)?;
            match other {
                "mods" => mods::mods_dir(&dir),
                "screenshots" => dir.join("screenshots"),
                "resourcepacks" => dir.join("resourcepacks"),
                "log" => {
                    let log = log_file(&dir);
                    if !log.exists() {
                        return Err("There is no log yet: launch the game first".into());
                    }
                    log
                }
                _ => dir,
            }
        }
    };
    if !path.exists() {
        std::fs::create_dir_all(&path).map_err(err)?;
    }
    app.opener().open_path(path.to_string_lossy(), None::<&str>).map_err(err)
}

#[tauri::command]
fn open_url(app: AppHandle, url: String) -> CmdResult<()> {
    if !url.starts_with("https://") {
        return Err("Invalid link".into());
    }
    app.opener().open_url(url, None::<&str>).map_err(err)
}

#[tauri::command]
async fn login_start(state: State<'_, AppState>) -> CmdResult<auth::DeviceCode> {
    let client_id = state.settings.lock().unwrap().microsoft_client_id.clone();
    auth::start_device_code(&state.http, &client_id).await.map_err(|e| format!("{e:#}"))
}

#[tauri::command]
async fn login_poll(state: State<'_, AppState>, device_code: String) -> CmdResult<Option<AccountView>> {
    let client_id = state.settings.lock().unwrap().microsoft_client_id.clone();
    let Some(account) = auth::poll_device_code(&state.http, &client_id, &device_code).await.map_err(|e| format!("{e:#}"))? else {
        return Ok(None);
    };
    let view = AccountView::from(&account);
    {
        let mut accounts = state.accounts.lock().unwrap();
        accounts.accounts.retain(|a| a.uuid() != account.uuid());
        accounts.accounts.push(account);
        accounts.owns_game = true;
    }
    state.settings.lock().unwrap().selected_account = view.uuid.clone();
    state.save_accounts()?;
    state.save_settings()?;
    Ok(Some(view))
}

#[tauri::command]
fn add_offline(state: State<'_, AppState>, name: String) -> CmdResult<AccountView> {
    let name = name.trim().to_string();
    if !state.accounts.lock().unwrap().owns_game {
        return Err("Log in with a Microsoft account that owns Minecraft first".into());
    }
    if !(3..=16).contains(&name.len()) || !name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_') {
        return Err("Names use 3-16 letters, numbers or _".into());
    }
    let account = Account::offline(&name);
    let view = AccountView::from(&account);
    {
        let mut accounts = state.accounts.lock().unwrap();
        accounts.accounts.retain(|a| a.uuid() != account.uuid());
        accounts.accounts.push(account);
    }
    state.settings.lock().unwrap().selected_account = view.uuid.clone();
    state.save_accounts()?;
    state.save_settings()?;
    Ok(view)
}

#[tauri::command]
fn remove_account(state: State<'_, AppState>, uuid: String) -> CmdResult<()> {
    state.accounts.lock().unwrap().accounts.retain(|a| a.uuid() != uuid);
    state.save_accounts()
}

#[tauri::command]
fn select_account(state: State<'_, AppState>, uuid: String) -> CmdResult<()> {
    state.settings.lock().unwrap().selected_account = uuid;
    state.save_settings()
}

#[tauri::command]
async fn launch(app: AppHandle, state: State<'_, AppState>, id: String) -> CmdResult<()> {
    {
        let mut running = state.running.lock().unwrap();
        if running.is_some() {
            return Err("The game is already running".into());
        }
        *running = Some(id.clone());
    }
    let result = launch_inner(&app, &state, &id).await;
    if result.is_err() {
        *state.running.lock().unwrap() = None;
        state.set_presence("In the launcher", "");
    }
    result
}

fn progress_emitter(app: &AppHandle) -> fc_core::Progress {
    let emitter = app.clone();
    Arc::new(move |stage: &str, done: u64, total: u64| {
        let _ = emitter.emit("progress", ProgressEvent { stage: stage.to_string(), done, total });
    })
}

/// Cierra o minimiza el launcher según Configuración. Devuelve true si se va a cerrar.
fn after_launch(app: &AppHandle, settings: &Settings) -> bool {
    match settings.after_launch {
        AfterLaunch::Close => {
            // El juego sigue solo; el launcher se cierra para no gastar memoria mientras juegas.
            let app = app.clone();
            tauri::async_runtime::spawn(async move {
                tokio::time::sleep(std::time::Duration::from_millis(1500)).await;
                app.exit(0);
            });
            true
        }
        AfterLaunch::Minimize => {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.minimize();
            }
            false
        }
        AfterLaunch::KeepOpen => false,
    }
}

/// Launch con el launcher oficial: lo deja todo listo, abre el launcher oficial con el perfil elegido
/// y el juego arranca con su login al darle a Jugar.
async fn launch_official(app: &AppHandle, state: &AppState, settings: &Settings, profile: &Profile) -> CmdResult<()> {
    let progress = progress_emitter(app);
    official::install(&state.http, &state.paths, settings, profile, &progress).await.map_err(|e| format!("{e:#}"))?;
    progress("Opening the Minecraft Launcher", 0, 0);
    if !official::open_launcher() {
        return Err("FreedomClient is ready in the official Minecraft Launcher, but the launcher was not found. \
            Install it from minecraft.net, or choose \"FreedomClient\" in Settings → Launch with."
            .into());
    }
    if let Some(p) = state.profiles.lock().unwrap().iter_mut().find(|p| p.id == profile.id) {
        p.last_played = now();
    }
    let _ = state.save_profiles();
    // El launcher oficial se encarga del juego: aquí no hay proceso que vigilar.
    *state.running.lock().unwrap() = None;
    let _ = app.emit("launched-official", profile.id.clone());
    after_launch(app, settings);
    Ok(())
}

async fn launch_inner(app: &AppHandle, state: &AppState, id: &str) -> CmdResult<()> {
    let settings = state.settings.lock().unwrap().clone();
    let profile = state.profile(id)?;
    if settings.launch_with == LaunchWith::Official {
        return launch_official(app, state, &settings, &profile).await;
    }
    let account = {
        let accounts = state.accounts.lock().unwrap();
        accounts
            .accounts
            .iter()
            .find(|a| a.uuid() == settings.selected_account)
            .or_else(|| accounts.accounts.first())
            .cloned()
            .ok_or_else(|| "Add an account first".to_string())?
    };
    state.set_presence("Launching the game", &profile.name);

    let progress = progress_emitter(app);
    let account = match &account {
        Account::Microsoft { .. } => {
            progress("Logging in", 0, 0);
            match auth::refresh_if_needed(&state.http, &settings.microsoft_client_id, &account).await {
                Ok(Some(fresh)) => {
                    let mut accounts = state.accounts.lock().unwrap();
                    if let Some(slot) = accounts.accounts.iter_mut().find(|a| a.uuid() == fresh.uuid()) {
                        *slot = fresh.clone();
                    }
                    drop(accounts);
                    state.save_accounts()?;
                    fresh
                }
                Ok(None) => account,
                Err(e) => return Err(format!("{e:#}")),
            }
        }
        Account::Offline { .. } => account,
    };

    let prepared = fc_core::prepare(&state.http, &state.paths, &settings, &profile, &progress).await.map_err(|e| format!("{e:#}"))?;
    progress("Starting Minecraft", 0, 0);
    let extra = format!("{} {}", settings.extra_jvm_args, profile.extra_jvm_args);
    let options = LaunchOptions {
        account,
        memory_mb: fc_core::memory_for(&settings, &profile),
        gc: settings.gc,
        extra_jvm_args: extra,
        width: settings.width,
        height: settings.height,
        fullscreen: settings.fullscreen,
        server: profile.server.clone(),
        discord_app_id: if settings.discord_rpc { settings.discord_app_id.clone() } else { String::new() },
    };
    let command = build_command(&state.paths, &prepared, &options);
    let mut child = spawn(command, &prepared.game_dir).map_err(|e| format!("{e:#}"))?;

    if let Some(p) = state.profiles.lock().unwrap().iter_mut().find(|p| p.id == profile.id) {
        p.last_played = now();
    }
    let _ = state.save_profiles();
    state.set_presence("Playing Minecraft 1.21.11", &profile.name);
    let _ = app.emit("launched", profile.id.clone());

    if after_launch(app, &settings) {
        return Ok(());
    }

    let app = app.clone();
    std::thread::spawn(move || {
        let code = child.wait().ok().and_then(|s| s.code());
        let state = app.state::<AppState>();
        *state.running.lock().unwrap() = None;
        state.set_presence("In the launcher", "");
        if let Some(window) = app.get_webview_window("main") {
            let _ = window.unminimize();
            let _ = window.set_focus();
        }
        let _ = app.emit("game-exit", code);
    });
    Ok(())
}

fn main() {
    let paths = Paths::new(Paths::default_root());
    let _ = std::fs::create_dir_all(&paths.root);
    let mut settings: Settings = settings::load(&settings::settings_path(&paths));
    let profiles = settings::load_profiles(&paths);
    if !profiles.iter().any(|p| p.id == settings.selected_profile) {
        settings.selected_profile = profiles[0].id.clone();
    }
    let accounts: Accounts = settings::load(&settings::accounts_path(&paths));
    let _ = settings::save(&settings::settings_path(&paths), &settings);
    let state = AppState {
        http: fc_core::http::client(),
        settings: Mutex::new(settings),
        profiles: Mutex::new(profiles),
        accounts: Mutex::new(accounts),
        presence: Mutex::new(None),
        running: Mutex::new(None),
        started: now(),
        paths,
    };

    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .manage(state)
        .setup(|app| {
            app.state::<AppState>().restart_presence();
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            load_state,
            save_settings,
            select_profile,
            save_profile,
            delete_profile,
            list_mods,
            add_mods,
            toggle_mod,
            remove_mod,
            open_path,
            open_url,
            login_start,
            login_poll,
            add_offline,
            remove_account,
            select_account,
            launch
        ])
        .run(tauri::generate_context!())
        .expect("error while running the launcher");
}
