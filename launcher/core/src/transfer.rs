//! Exportar e importar perfiles en un archivo .fcprofile (un zip) para llevar la configuración a otro PC:
//! opciones y teclas de Minecraft, la configuración de todos los mods, la lista de servidores y, si se quiere,
//! los mods externos, resource packs y shaders. Nunca incluye mundos, logs ni capturas.

use anyhow::{Context, Result, bail};
use serde::{Deserialize, Serialize};
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use zip::write::SimpleFileOptions;

use crate::client::JAR_NAME;
use crate::paths::Paths;
use crate::settings::{Profile, new_profile_id, now};

pub const EXTENSION: &str = "fcprofile";
const FORMAT: u32 = 1;

#[derive(Clone, Copy, Debug, Default, Deserialize)]
pub struct ExportOptions {
    #[serde(default)]
    pub mods: bool,
    #[serde(default)]
    pub resource_packs: bool,
    #[serde(default)]
    pub shaders: bool,
}

#[derive(Serialize, Deserialize)]
struct Manifest {
    format: u32,
    minecraft: String,
    profile: Profile,
}

#[derive(Debug, Default, Serialize)]
pub struct Summary {
    pub files: usize,
    pub bytes: u64,
}

/// Archivos sueltos de la carpeta del juego que siempre van.
const FILES: &[&str] = &["options.txt", "servers.dat", "optionsshaders.txt"];
/// Archivos de config que no tienen sentido en otro PC.
const SKIP_CONFIG: &[&str] = &["freedomclient-state.json"];

pub fn export(paths: &Paths, profile: &Profile, target: &Path, options: ExportOptions) -> Result<Summary> {
    let game = paths.instance(&profile.id);
    let tmp = target.with_extension("fcprofile.tmp");
    let file = std::fs::File::create(&tmp).with_context(|| format!("creating {}", target.display()))?;
    let mut zip = zip::ZipWriter::new(file);
    let deflated = SimpleFileOptions::default().compression_method(zip::CompressionMethod::Deflated);
    let mut summary = Summary::default();

    let mut exported = profile.clone();
    exported.id = String::new();
    exported.last_played = 0;
    let manifest = Manifest { format: FORMAT, minecraft: crate::MINECRAFT_VERSION.into(), profile: exported };
    zip.start_file("profile.json", deflated)?;
    zip.write_all(&serde_json::to_vec_pretty(&manifest)?)?;

    let mut add = |zip: &mut zip::ZipWriter<std::fs::File>, path: &Path, name: String| -> Result<()> {
        let bytes = std::fs::read(path)?;
        // Los .jar y los zip ya van comprimidos.
        let stored = name.ends_with(".jar") || name.ends_with(".zip") || name.ends_with(".disabled");
        let method = if stored { SimpleFileOptions::default().compression_method(zip::CompressionMethod::Stored) } else { deflated };
        zip.start_file(name, method)?;
        zip.write_all(&bytes)?;
        summary.files += 1;
        summary.bytes += bytes.len() as u64;
        Ok(())
    };

    for name in FILES {
        let path = game.join(name);
        if path.is_file() {
            add(&mut zip, &path, format!("game/{name}"))?;
        }
    }
    for file in walk(&game.join("config")) {
        let relative = relative(&game, &file);
        if SKIP_CONFIG.iter().any(|skip| relative.ends_with(skip)) {
            continue;
        }
        add(&mut zip, &file, format!("game/{relative}"))?;
    }
    let mut folders = Vec::new();
    if options.resource_packs {
        folders.push("resourcepacks");
    }
    if options.shaders {
        folders.push("shaderpacks");
    }
    for folder in folders {
        for file in walk(&game.join(folder)) {
            add(&mut zip, &file, format!("game/{}", relative(&game, &file)))?;
        }
    }
    if options.mods {
        for file in walk(&game.join("mods")) {
            let name = file.file_name().unwrap_or_default().to_string_lossy().into_owned();
            // FreedomClient no hace falta: el launcher pone siempre la última versión.
            if name == JAR_NAME || file.parent() != Some(game.join("mods").as_path()) {
                continue;
            }
            add(&mut zip, &file, format!("game/mods/{name}"))?;
        }
    }
    zip.finish()?;
    std::fs::rename(&tmp, target)?;
    Ok(summary)
}

