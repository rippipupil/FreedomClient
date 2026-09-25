//! Inicio de sesión con Microsoft (código de dispositivo) → Xbox Live → Minecraft.
//!
//! Necesita el ID de una aplicación de Azure que Microsoft haya aprobado para la API de Minecraft
//! (https://aka.ms/mce-reviewappid). Se configura en Settings → Advanced.

use anyhow::{Context, Result, anyhow, bail};
use serde::Deserialize;
use serde_json::json;

use crate::settings::{Account, now};

/// ID de aplicación de Azure por defecto (vacío hasta que Microsoft apruebe la del cliente).
pub const DEFAULT_CLIENT_ID: &str = "";
const SCOPE: &str = "XboxLive.signin offline_access";
const DEVICE_CODE_URL: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
const TOKEN_URL: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

#[derive(Clone, Debug, Deserialize, serde::Serialize)]
pub struct DeviceCode {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_in: u64,
    pub interval: u64,
    #[serde(default)]
    pub message: String,
}

#[derive(Deserialize)]
struct MsToken {
    access_token: String,
    refresh_token: Option<String>,
}

#[derive(Deserialize)]
struct MsError {
    error: String,
    error_description: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "PascalCase")]
struct XboxToken {
    token: String,
    display_claims: XboxClaims,
}

#[derive(Deserialize)]
struct XboxClaims {
    xui: Vec<serde_json::Map<String, serde_json::Value>>,
}

#[derive(Deserialize)]
struct McLogin {
    access_token: String,
    expires_in: u64,
}

#[derive(Deserialize)]
struct McProfile {
    id: String,
    name: String,
}

fn require_client_id(client_id: &str) -> Result<()> {
    if client_id.trim().is_empty() {
        bail!("Microsoft login needs an Azure application ID. Add it in Settings → Advanced.");
    }
    Ok(())
}

pub async fn start_device_code(http: &reqwest::Client, client_id: &str) -> Result<DeviceCode> {
    require_client_id(client_id)?;
    let response = http.post(DEVICE_CODE_URL).form(&[("client_id", client_id), ("scope", SCOPE)]).send().await?;
    if !response.status().is_success() {
        let error: MsError = response.json().await?;
        bail!("Microsoft refused the login: {}", error.error_description.unwrap_or(error.error));
    }
    Ok(response.json().await?)
}

/// Pregunta si el jugador ya ha puesto el código. `None` mientras siga esperando.
pub async fn poll_device_code(http: &reqwest::Client, client_id: &str, device_code: &str) -> Result<Option<Account>> {
    let response = http
        .post(TOKEN_URL)
        .form(&[
            ("client_id", client_id),
            ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
            ("device_code", device_code),
        ])
        .send()
        .await?;
    if !response.status().is_success() {
        let error: MsError = response.json().await?;
        return match error.error.as_str() {
            "authorization_pending" | "slow_down" => Ok(None),
            "expired_token" => Err(anyhow!("The code expired. Try again.")),
            "authorization_declined" => Err(anyhow!("The login was cancelled.")),
            _ => Err(anyhow!(error.error_description.unwrap_or(error.error))),
        };
    }
    let token: MsToken = response.json().await?;
    Ok(Some(minecraft_login(http, &token.access_token, token.refresh_token.unwrap_or_default()).await?))
}

/// Renueva la sesión de una cuenta de Microsoft si el token de Minecraft ha caducado (o va a caducar).
pub async fn refresh_if_needed(http: &reqwest::Client, client_id: &str, account: &Account) -> Result<Option<Account>> {
    let Account::Microsoft { refresh_token, expires_at, .. } = account else { return Ok(None) };
    if *expires_at > now() + 600 {
        return Ok(None);
    }
    require_client_id(client_id)?;
    let response = http
        .post(TOKEN_URL)
        .form(&[
            ("client_id", client_id),
            ("grant_type", "refresh_token"),
            ("refresh_token", refresh_token.as_str()),
            ("scope", SCOPE),
        ])
        .send()
        .await?;
    if !response.status().is_success() {
        bail!("Your Microsoft session expired. Log in again.");
    }
    let token: MsToken = response.json().await?;
    let refresh = token.refresh_token.unwrap_or_else(|| refresh_token.clone());
    Ok(Some(minecraft_login(http, &token.access_token, refresh).await?))
}

async fn minecraft_login(http: &reqwest::Client, ms_access_token: &str, refresh_token: String) -> Result<Account> {
    let xbl: XboxToken = http
        .post("https://user.auth.xboxlive.com/user/authenticate")
        .json(&json!({
            "Properties": {"AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com", "RpsTicket": format!("d={ms_access_token}")},
            "RelyingParty": "http://auth.xboxlive.com",
            "TokenType": "JWT"
        }))
        .send()
        .await?
        .error_for_status()
        .context("Xbox Live login failed")?
        .json()
        .await?;
    let uhs = xbl
        .display_claims
        .xui
        .first()
        .and_then(|c| c.get("uhs"))
        .and_then(|v| v.as_str())
        .context("Xbox Live did not return a user hash")?
        .to_string();

    let response = http
        .post("https://xsts.auth.xboxlive.com/xsts/authorize")
        .json(&json!({
            "Properties": {"SandboxId": "RETAIL", "UserTokens": [xbl.token]},
            "RelyingParty": "rp://api.minecraftservices.com/",
            "TokenType": "JWT"
        }))
        .send()
        .await?;
    if response.status().as_u16() == 401 {
        let body: serde_json::Value = response.json().await.unwrap_or_default();
        let message = match body.get("XErr").and_then(|v| v.as_u64()) {
            Some(2148916233) => "This Microsoft account has no Xbox profile. Create one at xbox.com and try again.",
            Some(2148916235) => "Xbox Live is not available in your country.",
            Some(2148916236) | Some(2148916237) => "This account needs adult verification on xbox.com.",
            Some(2148916238) => "This is a child account: an adult must add it to a Microsoft family.",
            _ => "Xbox Live refused the login.",
        };
        bail!(message);
    }
    let xsts: XboxToken = response.error_for_status()?.json().await?;
    let xuid = xsts
        .display_claims
        .xui
        .first()
        .and_then(|c| c.get("xid"))
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();

    let response = http
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&json!({"identityToken": format!("XBL3.0 x={uhs};{}", xsts.token)}))
        .send()
        .await?;
    if response.status().as_u16() == 403 {
        bail!("Minecraft refused this launcher's Azure application ID (it must be approved by Microsoft).");
    }
    let login: McLogin = response.error_for_status().context("Minecraft login failed")?.json().await?;

    let response = http.get("https://api.minecraftservices.com/minecraft/profile").bearer_auth(&login.access_token).send().await?;
    if response.status().as_u16() == 404 {
        bail!("This Microsoft account does not own Minecraft: Java Edition.");
    }
    let profile: McProfile = response.error_for_status().context("could not read the Minecraft profile")?.json().await?;
    Ok(Account::Microsoft {
        uuid: profile.id,
        name: profile.name,
        access_token: login.access_token,
        refresh_token,
        expires_at: now() + login.expires_in,
        xuid,
    })
}
