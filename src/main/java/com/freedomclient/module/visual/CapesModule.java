package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Capes: muestra las capas de OptiFine de los jugadores que no tienen capa de Minecraft. */
public class CapesModule extends Module {
	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	private static final String OPTIFINE_URL = "http://s.optifine.net/capes/%s.png";
	private static final int MAX_CACHED = 512;
	private static CapesModule instance;

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	/** Nombre en minúsculas -> capa descargada, o NONE si no tiene / aún se está descargando. */
	private final Map<String, ClientAsset.Texture> capes = new ConcurrentHashMap<>();
	private static final ClientAsset.Texture NONE = new ClientAsset.ResourceTexture(FreedomClient.id("capes/none"), FreedomClient.id("capes/none"));

	public CapesModule() {
		super("Capes", "Shows the OptiFine capes of other players.", Category.VISUAL, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Capa de OptiFine del jugador, o null si no tiene o todavía no se ha descargado. */
	public static ClientAsset.Texture capeFor(String name) {
		if (instance == null || !instance.isEnabled() || !VALID_NAME.matcher(name).matches()) return null;
		String key = name.toLowerCase(Locale.ROOT);
		ClientAsset.Texture cape = instance.capes.get(key);
		if (cape == null) {
			if (instance.capes.size() >= MAX_CACHED) return null;
			instance.capes.put(key, NONE);
			instance.download(key);
			return null;
		}
		return cape == NONE ? null : cape;
	}

	private void download(String name) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(OPTIFINE_URL, name)))
				.timeout(Duration.ofSeconds(10)).GET().build();
		http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(response -> {
			if (response.statusCode() != 200) return;
			try {
				NativeImage image = toCapeLayout(NativeImage.read(response.body()));
				Identifier id = FreedomClient.id("capes/" + name);
				Minecraft client = Minecraft.getInstance();
				client.execute(() -> {
					client.getTextureManager().register(id, new DynamicTexture(() -> "OptiFine cape of " + name, image));
					capes.put(name, new ClientAsset.ResourceTexture(id, id));
				});
			} catch (Exception e) {
				FreedomClient.LOGGER.debug("Could not load the OptiFine cape of {}", name, e);
			}
		});
	}

	/** Las capas de OptiFine miden 46x22 (o múltiplos); se copian a un lienzo 64x32 como las de Minecraft. */
	private static NativeImage toCapeLayout(NativeImage source) {
		int width = 64;
		int height = 32;
		while (width < source.getWidth() || height < source.getHeight()) {
			width *= 2;
			height *= 2;
		}
		if (width == source.getWidth() && height == source.getHeight()) return source;

		NativeImage result = new NativeImage(width, height, true);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				result.setPixel(x, y, source.getPixel(x, y));
			}
		}
		source.close();
		return result;
	}

	@Override
	protected void onDisable(Minecraft client) {
		capes.clear();
	}
}