/// Importa un .fcprofile como un perfil nuevo. Devuelve el perfil (sin guardar en la lista).
pub fn import(paths: &Paths, source: &Path, existing_names: &[String]) -> Result<Profile> {
    let file = std::fs::File::open(source).with_context(|| format!("opening {}", source.display()))?;
    let mut archive = zip::ZipArchive::new(file).context("this is not a FreedomClient profile file")?;
    let manifest: Manifest = {
        let mut entry = archive.by_name("profile.json").context("this is not a FreedomClient profile file")?;
        let mut text = String::new();
        entry.read_to_string(&mut text)?;
        serde_json::from_str(&text).context("the profile file is damaged")?
    };
    if manifest.format > FORMAT {
        bail!("This profile was made with a newer launcher. Update the launcher first.");
    }

    let mut profile = manifest.profile;
    profile.id = new_profile_id();
    profile.created = now();
    profile.last_played = 0;
    let mut name = profile.name.trim().to_string();
    if name.is_empty() {
        name = "Imported".into();
    }
    let base = name.clone();
    let mut n = 2;
    while existing_names.iter().any(|existing| existing.eq_ignore_ascii_case(&name)) {
        name = format!("{base} ({n})");
        n += 1;
    }
    profile.name = name;

    let game = paths.instance(&profile.id);
    std::fs::create_dir_all(game.join("mods"))?;
    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;
        // enclosed_name descarta rutas peligrosas ("../", absolutas).
        let Some(path) = entry.enclosed_name() else { continue };
        let Ok(relative) = path.strip_prefix("game") else { continue };
        if entry.is_dir() || relative.as_os_str().is_empty() {
            continue;
        }
        let target = game.join(relative);
        if let Some(parent) = target.parent() {
            std::fs::create_dir_all(parent)?;
        }
        let mut out = std::fs::File::create(&target)?;
        std::io::copy(&mut entry, &mut out)?;
    }
    Ok(profile)
}

fn walk(dir: &Path) -> Vec<PathBuf> {
    let mut files = Vec::new();
    let Ok(entries) = std::fs::read_dir(dir) else { return files };
    for entry in entries.flatten() {
        let path = entry.path();
        if path.is_dir() {
            files.extend(walk(&path));
        } else if path.is_file() {
            files.push(path);
        }
    }
    files.sort();
    files
}

fn relative(base: &Path, path: &Path) -> String {
    crate::paths::url_path(path.strip_prefix(base).unwrap_or(path))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trip() {
        let root = std::env::temp_dir().join(format!("fc-transfer-test-{}", std::process::id()));
        let paths = Paths::new(&root);
        let profile = Profile { id: "source".into(), name: "PvP".into(), server: "play.example.net".into(), ..Default::default() };
        let game = paths.instance("source");
        std::fs::create_dir_all(game.join("config/sodium")).unwrap();
        std::fs::create_dir_all(game.join("mods")).unwrap();
        std::fs::create_dir_all(game.join("saves/World")).unwrap();
        std::fs::write(game.join("options.txt"), "key_key.jump:key.keyboard.space\n").unwrap();
        std::fs::write(game.join("servers.dat"), [1, 2, 3]).unwrap();
        std::fs::write(game.join("config/freedomclient.json"), "{\"modules\":{}}").unwrap();
        std::fs::write(game.join("config/freedomclient-state.json"), "{}").unwrap();
        std::fs::write(game.join("config/sodium/options.json"), "{}").unwrap();
        std::fs::write(game.join("mods/extra.jar"), [0u8; 10]).unwrap();
        std::fs::write(game.join("mods").join(JAR_NAME), [0u8; 10]).unwrap();
        std::fs::write(game.join("saves/World/level.dat"), [0u8; 10]).unwrap();

        let file = root.join("pvp.fcprofile");
        export(&paths, &profile, &file, ExportOptions { mods: true, ..Default::default() }).unwrap();
        let imported = import(&paths, &file, &["PvP".to_string()]).unwrap();
        assert_eq!(imported.name, "PvP (2)");
        assert_eq!(imported.server, "play.example.net");
        let target = paths.instance(&imported.id);
        assert!(target.join("options.txt").is_file());
        assert!(target.join("servers.dat").is_file());
        assert!(target.join("config/freedomclient.json").is_file());
        assert!(target.join("config/sodium/options.json").is_file());
        assert!(target.join("mods/extra.jar").is_file());
        assert!(!target.join("config/freedomclient-state.json").exists());
        assert!(!target.join("mods").join(JAR_NAME).exists());
        assert!(!target.join("saves").exists());
        let _ = std::fs::remove_dir_all(root);
    }
}
