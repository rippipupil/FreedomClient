package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Personaliza las hitboxes que se ven con F3 + B: colores, qué líneas se dibujan y de qué entidades. */
public class CustomHitboxesModule extends Module {
	private final ColorSetting playerColor = add(new ColorSetting("Player color", "Hitbox color of players.", 0xFFFF3B3B, true));
	private final ColorSetting entityColor = add(new ColorSetting("Entity color", "Hitbox color of mobs and other entities.", 0xFFF5F1E8, true));
	private final ColorSetting targetColor = add(new ColorSetting("Target color", "Hitbox color of the entity you are aiming at.", 0xFFF2C94C, true));
	private final BooleanSetting reachColorEnabled = add(new BooleanSetting("In-reach color", "Change the hitbox color when the entity is close enough to hit.", true));
	private final ColorSetting reachColor = add(new ColorSetting("Reach color", "Hitbox color of entities you can hit right now.", 0xFF5DFF7A, true));
	private final BooleanSetting playersOnly = add(new BooleanSetting("Players only", "Only draw hitboxes of players.", false));
	private final BooleanSetting eyeLine = add(new BooleanSetting("Eye line", "Draw the red eye height line.", true));
	private final BooleanSetting lookVector = add(new BooleanSetting("Look direction", "Draw the blue arrow showing where entities look.", true));
	private final BooleanSetting motion = add(new BooleanSetting("Movement arrow", "Draw the yellow arrow showing entity movement.", false));

	public CustomHitboxesModule() {
		super("Custom Hitboxes", "Change the color of hitboxes (F3 + B), highlight enemies in reach and choose which lines are drawn.",
				Category.VISUAL, true);
		reachColor.visibleWhen(reachColorEnabled::get);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static CustomHitboxesModule active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		CustomHitboxesModule module = manager.get(CustomHitboxesModule.class);
		return module.isEnabled() ? module : null;
	}

	public static boolean shouldSkip(Entity entity) {
		CustomHitboxesModule module = active();
		return module != null && module.playersOnly.get() && !(entity instanceof Player);
	}

	public static int boxColor(Entity entity, int vanilla) {
		CustomHitboxesModule module = active();
		if (module == null) return vanilla;
		Minecraft client = Minecraft.getInstance();
		if (entity == client.crosshairPickEntity) return module.targetColor.get();
		// Al alcance: a la distancia de golpe del jugador (3 bloques en supervivencia), medida hasta su hitbox.
		if (module.reachColorEnabled.get() && client.player != null && entity != client.player
				&& client.player.isWithinEntityInteractionRange(entity, 0.0)) {
			return module.reachColor.get();
		}
		return entity instanceof Player ? module.playerColor.get() : module.entityColor.get();
	}

	public static int eyeLineColor(int vanilla) {
		CustomHitboxesModule module = active();
		return module == null ? vanilla : module.eyeLine.get() ? vanilla : 0;
	}

	public static int lookColor(int vanilla) {
		CustomHitboxesModule module = active();
		return module == null ? vanilla : module.lookVector.get() ? vanilla : 0;
	}

	public static int motionColor(int vanilla) {
		CustomHitboxesModule module = active();
		return module == null ? vanilla : module.motion.get() ? vanilla : 0;
	}
}
