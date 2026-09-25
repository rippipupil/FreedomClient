use anyhow::{Context, Result, bail};
use serde::Serialize;
use std::path::{Path, PathBuf};

use crate::client::JAR_NAME;

/// Un mod externo de la carpeta mods del perfil.
#[derive(Clone, Debug, Serialize)]
pub struct ModInfo {
    pub file: String,
    pub id: String,
    pub name: String,
    pub version: String,
    pub description: String,
    pub enabled: bool,
    pub size: u64,
}

const DISABLED: &str = ".disabled";

pub fn mods_dir(game_dir: &Path) -> PathBuf {
    game_dir.join("mods")
}

/// Mods que ha puesto el jugador (FreedomClient va aparte y lo gestiona el launcher).
pub fn list(game_dir: &Path) -> Vec<ModInfo> {
    let Ok(entries) = std::fs::read_dir(mods_dir(game_dir)) else { return Vec::new() };
    let mut mods: Vec<ModInfo> = entries
        .flatten()
        .filter_map(|entry| {
            let file = entry.file_name().to_string_lossy().into_owned();
            let enabled = file.ends_with(".jar");
            if (!enabled && !file.ends_with(&format!(".jar{DISABLED}"))) || file == JAR_NAME {
                return None;
            }
            let size = entry.metadata().map(|m| m.len()).unwrap_or(0);
            let meta = read_metadata(&entry.path());
            let fallback = file.trim_end_matches(DISABLED).trim_end_matches(".jar").to_string();
            Some(ModInfo {
                name: meta.as_ref().and_then(|m| m.1.clone()).unwrap_or_else(|| fallback.clone()),
                id: meta.as_ref().map(|m| m.0.clone()).unwrap_or(fallback),
                version: meta.as_ref().and_then(|m| m.2.clone()).unwrap_or_default(),
                description: meta.and_then(|m| m.3).unwrap_or_default(),
                file,
                enabled,
                size,
            })
        })
        .collect();
    mods.sort_by_key(|m| m.name.to_lowercase());
    mods
}

/// id, nombre, versión y descripción del fabric.mod.json que va dentro del .jar.
fn read_metadata(jar: &Path) -> Option<(String, Option<String>, Option<String>, Option<String>)> {
    let file = std::fs::File::open(jar).ok()?;
    let mut archive = zip::ZipArchive::new(file).ok()?;
    let entry = archive.by_name("fabric.mod.json").ok()?;
    let json: serde_json::Value = serde_json::from_reader(entry).ok()?;
    let text = |key: &str| json.get(key).and_then(|v| v.as_str()).map(str::to_string);
    Some((text("id")?, text("name"), text("version"), text("description")))
}

/// Copia mods al perfil. Devuelve cuántos se han añadido.
pub fn add(game_dir: &Path, files: &[PathBuf]) -> Result<usize> {
    let dir = mods_dir(game_dir);
    std::fs::create_dir_all(&dir)?;
    let mut added = 0;
    for file in files {
        let name = file.file_name().context("invalid file")?.to_string_lossy().into_owned();
        if !name.to_lowercase().ends_with(".jar") {
            continue;
        }
        if name == JAR_NAME {
            continue;
        }
        std::fs::copy(file, dir.join(&name)).with_context(|| format!("copying {name}"))?;
        let _ = std::fs::remove_file(dir.join(format!("{name}{DISABLED}")));
        added += 1;
    }
    Ok(added)
}

fn checked(game_dir: &Path, file: &str) -> Result<PathBuf> {
    if file.contains(['/', '\\']) || file.contains("..") || file == JAR_NAME {
        bail!("invalid mod file");
    }
    Ok(mods_dir(game_dir).join(file))
}

/// Activa o desactiva un mod renombrándolo a .jar.disabled (Fabric no carga esos).
pub fn set_enabled(game_dir: &Path, file: &str, enabled: bool) -> Result<()> {
    let path = checked(game_dir, file)?;
    let base = file.trim_end_matches(DISABLED);
    let target = if enabled { base.to_string() } else { format!("{base}{DISABLED}") };
    if target != file {
        std::fs::rename(path, mods_dir(game_dir).join(target))?;
    }
    Ok(())
}

pub fn remove(game_dir: &Path, file: &str) -> Result<()> {
    Ok(std::fs::remove_file(checked(game_dir, file)?)?)
}

/// Pone el .jar de FreedomClient en la carpeta mods del perfil (copia solo si ha cambiado).
pub fn install_client(game_dir: &Path, cached_jar: &Path) -> Result<()> {
    let dir = mods_dir(game_dir);
    std::fs::create_dir_all(&dir)?;
    let target = dir.join(JAR_NAME);
    let same = match (std::fs::metadata(&target), std::fs::metadata(cached_jar)) {
        (Ok(a), Ok(b)) => a.len() == b.len() && crate::http::sha1_file(&target).ok() == crate::http::sha1_file(cached_jar).ok(),
        _ => false,
    };
    if !same {
        std::fs::copy(cached_jar, &target)?;
    }
    Ok(())
}
