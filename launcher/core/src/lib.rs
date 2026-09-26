//! Lógica del launcher de FreedomClient: instalar Minecraft 1.21.11 con Fabric y FreedomClient, y lanzarlo.

pub mod auth;
pub mod client;
pub mod discord;
pub mod fabric;
pub mod http;
pub mod java;
pub mod launch;
pub mod minecraft;
pub mod mods;
pub mod official;
pub mod options;
pub mod paths;
pub mod rules;
pub mod settings;
pub mod system;

pub use reqwest;

use anyhow::Result;
use std::collections::HashSet;
use std::path::PathBuf;
use std::sync::Arc;

use crate::http::Download;
use crate::minecraft::VersionJson;
use crate::paths::{Paths, library_key};
use crate::rules::Features;
use crate::settings::{Profile, Settings};

pub const MINECRAFT_VERSION: &str = "1.21.11";
/// Java que se usa si la versión no dice cuál (1.21.x usa Java 21).
const DEFAULT_JAVA_COMPONENT: &str = "java-runtime-delta";

/// Aviso de progreso: fase, hecho y total (en bytes; total 0 = sin barra).
pub type Progress = Arc<dyn Fn(&str, u64, u64) + Send + Sync>;

/// Juego listo para lanzar.
pub struct Prepared {
    pub java: PathBuf,
    pub vanilla: VersionJson,
    pub fabric: VersionJson,
    pub classpath: Vec<PathBuf>,
    pub asset_index: String,
    pub log_argument: Option<String>,
    pub game_dir: PathBuf,
    pub natives_dir: PathBuf,
    pub client_updated: bool,
}

/// Descarga y comprueba todo lo que hace falta para el perfil: Minecraft, Fabric, Java y FreedomClient.
pub async fn prepare(
    http: &reqwest::Client,
    paths: &Paths,
    settings: &Settings,
    profile: &Profile,
    progress: &Progress,
) -> Result<Prepared> {
    progress("Checking Minecraft", 0, 0);
    let vanilla = minecraft::version_json(http, paths, MINECRAFT_VERSION).await?;
    progress("Checking Fabric", 0, 0);
    let loader = fabric::latest_loader(http, paths, MINECRAFT_VERSION).await;
    let fabric = fabric::profile(http, paths, MINECRAFT_VERSION, &loader).await?;

    // Librerías de Fabric primero: si una está en las dos, gana la versión de Fabric.
    let features = Features::default();
    let mut seen = HashSet::new();
    let mut downloads: Vec<Download> = Vec::new();
    let mut classpath = Vec::new();
    for library in fabric.libraries.iter().chain(vanilla.libraries.iter()) {
        if !seen.insert(library_key(&library.name)) {
            continue;
        }
        if let Some(download) = library.resolve(paths, &features) {
            classpath.push(download.path.clone());
            downloads.push(download);
        }
    }
    let client_jar = minecraft::client_jar(paths, &vanilla)?;
    classpath.push(client_jar.path.clone());
    downloads.push(client_jar.clone());
    let log = minecraft::log_config(paths, &vanilla);
    if let Some((_, download)) = &log {
        downloads.push(download.clone());
    }
    progress("Checking assets", 0, 0);
    downloads.extend(minecraft::asset_downloads(http, paths, &vanilla).await?);
    let concurrency = settings.concurrent_downloads.clamp(1, 64) as usize;
    http::download_all(http, downloads, concurrency, "Downloading Minecraft", progress).await?;

    let java = if !settings.java_path.trim().is_empty() {
        PathBuf::from(settings.java_path.trim())
    } else {
        progress("Checking Java", 0, 0);
        let component = vanilla.java_version.as_ref().map(|j| j.component.as_str()).unwrap_or(DEFAULT_JAVA_COMPONENT);
        java::ensure_runtime(http, paths, component, progress).await?
    };

    progress("Updating FreedomClient", 0, 0);
    let (client, client_updated) = client::ensure_latest(http, paths, settings.auto_update_client).await?;
    let game_dir = paths.instance(&profile.id);
    mods::install_client(&game_dir, &client)?;
    options::write_defaults(&game_dir, minecraft::data_version(&client_jar.path))?;
    let natives_dir = paths.versions().join(MINECRAFT_VERSION).join("natives");
    std::fs::create_dir_all(&natives_dir)?;

    Ok(Prepared {
        java,
        asset_index: vanilla.asset_index.as_ref().map(|a| a.id.clone()).unwrap_or_default(),
        vanilla,
        fabric,
        classpath,
        log_argument: log.map(|(arg, _)| arg),
        game_dir,
        natives_dir,
        client_updated,
    })
}

/// Memoria que se le da al juego: la del perfil, la de Configuración o la automática.
pub fn memory_for(settings: &Settings, profile: &Profile) -> u32 {
    if profile.memory_mb > 0 {
        profile.memory_mb
    } else if settings.memory_mb > 0 {
        settings.memory_mb
    } else {
        settings::auto_memory_mb(system::total_memory_mb())
    }
}
