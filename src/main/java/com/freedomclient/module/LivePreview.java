package com.freedomclient.module;

import net.minecraft.client.CameraType;

/**
 * Mods cuyos cambios se ven en el propio juego (las manos, el campo de visión, el cielo...). Al abrir sus ajustes,
 * la ventana del menú se va a la izquierda y deja ver el juego a la derecha, que cambia en tiempo real al mover cada
 * opción.
 */
public interface LivePreview {
	/** Cámara con la que se ve mejor el cambio (se pone mientras están abiertos los ajustes), o null para no tocarla. */
	default CameraType previewCamera() {
		return null;
	}
}
