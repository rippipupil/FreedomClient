use anyhow::{Context, Result};
use serde_json::Value;
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};

use crate::Prepared;
use crate::paths::Paths;
use crate::rules::{Features, Rule, allowed};
use crate::settings::{Account, GcPreset};

/// Todo lo necesario para arrancar el juego una vez preparado.
pub struct LaunchOptions {
    pub account: Account,
    pub memory_mb: u32,
    pub gc: GcPreset,
    pub extra_jvm_args: String,
    pub width: u32,
    pub height: u32,
    pub fullscreen: bool,
    pub server: String,
    /// ID de la aplicación de Discord para que el mod muestre el Rich Presence dentro del juego.
    pub discord_app_id: String,
}

/// Flags de G1 para el cliente: pausas cortas (sin tirones) y colecciones jóvenes frecuentes y baratas.
const G1_FLAGS: &[&str] = &[
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=37",
    "-XX:G1NewSizePercent=23",
    "-XX:G1ReservePercent=20",
    "-XX:G1HeapRegionSize=16M",
    "-XX:G1HeapWastePercent=20",
    "-XX:G1MixedGCCountTarget=3",
    "-XX:InitiatingHeapOccupancyPercent=10",
    "-XX:SurvivorRatio=32",
    "-XX:MaxTenuringThreshold=1",
    "-XX:+ParallelRefProcEnabled",
    "-XX:+DisableExplicitGC",
    "-XX:+PerfDisableSharedMem",
];

const ZGC_FLAGS: &[&str] = &["-XX:+UseZGC", "-XX:+ZGenerational", "-XX:+DisableExplicitGC", "-XX:+PerfDisableSharedMem"];

/// Separa argumentos como una consola: por espacios, respetando comillas.
pub fn split_args(text: &str) -> Vec<String> {
    let mut args = Vec::new();
    let mut current = String::new();
    let mut quote: Option<char> = None;
    let mut has = false;
    for c in text.chars() {
        match (quote, c) {
            (Some(q), c) if c == q => quote = None,
            (None, '"' | '\'') => {
                quote = Some(c);
                has = true;
            }
            (None, c) if c.is_whitespace() => {
                if has {
                    args.push(std::mem::take(&mut current));
                    has = false;
                }
            }
            (_, c) => {
                current.push(c);
                has = true;
            }
        }
    }
    if has {
        args.push(current);
    }
    args
}

fn substitute(arg: &str, vars: &HashMap<&str, String>) -> String {
    let mut out = String::with_capacity(arg.len());
    let mut rest = arg;
    while let Some(start) = rest.find("${") {
        out.push_str(&rest[..start]);
        match rest[start..].find('}') {
            Some(end) => {
                let key = &rest[start + 2..start + end];
                match vars.get(key) {
                    Some(value) => out.push_str(value),
                    None => out.push_str(&rest[start..start + end + 1]),
                }
                rest = &rest[start + end + 1..];
            }
            None => {
                out.push_str(&rest[start..]);
                rest = "";
            }
        }
    }
    out.push_str(rest);
    out
}

/// Argumentos de un JSON de versión (texto directo u objetos con reglas).
fn collect(values: &[Value], features: &Features, vars: &HashMap<&str, String>, out: &mut Vec<String>) {
    for value in values {
        match value {
            Value::String(s) => out.push(substitute(s, vars)),
            Value::Object(obj) => {
                let rules: Option<Vec<Rule>> = obj.get("rules").and_then(|r| serde_json::from_value(r.clone()).ok());
                if !allowed(rules.as_deref(), features) {
                    continue;
                }
                match obj.get("value") {
                    Some(Value::String(s)) => out.push(substitute(s, vars)),
                    Some(Value::Array(list)) => {
                        out.extend(list.iter().filter_map(Value::as_str).map(|s| substitute(s, vars)));
                    }
                    _ => {}
                }
            }
            _ => {}
        }
    }
}

