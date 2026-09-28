package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.cosmetic.PetCosmetic;
import com.freedomclient.cosmetic.PetFollower;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Mascota que vuela a tu lado y te sigue con suavidad (como las otras mascotas), con los mismos estados de ánimo:
 * saluda, celebra, duerme y se esconde. Cada una dibuja su modelo en {@link #renderPet}.
 */
public abstract class FollowPetCosmetic extends VoxCosmetic {
	public final ModeSetting side;
	public final NumberSetting size;
	private final PetFollower follower = new PetFollower();

	protected FollowPetCosmetic(String name, String description) {
		this(name, description, "Left");
	}

	protected FollowPetCosmetic(String name, String description, String defaultSide) {
		super(name, description, CosmeticSlot.PET);
		side = add(new ModeSetting("Side", "Which shoulder the pet flies next to.", defaultSide, "Right", "Left"));
		size = add(new NumberSetting("Size", "Size of the pet.", 0.45, 0.3, 0.8, 0.02, "x"));
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player == null) {
			follower.reset();
			return;
		}
		follower.tick(client.player, PetCosmetic.slot(side.is("Right") ? -1.0F : 1.0F), 0.2F);
	}

	/** Dibuja la mascota con los pies en el origen, mirando hacia -z. */
	protected abstract void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds);

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		Vec3 position = follower.modelPosition(state.x, state.y, state.z, state.bodyRot, state.scale, state.ageInTicks % 1.0F);
		if (position == null) return;
		poseStack.pushPose();
		poseStack.translate(position.x, position.y, position.z);
		float s = size.getFloat();
		poseStack.scale(s, s, s);
		renderPet(poseStack, collector, light, state.ageInTicks, PetBehavior.mood(), PetBehavior.moodSeconds());
		poseStack.popPose();
	}
}
