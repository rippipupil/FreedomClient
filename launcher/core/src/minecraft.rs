use anyhow::{Context, Result, anyhow};
use serde::Deserialize;
use serde_json::Value;
use std::collections::HashMap;
use std::path::PathBuf;
use std::time::Duration;

use crate::http::{self, Download};
use crate::paths::{Paths, maven_path, url_path};
use crate::rules::{Features, Rule, allowed};

pub const MANIFEST_URL: &str = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
pub const RESOURCES_URL: &str = "https://resources.download.minecraft.net";
pub const LIBRARIES_URL: &str = "https://libraries.minecraft.net/";

#[derive(Deserialize)]
struct Manifest {
    versions: Vec<ManifestVersion>,
}

#[derive(Deserialize)]
struct ManifestVersion {
    id: String,
    url: String,
    sha1: Option<String>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct VersionJson {
    pub id: String,
    #[serde(rename = "mainClass")]
    pub main_class: String,
    #[serde(default)]
    pub arguments: Arguments,
    #[serde(default)]
    pub libraries: Vec<Library>,
    #[serde(rename = "assetIndex")]
    pub asset_index: Option<AssetIndexRef>,
    pub downloads: Option<VersionDownloads>,
    #[serde(rename = "javaVersion")]
    pub java_version: Option<JavaVersion>,
    pub logging: Option<Logging>,
    #[serde(rename = "type")]
    pub kind: Option<String>,
}

#[derive(Clone, Debug, Default, Deserialize)]
pub struct Arguments {
    #[serde(default)]
    pub game: Vec<Value>,
    #[serde(default)]
    pub jvm: Vec<Value>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct Library {
    pub name: String,
    pub downloads: Option<LibraryDownloads>,
    pub rules: Option<Vec<Rule>>,
    /// Repositorio Maven (librerías de Fabric).
    pub url: Option<String>,
    pub sha1: Option<String>,
    pub size: Option<u64>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct LibraryDownloads {
    pub artifact: Option<Artifact>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct Artifact {
    pub path: Option<String>,
    pub sha1: Option<String>,
    pub size: Option<u64>,
    pub url: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct AssetIndexRef {
    pub id: String,
    pub sha1: Option<String>,
    pub url: String,
}

#[derive(Clone, Debug, Deserialize)]
pub struct VersionDownloads {
    pub client: Artifact,
}

#[derive(Clone, Debug, Deserialize)]
pub struct JavaVersion {
    pub component: String,
    #[serde(rename = "majorVersion")]
    pub major_version: u32,
}

#[derive(Clone, Debug, Deserialize)]
pub struct Logging {
    pub client: Option<LoggingClient>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct LoggingClient {
    pub argument: String,
    pub file: LoggingFile,
}

#[derive(Clone, Debug, Deserialize)]
pub struct LoggingFile {
    pub id: String,
    pub sha1: Option<String>,
    pub size: Option<u64>,
    pub url: String,
}

#[derive(Deserialize)]
struct AssetIndex {
    objects: HashMap<String, AssetObject>,
}

#[derive(Deserialize)]
struct AssetObject {
    hash: String,
    size: u64,
}

/// JSON de la versión de Minecraft (se guarda y se reutiliza mientras el hash coincida).
pub async fn version_json(http: &reqwest::Client, paths: &Paths, version: &str) -> Result<VersionJson> {
    let path = paths.versions().join(version).join(format!("{version}.json"));
    let manifest: Result<Manifest> =
        http::cached_json(http, MANIFEST_URL, &paths.meta().join("version_manifest_v2.json"), Duration::from_secs(6 * 3600)).await;
    match manifest {
        Ok(manifest) => {
            let entry = manifest
                .versions
                .iter()
                .find(|v| v.id == version)
                .ok_or_else(|| anyhow!("Minecraft {version} is not in Mojang's version list"))?;
            http::verified_json(http, &entry.url, entry.sha1.as_deref(), &path).await
        }
        // Sin internet: vale la copia que ya había.
        Err(e) => http::read_json(&path).map_err(|_| e),
    }
}

impl Library {
    /// Ruta local y descarga de la librería, si aplica en este sistema.
    pub fn resolve(&self, paths: &Paths, features: &Features) -> Option<Download> {
        if !allowed(self.rules.as_deref(), features) {
            return None;
        }
        if let Some(artifact) = self.downloads.as_ref().and_then(|d| d.artifact.as_ref()) {
            let relative = artifact
                .path
                .clone()
                .map(PathBuf::from)
                .or_else(|| maven_path(&self.name))?;
            return Some(Download::new(&artifact.url, paths.libraries().join(relative), artifact.sha1.clone(), artifact.size));
        }
        // Formato de Fabric: solo nombre Maven y repositorio.
        let relative = maven_path(&self.name)?;
        let base = self.url.clone().unwrap_or_else(|| LIBRARIES_URL.to_string());
        let url = format!("{}/{}", base.trim_end_matches('/'), url_path(&relative));
        Some(Download::new(url, paths.libraries().join(relative), self.sha1.clone(), self.size))
    }
}

/// Descargas del juego base: el .jar del cliente, la configuración de logs y los assets.
pub async fn asset_downloads(http: &reqwest::Client, paths: &Paths, version: &VersionJson) -> Result<Vec<Download>> {
    let index_ref = version.asset_index.as_ref().context("version has no asset index")?;
    let index_path = paths.assets().join("indexes").join(format!("{}.json", index_ref.id));
    let index: AssetIndex = http::verified_json(http, &index_ref.url, index_ref.sha1.as_deref(), &index_path).await?;
    let objects = paths.assets().join("objects");
    let mut seen = std::collections::HashSet::new();
    Ok(index
        .objects
        .into_values()
        .filter(|o| seen.insert(o.hash.clone()))
        .map(|o| {
            let prefix = &o.hash[..2];
            Download::new(
                format!("{RESOURCES_URL}/{prefix}/{}", o.hash),
                objects.join(prefix).join(&o.hash),
                Some(o.hash.clone()),
                Some(o.size),
            )
        })
        .collect())
}

pub fn client_jar(paths: &Paths, version: &VersionJson) -> Result<Download> {
    let client = &version.downloads.as_ref().context("version has no client download")?.client;
    Ok(Download::new(
        &client.url,
        paths.versions().join(&version.id).join(format!("{}.jar", version.id)),
        client.sha1.clone(),
        client.size,
    ))
}

/// Configuración de log4j de Mojang: el argumento de la JVM y el archivo.
pub fn log_config(paths: &Paths, version: &VersionJson) -> Option<(String, Download)> {
    let client = version.logging.as_ref()?.client.as_ref()?;
    let path = paths.assets().join("log_configs").join(&client.file.id);
    let argument = client.argument.replace("${path}", &path.to_string_lossy());
    Some((argument, Download::new(&client.file.url, path, client.file.sha1.clone(), client.file.size)))
}

/// Versión de datos del juego (va en options.txt), leída del version.json que hay dentro del .jar.
pub fn data_version(client_jar: &std::path::Path) -> Option<i64> {
    let file = std::fs::File::open(client_jar).ok()?;
    let mut archive = zip::ZipArchive::new(file).ok()?;
    let entry = archive.by_name("version.json").ok()?;
    let json: Value = serde_json::from_reader(entry).ok()?;
    json.get("world_version").and_then(Value::as_i64)
}
