use serde::Deserialize;
use std::collections::HashMap;

/// Regla de los JSON de Mojang: permite o prohíbe una librería o argumento según el sistema y las "features".
#[derive(Clone, Debug, Deserialize)]
pub struct Rule {
    pub action: String,
    #[serde(default)]
    pub os: Option<OsRule>,
    #[serde(default)]
    pub features: Option<HashMap<String, bool>>,
}

#[derive(Clone, Debug, Deserialize)]
pub struct OsRule {
    pub name: Option<String>,
    pub arch: Option<String>,
}

/// Nombre del sistema tal y como lo escribe Mojang.
pub fn os_name() -> &'static str {
    if cfg!(windows) {
        "windows"
    } else if cfg!(target_os = "macos") {
        "osx"
    } else {
        "linux"
    }
}

pub fn os_arch() -> &'static str {
    if cfg!(target_arch = "x86") {
        "x86"
    } else if cfg!(target_arch = "aarch64") {
        "arm64"
    } else {
        "x86_64"
    }
}

/// "features" activas al lanzar (resolución propia, conexión directa a un servidor...).
#[derive(Clone, Debug, Default)]
pub struct Features {
    pub custom_resolution: bool,
    pub quick_play_multiplayer: bool,
}

impl Features {
    fn get(&self, name: &str) -> bool {
        match name {
            "has_custom_resolution" => self.custom_resolution,
            "is_quick_play_multiplayer" => self.quick_play_multiplayer,
            _ => false,
        }
    }
}

pub fn allowed(rules: Option<&[Rule]>, features: &Features) -> bool {
    let Some(rules) = rules else { return true };
    if rules.is_empty() {
        return true;
    }
    let mut allow = false;
    for rule in rules {
        let os_matches = rule.os.as_ref().map_or(true, |os| {
            os.name.as_deref().map_or(true, |n| n == os_name()) && os.arch.as_deref().map_or(true, |a| a == os_arch())
        });
        let features_match = rule
            .features
            .as_ref()
            .map_or(true, |f| f.iter().all(|(name, value)| features.get(name) == *value));
        if os_matches && features_match {
            allow = rule.action == "allow";
        }
    }
    allow
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn os_rules() {
        let rules: Vec<Rule> = serde_json::from_str(&format!(r#"[{{"action":"allow","os":{{"name":"{}"}}}}]"#, os_name())).unwrap();
        assert!(allowed(Some(&rules), &Features::default()));
        let rules: Vec<Rule> = serde_json::from_str(r#"[{"action":"allow","os":{"name":"nothing"}}]"#).unwrap();
        assert!(!allowed(Some(&rules), &Features::default()));
        let rules: Vec<Rule> = serde_json::from_str(r#"[{"action":"allow","features":{"is_demo_user":true}}]"#).unwrap();
        assert!(!allowed(Some(&rules), &Features::default()));
        let rules: Vec<Rule> = serde_json::from_str(r#"[{"action":"allow","features":{"has_custom_resolution":true}}]"#).unwrap();
        assert!(allowed(Some(&rules), &Features { custom_resolution: true, ..Default::default() }));
    }
}
