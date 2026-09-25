package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Health Indicators: muestra la vida de los jugadores (y de los mobs con nombre) junto a su nombre,
 * como número, como corazones o ambos. El panel del objetivo es el elemento de HUD "Target HUD".
 */
public class HealthIndicatorsModule extends Module {
	private final ModeSetting style = add(new ModeSetting("Style", "How health is shown next to names.", "Number", "Number", "Hearts", "Both"));
	private final BooleanSetting playersOnly = add(new BooleanSetting("Players only", "Only show health on players.", true));
	private final BooleanSetting absorption = add(new BooleanSetting("Include absorption", "Add golden absorption hearts to the health.", true));

	public HealthIndicatorsModule() {
		super("Health Indicators", "Shows health next to player names as a number and/or hearts. Use Target HUD for a health panel.", Category.PVP, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	/** Llamado desde EntityRendererMixin con el nombre que va a mostrar vanilla. */
	public static Component decorateName(Entity entity, Component name) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null || !(entity instanceof LivingEntity living)) return name;

		HealthIndicatorsModule module = manager.get(HealthIndicatorsModule.class);
		if (!module.isEnabled() || (module.playersOnly.get() && !(entity instanceof Player))) return name;

		float health = living.getHealth() + (module.absorption.get() ? living.getAbsorptionAmount() : 0);
		float fraction = health / Math.max(1.0F, living.getMaxHealth());
		int color = fraction > 0.6F ? 0x55FF55 : fraction > 0.3F ? 0xFFFF55 : 0xFF5555;

		MutableComponent result = name.copy();
		if (!module.style.is("Hearts")) {
			String number = health == Math.floor(health) ? String.valueOf((int) health) : String.format(Locale.ROOT, "%.1f", health);
			result.append(Component.literal(" " + number).withColor(color));
		}
		if (!module.style.is("Number")) {
			int hearts = Math.max(0, Math.round(health / 2.0F));
			int maxHearts = Math.max(hearts, Math.round(living.getMaxHealth() / 2.0F));
			StringBuilder full = new StringBuilder();
			StringBuilder empty = new StringBuilder();
			for (int i = 0; i < Math.min(hearts, 10); i++) full.append('❤');
			for (int i = hearts; i < Math.min(maxHearts, 10); i++) empty.append('❤');
			result.append(Component.literal(" " + full).withColor(0xFF3B3B));
			if (empty.length() > 0) result.append(Component.literal(empty.toString()).withColor(0x555555));
		} else {
			result.append(Component.literal(" ❤").withColor(0xFF3B3B));
		}
		return result;
	}
}
