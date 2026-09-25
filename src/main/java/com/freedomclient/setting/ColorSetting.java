package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/** Color ARGB. En la config se guarda como "#AARRGGBB". */
public class ColorSetting extends Setting<Integer> {
	private final boolean allowAlpha;

	public ColorSetting(String name, String description, int defaultArgb, boolean allowAlpha) {
		super(name, description, defaultArgb);
		this.allowAlpha = allowAlpha;
	}

	public boolean allowsAlpha() {
		return allowAlpha;
	}

	@Override
	public void set(Integer value) {
		super.set(allowAlpha ? value : value | 0xFF000000);
	}

	public static String toHex(int argb) {
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	public static Integer parseHex(String text) {
		String hex = text.startsWith("#") ? text.substring(1) : text;
		try {
			if (hex.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
			if (hex.length() == 8) return Integer.parseUnsignedInt(hex, 16);
		} catch (NumberFormatException ignored) {
		}
		return null;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(toHex(get()));
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive()) {
			Integer color = parseHex(json.getAsString());
			if (color != null) set(color);
		}
	}
}
