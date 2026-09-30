package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.ActionSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.setting.PreviewSetting;
import com.freedomclient.ui.theme.ThemeManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * INV: cambia el aspecto del inventario (y, si quieres, de cofres, barriles, cofres de ender y cajas de shulker).
 * Fondo con tu propia imagen, colores del fondo, del borde, de las casillas y del texto, grosor del borde y
 * esquinas redondeadas. Los colores pueden seguir el tema del menú. La imagen se guarda en
 * .minecraft/freedomclient/inventory/background.png.
 */
public class InvModule extends Module {
	private static final Identifier IMAGE = FreedomClient.id("inv/background");
	/** Lado máximo de la imagen: las más grandes se reducen para no gastar memoria de vídeo. */
	private static final int MAX_IMAGE = 1024;
	private static InvModule instance;

	private final ModeSetting screens = add(new ModeSetting("Screens", "Where the custom look is used.",
			"Inventory and chests", "Inventory and chests", "Inventory only"));
	private final ModeSetting colors = add(new ModeSetting("Colors", "Use the colors of the menu theme or your own.",
			"Menu theme", "Menu theme", "Custom"));
	private final ModeSetting corners = add(new ModeSetting("Corners", "Shape of the window corners.", "Rounded", "Rounded", "Square"));
	private final ModeSetting imageFit = add(new ModeSetting("Image fit",
			"Fill: covers the whole window (cuts the edges). Stretch: squeezes the whole image in. Fit: the whole image, centered.",
			"Fill", "Fill", "Stretch", "Fit"));
	private final PreviewSetting preview = add(new PreviewSetting("Preview", "How your inventory will look.", 120, this::renderPreview));
	private final NumberSetting imageZoom = add(new NumberSetting("Image zoom", "Make the picture bigger to show only a part of it.", 100, 100, 300, 5, "%"));
	private final NumberSetting imageX = add(new NumberSetting("Image left/right", "Move the picture to show more of its left or right side.", 0, -100, 100, 5, "%"));
	private final NumberSetting imageY = add(new NumberSetting("Image up/down", "Move the picture to show more of its top or bottom.", 0, -100, 100, 5, "%"));
	private final NumberSetting borderSize = add(new NumberSetting("Border size", "Thickness of the window border.", 2, 1, 4, 1, " px"));
	private final NumberSetting imageOpacity = add(new NumberSetting("Image opacity", "How visible the background image is.", 70, 10, 100, 5, "%"));
	private final ColorSetting background = add(new ColorSetting("Background", "Color of the window (under the image).", 0xE83A0F1A, true));
	private final ColorSetting border = add(new ColorSetting("Border", "Color of the window border.", 0xFFFF8C42, false));
	private final ColorSetting slot = add(new ColorSetting("Slots", "Color inside the item slots.", 0xB0140407, true));
	private final ColorSetting slotBorder = add(new ColorSetting("Slot border", "Color of the slot outlines.", 0xFF7A1F2B, true));
	private final ColorSetting text = add(new ColorSetting("Text", "Color of the titles (Crafting, Inventory, chest name...).", 0xFFF5F1E8, false));

	private DynamicTexture texture;
	private int imageWidth;
	private int imageHeight;
	private boolean loaded;

