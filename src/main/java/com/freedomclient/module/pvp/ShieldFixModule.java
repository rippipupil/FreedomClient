package com.freedomclient.module.pvp;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Baja y encoge el escudo en primera persona para que no tape la vista al bloquear. */
public class ShieldFixModule extends Module {
	private final NumberSetting lower = add(new NumberSetting("Lower by", "How much lower the shield is drawn.", 0.2, 0, 0.6, 0.05));
	private final NumberSetting size = add(new NumberSetting("Size", "Size of the shield in first person.", 0.85, 0.5, 1, 0.05, "x"));

	public ShieldFixModule() {
		super("Shield Fix", "Lowers the shield in first person so it doesn't block your view.", Category.PVP, true);
	}

	public void apply(PoseStack poseStack, AbstractClientPlayer player, ItemStack stack) {
		if (!isEnabled() || !stack.is(Items.SHIELD)) return;
		poseStack.translate(0.0F, -lower.getFloat(), 0.0F);
		float s = size.getFloat();
		poseStack.scale(s, s, s);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
