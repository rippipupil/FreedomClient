package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.BitSet;

/** Cuadrícula de píxeles encendidos/apagados, usada para dibujar una mira propia. */
public class PixelGridSetting extends Setting<BitSet> {
	private final int size;

	public PixelGridSetting(String name, String description, int size, String defaultPattern) {
		super(name, description, parse(defaultPattern, size));
		this.size = size;
		// El valor debe ser una copia para no modificar el patrón por defecto.
		set((BitSet) getDefault().clone());
	}

	public int getSize() {
		return size;
	}

	public boolean isSet(int x, int y) {
		return get().get(y * size + x);
	}

	public void toggle(int x, int y) {
		get().flip(y * size + x);
	}

	public void setPixel(int x, int y, boolean on) {
		get().set(y * size + x, on);
	}

	public void clear() {
		get().clear();
	}

	@Override
	public void reset() {
		BitSet copy = (BitSet) getDefault().clone();
		set(copy);
	}

	/** Patrón como filas de '#' (encendido) y '.' (apagado) separadas por '/'. */
	private static BitSet parse(String pattern, int size) {
		BitSet bits = new BitSet(size * size);
		String[] rows = pattern.split("/");
		for (int y = 0; y < Math.min(size, rows.length); y++) {
			for (int x = 0; x < Math.min(size, rows[y].length()); x++) {
				if (rows[y].charAt(x) == '#') bits.set(y * size + x);
			}
		}
		return bits;
	}

	private String serialize() {
		StringBuilder builder = new StringBuilder();
		for (int y = 0; y < size; y++) {
			if (y > 0) builder.append('/');
			for (int x = 0; x < size; x++) builder.append(isSet(x, y) ? '#' : '.');
		}
		return builder.toString();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(serialize());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive()) set(parse(json.getAsString(), size));
	}
}
