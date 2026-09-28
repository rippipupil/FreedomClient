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

	/** Color de cada capa de corazones: la primera roja y las siguientes como en los servidores con vida extra. */
	private static final int[] LAYERS = {0xFF3B3B, 0xFF8C42, 0xF2C94C, 0x6BE35A, 0x3FD7FF, 0x5B7BFF, 0xB26BFF, 0xFF6FA8};

	/**
	 * Corazones apilados como la barra de vida de vanilla con vida extra: 10 huecos por fila; los corazones que
	 * pasan de 10 se ponen encima con el color de la siguiente capa y se indica cuántas filas hay (x2, x3…).
	 * La absorción va aparte en dorado, también apilada.
	 */
	public static Component hearts(float health, float maxHealth, float absorption) {
		int total = Math.max(0, (int) Math.ceil(health / 2.0F));
		int max = Math.max(total, (int) Math.ceil(maxHealth / 2.0F));
		MutableComponent result = Component.empty();
		int layers = Math.max(1, (total + 9) / 10);
		int top = total == 0 ? 0 : total - (layers - 1) * 10;
		int topColor = LAYERS[(layers - 1) % LAYERS.length];
		int belowColor = layers > 1 ? LAYERS[(layers - 2) % LAYERS.length] : 0x555555;
		// Si la vida máxima es menor de 10 corazones solo se dibujan los huecos que hay.
		int slots = Math.min(10, Math.max(max, 1));
		StringBuilder upper = new StringBuilder();
		StringBuilder lower = new StringBuilder();
		for (int i = 0; i < slots; i++) (i < top ? upper : lower).append('❤');
		result.append(Component.literal(upper.toString()).withColor(topColor));
		if (lower.length() > 0) result.append(Component.literal(lower.toString()).withColor(belowColor));
		if (layers > 1) result.append(Component.literal(" x" + layers).withColor(topColor));
		int golden = (int) Math.ceil(absorption / 2.0F);
		if (golden > 0) {
			int goldenLayers = (golden + 9) / 10;
			result.append(Component.literal(" " + "❤".repeat(Math.min(golden, 10))).withColor(0xF2C94C));
			if (goldenLayers > 1) result.append(Component.literal(" x" + goldenLayers).withColor(0xF2C94C));
		}
		return result;
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
			result.append(" ").append(hearts(living.getHealth(), living.getMaxHealth(),
					module.absorption.get() ? living.getAbsorptionAmount() : 0.0F));
		} else {
			result.append(Component.literal(" ❤").withColor(0xFF3B3B));
		}
		return result;
	}
}
