//! Autoactualización del launcher: al abrirse mira la release "launcher" de GitHub y, si hay una versión
//! nueva, descarga el .exe, lo pone en el sitio del actual y se vuelve a abrir.
//!
//! En Windows se puede renombrar un .exe que está en marcha (pero no borrarlo): el actual pasa a
//! "<nombre>.old", el nuevo ocupa su nombre y el .old se borra en el siguiente arranque. Sirve igual para la
//! versión instalada que para la portable, porque las dos son el mismo ejecutable.

use anyhow::{Context, Result, bail};
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Duration;

use crate::Progress;
use crate::http::{self, Download};

const VERSION_URL: &str = "https://github.com/rippipupil/FreedomClient/releases/download/launcher/launcher-version.txt";
const EXE_URL: &str = "https://github.com/rippipupil/FreedomClient/releases/download/launcher/FreedomClient-Launcher-Portable.exe";

/// Versión publicada del launcher (el archivo launcher-version.txt de la release).
#[derive(Clone, Debug, Default, PartialEq, Eq)]
pub struct Release {
    pub commit: String,
    pub sha1: String,
    pub size: u64,
}

impl Release {
    /// Formato "clave=valor" por línea: commit, sha1 y size del .exe portable.
    pub fn parse(text: &str) -> Result<Self> {
        let mut release = Release::default();
        for line in text.lines() {
            let Some((key, value)) = line.split_once('=') else { continue };
            match key.trim() {
                "commit" => release.commit = value.trim().to_string(),
                "sha1" => release.sha1 = value.trim().to_lowercase(),
                "size" => release.size = value.trim().parse().unwrap_or(0),
                _ => {}
            }
        }
        if release.commit.len() < 7 || release.sha1.len() != 40 || release.size < 1_000_000 {
            bail!("the launcher version file is incomplete");
        }
        Ok(release)
    }
}

/// Última versión publicada. Las descargas de releases no gastan el límite de la API de GitHub.
pub async fn latest(http: &reqwest::Client) -> Result<Release> {
    let text = http
        .get(VERSION_URL)
        .timeout(Duration::from_secs(8))
        .send()
        .await?
        .error_for_status()?
        .text()
        .await?;
    Release::parse(&text)
}

/// ¿Hay que actualizar? Solo si se sabe el commit de esta compilación y el publicado es otro.
pub fn is_newer(current_commit: &str, release: &Release) -> bool {
    !current_commit.is_empty() && !release.commit.eq_ignore_ascii_case(current_commit)
}

fn sibling(exe: &Path, suffix: &str) -> PathBuf {
    let mut name = exe.file_name().unwrap_or_default().to_os_string();
    name.push(suffix);
    exe.with_file_name(name)
}

/// Descarga el .exe nuevo junto al actual (tiene que estar en la misma carpeta para poder renombrarlo)
/// y comprueba su hash.
pub async fn download(http: &reqwest::Client, release: &Release, exe: &Path, progress: &Progress) -> Result<PathBuf> {
    let target = sibling(exe, ".new");
    let _ = std::fs::remove_file(&target);
    let _ = std::fs::remove_file(http::part_path(&target));
    let item = Download::new(EXE_URL, &target, Some(release.sha1.clone()), Some(release.size));
    let done = std::sync::Arc::new(AtomicU64::new(0));
    let ticker = {
        let done = done.clone();
        let progress = progress.clone();
        let total = release.size;
        tokio::spawn(async move {
            loop {
                progress("Updating the launcher", done.load(Ordering::Relaxed).min(total), total);
                tokio::time::sleep(Duration::from_millis(150)).await;
            }
        })
    };
    let result = http::download_file(http, &item, &done).await;
    ticker.abort();
    result.context("could not download the new launcher")?;
    progress("Updating the launcher", release.size, release.size);
    Ok(target)
}

/// Pone el .exe descargado en el sitio del actual. Si algo falla deja el actual como estaba.
pub fn apply(new: &Path, exe: &Path) -> Result<()> {
    let old = sibling(exe, ".old");
    let _ = std::fs::remove_file(&old);
    std::fs::rename(exe, &old).context("could not move the current launcher")?;
    if let Err(e) = std::fs::rename(new, exe) {
        let _ = std::fs::rename(&old, exe);
        return Err(anyhow::Error::from(e).context("could not put the new launcher in place"));
    }
    Ok(())
}

/// Borra lo que quede de una actualización anterior (el .exe viejo y descargas a medias).
pub fn cleanup(exe: &Path) {
    for suffix in [".old", ".new", ".new.part"] {
        let _ = std::fs::remove_file(sibling(exe, suffix));
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_the_version_file() {
        let sha = "a".repeat(40);
        let release = Release::parse(&format!("commit=1234567abc\nsha1={}\nsize=12000000\n", sha.to_uppercase())).unwrap();
        assert_eq!(release, Release { commit: "1234567abc".into(), sha1: sha, size: 12_000_000 });
        assert!(Release::parse("commit=1234567\n").is_err());
        assert!(is_newer("abcdef1", &release));
        assert!(!is_newer("1234567ABC", &release));
        // Una compilación local (sin commit) no se actualiza nunca.
        assert!(!is_newer("", &release));
    }

    #[test]
    fn swaps_the_executable() {
        let dir = std::env::temp_dir().join(format!("fc-selfupdate-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let exe = dir.join("FreedomClient.exe");
        std::fs::write(&exe, "old").unwrap();
        let new = sibling(&exe, ".new");
        std::fs::write(&new, "new").unwrap();
        apply(&new, &exe).unwrap();
        assert_eq!(std::fs::read_to_string(&exe).unwrap(), "new");
        assert_eq!(std::fs::read_to_string(sibling(&exe, ".old")).unwrap(), "old");
        assert!(!new.exists());
        // Si falta el nuevo, el actual se queda donde estaba.
        assert!(apply(&new, &exe).is_err());
        assert_eq!(std::fs::read_to_string(&exe).unwrap(), "new");
        cleanup(&exe);
        assert!(!sibling(&exe, ".old").exists());
        let _ = std::fs::remove_dir_all(dir);
    }
}
