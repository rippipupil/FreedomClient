package com.freedomclient.ui.theme;

import com.freedomclient.setting.ColorSetting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/** Tema actual del menú: un preset más los colores que el jugador haya cambiado encima. */
public final class ThemeManager {
	private static ThemePreset preset = ThemePreset.RED_SUNSET;
	private static final Map<ThemeColor, Integer> OVERRIDES = new EnumMap<>(ThemeColor.class);
	/** Opacidad del fondo de la ventana (0..1). */
	private static float opacity = 0.92F;

	private ThemeManager() {
	}

	public static int get(ThemeColor color) {
		return OVERRIDES.getOrDefault(color, preset.get(color));
	}

	public static ThemePreset getPreset() {
		return preset;
	}

	/** Cambia de preset y descarta los colores personalizados. */
	public static void setPreset(ThemePreset newPreset) {
		preset = newPreset;
		OVERRIDES.clear();
	}

	public static void setColor(ThemeColor color, int argb) {
		OVERRIDES.put(color, 0xFF000000 | argb);
	}

	public static boolean isCustomized(ThemeColor color) {
		return OVERRIDES.containsKey(color);
	}

	public static void resetColors() {
		OVERRIDES.clear();
	}

	public static float getOpacity() {
		return opacity;
	}

	public static void setOpacity(float value) {
		opacity = Mth.clamp(value, 0.3F, 1.0F);
	}

	// Colores derivados que usa el menú.

	public static int background() {
		return withAlpha(get(ThemeColor.BACKGROUND), opacity);
	}

	public static int card() {
		return get(ThemeColor.CARD);
	}

	public static int cardHover() {
		return mix(get(ThemeColor.CARD), get(ThemeColor.HIGHLIGHT), 0.18F);
	}

	public static int border() {
		return get(ThemeColor.BORDER);
	}

	public static int accent() {
		return get(ThemeColor.ACCENT);
	}

	public static int highlight() {
		return get(ThemeColor.HIGHLIGHT);
	}

	public static int text() {
		return get(ThemeColor.TEXT);
	}

	public static int textMuted() {
		return mix(get(ThemeColor.TEXT), get(ThemeColor.CARD), 0.4F);
	}

	/** Color oscuro para sombras y huecos (fondo del interruptor apagado, sombra de la ventana). */
	public static int shade() {
		return mix(get(ThemeColor.BACKGROUND), 0xFF000000, 0.5F);
	}

	public static int withAlpha(int argb, float alpha) {
		return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (argb & 0xFFFFFF);
	}

	/** Mezcla dos colores ARGB; {@code t = 0} devuelve {@code a} y {@code t = 1} devuelve {@code b}. */
	public static int mix(int a, int b, float t) {
		int alpha = (int) Mth.lerp(t, a >>> 24, b >>> 24);
		int red = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int green = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int blue = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	public static JsonObject save() {
		JsonObject json = new JsonObject();
		json.addProperty("preset", preset.name());
		json.addProperty("opacity", opacity);

		JsonObject colors = new JsonObject();
		OVERRIDES.forEach((color, value) -> colors.addProperty(color.getKey(), ColorSetting.toHex(value)));
		json.add("colors", colors);
		return json;
	}

	public static void load(JsonObject json) {
		JsonElement presetName = json.get("preset");
		if (presetName != null) {
			for (ThemePreset candidate : ThemePreset.values()) {
				if (candidate.name().equals(presetName.getAsString())) {
					preset = candidate;
				}
			}
		}

		JsonElement opacityValue = json.get("opacity");
		if (opacityValue != null && opacityValue.isJsonPrimitive()) {
			setOpacity(opacityValue.getAsFloat());
		}

		OVERRIDES.clear();
		JsonObject colors = json.getAsJsonObject("colors");
		if (colors != null) {
			for (ThemeColor color : ThemeColor.values()) {
				JsonElement value = colors.get(color.getKey());
				Integer argb = value != null ? ColorSetting.parseHex(value.getAsString()) : null;
				if (argb != null) OVERRIDES.put(color, argb);
			}
		}
	}
}
