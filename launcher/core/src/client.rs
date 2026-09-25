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
    let latest = match latest_commit(http).await {
        Ok(commit) => commit,
        Err(e) => {
            if present {
                return Ok((jar, false));
            }
            return Err(e.context("could not reach GitHub to download FreedomClient"));
        }
    };
    if present && installed.commit == latest {
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
    std::fs::write(&state_path, serde_json::to_string(&Installed { commit: latest })?)?;
    Ok((jar, present))
}

/// Commit instalado (para enseñarlo en el launcher).
pub fn installed_commit(paths: &Paths) -> Option<String> {
    http::read_json::<Installed>(&paths.cache().join("freedomclient.json")).ok().map(|i| i.commit).filter(|c| !c.is_empty())
}
