package com.freedomclient.cosmetic;

import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public class CloudPetCosmetic extends CosmeticModule {
	public final ModeSetting side = add(new ModeSetting("Side", "Which shoulder the cloud floats next to.", "Left", "Left", "Right"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the cloud.", 0.5, 0.3, 0.8, 0.05, "x"));
	public final PetFollower follower = new PetFollower();

	public CloudPetCosmetic() {
		super("Cloud Pet", "A little winged pixel cloud that follows you around and reacts like the Angel Devil pet.");
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player == null) {
			follower.reset();
			return;
		}
		// La nube va un poco más despacio que la otra mascota, para que se note que te sigue flotando.
		follower.tick(client.player, PetCosmetic.slot(side.is("Right") ? -1.0F : 1.0F), 0.12F);
	}
}
