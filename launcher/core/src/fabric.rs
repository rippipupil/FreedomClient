use anyhow::Result;
use serde::Deserialize;
use std::time::Duration;

use crate::http;
use crate::minecraft::VersionJson;
use crate::paths::Paths;

const META: &str = "https://meta.fabricmc.net/v2/versions/loader";
/// Versión mínima con la que se compila FreedomClient; se usa si no se puede preguntar a Fabric.
pub const FALLBACK_LOADER: &str = "0.19.3";

#[derive(Deserialize)]
struct LoaderEntry {
    loader: LoaderVersion,
}

#[derive(Deserialize)]
struct LoaderVersion {
    version: String,
    stable: bool,
}

/// Última versión estable de Fabric Loader para esta versión de Minecraft.
pub async fn latest_loader(http: &reqwest::Client, paths: &Paths, minecraft: &str) -> String {
    let cache = paths.meta().join(format!("fabric-loaders-{minecraft}.json"));
    let list: Result<Vec<LoaderEntry>> =
        http::cached_json(http, &format!("{META}/{minecraft}"), &cache, Duration::from_secs(6 * 3600)).await;
    list.ok()
        .and_then(|l| l.into_iter().find(|e| e.loader.stable).map(|e| e.loader.version))
        .filter(|v| version_at_least(v, FALLBACK_LOADER))
        .unwrap_or_else(|| FALLBACK_LOADER.to_string())
}

/// Perfil de lanzamiento de Fabric (clase principal y librerías extra).
pub async fn profile(http: &reqwest::Client, paths: &Paths, minecraft: &str, loader: &str) -> Result<VersionJson> {
    let id = format!("fabric-loader-{loader}-{minecraft}");
    let path = paths.versions().join(&id).join(format!("{id}.json"));
    if let Ok(profile) = http::read_json(&path) {
        return Ok(profile);
    }
    http::verified_json(http, &format!("{META}/{minecraft}/{loader}/profile/json"), None, &path).await
}

fn version_at_least(version: &str, minimum: &str) -> bool {
    let parse = |s: &str| s.split(['.', '+', '-']).map(|p| p.parse::<u32>().unwrap_or(0)).collect::<Vec<_>>();
    parse(version) >= parse(minimum)
}

#[cfg(test)]
mod tests {
    #[test]
    fn versions() {
        assert!(super::version_at_least("0.19.3", "0.19.3"));
        assert!(super::version_at_least("0.20.0", "0.19.3"));
        assert!(!super::version_at_least("0.18.9", "0.19.3"));
    }
}
