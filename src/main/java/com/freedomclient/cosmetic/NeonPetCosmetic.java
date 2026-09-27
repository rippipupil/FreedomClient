package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Mascota Neon: un Funko Pop pixel de Neon que te sigue y reacciona como las otras mascotas. */
public class NeonPetCosmetic extends CosmeticModule {
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the pet floats next to.", "Left", "Right", "Left"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the pet.", 0.42, 0.3, 0.7, 0.02, "x"));
	public final PetFollower follower = new PetFollower();

	public NeonPetCosmetic() {
		super("Neon Pet", "A pixel Funko Pop of Neon: big head, blue hair buns, her suit and energy pack. Waves, cheers, naps and hides.",
				CosmeticSlot.PET, false);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player == null) {
			follower.reset();
			return;
		}
		follower.tick(client.player, PetCosmetic.slot(side.is("Right") ? -1.0F : 1.0F), 0.2F);
	}
}
