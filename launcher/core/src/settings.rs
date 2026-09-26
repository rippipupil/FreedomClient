use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::path::Path;

use crate::paths::Paths;

/// Recolector de basura de Java.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum GcPreset {
    /// G1 afinado para que no haya tirones (por defecto).
    #[default]
    Optimized,
    /// ZGC generacional: pausas mínimas, gasta algo más de memoria.
    Zgc,
    /// Lo que decida Java.
    Vanilla,
}

/// Qué hace el launcher cuando empieza la partida.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AfterLaunch {
    /// Se cierra para no gastar nada mientras juegas (por defecto).
    #[default]
    Close,
    Minimize,
    KeepOpen,
}

/// Con qué se abre el juego al pulsar Launch.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum LaunchWith {
    /// Launcher oficial de Minecraft con el perfil FreedomClient (su login; por defecto).
    #[default]
    Official,
    /// Directamente desde este launcher (necesita el login de Microsoft propio).
    Freedom,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(default)]
pub struct Settings {
    pub launch_with: LaunchWith,
    /// Memoria para el juego en MB; 0 = automática según la RAM del PC.
    pub memory_mb: u32,
    pub gc: GcPreset,
    pub extra_jvm_args: String,
    /// Java propio; vacío = el de Mojang que descarga el launcher.
    pub java_path: String,
    pub width: u32,
    pub height: u32,
    pub fullscreen: bool,
    pub after_launch: AfterLaunch,
    pub auto_update_client: bool,
    pub concurrent_downloads: u32,
    pub discord_rpc: bool,
    pub discord_app_id: String,
    /// ID de aplicación de Azure para iniciar sesión con Microsoft.
    pub microsoft_client_id: String,
    pub selected_profile: String,
    pub selected_account: String,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            launch_with: LaunchWith::Official,
            memory_mb: 0,
            gc: GcPreset::Optimized,
            extra_jvm_args: String::new(),
            java_path: String::new(),
            width: 1280,
            height: 720,
            fullscreen: false,
            after_launch: AfterLaunch::Close,
            auto_update_client: true,
            concurrent_downloads: 32,
            discord_rpc: true,
            discord_app_id: crate::discord::DEFAULT_APP_ID.to_string(),
            microsoft_client_id: crate::auth::DEFAULT_CLIENT_ID.to_string(),
            selected_profile: String::new(),
            selected_account: String::new(),
        }
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(default)]
pub struct Profile {
    pub id: String,
    pub name: String,
    /// Color del icono del perfil en el launcher.
    pub color: String,
    /// Memoria propia de este perfil en MB (0 = la de Configuración).
    pub memory_mb: u32,
    pub extra_jvm_args: String,
    /// Servidor al que entrar directamente al lanzar (vacío = menú principal).
    pub server: String,
    pub created: u64,
    pub last_played: u64,
}

impl Default for Profile {
    fn default() -> Self {
        Self {
            id: String::new(),
            name: "FreedomClient".into(),
            color: "#F2C94C".into(),
            memory_mb: 0,
            extra_jvm_args: String::new(),
            server: String::new(),
            created: now(),
            last_played: 0,
        }
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum Account {
    Microsoft {
        uuid: String,
        name: String,
        access_token: String,
        refresh_token: String,
        /// Segundos UNIX en los que caduca el token de Minecraft.
        expires_at: u64,
        #[serde(default)]
        xuid: String,
    },
    Offline {
        uuid: String,
        name: String,
    },
}

impl Account {
    pub fn uuid(&self) -> &str {
        match self {
            Account::Microsoft { uuid, .. } | Account::Offline { uuid, .. } => uuid,
        }
    }
    pub fn name(&self) -> &str {
        match self {
            Account::Microsoft { name, .. } | Account::Offline { name, .. } => name,
        }
    }

    /// Cuenta sin conexión con la misma UUID que usa Minecraft para "OfflinePlayer:<nombre>".
    pub fn offline(name: &str) -> Self {
        use md5::{Digest, Md5};
        let mut hash: [u8; 16] = Md5::digest(format!("OfflinePlayer:{name}").as_bytes()).into();
        hash[6] = (hash[6] & 0x0f) | 0x30;
        hash[8] = (hash[8] & 0x3f) | 0x80;
        Account::Offline { uuid: uuid::Uuid::from_bytes(hash).simple().to_string(), name: name.to_string() }
    }
}

#[derive(Clone, Debug, Default, Serialize, Deserialize)]
#[serde(default)]
pub struct Accounts {
    pub accounts: Vec<Account>,
    /// Se pone a true cuando alguien inicia sesión con una cuenta de Microsoft que tiene Minecraft;
    /// solo entonces se permiten cuentas sin conexión.
    pub owns_game: bool,
}

pub fn now() -> u64 {
    std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

pub fn load<T: Default + for<'de> Deserialize<'de>>(path: &Path) -> T {
    std::fs::read(path).ok().and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default()
}

/// Guarda de forma segura: primero a un temporal y luego se renombra, para no dejar el archivo a medias.
pub fn save<T: Serialize>(path: &Path, value: &T) -> Result<()> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    let tmp = path.with_extension("tmp");
    std::fs::write(&tmp, serde_json::to_vec_pretty(value)?)?;
    std::fs::rename(tmp, path)?;
    Ok(())
}

pub fn settings_path(paths: &Paths) -> std::path::PathBuf {
    paths.file("launcher.json")
}
pub fn profiles_path(paths: &Paths) -> std::path::PathBuf {
    paths.file("profiles.json")
}
pub fn accounts_path(paths: &Paths) -> std::path::PathBuf {
    paths.file("accounts.json")
}

pub fn load_profiles(paths: &Paths) -> Vec<Profile> {
    let mut profiles: Vec<Profile> = load(&profiles_path(paths));
    if profiles.is_empty() {
        profiles.push(Profile { id: "default".into(), ..Default::default() });
        let _ = save(&profiles_path(paths), &profiles);
    }
    profiles
}

pub fn new_profile_id() -> String {
    uuid::Uuid::new_v4().simple().to_string()[..12].to_string()
}

/// Memoria automática: suficiente para PvP con FreedomClient sin quitarle RAM al sistema.
pub fn auto_memory_mb(total_mb: u64) -> u32 {
    match total_mb {
        m if m >= 15_000 => 4096,
        m if m >= 7_500 => 3072,
        _ => 2048,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn offline_uuid_matches_minecraft() {
        // UUID que genera Minecraft para "OfflinePlayer:Notch".
        assert_eq!(Account::offline("Notch").uuid(), "b50ad385829d3141a2167e7d7539ba7f");
    }

    #[test]
    fn memory() {
        assert_eq!(auto_memory_mb(16_000), 4096);
        assert_eq!(auto_memory_mb(8_000), 3072);
        assert_eq!(auto_memory_mb(4_000), 2048);
    }
}
