use anyhow::{Result, bail};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::sync::atomic::AtomicU64;
use std::time::Duration;

use crate::http::{self, Download};
use crate::paths::Paths;

pub const REPO: &str = "rippipupil/FreedomClient";
pub const JAR_NAME: &str = "FreedomClient-1.21.11.jar";
const TAG_API: &str = "https://api.github.com/repos/rippipupil/FreedomClient/git/ref/tags/latest";
const JAR_URL: &str = "https://github.com/rippipupil/FreedomClient/releases/download/latest/FreedomClient-1.21.11.jar";

#[derive(Default, Serialize, Deserialize)]
struct Installed {
    /// Archivo de la release que se descargó (cambia con cada versión nueva).
    #[serde(default)]
    asset: String,
    /// Commit de esa versión, si la API de GitHub respondió (solo para enseñarlo).
    #[serde(default)]
    commit: String,
}

#[derive(Deserialize)]
struct TagRef {
    object: TagObject,
}

#[derive(Deserialize)]
struct TagObject {
    sha: String,
}

pub fn cached_jar(paths: &Paths) -> PathBuf {
    paths.cache().join(JAR_NAME)
}

/// Commit de la última versión publicada de FreedomClient (la etiqueta "latest" de GitHub).
async fn latest_commit(http: &reqwest::Client) -> Result<String> {
    let tag: TagRef = http
        .get(TAG_API)
        .header("Accept", "application/vnd.github+json")
        .timeout(Duration::from_secs(8))
        .send()
        .await?
        .error_for_status()?
        .json()
        .await?;
    Ok(tag.object.sha)
}

/// Identificador de la última versión sin la API de GitHub (que tiene un límite de peticiones por IP):
/// la descarga redirige a un archivo que cambia cada vez que se publica un .jar nuevo.
async fn latest_asset() -> Result<String> {
    let http = reqwest::Client::builder()
        .user_agent(crate::http::USER_AGENT)
        .redirect(reqwest::redirect::Policy::none())
        .timeout(Duration::from_secs(8))
        .build()?;
    let response = http.head(JAR_URL).send().await?;
    let location = response
        .headers()
        .get(reqwest::header::LOCATION)
        .and_then(|v| v.to_str().ok())
        .ok_or_else(|| anyhow::anyhow!("GitHub did not redirect the download ({})", response.status()))?;
    Ok(location.split('?').next().unwrap_or(location).to_string())
}

/// Deja en la caché el .jar más reciente de FreedomClient. Si no hay internet vale el que ya había.
/// Devuelve la ruta del .jar y si se ha actualizado.
pub async fn ensure_latest(http: &reqwest::Client, paths: &Paths, auto_update: bool) -> Result<(PathBuf, bool)> {
    let jar = cached_jar(paths);
    let state_path = paths.cache().join("freedomclient.json");
    let installed: Installed = http::read_json(&state_path).unwrap_or_default();
    let present = jar.is_file();
    if present && !auto_update {
        return Ok((jar, false));
    }
    let latest = match latest_asset().await {
        Ok(asset) => asset,
        Err(e) => {
            if present {
                return Ok((jar, false));
            }
            return Err(e.context("could not reach GitHub to download FreedomClient"));
        }
    };
    if present && installed.asset == latest {
        return Ok((jar, false));
    }
    let download = Download::new(JAR_URL, &jar, None, None);
    let _ = std::fs::remove_file(jar.with_extension("part"));
    let result = http::download_file(http, &download, &AtomicU64::new(0)).await;
    if let Err(e) = result {
        if present {
            return Ok((jar, false));
        }
        return Err(e);
    }
    if std::fs::metadata(&jar).map(|m| m.len()).unwrap_or(0) < 100_000 {
        bail!("the downloaded FreedomClient jar is too small");
    }
    let commit = latest_commit(http).await.unwrap_or_default();
    std::fs::write(&state_path, serde_json::to_string(&Installed { asset: latest, commit })?)?;
    Ok((jar, present))
}

/// Commit instalado (para enseñarlo en el launcher).
pub fn installed_commit(paths: &Paths) -> Option<String> {
    http::read_json::<Installed>(&paths.cache().join("freedomclient.json"))
        .ok()
        .map(|i| i.commit)
        .filter(|c| c.len() == 40 && c.chars().all(|ch| ch.is_ascii_hexdigit()))
}

#[cfg(test)]
mod tests {
    /// Necesita internet: cargo test -p fc-core -- --ignored
    #[tokio::test]
    #[ignore]
    async fn downloads_latest_jar() {
        let dir = std::env::temp_dir().join(format!("fc-client-test-{}", std::process::id()));
        let paths = crate::paths::Paths::new(&dir);
        let http = crate::http::client();
        let (jar, _) = super::ensure_latest(&http, &paths, true).await.unwrap();
        assert!(std::fs::metadata(&jar).unwrap().len() > 100_000);
        // La segunda vez no se vuelve a descargar.
        let modified = std::fs::metadata(&jar).unwrap().modified().unwrap();
        super::ensure_latest(&http, &paths, true).await.unwrap();
        assert_eq!(std::fs::metadata(&jar).unwrap().modified().unwrap(), modified);
        let _ = std::fs::remove_dir_all(dir);
    }
}
