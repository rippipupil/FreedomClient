//! Prueba de CI: instala todo en una carpeta limpia y lanza el juego con una cuenta sin conexión.
//! Termina bien cuando el log del juego dice que FreedomClient ha cargado y la pantalla de título está lista.
//!
//! Uso: FC_LAUNCHER_HOME=/tmp/fc fc-ci [segundos máximos]

use anyhow::{Result, bail};
use fc_core::launch::{LaunchOptions, build_command, spawn};
use fc_core::paths::Paths;
use fc_core::settings::{Account, Profile, Settings};
use std::sync::Arc;
use std::time::{Duration, Instant};

#[tokio::main]
async fn main() -> Result<()> {
    let timeout: u64 = std::env::args().nth(1).and_then(|s| s.parse().ok()).unwrap_or(600);
    let paths = Paths::new(Paths::default_root());
    let http = fc_core::http::client();
    let settings = Settings::default();
    let profile = Profile { id: "ci".into(), ..Default::default() };
    let last = Arc::new(std::sync::Mutex::new(String::new()));
    let progress: fc_core::Progress = {
        let last = last.clone();
        Arc::new(move |stage: &str, done: u64, total: u64| {
            let line = if total > 0 { format!("{stage}: {}%", done * 100 / total / 10 * 10) } else { stage.to_string() };
            let mut last = last.lock().unwrap();
            if *last != line {
                println!("{line}");
                *last = line;
            }
        })
    };

    let started = Instant::now();
    let prepared = fc_core::prepare(&http, &paths, &settings, &profile, &progress).await?;
    println!("Prepared in {:.1}s (java: {})", started.elapsed().as_secs_f32(), prepared.java.display());
    // La segunda vez no debería descargar nada: comprueba que la caché funciona y que es rápido.
    let again = Instant::now();
    fc_core::prepare(&http, &paths, &settings, &profile, &progress).await?;
    println!("Second prepare took {:.2}s", again.elapsed().as_secs_f32());

    let options = LaunchOptions {
        account: Account::offline("FreedomCI"),
        memory_mb: fc_core::memory_for(&settings, &profile),
        gc: settings.gc,
        extra_jvm_args: String::new(),
        width: 1280,
        height: 720,
        fullscreen: false,
        server: String::new(),
        discord_app_id: String::new(),
    };
    let command = build_command(&paths, &prepared, &options);
    println!("Command: {:?}", command.get_args().filter(|a| !a.to_string_lossy().contains(std::path::MAIN_SEPARATOR)).collect::<Vec<_>>());
    let mut child = spawn(command, &prepared.game_dir)?;
    let log = prepared.game_dir.join("logs").join("latest.log");
    let deadline = Instant::now() + Duration::from_secs(timeout);
    // Sin gráficos (Windows en CI) basta con ver que Fabric carga los mods: FC_CI_UNTIL="Loading".
    let until = std::env::var("FC_CI_UNTIL").ok();
    let reached = |text: &str| match &until {
        Some(marker) => text.contains(marker.as_str()),
        None => text.contains("FreedomClient loaded") && text.contains("Created:"),
    };
    loop {
        if let Some(status) = child.try_wait()? {
            let text = std::fs::read_to_string(&log).unwrap_or_default();
            print_output(&prepared.game_dir);
            if until.is_some() && reached(&text) {
                println!("The game got far enough before exiting ({status})");
                return Ok(());
            }
            bail!("the game exited early with {status}");
        }
        let text = std::fs::read_to_string(&log).unwrap_or_default();
        if until.is_some() && reached(&text) {
            println!("The game got far enough");
            let _ = child.kill();
            return Ok(());
        }
        if reached(&text) {
            // Se deja unos segundos para que llegue a la pantalla de título y se hace la captura desde el workflow.
            tokio::time::sleep(Duration::from_secs(20)).await;
            println!("The game started correctly");
            if let Ok(path) = std::env::var("FC_CI_READY_FILE") {
                std::fs::write(path, "ready")?;
                tokio::time::sleep(Duration::from_secs(8)).await;
            }
            let _ = child.kill();
            return Ok(());
        }
        if Instant::now() > deadline {
            let _ = child.kill();
            print_output(&prepared.game_dir);
            bail!("timed out waiting for the game");
        }
        tokio::time::sleep(Duration::from_secs(2)).await;
    }
}

fn print_output(game_dir: &std::path::Path) {
    for name in ["launcher-output.log", "latest.log"] {
        if let Ok(text) = std::fs::read_to_string(game_dir.join("logs").join(name)) {
            println!("===== {name} =====");
            for line in text.lines().rev().take(150).collect::<Vec<_>>().into_iter().rev() {
                println!("{line}");
            }
        }
    }
}
