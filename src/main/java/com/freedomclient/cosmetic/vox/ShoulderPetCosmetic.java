package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Mascota sentada en tu hombro. Cada pocos segundos hace un "emote" (saludar, saltar, girar…) y el resto del tiempo
 * respira y mira alrededor. Si estás AFK se duerme.
 */
public abstract class ShoulderPetCosmetic extends VoxCosmetic {
	/** Ticks entre emotes y lo que dura cada uno. */
	private static final int CYCLE = 140;
	private static final int EMOTE = 44;

	public final ModeSetting side = add(new ModeSetting("Shoulder", "Which shoulder the pet sits on.", "Left", "Left", "Right"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the pet.", 0.38, 0.2, 0.5, 0.02, "x"));

	protected ShoulderPetCosmetic(String name, String description) {
		super(name, description, CosmeticSlot.PET);
	}

	/** Cuántos emotes distintos tiene la mascota. */
	protected abstract int emotes();

	/**
	 * Dibuja la mascota sentada con el trasero en el origen, mirando hacia -z. {@code emote} es -1 si no está haciendo
	 * ninguno; {@code progress} va de 0 a 1 durante el emote.
	 */
	protected abstract void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, int emote, float progress,
			boolean sleeping);

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		float time = state.ageInTicks;
		int cycle = (int) (time / CYCLE);
		float inCycle = time - cycle * CYCLE;
		boolean sleeping = PetBehavior.mood() == PetBehavior.Mood.SLEEP;
		int emote = !sleeping && inCycle < EMOTE ? Math.floorMod(cycle * 7 + 3, emotes()) : -1;
		float progress = emote < 0 ? 0.0F : inCycle / EMOTE;
		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// En el borde de fuera del hombro, para que la cabeza no la tape al girar (sus esquinas llegan a 5.7 px del
		// centro) ni vista de frente. El brazo va de 4 a 8 px del centro del cuerpo.
		float x = side.is("Left") ? 8.3F : -8.3F;
		poseStack.translate(x / 16.0F, (state.chestEquipment.isEmpty() ? -0.6F : -1.4F) / 16.0F, 0.2F / 16.0F);
		float s = size.getFloat();
		poseStack.scale(s, s, s);
		renderPet(poseStack, collector, light, time, emote, progress, sleeping);
		poseStack.popPose();
	}
}
