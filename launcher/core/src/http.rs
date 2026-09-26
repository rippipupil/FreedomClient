use anyhow::{Context, Result, bail};
use futures_util::StreamExt;
use serde::de::DeserializeOwned;
use sha1::{Digest, Sha1};
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{Duration, SystemTime};
use tokio::io::AsyncWriteExt;

use crate::Progress;

pub const USER_AGENT: &str = concat!("FreedomLauncher/", env!("CARGO_PKG_VERSION"));

pub fn client() -> reqwest::Client {
    reqwest::Client::builder()
        .user_agent(USER_AGENT)
        .connect_timeout(Duration::from_secs(10))
        .read_timeout(Duration::from_secs(30))
        .build()
        .expect("http client")
}

/// Un archivo que hay que tener en disco.
#[derive(Clone, Debug)]
pub struct Download {
    pub url: String,
    pub path: PathBuf,
    pub sha1: Option<String>,
    pub size: Option<u64>,
    pub executable: bool,
}

impl Download {
    pub fn new(url: impl Into<String>, path: impl Into<PathBuf>, sha1: Option<String>, size: Option<u64>) -> Self {
        Self { url: url.into(), path: path.into(), sha1, size, executable: false }
    }

    /// Comprobación rápida: existe y tiene el tamaño esperado. Si no se sabe el tamaño se mira el hash.
    fn is_present(&self) -> bool {
        let Ok(meta) = std::fs::metadata(&self.path) else { return false };
        match (self.size, &self.sha1) {
            (Some(size), _) => meta.len() == size,
            (None, Some(sha1)) => sha1_file(&self.path).map(|h| h.eq_ignore_ascii_case(sha1)).unwrap_or(false),
            (None, None) => meta.len() > 0,
        }
    }
}

/// Archivo temporal de una descarga: se añade ".part" al nombre entero. (Cambiar la extensión haría que
/// java.security y java.policy compartieran "java.part" y se pisaran al bajarse a la vez.)
pub fn part_path(path: &Path) -> PathBuf {
    let mut name = path.file_name().unwrap_or_default().to_os_string();
    name.push(".part");
    path.with_file_name(name)
}

pub fn sha1_bytes(bytes: &[u8]) -> String {
    hex::encode(Sha1::digest(bytes))
}

pub fn sha1_file(path: &Path) -> Result<String> {
    use std::io::Read;
    let mut file = std::fs::File::open(path)?;
    let mut hasher = Sha1::new();
    let mut buffer = vec![0u8; 64 * 1024];
    loop {
        let read = file.read(&mut buffer)?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }
    Ok(hex::encode(hasher.finalize()))
}

/// Descarga un archivo comprobando su hash. Escribe en un .part y lo renombra al terminar.
pub async fn download_file(http: &reqwest::Client, item: &Download, bytes_done: &AtomicU64) -> Result<()> {
    if let Some(parent) = item.path.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }
    let mut last_error = None;
    for attempt in 0..3 {
        if attempt > 0 {
            tokio::time::sleep(Duration::from_millis(500 * attempt)).await;
        }
        match try_download(http, item, bytes_done).await {
            Ok(()) => return Ok(()),
            Err(e) => last_error = Some(e),
        }
    }
    Err(last_error.unwrap()).with_context(|| format!("downloading {}", item.url))
}

async fn try_download(http: &reqwest::Client, item: &Download, bytes_done: &AtomicU64) -> Result<()> {
    let part = part_path(&item.path);
    let response = http.get(&item.url).send().await?.error_for_status()?;
    let mut file = tokio::fs::File::create(&part).await?;
    let mut hasher = Sha1::new();
    let mut stream = response.bytes_stream();
    let mut written = 0u64;
    while let Some(chunk) = stream.next().await {
        let chunk = chunk?;
        hasher.update(&chunk);
        file.write_all(&chunk).await?;
        written += chunk.len() as u64;
        bytes_done.fetch_add(chunk.len() as u64, Ordering::Relaxed);
    }
    file.flush().await?;
    drop(file);
    if let Some(expected) = &item.sha1 {
        let actual = hex::encode(hasher.finalize());
        if !actual.eq_ignore_ascii_case(expected) {
            let _ = tokio::fs::remove_file(&part).await;
            bytes_done.fetch_sub(written, Ordering::Relaxed);
            bail!("hash mismatch for {} (expected {expected}, got {actual})", item.url);
        }
    }
    tokio::fs::rename(&part, &item.path).await?;
    #[cfg(unix)]
    if item.executable {
        use std::os::unix::fs::PermissionsExt;
        tokio::fs::set_permissions(&item.path, std::fs::Permissions::from_mode(0o755)).await?;
    }
    Ok(())
}

