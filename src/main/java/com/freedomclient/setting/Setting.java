package com.freedomclient.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Opción configurable de un módulo, mostrada en su página de ajustes y guardada en la config. */
public abstract class Setting<T> {
	private final String name;
	private final String description;
	private final T defaultValue;
	protected T value;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public T getDefault() {
		return defaultValue;
	}

	public void reset() {
		set(defaultValue);
	}

	/** Oculta la opción en el menú mientras la condición sea falsa (por ejemplo, si depende de otra). */
	public void visibleWhen(BooleanSupplier visibility) {
		this.visibility = visibility;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	/** Si se guarda en la config (las acciones no). */
	public boolean isSaved() {
		return true;
	}

	public abstract JsonElement toJson();

	/** Carga el valor desde JSON; ignora valores con un tipo inesperado. */
	public abstract void fromJson(JsonElement json);
}
