//! Jugar con el launcher oficial de Minecraft: se le instala Fabric y un perfil "FreedomClient" que usa la
//! carpeta de nuestro perfil (con FreedomClient, los mods externos y las opciones optimizadas) y la RAM y los
//! flags de Java de Configuración. El inicio de sesión lo hace el launcher oficial.

use anyhow::{Context, Result, bail};
use base64::Engine;
use serde_json::{Map, Value, json};
use std::path::{Path, PathBuf};
use std::process::Command;

use crate::launch::{jvm_tuning, split_args};
use crate::paths::Paths;
use crate::settings::{Profile, Settings, now};
use crate::{Progress, client, fabric, minecraft, mods, options};

/// Versión de datos de 1.21.11 (la que va en options.txt) por si todavía no hay .jar del juego para leerla.
const DATA_VERSION: i64 = 4671;
const ICON: &[u8] = include_bytes!("../../app/icons/128x128.png");

/// Carpeta .minecraft del launcher oficial (FC_OFFICIAL_MINECRAFT la sustituye en los tests).
pub fn minecraft_dir() -> PathBuf {
    if let Some(dir) = std::env::var_os("FC_OFFICIAL_MINECRAFT") {
        return PathBuf::from(dir);
    }
    if cfg!(windows) {
        dirs::config_dir().unwrap_or_default().join(".minecraft")
    } else if cfg!(target_os = "macos") {
        dirs::data_dir().unwrap_or_default().join("minecraft")
    } else {
        dirs::home_dir().unwrap_or_default().join(".minecraft")
    }
}

/// Clave del perfil en launcher_profiles.json.
fn profile_key(profile: &Profile) -> String {
    format!("freedomclient-{}", profile.id)
}

/// Deja todo listo en el launcher oficial y marca el perfil como el último usado para que salga elegido.
pub async fn install(
    http: &reqwest::Client,
    paths: &Paths,
    settings: &Settings,
    profile: &Profile,
    progress: &Progress,
) -> Result<()> {
    let minecraft = minecraft_dir();
    std::fs::create_dir_all(&minecraft)?;

    progress("Checking Fabric", 0, 0);
    let loader = fabric::latest_loader(http, paths, crate::MINECRAFT_VERSION).await;
    let version_id = format!("fabric-loader-{loader}-{}", crate::MINECRAFT_VERSION);
    fabric::profile(http, paths, crate::MINECRAFT_VERSION, &loader).await?;
    // El JSON de Fabric va a versions/ del launcher oficial, igual que hace el instalador de Fabric.
    let source = paths.versions().join(&version_id).join(format!("{version_id}.json"));
    let target_dir = minecraft.join("versions").join(&version_id);
    std::fs::create_dir_all(&target_dir)?;
    std::fs::copy(&source, target_dir.join(format!("{version_id}.json"))).context("installing Fabric")?;
    let dummy_jar = target_dir.join(format!("{version_id}.jar"));
    if !dummy_jar.exists() {
        std::fs::write(&dummy_jar, [])?;
    }

    progress("Updating FreedomClient", 0, 0);
    let (jar, _) = client::ensure_latest(http, paths, settings.auto_update_client).await?;
    let game_dir = paths.instance(&profile.id);
    mods::install_client(&game_dir, &jar)?;
    let data_version = minecraft::data_version(&paths.versions().join("1.21.11").join("1.21.11.jar")).unwrap_or(DATA_VERSION);
    options::write_defaults(&game_dir, Some(data_version))?;

    progress("Setting up the official launcher", 0, 0);
    write_profile(&minecraft, settings, profile, &version_id, &game_dir)
}

fn write_profile(minecraft: &Path, settings: &Settings, profile: &Profile, version_id: &str, game_dir: &Path) -> Result<()> {
    let path = minecraft.join("launcher_profiles.json");
    let mut root: Value = match std::fs::read(&path) {
        Ok(bytes) => serde_json::from_slice(&bytes).context("launcher_profiles.json is not valid")?,
        Err(_) => json!({"profiles": {}, "settings": {}, "version": 3}),
    };
    // Copia de seguridad la primera vez que se toca.
    let backup = minecraft.join("launcher_profiles.json.freedomclient-backup");
    if path.exists() && !backup.exists() {
        std::fs::copy(&path, &backup)?;
    }
    if !root.is_object() {
        bail!("launcher_profiles.json is not valid");
    }
    let profiles = root
        .as_object_mut()
        .unwrap()
        .entry("profiles")
        .or_insert_with(|| Value::Object(Map::new()));
    if !profiles.is_object() {
        *profiles = Value::Object(Map::new());
    }

    let mut java_args = jvm_tuning(crate::memory_for(settings, profile), settings.gc);
    if settings.discord_rpc && !settings.discord_app_id.trim().is_empty() {
        java_args.push(format!("-Dfreedomclient.discordAppId={}", settings.discord_app_id.trim()));
    }
    // El launcher oficial no deja pasar --quickPlayMultiplayer: el mod se conecta él solo al llegar al menú.
    if !profile.server.trim().is_empty() {
        java_args.push(format!("-Dfreedomclient.autoJoin={}", profile.server.trim()));
    }
    java_args.extend(split_args(&settings.extra_jvm_args));
    java_args.extend(split_args(&profile.extra_jvm_args));
    let quoted: Vec<String> = java_args.iter().map(|a| if a.contains(' ') { format!("\"{a}\"") } else { a.clone() }).collect();

    let key = profile_key(profile);
    let existing = profiles.get(&key).cloned().unwrap_or_else(|| json!({}));
    let created = existing.get("created").cloned().unwrap_or_else(|| Value::String(iso_time(now())));
    let name = if profile.name == "FreedomClient" { "FreedomClient".to_string() } else { format!("FreedomClient · {}", profile.name) };
    let mut entry = json!({
        "name": name,
        "type": "custom",
        "created": created,
        "lastUsed": iso_time(now()),
        "lastVersionId": version_id,
        "gameDir": game_dir.to_string_lossy(),
        "javaArgs": quoted.join(" "),
        "icon": format!("data:image/png;base64,{}", base64::engine::general_purpose::STANDARD.encode(ICON)),
    });
    if !settings.fullscreen && settings.width > 0 && settings.height > 0 {
        entry["resolution"] = json!({"width": settings.width, "height": settings.height});
    }
    if !settings.java_path.trim().is_empty() {
        entry["javaDir"] = Value::String(settings.java_path.trim().to_string());
    }
    profiles[key] = entry;

    let tmp = path.with_file_name("launcher_profiles.json.tmp");
    std::fs::write(&tmp, serde_json::to_vec_pretty(&root)?)?;
    std::fs::rename(tmp, &path)?;
    Ok(())
}

