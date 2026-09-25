use std::path::{Path, PathBuf};

/// Carpetas del launcher. Todo cuelga de una carpeta propia para no tocar la .minecraft del launcher oficial.
#[derive(Clone, Debug)]
pub struct Paths {
    pub root: PathBuf,
}

impl Paths {
    /// Windows: %APPDATA%\.freedomclient, macOS: ~/Library/Application Support/FreedomClient, Linux: ~/.freedomclient.
    /// La variable FC_LAUNCHER_HOME la sustituye (la usan los tests de CI).
    pub fn default_root() -> PathBuf {
        if let Some(home) = std::env::var_os("FC_LAUNCHER_HOME") {
            return PathBuf::from(home);
        }
        if cfg!(windows) {
            dirs::config_dir().unwrap_or_else(|| PathBuf::from(".")).join(".freedomclient")
        } else if cfg!(target_os = "macos") {
            dirs::data_dir().unwrap_or_else(|| PathBuf::from(".")).join("FreedomClient")
        } else {
            dirs::home_dir().unwrap_or_else(|| PathBuf::from(".")).join(".freedomclient")
        }
    }

    pub fn new(root: impl Into<PathBuf>) -> Self {
        Self { root: root.into() }
    }

    pub fn meta(&self) -> PathBuf {
        self.root.join("meta")
    }
    pub fn versions(&self) -> PathBuf {
        self.root.join("versions")
    }
    pub fn libraries(&self) -> PathBuf {
        self.root.join("libraries")
    }
    pub fn assets(&self) -> PathBuf {
        self.root.join("assets")
    }
    pub fn runtimes(&self) -> PathBuf {
        self.root.join("runtime")
    }
    pub fn instances(&self) -> PathBuf {
        self.root.join("instances")
    }
    pub fn instance(&self, profile_id: &str) -> PathBuf {
        self.instances().join(profile_id)
    }
    pub fn cache(&self) -> PathBuf {
        self.root.join("cache")
    }
    pub fn file(&self, name: &str) -> PathBuf {
        self.root.join(name)
    }
}

/// Ruta de Maven a partir de "grupo:artefacto:versión[:clasificador][@extensión]".
pub fn maven_path(name: &str) -> Option<PathBuf> {
    let (coords, ext) = match name.split_once('@') {
        Some((c, e)) => (c, e),
        None => (name, "jar"),
    };
    let parts: Vec<&str> = coords.split(':').collect();
    if parts.len() < 3 {
        return None;
    }
    let (group, artifact, version) = (parts[0], parts[1], parts[2]);
    let file = match parts.get(3) {
        Some(classifier) => format!("{artifact}-{version}-{classifier}.{ext}"),
        None => format!("{artifact}-{version}.{ext}"),
    };
    let mut path = PathBuf::new();
    for segment in group.split('.') {
        path.push(segment);
    }
    path.push(artifact);
    path.push(version);
    path.push(file);
    Some(path)
}

/// Clave para quitar duplicados del classpath: grupo:artefacto[:clasificador], sin la versión.
pub fn library_key(name: &str) -> String {
    let coords = name.split('@').next().unwrap_or(name);
    let parts: Vec<&str> = coords.split(':').collect();
    match parts.len() {
        0..=2 => coords.to_string(),
        3 => format!("{}:{}", parts[0], parts[1]),
        _ => format!("{}:{}:{}", parts[0], parts[1], parts[3]),
    }
}

pub fn url_path(path: &Path) -> String {
    path.components().map(|c| c.as_os_str().to_string_lossy().into_owned()).collect::<Vec<_>>().join("/")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maven_paths() {
        assert_eq!(
            url_path(&maven_path("net.fabricmc:fabric-loader:0.19.3").unwrap()),
            "net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar"
        );
        assert_eq!(
            url_path(&maven_path("org.lwjgl:lwjgl:3.3.3:natives-windows").unwrap()),
            "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows.jar"
        );
        assert_eq!(library_key("org.ow2.asm:asm:9.8"), "org.ow2.asm:asm");
        assert_eq!(library_key("org.lwjgl:lwjgl:3.3.3:natives-linux"), "org.lwjgl:lwjgl:natives-linux");
    }
}
