//! Discord Rich Presence por el IPC local de Discord (el mismo protocolo que usa el mod).
//! Cada mensaje es: código (u32 LE), longitud (u32 LE) y JSON en UTF-8.

use serde_json::{Value, json};
use std::io::{Read, Write};
use std::sync::mpsc::{Receiver, RecvTimeoutError, Sender, channel};
use std::time::Duration;

/// ID de la aplicación de Discord de FreedomClient (vacío hasta tenerla; se puede poner en Settings).
pub const DEFAULT_APP_ID: &str = "";

trait Transport: Read + Write + Send {}
impl<T: Read + Write + Send> Transport for T {}

#[derive(Clone, Debug, PartialEq)]
pub struct Activity {
    pub details: String,
    pub state: String,
    pub start: u64,
}

enum Message {
    Set(Option<Activity>),
    Stop,
}

/// Hilo en segundo plano que mantiene la conexión con Discord y reintenta si Discord se abre más tarde.
pub struct Presence {
    tx: Sender<Message>,
}

impl Presence {
    pub fn start(app_id: String) -> Option<Self> {
        if app_id.trim().is_empty() {
            return None;
        }
        let (tx, rx) = channel();
        std::thread::Builder::new().name("discord-rpc".into()).spawn(move || run(app_id, rx)).ok()?;
        Some(Self { tx })
    }

    pub fn set(&self, activity: Option<Activity>) {
        let _ = self.tx.send(Message::Set(activity));
    }
}

impl Drop for Presence {
    fn drop(&mut self) {
        let _ = self.tx.send(Message::Stop);
    }
}

fn run(app_id: String, rx: Receiver<Message>) {
    let mut wanted: Option<Activity> = None;
    let mut connection: Option<Box<dyn Transport>> = None;
    let mut dirty = true;
    loop {
        match rx.recv_timeout(Duration::from_secs(15)) {
            Ok(Message::Set(activity)) => {
                dirty |= activity != wanted;
                wanted = activity;
            }
            Ok(Message::Stop) | Err(RecvTimeoutError::Disconnected) => {
                if let Some(mut c) = connection.take() {
                    let _ = set_activity(&mut c, None);
                }
                return;
            }
            Err(RecvTimeoutError::Timeout) => {}
        }
        if connection.is_none() {
            connection = connect(&app_id);
            dirty = true;
        }
        if dirty {
            if let Some(c) = connection.as_mut() {
                if set_activity(c, wanted.as_ref()).is_ok() {
                    dirty = false;
                } else {
                    connection = None;
                }
            }
        }
    }
}

fn connect(app_id: &str) -> Option<Box<dyn Transport>> {
    for i in 0..10 {
        let Some(mut transport) = open(i) else { continue };
        if send(&mut transport, 0, &json!({"v": 1, "client_id": app_id})).is_ok() && read(&mut transport).is_ok() {
            return Some(transport);
        }
    }
    None
}

#[cfg(windows)]
fn open(index: u32) -> Option<Box<dyn Transport>> {
    let file = std::fs::OpenOptions::new().read(true).write(true).open(format!(r"\\.\pipe\discord-ipc-{index}")).ok()?;
    Some(Box::new(file))
}

#[cfg(unix)]
fn open(index: u32) -> Option<Box<dyn Transport>> {
    let name = format!("discord-ipc-{index}");
    let mut dirs = Vec::new();
    for var in ["XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"] {
        if let Some(value) = std::env::var_os(var) {
            let base = std::path::PathBuf::from(value);
            dirs.push(base.join("app/com.discordapp.Discord"));
            dirs.push(base.join("snap.discord"));
            dirs.push(base);
        }
    }
    dirs.push("/tmp".into());
    for dir in dirs {
        let path = dir.join(&name);
        if let Ok(stream) = std::os::unix::net::UnixStream::connect(&path) {
            let _ = stream.set_read_timeout(Some(Duration::from_secs(5)));
            return Some(Box::new(stream));
        }
    }
    None
}

fn set_activity(transport: &mut Box<dyn Transport>, activity: Option<&Activity>) -> std::io::Result<()> {
    let mut args = json!({"pid": std::process::id()});
    if let Some(a) = activity {
        let mut value = json!({
            "details": a.details,
            "timestamps": {"start": a.start},
            "assets": {"large_image": "logo", "large_text": "FreedomClient for Minecraft 1.21.11"},
        });
        if !a.state.is_empty() {
            value["state"] = Value::String(a.state.clone());
        }
        args["activity"] = value;
    }
    let nonce = uuid::Uuid::new_v4().to_string();
    send(transport, 1, &json!({"cmd": "SET_ACTIVITY", "args": args, "nonce": nonce}))?;
    read(transport).map(|_| ())
}

fn send(transport: &mut Box<dyn Transport>, opcode: u32, value: &Value) -> std::io::Result<()> {
    let payload = serde_json::to_vec(value)?;
    let mut frame = Vec::with_capacity(8 + payload.len());
    frame.extend_from_slice(&opcode.to_le_bytes());
    frame.extend_from_slice(&(payload.len() as u32).to_le_bytes());
    frame.extend_from_slice(&payload);
    transport.write_all(&frame)?;
    transport.flush()
}

fn read(transport: &mut Box<dyn Transport>) -> std::io::Result<Value> {
    let mut header = [0u8; 8];
    transport.read_exact(&mut header)?;
    let opcode = u32::from_le_bytes(header[..4].try_into().unwrap());
    let length = u32::from_le_bytes(header[4..].try_into().unwrap()) as usize;
    if length > 1 << 20 {
        return Err(std::io::Error::other("bad Discord frame"));
    }
    let mut body = vec![0u8; length];
    transport.read_exact(&mut body)?;
    if opcode == 2 {
        return Err(std::io::Error::other("Discord closed the connection"));
    }
    Ok(serde_json::from_slice(&body).unwrap_or(Value::Null))
}