/// Descarga en paralelo todo lo que falte e informa del progreso en bytes.
pub async fn download_all(
    http: &reqwest::Client,
    items: Vec<Download>,
    concurrency: usize,
    stage: &str,
    progress: &Progress,
) -> Result<()> {
    let missing: Vec<Download> = tokio::task::spawn_blocking(move || items.into_iter().filter(|d| !d.is_present()).collect())
        .await?;
    if missing.is_empty() {
        return Ok(());
    }
    let total: u64 = missing.iter().map(|d| d.size.unwrap_or(0)).sum();
    let done = Arc::new(AtomicU64::new(0));
    let reporter = {
        let done = done.clone();
        let progress = progress.clone();
        let stage = stage.to_string();
        tokio::spawn(async move {
            loop {
                progress(&stage, done.load(Ordering::Relaxed).min(total), total);
                tokio::time::sleep(Duration::from_millis(150)).await;
            }
        })
    };
    let results: Vec<Result<()>> = futures_util::stream::iter(missing)
        .map(|item| {
            let done = done.clone();
            let http = http.clone();
            async move { download_file(&http, &item, &done).await }
        })
        .buffer_unordered(concurrency.max(1))
        .collect()
        .await;
    reporter.abort();
    progress(stage, total, total);
    results.into_iter().collect::<Result<Vec<()>>>()?;
    Ok(())
}

/// JSON de una URL guardado en disco. Si la copia tiene menos de `max_age` se usa sin preguntar;
/// si no hay internet se usa la copia vieja para poder jugar sin conexión.
pub async fn cached_json<T: DeserializeOwned>(
    http: &reqwest::Client,
    url: &str,
    cache: &Path,
    max_age: Duration,
) -> Result<T> {
    let fresh = std::fs::metadata(cache)
        .and_then(|m| m.modified())
        .ok()
        .and_then(|t| SystemTime::now().duration_since(t).ok())
        .map(|age| age < max_age)
        .unwrap_or(false);
    if fresh {
        if let Ok(value) = read_json(cache) {
            return Ok(value);
        }
    }
    match fetch_text(http, url).await {
        Ok(text) => {
            let value = serde_json::from_str(&text).with_context(|| format!("parsing {url}"))?;
            if let Some(parent) = cache.parent() {
                std::fs::create_dir_all(parent)?;
            }
            std::fs::write(cache, text)?;
            Ok(value)
        }
        Err(e) => read_json(cache).map_err(|_| e),
    }
}

/// JSON con hash conocido (JSON de versión, índice de assets...): se descarga una vez y se reutiliza.
pub async fn verified_json<T: DeserializeOwned>(
    http: &reqwest::Client,
    url: &str,
    sha1: Option<&str>,
    path: &Path,
) -> Result<T> {
    if let Ok(bytes) = std::fs::read(path) {
        if sha1.map(|s| sha1_bytes(&bytes).eq_ignore_ascii_case(s)).unwrap_or(true) {
            if let Ok(value) = serde_json::from_slice(&bytes) {
                return Ok(value);
            }
        }
    }
    let text = fetch_text(http, url).await?;
    if let Some(expected) = sha1 {
        let actual = sha1_bytes(text.as_bytes());
        if !actual.eq_ignore_ascii_case(expected) {
            bail!("hash mismatch for {url}");
        }
    }
    let value = serde_json::from_str(&text).with_context(|| format!("parsing {url}"))?;
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    std::fs::write(path, text)?;
    Ok(value)
}

pub async fn fetch_text(http: &reqwest::Client, url: &str) -> Result<String> {
    Ok(http.get(url).timeout(Duration::from_secs(15)).send().await?.error_for_status()?.text().await?)
}

pub fn read_json<T: DeserializeOwned>(path: &Path) -> Result<T> {
    Ok(serde_json::from_slice(&std::fs::read(path)?)?)
}

#[cfg(test)]
mod tests {
    #[test]
    fn part_paths_do_not_collide() {
        let a = super::part_path(std::path::Path::new("conf/security/java.security"));
        let b = super::part_path(std::path::Path::new("conf/security/java.policy"));
        assert_ne!(a, b);
        assert!(a.ends_with("java.security.part"));
    }
}