/// Abre el launcher oficial. Devuelve false si no se encuentra.
pub fn open_launcher() -> bool {
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        let mut candidates = Vec::new();
        for var in ["ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA"] {
            if let Some(base) = std::env::var_os(var) {
                let base = PathBuf::from(base);
                candidates.push(base.join("Minecraft Launcher").join("MinecraftLauncher.exe"));
                candidates.push(base.join("Programs").join("Minecraft Launcher").join("MinecraftLauncher.exe"));
            }
        }
        for exe in candidates {
            if exe.is_file() && Command::new(&exe).spawn().is_ok() {
                return true;
            }
        }
        // Launcher de la Microsoft Store / app de Xbox.
        store_installed()
            && Command::new("explorer.exe")
                .arg(r"shell:AppsFolder\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft")
                .creation_flags(CREATE_NO_WINDOW)
                .spawn()
                .is_ok()
    }
    #[cfg(target_os = "macos")]
    {
        Command::new("open").args(["-a", "Minecraft"]).status().map(|s| s.success()).unwrap_or(false)
    }
    #[cfg(all(unix, not(target_os = "macos")))]
    {
        Command::new("minecraft-launcher").spawn().is_ok()
    }
}

/// Si el launcher de la Store está instalado (su carpeta de paquete existe).
#[cfg(windows)]
fn store_installed() -> bool {
    dirs::data_local_dir()
        .map(|d| d.join("Packages").join("Microsoft.4297127D64EC6_8wekyb3d8bbwe").exists())
        .unwrap_or(false)
}

/// Fecha en formato ISO 8601 (UTC), como la escribe el launcher oficial.
fn iso_time(secs: u64) -> String {
    let days = (secs / 86_400) as i64;
    let rem = secs % 86_400;
    // Algoritmo de días civiles (Howard Hinnant).
    let z = days + 719_468;
    let era = z.div_euclid(146_097);
    let doe = z.rem_euclid(146_097);
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let day = doy - (153 * mp + 2) / 5 + 1;
    let month = if mp < 10 { mp + 3 } else { mp - 9 };
    let year = yoe + era * 400 + if month <= 2 { 1 } else { 0 };
    format!("{year:04}-{month:02}-{day:02}T{:02}:{:02}:{:02}.000Z", rem / 3600, rem % 3600 / 60, rem % 60)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn iso_dates() {
        assert_eq!(iso_time(0), "1970-01-01T00:00:00.000Z");
        assert_eq!(iso_time(1_790_380_800), "2026-09-26T00:00:00.000Z");
    }

    #[test]
    fn writes_profile_and_keeps_others() {
        let dir = std::env::temp_dir().join(format!("fc-official-test-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        std::fs::write(
            dir.join("launcher_profiles.json"),
            r#"{"profiles":{"abc":{"name":"Other","type":"custom","lastVersionId":"1.20.1"}},"settings":{"x":1},"version":3}"#,
        )
        .unwrap();
        let settings = Settings { memory_mb: 3072, ..Default::default() };
        let profile = Profile { id: "default".into(), ..Default::default() };
        write_profile(&dir, &settings, &profile, "fabric-loader-0.19.3-1.21.11", Path::new("/games/fc")).unwrap();
        let root: Value = serde_json::from_slice(&std::fs::read(dir.join("launcher_profiles.json")).unwrap()).unwrap();
        assert_eq!(root["profiles"]["abc"]["name"], "Other");
        assert_eq!(root["settings"]["x"], 1);
        let fc = &root["profiles"]["freedomclient-default"];
        assert_eq!(fc["lastVersionId"], "fabric-loader-0.19.3-1.21.11");
        assert!(fc["javaArgs"].as_str().unwrap().contains("-Xmx3072M"));
        assert!(fc["icon"].as_str().unwrap().starts_with("data:image/png;base64,"));
        assert!(dir.join("launcher_profiles.json.freedomclient-backup").exists());
        let _ = std::fs::remove_dir_all(dir);
    }
}
