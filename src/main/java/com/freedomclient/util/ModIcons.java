package com.freedomclient.util;

import com.freedomclient.FreedomClient;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carga el icono original de un mod incluido (desde su propio .jar) como textura, reducido a {@link #SIZE} px
 * para que se vea nítido en las tarjetas del menú.
 */
public final class ModIcons {
	public static final int SIZE = 32;
	private static final int DEFAULT_TEXTURE_SIZE = 16;

	private static final Map<String, Identifier> ICONS = new HashMap<>();
	private static final Map<Identifier, Integer> TEXTURE_SIZES = new HashMap<>();

	private ModIcons() {
	}

	/** Icono del mod, o {@code fallback} si el mod no está cargado o no tiene icono. Llamar desde el hilo de render. */
	public static Identifier get(String modId, Identifier fallback) {
		Identifier icon = ICONS.computeIfAbsent(modId, ModIcons::load);
		return icon != null ? icon : fallback;
	}

	private static final Map<Identifier, Identifier> FALLBACKS = new HashMap<>();

	/** Devuelve el icono si existe en los recursos, o el engranaje genérico si no. */
	public static Identifier orFallback(Identifier icon) {
		return FALLBACKS.computeIfAbsent(icon, id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent()
				? id : FreedomClient.id("textures/icon/gear.png"));
	}

	/** Tamaño en píxeles de la textura de un icono (los iconos propios son de 16x16). */
	public static int textureSize(Identifier icon) {
		return TEXTURE_SIZES.getOrDefault(icon, DEFAULT_TEXTURE_SIZE);
	}

	private static Identifier load(String modId) {
		Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
		if (container.isEmpty()) return null;

		Optional<String> iconPath = container.get().getMetadata().getIconPath(128);
		if (iconPath.isEmpty()) return null;

		Optional<Path> path = container.get().findPath(iconPath.get());
		if (path.isEmpty()) return null;

		try (InputStream stream = Files.newInputStream(path.get()); NativeImage original = NativeImage.read(stream)) {
			NativeImage scaled = downscale(original, SIZE);
			Identifier id = FreedomClient.id("mod_icon/" + modId);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "FreedomClient mod icon " + modId, scaled));
			TEXTURE_SIZES.put(id, SIZE);
			return id;
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not load the icon of {}", modId, e);
			return null;
		}
	}

	/** Reduce la imagen promediando bloques de píxeles (con alfa premultiplicado para no oscurecer los bordes). */
	private static NativeImage downscale(NativeImage source, int size) {
		NativeImage result = new NativeImage(size, size, false);
		int width = source.getWidth();
		int height = source.getHeight();

		for (int y = 0; y < size; y++) {
			int y0 = y * height / size;
			int y1 = Math.max(y0 + 1, (y + 1) * height / size);
			for (int x = 0; x < size; x++) {
				int x0 = x * width / size;
				int x1 = Math.max(x0 + 1, (x + 1) * width / size);

				long a = 0, r = 0, g = 0, b = 0;
				int count = 0;
				for (int sy = y0; sy < y1; sy++) {
					for (int sx = x0; sx < x1; sx++) {
						int argb = source.getPixel(sx, sy);
						int alpha = argb >>> 24;
						a += alpha;
						r += (long) ((argb >> 16) & 0xFF) * alpha;
						g += (long) ((argb >> 8) & 0xFF) * alpha;
						b += (long) (argb & 0xFF) * alpha;
						count++;
					}
				}

				int argb = 0;
				if (a > 0) {
					argb = (int) (a / count) << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
				}
				result.setPixel(x, y, argb);
			}
		}
		return result;
	}
}