	public InvModule() {
		super("INV", "Customize your inventory and chests: your own background image and the colors of the border, slots and text.",
				Category.VISUAL, false);
		instance = this;
		for (ColorSetting color : new ColorSetting[] {background, border, slot, slotBorder, text}) {
			color.visibleWhen(() -> colors.is("Custom"));
		}
		imageFit.visibleWhen(this::hasImage);
		imageOpacity.visibleWhen(this::hasImage);
		imageZoom.visibleWhen(() -> hasImage() && !imageFit.is("Stretch"));
		imageX.visibleWhen(() -> hasImage() && !imageFit.is("Stretch"));
		imageY.visibleWhen(() -> hasImage() && !imageFit.is("Stretch"));
		add(new ActionSetting("Background image", "Pick a picture (PNG or JPG) for the inventory background.", "Choose", this::chooseImage));
		ActionSetting remove = add(new ActionSetting("Remove image", "Go back to a plain colored background.", "Remove", this::removeImage));
		remove.visibleWhen(this::hasImage);
		add(new ActionSetting("Image folder", "Open the folder where the image is saved (inventory/background.png).", "Open",
				() -> Util.getPlatform().openPath(folder())));
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	// ---------- Cuándo se usa ----------

	/** El módulo si está activo y cambia el aspecto de esta pantalla, o null. */
	public static InvModule active(Screen screen) {
		InvModule module = instance;
		if (module == null || !module.isEnabled()) return null;
		if (screen instanceof InventoryScreen) return module;
		if (module.screens.is("Inventory and chests") && (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen)) return module;
		return null;
	}

	/** El módulo si está activo (para dibujar otras ventanas con su estilo, como la vista previa de las shulkers), o null. */
	public static InvModule styled() {
		InvModule module = instance;
		return module != null && module.isEnabled() ? module : null;
	}

	// ---------- Colores ----------

	private boolean themed() {
		return colors.is("Menu theme");
	}

	int backgroundColor() {
		return themed() ? ThemeManager.withAlpha(ThemeManager.background(), 0.92F) : background.get();
	}

	int borderColor() {
		return themed() ? ThemeManager.border() : border.get();
	}

	int slotColor() {
		return themed() ? ThemeManager.withAlpha(ThemeManager.shade(), 0.75F) : slot.get();
	}

	int slotBorderColor() {
		return themed() ? ThemeManager.withAlpha(ThemeManager.mix(ThemeManager.border(), 0xFF000000, 0.35F), 0.9F) : slotBorder.get();
	}

	public int textColor() {
		return themed() ? ThemeManager.text() : text.get();
	}

	// ---------- Dibujo ----------

	/**
	 * Dibuja la ventana entera en lugar de la textura del juego: fondo, imagen, borde, casillas y, en el inventario,
	 * el recuadro del jugador y la flecha de fabricar. Las cosas (objetos, jugador) se dibujan encima después.
	 */
	public void drawWindow(GuiGraphics g, int x, int y, int w, int h, List<Slot> slots, boolean playerInventory) {
		int thickness = borderSize.getInt();
		boolean rounded = corners.is("Rounded");
		// Fondo (sin las esquinas si son redondeadas).
		fillRounded(g, x, y, w, h, backgroundColor(), rounded ? thickness + 1 : 0);
		drawImage(g, x + thickness, y + thickness, w - thickness * 2, h - thickness * 2);
		frame(g, x, y, w, h, thickness, borderColor(), rounded);
		if (playerInventory) {
			// Recuadro donde se ve al jugador y flecha de la mesa de crafteo de 2x2.
			g.fill(x + 26, y + 8, x + 75, y + 78, ThemeManager.mix(slotColor(), 0xFF000000, 0.5F));
			frame(g, x + 25, y + 7, 51, 72, 1, slotBorderColor(), false);
			arrow(g, x + 135, y + 30, slotBorderColor());
		}
		for (Slot s : slots) {
			if (!s.isActive()) continue;
			int sx = x + s.x - 1;
			int sy = y + s.y - 1;
			g.fill(sx + 1, sy + 1, sx + 17, sy + 17, slotColor());
			frame(g, sx, sy, 18, 18, 1, slotBorderColor(), false);
		}
	}

	/** Relleno con las esquinas recortadas en escalera de {@code cut} píxeles. */
	private static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color, int cut) {
		if (cut <= 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x, y + cut, x + w, y + h - cut, color);
		for (int i = 0; i < cut; i++) {
			int inset = cut - i;
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
		}
	}

	/** Borde de {@code t} píxeles; con esquinas redondeadas se recortan como en el menú del cliente. */
	private static void frame(GuiGraphics g, int x, int y, int w, int h, int t, int color, boolean rounded) {
		int c = rounded ? t : 0;
		g.fill(x + c, y, x + w - c, y + t, color);
		g.fill(x + c, y + h - t, x + w - c, y + h, color);
		g.fill(x, y + c, x + t, y + h - c, color);
		g.fill(x + w - t, y + c, x + w, y + h - c, color);
		if (rounded) {
			// Esquinas en diagonal: un escalón de t píxeles hacia dentro.
			g.fill(x + t, y + t, x + t * 2, y + t * 2, color);
			g.fill(x + w - t * 2, y + t, x + w - t, y + t * 2, color);
			g.fill(x + t, y + h - t * 2, x + t * 2, y + h - t, color);
			g.fill(x + w - t * 2, y + h - t * 2, x + w - t, y + h - t, color);
		}
	}

