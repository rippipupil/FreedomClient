use anyhow::Result;
use std::path::Path;

/// Opciones de Minecraft para un perfil nuevo, pensadas para dar el máximo de FPS en PvP.
/// Solo se escriben si el perfil todavía no tiene options.txt: lo que cambie el jugador se respeta.
const OPTIMIZED: &[(&str, &str)] = &[
    // En 1.21.11 los presets (fast/fancy/fabulous) pisan las opciones gráficas: "custom" respeta las de abajo.
    ("graphicsPreset", "\"custom\""),
    ("enableVsync", "false"),
    ("maxFps", "260"),
    ("renderDistance", "10"),
    ("simulationDistance", "8"),
    ("renderClouds", "\"false\""),
    ("entityShadows", "false"),
    ("biomeBlendRadius", "0"),
    ("particles", "1"),
    ("entityDistanceScaling", "0.75"),
    ("mipmapLevels", "2"),
    ("prioritizeChunkUpdates", "0"),
    ("inactivityFpsLimit", "\"afk\""),
    ("onboardAccessibility", "false"),
    ("skipMultiplayerWarning", "true"),
    ("joinedFirstServer", "true"),
    ("tutorialStep", "none"),
    ("narrator", "0"),
];

pub fn write_defaults(game_dir: &Path, data_version: Option<i64>) -> Result<bool> {
    let path = game_dir.join("options.txt");
    if path.exists() {
        return Ok(false);
    }
    std::fs::create_dir_all(game_dir)?;
    let mut text = String::new();
    if let Some(version) = data_version {
        text.push_str(&format!("version:{version}\n"));
    }
    for (key, value) in OPTIMIZED {
        text.push_str(&format!("{key}:{value}\n"));
    }
    std::fs::write(path, text)?;
    Ok(true)
}
