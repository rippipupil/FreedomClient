package com.freedomclient.module.visual;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Hide Armor: no dibuja las piezas de armadura que elijas en tu jugador (en F5, el inventario y el menú). Solo cambia
 * lo que ves tú: la armadura sigue puesta y protegiendo, y los demás jugadores la siguen viendo.
 */
public class HideArmorModule extends Module {
	private static HideArmorModule instance;

	private final BooleanSetting helmet = add(new BooleanSetting("Helmet", "Hide your helmet.", true));
	private final BooleanSetting chestplate = add(new BooleanSetting("Chestplate", "Hide your chestplate.", true));
	private final BooleanSetting leggings = add(new BooleanSetting("Leggings", "Hide your leggings.", true));
	private final BooleanSetting boots = add(new BooleanSetting("Boots", "Hide your boots.", true));
	private final BooleanSetting elytra = add(new BooleanSetting("Elytra", "Hide your elytra on your back.", false));

	public HideArmorModule() {
		super("Hide Armor", "Hide the armor pieces you choose on your own player. It still protects you and others still see it.",
				Category.VISUAL, false);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static boolean isOwnPlayer(HumanoidRenderState state) {
		Minecraft client = Minecraft.getInstance();
		return state instanceof AvatarRenderState avatar && client.player != null && avatar.id == client.player.getId();
	}

	/** Si hay que saltarse esta pieza de armadura al dibujar a este jugador. */
	public static boolean hides(HumanoidRenderState state, EquipmentSlot slot) {
		HideArmorModule module = instance;
		if (module == null || !module.isEnabled() || !isOwnPlayer(state)) return false;
		return switch (slot) {
			case HEAD -> module.helmet.get();
			case CHEST -> module.chestplate.get();
			case LEGS -> module.leggings.get();
			case FEET -> module.boots.get();
			default -> false;
		};
	}

	/** Si hay que saltarse la élitra de este jugador. */
	public static boolean hidesElytra(HumanoidRenderState state) {
		HideArmorModule module = instance;
		return module != null && module.isEnabled() && module.elytra.get() && isOwnPlayer(state);
	}
}
