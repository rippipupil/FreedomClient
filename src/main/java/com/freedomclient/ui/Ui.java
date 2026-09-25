package com.freedomclient.ui;

import com.freedomclient.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interfaz en "modo inmediato": cada frame se dibuja todo y se registran las zonas clicables;
 * los clics del frame siguiente se comprueban contra esas zonas. Así las listas y animaciones
 * no necesitan widgets persistentes.
 */
public final class Ui {
	@FunctionalInterface
	public interface ClickHandler {
		boolean onClick(double mouseX, double mouseY, int button);
	}

	@FunctionalInterface
	public interface DragHandler {
		void onDrag(double mouseX, double mouseY);
	}

	@FunctionalInterface
	public interface ScrollHandler {
		void onScroll(double amount);
	}

	private record Rect(int x1, int y1, int x2, int y2) {
		boolean contains(double x, double y) {
			return x >= x1 && x < x2 && y >= y1 && y < y2;
		}

		Rect intersect(Rect other) {
			return new Rect(Math.max(x1, other.x1), Math.max(y1, other.y1), Math.min(x2, other.x2), Math.min(y2, other.y2));
		}
	}

	private record Hit(Rect rect, ClickHandler handler) {
	}

	private record ScrollHit(Rect rect, ScrollHandler handler) {
	}

	public final Minecraft minecraft = Minecraft.getInstance();
	public GuiGraphics g;
	public Font font;
	public int mouseX;
	public int mouseY;

	private final List<Hit> hits = new ArrayList<>();
	private final List<ScrollHit> scrolls = new ArrayList<>();
	private final Deque<Rect> clips = new ArrayDeque<>();
	private final Map<Object, Float> animations = new HashMap<>();
	private DragHandler drag;
	private Object focused;
	private KeybindSetting listeningKeybind;
	private String tooltip;
	private long lastFrameNanos;
	private float frameSeconds;

	public void begin(GuiGraphics graphics, int mouseX, int mouseY) {
		this.g = graphics;
		this.font = minecraft.font;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		hits.clear();
		scrolls.clear();
		clips.clear();
		tooltip = null;

		long now = System.nanoTime();
		frameSeconds = lastFrameNanos == 0 ? 0.0F : Math.min((now - lastFrameNanos) / 1.0E9F, 0.1F);
		lastFrameNanos = now;
	}

	public void end() {
		if (tooltip != null) {
			g.setTooltipForNextFrame(font, font.split(Component.literal(tooltip), 180), mouseX, mouseY);
		}
	}

	// Zonas clicables, desplazamiento y recorte.

	private Rect clipped(int x, int y, int w, int h) {
		Rect rect = new Rect(x, y, x + w, y + h);
		return clips.isEmpty() ? rect : rect.intersect(clips.peek());
	}

	public boolean hovered(int x, int y, int w, int h) {
		return drag == null && clipped(x, y, w, h).contains(mouseX, mouseY);
	}

	public void click(int x, int y, int w, int h, ClickHandler handler) {
		hits.add(new Hit(clipped(x, y, w, h), handler));
	}

	public void scroll(int x, int y, int w, int h, ScrollHandler handler) {
		scrolls.add(new ScrollHit(clipped(x, y, w, h), handler));
	}

	public void pushClip(int x, int y, int w, int h) {
		Rect rect = clipped(x, y, w, h);
		clips.push(rect);
		g.enableScissor(rect.x1, rect.y1, rect.x2, rect.y2);
	}

	public void popClip() {
		clips.pop();
		g.disableScissor();
	}

	public void startDrag(DragHandler handler, double mouseX, double mouseY) {
		drag = handler;
		handler.onDrag(mouseX, mouseY);
	}

	public void tooltip(String text) {
		tooltip = text;
	}

	public void playClick() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	// Foco de teclado (campos de texto y asignación de teclas).

	public void focus(Object target) {
		focused = target;
		listeningKeybind = null;
	}

	public boolean isFocused(Object target) {
		return focused == target;
	}

	public void listenForKey(KeybindSetting keybind) {
		focused = null;
		listeningKeybind = keybind;
	}

	public boolean hasTextFocus() {
		return focused instanceof TextField || listeningKeybind != null;
	}

	public boolean isListening(KeybindSetting keybind) {
		return listeningKeybind == keybind;
	}

	/** Valor animado que se acerca suavemente a {@code target}; {@code key} identifica la animación. */
	public float animate(Object key, float target) {
		float value = animations.getOrDefault(key, target);
		value += (target - value) * Math.min(1.0F, frameSeconds * 16.0F);
		if (Math.abs(target - value) < 0.001F) value = target;
		animations.put(key, value);
		return value;
	}

	// Eventos reenviados desde la pantalla.

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Object previousFocus = focused;
		focused = null;
		listeningKeybind = null;

		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit hit = hits.get(i);
			if (hit.rect.contains(mouseX, mouseY) && hit.handler.onClick(mouseX, mouseY, button)) {
				return true;
			}
		}
		return previousFocus != null;
	}

	public boolean mouseDragged(double mouseX, double mouseY) {
		if (drag == null) return false;
		drag.onDrag(mouseX, mouseY);
		return true;
	}

	public boolean mouseReleased() {
		boolean wasDragging = drag != null;
		drag = null;
		return wasDragging;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		for (int i = scrolls.size() - 1; i >= 0; i--) {
			ScrollHit hit = scrolls.get(i);
			if (hit.rect.contains(mouseX, mouseY)) {
				hit.handler.onScroll(amount);
				return true;
			}
		}
		return false;
	}

	/** Devuelve true si la tecla la ha usado un campo de texto o una asignación de tecla. */
	public boolean keyPressed(int key, boolean control) {
		if (listeningKeybind != null) {
			if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
				listeningKeybind.set(KeybindSetting.NONE);
			} else if (key != GLFW.GLFW_KEY_ESCAPE) {
				listeningKeybind.set(key);
			}
			listeningKeybind = null;
			return true;
		}

		if (focused instanceof TextField field) {
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
				focused = null;
				return true;
			}
			return field.keyPressed(key, control);
		}
		return false;
	}

	public boolean charTyped(String chars) {
		if (focused instanceof TextField field) {
			field.charTyped(chars);
			return true;
		}
		return false;
	}
}
