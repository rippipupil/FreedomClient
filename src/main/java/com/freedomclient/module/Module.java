package com.freedomclient.module;

import com.freedomclient.FreedomClient;
import com.freedomclient.config.Config;
import com.freedomclient.setting.KeybindSetting;
import com.freedomclient.setting.Setting;
import com.freedomclient.util.ModIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Module {
	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final KeybindSetting keybind;
	private boolean enabled;

	protected Module(String name, String description, Category category, boolean enabledByDefault) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.enabled = enabledByDefault;
		this.keybind = new KeybindSetting("Keybind", "Key that toggles this mod.", KeybindSetting.NONE);
		if (canToggle()) {
			settings.add(keybind);
		}
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled || !canToggle()) return;

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
		if (canToggle()) {
			this.enabled = enabled;
		}
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

	/** Los módulos que no se pueden apagar (como los mods de rendimiento) devuelven false. */
	public boolean canToggle() {
		return true;
	}

	/** Icono de 16x16 de la tarjeta del módulo (o un engranaje si todavía no tiene icono propio). */
	public Identifier getIcon() {
		return ModIcons.orFallback(FreedomClient.id("textures/icon/" + getId() + ".png"));
	}

	/** Nombre en minúsculas y sin espacios, usado para iconos y claves de la config. */
	public String getId() {
		return name.toLowerCase(Locale.ROOT).replace(' ', '_');
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

	public KeybindSetting getKeybind() {
		return keybind;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}
}
