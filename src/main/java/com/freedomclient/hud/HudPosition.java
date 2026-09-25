package com.freedomclient.hud;

import com.google.gson.JsonObject;
import net.minecraft.util.Mth;

/**
 * Posición de un elemento del HUD. Cada eje se ancla al borde más cercano (izquierda/centro/derecha),
 * así el elemento se queda "pegado" a su esquina aunque cambie la resolución o la escala de la interfaz.
 */
public class HudPosition {
	public enum Anchor { START, CENTER, END }

	public static final float MIN_SCALE = 0.5F;
	public static final float MAX_SCALE = 3.0F;

	private Anchor anchorX;
	private Anchor anchorY;
	private int offsetX;
	private int offsetY;
	private float scale;

	private final Anchor defaultAnchorX;
	private final Anchor defaultAnchorY;
	private final int defaultOffsetX;
	private final int defaultOffsetY;

	public HudPosition(Anchor anchorX, int offsetX, Anchor anchorY, int offsetY) {
		this.defaultAnchorX = anchorX;
		this.defaultAnchorY = anchorY;
		this.defaultOffsetX = offsetX;
		this.defaultOffsetY = offsetY;
		reset();
	}

	public void reset() {
		anchorX = defaultAnchorX;
		anchorY = defaultAnchorY;
		offsetX = defaultOffsetX;
		offsetY = defaultOffsetY;
		scale = 1.0F;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = Mth.clamp(Math.round(scale * 20.0F) / 20.0F, MIN_SCALE, MAX_SCALE);
	}

	/** Coordenada X absoluta de la esquina superior izquierda para un elemento de ancho {@code width} (ya escalado). */
	public int resolveX(int screenWidth, int width) {
		return resolve(anchorX, offsetX, screenWidth, width);
	}

	public int resolveY(int screenHeight, int height) {
		return resolve(anchorY, offsetY, screenHeight, height);
	}

	private static int resolve(Anchor anchor, int offset, int screenSize, int size) {
		int value = switch (anchor) {
			case START -> offset;
			case CENTER -> screenSize / 2 + offset - size / 2;
			case END -> screenSize - offset - size;
		};
		return Mth.clamp(value, 0, Math.max(0, screenSize - size));
	}

	/** Coloca el elemento en una posición absoluta, eligiendo el ancla según el tercio de la pantalla. */
	public void setAbsolute(int x, int y, int width, int height, int screenWidth, int screenHeight) {
		anchorX = anchorFor(x + width / 2, screenWidth);
		anchorY = anchorFor(y + height / 2, screenHeight);
		offsetX = offsetFor(anchorX, x, width, screenWidth);
		offsetY = offsetFor(anchorY, y, height, screenHeight);
	}

	private static Anchor anchorFor(int center, int screenSize) {
		if (center < screenSize / 3) return Anchor.START;
		if (center > screenSize * 2 / 3) return Anchor.END;
		return Anchor.CENTER;
	}

	private static int offsetFor(Anchor anchor, int position, int size, int screenSize) {
		return switch (anchor) {
			case START -> position;
			case CENTER -> position + size / 2 - screenSize / 2;
			case END -> screenSize - position - size;
		};
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("anchorX", anchorX.name());
		json.addProperty("anchorY", anchorY.name());
		json.addProperty("x", offsetX);
		json.addProperty("y", offsetY);
		json.addProperty("scale", scale);
		return json;
	}

	public void fromJson(JsonObject json) {
		try {
			anchorX = Anchor.valueOf(json.get("anchorX").getAsString());
			anchorY = Anchor.valueOf(json.get("anchorY").getAsString());
			offsetX = json.get("x").getAsInt();
			offsetY = json.get("y").getAsInt();
			setScale(json.get("scale").getAsFloat());
		} catch (RuntimeException e) {
			reset();
		}
	}
}
