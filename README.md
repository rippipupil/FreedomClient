# FreedomClient

Cliente de Minecraft **1.21.11** hecho como mod de [Fabric](https://fabricmc.net/).

## Rendimiento

FreedomClient está pensado para dar el máximo de FPS. Trae integrados estos mods (no hace falta instalarlos aparte):

| Mod | Qué mejora |
|---|---|
| [Sodium](https://modrinth.com/mod/sodium) | Motor de renderizado nuevo: suele multiplicar los FPS |
| [Lithium](https://modrinth.com/mod/lithium) | Optimiza la física, la IA de mobs y la lógica del juego |
| [FerriteCore](https://modrinth.com/mod/ferrite-core) | Reduce mucho el uso de memoria RAM |
| [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) | Acelera el dibujado de HUD, texto, mapas y entidades |

También van incluidos [Continuity](https://modrinth.com/mod/continuity) (texturas conectadas),
[Debugify](https://modrinth.com/mod/debugify) (arregla bugs de vanilla), [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks)
(mejores controles del inventario) y [Fast IP Ping](https://modrinth.com/mod/fast-ip-ping) (lista de servidores más rápida).

Además, en la ClickGUI hay un botón **"Optimizar ajustes para FPS"**. Desactiva VSync, las nubes, las sombras de
entidades y la mezcla de biomas, quita el límite de FPS y pone las partículas al mínimo.

Licencias de los mods integrados: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Funciones

Abre el menú de módulos (ClickGUI) con **Shift derecho**.

| Módulo | Categoría | Tecla | Descripción |
|---|---|---|---|
| Watermark | HUD | — | Nombre y versión del cliente con color arcoíris |
| FPS | HUD | — | Fotogramas por segundo |
| Ping | HUD | — | Latencia con el servidor |
| Coordinates | HUD | — | Coordenadas XYZ y dirección |
| Keystrokes | HUD | — | W A S D, clics y espacio en pantalla |
| ArmorStatus | HUD | — | Armadura, objeto en mano y durabilidad |
| ModuleList | HUD | — | Lista de módulos activos (arriba a la derecha) |
| Fullbright | Render | G | Ilumina todo |
| Zoom | Render | C (mantener) | Zoom con cámara suave |
| ToggleSprint | Movimiento | V | Corre sin mantener la tecla |

Todas las teclas se pueden cambiar en *Opciones → Controles → Asignación de teclas → FreedomClient*.
Los módulos activados se guardan en `config/freedomclient.json`.

## Descarga

**[⬇ Descargar FreedomClient-1.21.11.jar](https://github.com/rippipupil/FreedomClient/releases/download/latest/FreedomClient-1.21.11.jar)**
(siempre es la última versión compilada)

## Launcher

**[⬇ Descargar FreedomClient Launcher para Windows](https://github.com/rippipupil/FreedomClient/releases/download/launcher/FreedomClient-Launcher-Setup.exe)**
([versión portable](https://github.com/rippipupil/FreedomClient/releases/download/launcher/FreedomClient-Launcher-Portable.exe))

El launcher instala Minecraft 1.21.11, Fabric, Java 21 y FreedomClient él solo y los mantiene actualizados.
Tiene perfiles (cada uno con su carpeta `.minecraft`, sus mods externos y su servidor para entrar directo),
botones para abrir la carpeta del juego y el log, Discord Rich Presence y ajustes de RAM y Java.
Por defecto, **Launch** prepara FreedomClient en el launcher oficial de Minecraft (Fabric, un perfil "FreedomClient"
con tus mods, la RAM y los flags de Java optimizados) y lo abre con ese perfil elegido: solo hay que darle a Jugar,
y el inicio de sesión es el de Mojang. En Configuración → Launch with se puede cambiar a lanzar directamente desde
FreedomClient (necesita el ID de aplicación de Azure para el login de Microsoft).
Para llevar tu configuración a otro PC: en Perfiles, **Export** guarda el perfil en un archivo `.fcprofile`
(opciones y teclas de Minecraft, la configuración de todos los mods, la lista de servidores y, si quieres, los mods,
resource packs y shaders; nunca los mundos) y en el otro PC **Import** (o arrastrar el archivo a la ventana) lo
crea como un perfil nuevo.
Viene configurado para gastar lo mínimo: memoria automática según tu PC, G1 afinado para que no haya tirones,
opciones del juego pensadas para FPS y el launcher se cierra al empezar la partida.

Código en [`launcher/`](launcher): `core` (Rust: descargas, instalación y lanzamiento) y `app` (Tauri + la interfaz en `ui`).

## Instalación manual

1. Instala [Fabric Loader](https://fabricmc.net/use/installer/) para Minecraft 1.21.11.
2. Pon `FreedomClient-1.21.11.jar` en `.minecraft/mods`. Fabric API y los mods de rendimiento ya van dentro.
3. Si ya tenías Sodium, Lithium, etc. en la carpeta `mods`, puedes borrarlos: Fabric usará la versión más nueva de cada uno.

Consejo: dale 4 GB de RAM al juego (`-Xmx4G` en los argumentos de Java del launcher). Más memoria no da más FPS.

## Compilar

Requiere Java 21.

```sh
./gradlew build          # el .jar queda en build/libs/
./gradlew runClient      # abre Minecraft con el cliente para probarlo
```

Cada push también se compila en GitHub Actions y el `.jar` queda como artefacto de la ejecución.

## Añadir un módulo

1. Crea una clase que extienda `Module` (o `TextHudModule` para una línea de texto en el HUD) en `src/main/java/com/freedomclient/module/...`.
2. Sobrescribe `onTick`, `onEnable` y/o `onDisable`.
3. Regístrala en el constructor de `ModuleManager`.