	/** Flecha pixel hacia la derecha (16x13) como la de la mesa de crafteo. */
	private static void arrow(GuiGraphics g, int x, int y, int color) {
		g.fill(x, y + 5, x + 10, y + 8, color);
		for (int i = 0; i < 7; i++) {
			g.fill(x + 9 + i, y + i, x + 10 + i, y + 13 - i, color);
		}
	}

	private void drawImage(GuiGraphics g, int x, int y, int w, int h) {
		ensureLoaded();
		if (texture == null || w <= 0 || h <= 0) return;
		int alpha = Math.round(imageOpacity.getFloat() / 100.0F * 255.0F);
		int color = alpha << 24 | 0xFFFFFF;
		switch (imageFit.get()) {
			case "Stretch" -> g.blit(RenderPipelines.GUI_TEXTURED, IMAGE, x, y, 0.0F, 0.0F, w, h, imageWidth, imageHeight, imageWidth, imageHeight, color);
			case "Fit" -> {
				// Entera y centrada; con zoom crece desde el centro y lo que se sale de la ventana se recorta.
				float scale = Math.min(w / (float) imageWidth, h / (float) imageHeight) * zoom();
				drawScaled(g, x, y, w, h, scale, color);
			}
			default -> {
				// Cubre toda la ventana con el centro de la imagen en el centro de la ventana (más el desplazamiento elegido).
				float scale = Math.max(w / (float) imageWidth, h / (float) imageHeight) * zoom();
				drawScaled(g, x, y, w, h, scale, color);
			}
		}
	}

	private float zoom() {
		return imageZoom.getFloat() / 100.0F;
	}

	/**
	 * Dibuja la imagen a escala {@code scale} (px de ventana por px de imagen) centrada en la ventana y movida según
	 * los ajustes, recortando lo que queda fuera. Así el centro de la imagen cae siempre en el centro de la ventana.
	 */
	private void drawScaled(GuiGraphics g, int x, int y, int w, int h, float scale, int color) {
		float drawnW = imageWidth * scale;
		float drawnH = imageHeight * scale;
		// Desplazamiento: 100% lleva el borde de la imagen al borde de la ventana (si sobra imagen para moverse).
		float left = x + (w - drawnW) / 2.0F - imageX.getFloat() / 100.0F * Math.abs(drawnW - w) / 2.0F;
		float top = y + (h - drawnH) / 2.0F - imageY.getFloat() / 100.0F * Math.abs(drawnH - h) / 2.0F;
		// Parte visible, en coordenadas de la ventana y de la imagen.
		float x0 = Math.max(x, left);
		float y0 = Math.max(y, top);
		float x1 = Math.min(x + w, left + drawnW);
		float y1 = Math.min(y + h, top + drawnH);
		int dx = Math.round(x0);
		int dy = Math.round(y0);
		int dw = Math.round(x1) - dx;
		int dh = Math.round(y1) - dy;
		if (dw <= 0 || dh <= 0) return;
		float u = (dx - left) / scale;
		float v = (dy - top) / scale;
		int uw = Math.max(1, Math.round(dw / scale));
		int vh = Math.max(1, Math.round(dh / scale));
		g.blit(RenderPipelines.GUI_TEXTURED, IMAGE, dx, dy, u, v, dw, dh, uw, vh, imageWidth, imageHeight, color);
	}