pub fn build_command(paths: &Paths, prepared: &Prepared, options: &LaunchOptions) -> Command {
    let separator = if cfg!(windows) { ";" } else { ":" };
    let classpath = prepared
        .classpath
        .iter()
        .map(|p| p.to_string_lossy().into_owned())
        .collect::<Vec<_>>()
        .join(separator);
    let custom_resolution = !options.fullscreen && options.width > 0 && options.height > 0;
    let features = Features { custom_resolution, quick_play_multiplayer: !options.server.trim().is_empty() };
    let (uuid, name, token, user_type, xuid) = match &options.account {
        Account::Microsoft { uuid, name, access_token, xuid, .. } => (uuid.clone(), name.clone(), access_token.clone(), "msa", xuid.clone()),
        Account::Offline { uuid, name } => (uuid.clone(), name.clone(), "0".to_string(), "legacy", "0".to_string()),
    };
    let vars: HashMap<&str, String> = HashMap::from([
        ("auth_player_name", name),
        ("version_name", crate::MINECRAFT_VERSION.to_string()),
        ("game_directory", prepared.game_dir.to_string_lossy().into_owned()),
        ("assets_root", paths.assets().to_string_lossy().into_owned()),
        ("assets_index_name", prepared.asset_index.clone()),
        ("auth_uuid", uuid),
        ("auth_access_token", token),
        ("clientid", String::new()),
        ("auth_xuid", xuid),
        ("user_type", user_type.to_string()),
        ("user_properties", "{}".to_string()),
        ("version_type", "release".to_string()),
        ("natives_directory", prepared.natives_dir.to_string_lossy().into_owned()),
        ("launcher_name", "FreedomClient".to_string()),
        ("launcher_version", env!("CARGO_PKG_VERSION").to_string()),
        ("classpath", classpath),
        ("classpath_separator", separator.to_string()),
        ("library_directory", paths.libraries().to_string_lossy().into_owned()),
        ("resolution_width", options.width.to_string()),
        ("resolution_height", options.height.to_string()),
        ("quickPlayMultiplayer", options.server.trim().to_string()),
    ]);

    let mut args: Vec<String> = Vec::new();
    let memory = options.memory_mb.max(1024);
    args.push(format!("-Xms{}M", memory.min(1024)));
    args.push(format!("-Xmx{memory}M"));
    match options.gc {
        GcPreset::Optimized => args.extend(G1_FLAGS.iter().map(|s| s.to_string())),
        GcPreset::Zgc => args.extend(ZGC_FLAGS.iter().map(|s| s.to_string())),
        GcPreset::Vanilla => {}
    }
    // El mod no se autoactualiza cuando lo gestiona el launcher.
    args.push("-Dfreedomclient.launcher=true".into());
    if !options.discord_app_id.trim().is_empty() {
        args.push(format!("-Dfreedomclient.discordAppId={}", options.discord_app_id.trim()));
    }
    if let Some(log) = &prepared.log_argument {
        args.push(log.clone());
    }
    collect(&prepared.fabric.arguments.jvm, &features, &vars, &mut args);
    collect(&prepared.vanilla.arguments.jvm, &features, &vars, &mut args);
    args.extend(split_args(&options.extra_jvm_args));
    args.push(prepared.fabric.main_class.clone());
    collect(&prepared.vanilla.arguments.game, &features, &vars, &mut args);
    collect(&prepared.fabric.arguments.game, &features, &vars, &mut args);
    if options.fullscreen {
        args.push("--fullscreen".into());
    }

    let mut command = Command::new(&prepared.java);
    command.args(args).current_dir(&prepared.game_dir);
    command
}

/// Arranca el juego. La salida de la consola va a logs/launcher-output.log por si falla antes de crear latest.log.
pub fn spawn(mut command: Command, game_dir: &Path) -> Result<Child> {
    let logs = game_dir.join("logs");
    std::fs::create_dir_all(&logs)?;
    let output = std::fs::File::create(logs.join("launcher-output.log"))?;
    command.stdin(Stdio::null()).stdout(output.try_clone()?).stderr(output);
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x0000_0200;
        command.creation_flags(CREATE_NO_WINDOW | CREATE_NEW_PROCESS_GROUP);
    }
    #[cfg(unix)]
    {
        use std::os::unix::process::CommandExt;
        command.process_group(0);
    }
    command.spawn().context("could not start Java")
}

/// El log más útil del perfil: latest.log del juego, o la salida de consola si el juego no llegó a arrancar.
pub fn log_file(game_dir: &Path) -> PathBuf {
    let latest = game_dir.join("logs").join("latest.log");
    let output = game_dir.join("logs").join("launcher-output.log");
    let modified = |p: &Path| std::fs::metadata(p).and_then(|m| m.modified()).ok();
    match (modified(&latest), modified(&output)) {
        (Some(a), Some(b)) if b > a => output,
        (Some(_), _) => latest,
        (None, Some(_)) => output,
        _ => latest,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn splits_arguments() {
        assert_eq!(split_args(r#"-Dfoo=bar  "-Dx=a b" -XX:+UseG1GC"#), vec!["-Dfoo=bar", "-Dx=a b", "-XX:+UseG1GC"]);
        assert!(split_args("   ").is_empty());
    }

    #[test]
    fn substitutes() {
        let vars = HashMap::from([("a", "1".to_string())]);
        assert_eq!(substitute("x${a}y${b}", &vars), "x1y${b}");
    }
}
