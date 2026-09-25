# FreedomClient

Cliente de Minecraft **1.21.11** hecho como mod de [Fabric](https://fabricmc.net/).

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

## Instalación

1. Instala [Fabric Loader](https://fabricmc.net/use/installer/) para Minecraft 1.21.11.
2. Descarga [Fabric API](https://modrinth.com/mod/fabric-api) para 1.21.11 y ponlo en `.minecraft/mods`.
3. Pon `freedomclient-<versión>.jar` en `.minecraft/mods`.

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