	/**
	 * Vista previa: el inventario de verdad (176x166, con sus casillas en su sitio) a escala, para que la imagen se
	 * recorte y se centre exactamente igual que al abrirlo en el juego.
	 */
	private void renderPreview(GuiGraphics g, int x, int y, int width, int height) {
		int w = 176;
		int h = 166;
		float scale = Math.min(1.0F, Math.min(width / (float) w, height / (float) h));
		java.util.List<Slot> fake = new java.util.ArrayList<>();
		net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(46);
		int index = 0;
		for (int i = 0; i < 4; i++) fake.add(new Slot(container, index++, 8, 8 + i * 18));
		fake.add(new Slot(container, index++, 77, 62));
		for (int i = 0; i < 4; i++) fake.add(new Slot(container, index++, 98 + (i % 2) * 18, 18 + (i / 2) * 18));
		fake.add(new Slot(container, index++, 154, 28));
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) fake.add(new Slot(container, index++, 8 + col * 18, 84 + row * 18));
		}
		for (int col = 0; col < 9; col++) fake.add(new Slot(container, index++, 8 + col * 18, 142));
		g.pose().pushMatrix();
		g.pose().translate(x + (width - w * scale) / 2.0F, y);
		g.pose().scale(scale, scale);
		drawWindow(g, 0, 0, w, h, fake, true);
		g.drawString(Minecraft.getInstance().font, "Crafting", 97, 6, textColor(), false);
		g.pose().popMatrix();
	}

	// ---------- Imagen ----------

	private static Path folder() {
		Path path = FabricLoader.getInstance().getGameDir().resolve(FreedomClient.MOD_ID).resolve("inventory");
		try {
			Files.createDirectories(path);
		} catch (Exception ignored) {
		}
		return path;
	}

	private static Path imagePath() {
		return folder().resolve("background.png");
	}

	/** Vuelve a leer la imagen guardada (por si ha cambiado el archivo). */
	public void reloadImage() {
		loaded = false;
		texture = null;
		ensureLoaded();
	}

	private boolean hasImage() {
		ensureLoaded();
		return texture != null;
	}

	/** Carga la imagen guardada la primera vez que hace falta (en el hilo de render). */
	private void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		Path path = imagePath();
		if (!Files.isRegularFile(path)) return;
		try (InputStream input = Files.newInputStream(path)) {
			setImage(NativeImage.read(input));
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not load the inventory image {}", path, e);
		}
	}

	private void setImage(NativeImage image) {
		NativeImage scaled = downscale(image);
		imageWidth = scaled.getWidth();
		imageHeight = scaled.getHeight();
		// Registrar con el mismo nombre sustituye (y cierra) la imagen anterior.
		texture = new DynamicTexture(() -> "FreedomClient inventory image", scaled);
		Minecraft.getInstance().getTextureManager().register(IMAGE, texture);
	}

	/** Reduce las imágenes muy grandes (vecino más cercano) para que no pesen en la memoria de vídeo. */
	private static NativeImage downscale(NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		if (w <= MAX_IMAGE && h <= MAX_IMAGE) return image;
		float scale = MAX_IMAGE / (float) Math.max(w, h);
		int nw = Math.max(1, Math.round(w * scale));
		int nh = Math.max(1, Math.round(h * scale));
		NativeImage out = new NativeImage(nw, nh, false);
		for (int y = 0; y < nh; y++) {
			for (int x = 0; x < nw; x++) {
				out.setPixel(x, y, image.getPixel(Math.min(w - 1, (int) (x / scale)), Math.min(h - 1, (int) (y / scale))));
			}
		}
		image.close();
		return out;
	}

	/** Abre el selector de archivos del sistema; la imagen elegida se guarda como PNG en la carpeta del cliente. */
	private void chooseImage() {
		String picked;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer filters = stack.mallocPointer(4);
			filters.put(stack.UTF8("*.png"));
			filters.put(stack.UTF8("*.jpg"));
			filters.put(stack.UTF8("*.jpeg"));
			filters.put(stack.UTF8("*.bmp"));
			filters.flip();
			picked = TinyFileDialogs.tinyfd_openFileDialog("Choose the inventory image", "", filters, "Images (PNG, JPG)", false);
		}
		if (picked == null || picked.isBlank()) return;
		Path source = Path.of(picked);
		try {
			NativeImage image = read(source);
			image.writeToFile(imagePath());
			loaded = true;
			setImage(image);
			message("§6INV:§r background image set.");
		} catch (Throwable e) {
			FreedomClient.LOGGER.warn("Could not read the image {}", source, e);
			message("§6INV:§r that image could not be read. Try a PNG file.");
		}
	}

	/** PNG con el lector del juego; JPG y otros con el de Java, convertidos píxel a píxel. */
	private static NativeImage read(Path source) throws Exception {
		String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
		if (name.endsWith(".png")) {
			try (InputStream input = Files.newInputStream(source)) {
				return NativeImage.read(input);
			}
		}
		java.awt.image.BufferedImage buffered = javax.imageio.ImageIO.read(source.toFile());
		if (buffered == null) throw new IllegalArgumentException("Unsupported image");
		NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);
		for (int y = 0; y < buffered.getHeight(); y++) {
			for (int x = 0; x < buffered.getWidth(); x++) {
				image.setPixel(x, y, buffered.getRGB(x, y));
			}
		}
		return image;
	}

	private void removeImage() {
		try {
			Files.deleteIfExists(imagePath());
		} catch (Exception e) {
			FreedomClient.LOGGER.warn("Could not delete the inventory image", e);
		}
		texture = null;
	}

	private static void message(String text) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) client.player.displayClientMessage(Component.literal(text), false);
	}
}
