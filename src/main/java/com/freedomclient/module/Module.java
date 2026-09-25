package com.freedomclient.module;

import com.freedomclient.FreedomClient;
import com.freedomclient.config.Config;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public abstract class Module {
	private final String name;
	private final String description;
	private final Category category;
	private boolean enabled;
	private KeyMapping toggleKey;

	protected Module(String name, String description, Category category, boolean enabledByDefault) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabled = enabledByDefault;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;

		this.enabled = enabled;
		Minecraft client = Minecraft.getInstance();
		if (enabled) {
			onEnable(client);
		} else {
			onDisable(client);
		}

		Config.save(FreedomClient.getModuleManager());
	}

	/** Aplica el estado guardado sin ejecutar onEnable/onDisable (el juego aún no ha terminado de cargar). */
	public void loadEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	protected void onEnable(Minecraft client) {
	}

	protected void onDisable(Minecraft client) {
	}

	/** Llamado cada tick del cliente mientras el módulo está activado. */
	public void onTick(Minecraft client) {
	}

	/** Llamado cuando el juego se cierra, esté o no activado el módulo. */
	public void onShutdown(Minecraft client) {
	}

	/** Si el módulo aparece en la lista de módulos activos del HUD. */
	public boolean isVisibleInModuleList() {
		return true;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public KeyMapping getToggleKey() {
		return toggleKey;
	}

	public void setToggleKey(KeyMapping toggleKey) {
		this.toggleKey = toggleKey;
	}
}
