package com.freedomclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
		this(name, description, defaultValue, min, max, step, "");
	}

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	@Override
	public void set(Double value) {
		double snapped = Math.round((value - min) / step) * step + min;
		super.set(Mth.clamp(snapped, min, max));
	}

	public int getInt() {
		return (int) Math.round(get());
	}

	public float getFloat() {
		return get().floatValue();
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	/** Posición del valor entre el mínimo y el máximo (0..1). */
	public double getProgress() {
		return (get() - min) / (max - min);
	}

	public void setProgress(double progress) {
		set(min + Mth.clamp(progress, 0.0, 1.0) * (max - min));
	}

	public String format() {
		String number = step >= 1.0 ? String.valueOf(getInt()) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", get());
		return number + suffix;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsDouble());
		}
	}
}
