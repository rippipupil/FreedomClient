use anyhow::{Context, Result, anyhow};
use serde::Deserialize;
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::time::Duration;

use crate::http::{self, Download};
use crate::paths::Paths;
use crate::Progress;

const RUNTIMES_URL: &str =
    "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

#[derive(Deserialize)]
struct RuntimeEntry {
    manifest: ManifestRef,
}

#[derive(Deserialize)]
struct ManifestRef {
    sha1: Option<String>,
    url: String,
}

#[derive(Deserialize)]
struct RuntimeManifest {
    files: HashMap<String, RuntimeFile>,
}

#[derive(Deserialize)]
struct RuntimeFile {
    #[serde(rename = "type")]
    kind: String,
    #[serde(default)]
    executable: bool,
    downloads: Option<RuntimeDownloads>,
    target: Option<String>,
}

#[derive(Deserialize)]
struct RuntimeDownloads {
    raw: RuntimeDownload,
}

#[derive(Deserialize)]
struct RuntimeDownload {
    sha1: Option<String>,
    size: Option<u64>,
    url: String,
}

/// Plataforma en la lista de Java de Mojang.
fn platform() -> &'static str {
    match (std::env::consts::OS, std::env::consts::ARCH) {
        ("windows", "aarch64") => "windows-arm64",
        ("windows", "x86") => "windows-x86",
        ("windows", _) => "windows-x64",
        ("macos", "aarch64") => "mac-os-arm64",
        ("macos", _) => "mac-os",
        ("linux", "x86") => "linux-i386",
        _ => "linux",
    }
}

/// Ejecutable de Java dentro de una carpeta de runtime. En Windows se usa javaw para que no salga consola.
pub fn executable_in(home: &Path) -> Option<PathBuf> {
    let names: &[&str] = if cfg!(windows) { &["bin/javaw.exe", "bin/java.exe"] } else { &["bin/java"] };
    for base in [home.to_path_buf(), home.join("jre.bundle/Contents/Home")] {
        for name in names {
            let candidate = base.join(name);
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }
    None
}

/// Descarga (si hace falta) el Java de Mojang que pide la versión y devuelve su ejecutable.
pub async fn ensure_runtime(http: &reqwest::Client, paths: &Paths, component: &str, progress: &Progress) -> Result<PathBuf> {
    let home = paths.runtimes().join(component);
    let marker = home.join(".manifest-sha1");
    let all: Result<HashMap<String, HashMap<String, Vec<RuntimeEntry>>>> =
        http::cached_json(http, RUNTIMES_URL, &paths.meta().join("java-runtimes.json"), Duration::from_secs(24 * 3600)).await;
    let all = match all {
        Ok(all) => all,
        Err(e) => return executable_in(&home).ok_or(e),
    };
    let entry = all
        .get(platform())
        .and_then(|p| p.get(component))
        .and_then(|list| list.first())
        .ok_or_else(|| anyhow!("Mojang has no Java runtime '{component}' for {}", platform()))?;
    // Si el runtime ya está completo con este mismo manifiesto no hace falta revisar archivo por archivo.
    if let (Some(sha1), Ok(installed)) = (&entry.manifest.sha1, std::fs::read_to_string(&marker)) {
        if installed.trim() == sha1 {
            if let Some(java) = executable_in(&home) {
                return Ok(java);
            }
        }
    }
    let manifest: RuntimeManifest = http::verified_json(
        http,
        &entry.manifest.url,
        entry.manifest.sha1.as_deref(),
        &paths.meta().join(format!("java-{component}-{}.json", platform())),
    )
    .await?;

    let mut downloads = Vec::new();
    let mut links = Vec::new();
    for (name, file) in &manifest.files {
        let path = home.join(name);
        match file.kind.as_str() {
            "directory" => std::fs::create_dir_all(&path)?,
            "file" => {
                let raw = &file.downloads.as_ref().context("runtime file without download")?.raw;
                let mut download = Download::new(&raw.url, path, raw.sha1.clone(), raw.size);
                download.executable = file.executable;
                downloads.push(download);
            }
            "link" => {
                if let Some(target) = &file.target {
                    links.push((path, target.clone()));
                }
            }
            _ => {}
        }
    }
    http::download_all(http, downloads, 16, "Downloading Java", progress).await?;
    #[cfg(unix)]
    for (path, target) in links {
        if std::fs::symlink_metadata(&path).is_err() {
            if let Some(parent) = path.parent() {
                std::fs::create_dir_all(parent)?;
            }
            std::os::unix::fs::symlink(target, &path)?;
        }
    }
    #[cfg(not(unix))]
    drop(links);
    if let Some(sha1) = &entry.manifest.sha1 {
        std::fs::write(&marker, sha1)?;
    }
    executable_in(&home).ok_or_else(|| anyhow!("the Java runtime was installed but no java executable was found"))
}
