package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Madeline (Celeste): flota a tu lado con su melena roja al viento, el plumífero azul claro, la mochila con el saco
 * enrollado y los ojos morados. Como en el juego, el pelo se vuelve azul cuando "gasta el dash" (corres y saltas) y
 * vuelve a ser rojo al tocar el suelo. Volando con élitros va en pose de dash, con el pelo azul y sombras detrás.
 * Saluda, se estira, mira hacia arriba y hace como que escala.
 */
public class MadelinePetCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("madeline_pet",
			'h', 0xFFAC3232, 'H', 0xFF7A1E26, 'i', 0xFFD8504A,
			'b', 0xFF5BA8E8, 'B', 0xFF2F6BB8, 'j', 0xFF9CD4FF,
			's', 0xFFF7D2B0, 'S', 0xFFE0AE90, 'k', 0xFF2A1A2E, 'e', 0xFF8A4AA0, 'w', 0xFFFFFFFF, 'm', 0xFF8A3A3A,
			'c', 0xFF7EC4D8, 'C', 0xFF4E8FB0, 'l', 0xFF3E3256, 'L', 0xFF2A2030, 'o', 0xFFC07A3A, 'O', 0xFF8A5222,
			'g', 0x664FC3F7);
	private static final int EMOTE_CYCLE = 150;
	private static final int EMOTE_LENGTH = 46;

	private Vox.Shape body;
	private Vox.Shape arm;
	private Vox.Shape headRed;
	private Vox.Shape headBlue;
	private Vox.Shape tailRed;
	private Vox.Shape tailBlue;
	private Vox.Shape ghost;
	private Vox.Shape lids;
	/** Pelo azul: ha "gastado el dash" (corriendo en el aire o volando) y aún no ha tocado el suelo. */
	private boolean blueHair;

	public MadelinePetCosmetic() {
		super("Madeline", "Celeste: Madeline floating next to you with her long red hair. Her hair turns blue when you dash (sprint-jump) "
				+ "and back to red when you land. Waves, stretches, looks up at the mountain and climbs.", "Left");
	}

	@Override
	public void onTick(Minecraft client) {
		super.onTick(client);
		LocalPlayer player = client.player;
		if (player == null) return;
		if (PetBehavior.flying() || !player.onGround() && player.isSprinting()) blueHair = true;
		else if (player.onGround()) blueHair = false;
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				// Piernas y botas.
				.box('l', -1.8F, -4.2F, -1.0F, 1.4F, 4.2F, 2.0F)
				.box('l', 0.4F, -4.2F, -1.0F, 1.4F, 4.2F, 2.0F)
				.box('L', -1.9F, -1.2F, -1.5F, 1.6F, 1.2F, 2.5F)
				.box('L', 0.3F, -1.2F, -1.5F, 1.6F, 1.2F, 2.5F)
				// Plumífero con sus franjas, cuello alto y cremallera.
				.box('c', -2.6F, -9.0F, -1.7F, 5.2F, 5.2F, 3.4F)
				.box('C', -2.75F, -7.4F, -1.85F, 5.5F, 0.5F, 3.7F)
				.box('C', -2.75F, -5.6F, -1.85F, 5.5F, 0.5F, 3.7F)
				.box('c', -2.0F, -9.8F, -1.4F, 4.0F, 0.9F, 2.8F)
				.box('C', -0.2F, -9.0F, -1.8F, 0.4F, 4.8F, 0.2F)
				// Mochila con el saco enrollado abajo.
				.box('o', -2.2F, -8.8F, 1.7F, 4.4F, 4.0F, 1.6F)
				.box('O', -2.6F, -5.2F, 1.8F, 5.2F, 1.4F, 1.8F)
				.box('O', -2.2F, -8.8F, 1.6F, 0.6F, 4.0F, 0.2F)
				.box('O', 1.6F, -8.8F, 1.6F, 0.6F, 4.0F, 0.2F);
		arm = new Vox.Shape(PALETTE)
				.box('c', -0.8F, 0.0F, -0.8F, 1.6F, 3.6F, 1.6F)
				.box('C', -0.85F, 2.8F, -0.85F, 1.7F, 0.4F, 1.7F)
				.box('s', -0.6F, 3.2F, -0.6F, 1.2F, 1.0F, 1.2F);
		headRed = head('h', 'H', 'i');
		headBlue = head('b', 'B', 'j');
		tailRed = tail('h', 'H');
		tailBlue = tail('b', 'B');
		// Párpados para cuando cierra los ojos.
		lids = new Vox.Shape(PALETTE);
		for (int side = -1; side <= 1; side += 2) {
			float cx = side * 1.3F;
			lids.box('s', cx - 0.85F, -3.35F, -3.2F, 1.7F, 1.8F, 0.2F).box('k', cx - 0.7F, -2.1F, -3.25F, 1.4F, 0.3F, 0.1F);
		}
		// Silueta translúcida azul para las sombras del dash.
		ghost = new Vox.Shape(PALETTE)
				.box('g', -2.6F, -9.0F, -1.7F, 5.2F, 9.0F, 3.4F)
				.box('g', -3.4F, -16.0F, -3.0F, 6.8F, 7.0F, 6.0F);
	}

	/** Cabeza con la cara, los ojos morados y la melena del color que toque. */
	private static Vox.Shape head(char hair, char dark, char light) {
		Vox.Shape s = new Vox.Shape(PALETTE)
				.box('s', -2.8F, -5.8F, -2.8F, 5.6F, 5.8F, 5.6F)
				.box('S', -2.8F, -0.4F, -2.85F, 5.6F, 0.4F, 0.1F)
				// Melena: casquete, nuca, lados largos y el flequillo que tapa media frente.
				.box(hair, -3.3F, -6.8F, -3.3F, 6.6F, 2.0F, 6.6F)
				.box(hair, -3.3F, -6.0F, 1.6F, 6.6F, 6.6F, 1.8F)
				.box(hair, -3.4F, -6.0F, -2.4F, 0.9F, 5.8F, 4.2F)
				.box(hair, 2.5F, -6.0F, -2.4F, 0.9F, 5.8F, 4.2F)
				.box(dark, -3.45F, -1.2F, -2.4F, 0.9F, 1.6F, 4.2F)
				.box(dark, 2.55F, -1.2F, -2.4F, 0.9F, 1.6F, 4.2F)
				.box(light, -2.0F, -6.9F, -2.0F, 2.6F, 0.4F, 1.4F)
				// Mechón de punta (el "antenita") que sale de lo alto.
				.box(hair, -0.4F, -8.4F, -0.6F, 0.8F, 1.8F, 0.8F)
				.box(light, 0.0F, -9.4F, -0.4F, 0.6F, 1.2F, 0.6F);
		// Flequillo del color de la melena.
		String[] fringe = {"hhhhh.h", "hhh.hhh", "h......"};
		for (int i = 0; i < fringe.length; i++) fringe[i] = fringe[i].replace('h', hair);
		s.art(fringe, -3.0F, -5.0F, -3.2F, 0.5F);
		// Ojos grandes morados con su brillo, cejas y boca pequeña.
		for (int side = -1; side <= 1; side += 2) {
			float cx = side * 1.3F;
			s.box('w', cx - 0.8F, -3.3F, -2.95F, 1.6F, 1.7F, 0.2F)
					.box('e', cx - 0.6F, -3.1F, -3.0F, 1.2F, 1.5F, 0.2F)
					.box('k', cx - 0.3F, -2.8F, -3.05F, 0.6F, 0.9F, 0.2F)
					.box('w', cx - 0.5F, -3.0F, -3.1F, 0.35F, 0.35F, 0.1F)
					.box('k', cx - 0.8F, -3.8F, -2.95F, 1.6F, 0.3F, 0.2F);
		}
		s.box('m', -0.5F, -1.3F, -2.95F, 1.0F, 0.4F, 0.2F);
		return s;
	}

	/** Melena larga que cae por la espalda, en tramos que ondean con el viento. */
	private static Vox.Shape tail(char hair, char dark) {
		return new Vox.Shape(PALETTE)
				.box(hair, -2.6F, 0.0F, -0.8F, 5.2F, 2.2F, 1.6F)
				.box(dark, -2.6F, 1.6F, -0.8F, 5.2F, 0.6F, 1.6F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (body == null) build();
		boolean flying = PetBehavior.flying();
		boolean blue = blueHair || flying;
		int cycle = (int) (time / EMOTE_CYCLE);
		float inCycle = time - cycle * EMOTE_CYCLE;
		int emote = mood == PetBehavior.Mood.IDLE && !flying && inCycle < EMOTE_LENGTH ? Math.floorMod(cycle * 5 + 1, 4) : -1;
		float progress = inCycle / EMOTE_LENGTH;
		float wave = Mth.sin(progress * Mth.PI);

		float bob = Mth.sin(time * 0.1F) * 0.8F;
		float rightArm = Mth.sin(time * 0.1F) * 6.0F;
		float leftArm = -Mth.sin(time * 0.1F) * 6.0F;
		float armsOut = 12.0F;
		float headPitch = 0.0F;
		float headTurn = Mth.sin(time * 0.03F) * 12.0F;
		float lean = 0.0F;
		float hairLift = 20.0F + Mth.sin(time * 0.15F) * 8.0F;
		boolean eyesClosed = false;
		switch (emote) {
			case 0 -> rightArm = -160.0F + Mth.sin(progress * 30.0F) * 20.0F * wave; // saluda
			case 1 -> { // se estira
				rightArm = -170.0F * wave;
				leftArm = -170.0F * wave;
				eyesClosed = progress > 0.3F && progress < 0.7F;
			}
			case 2 -> headPitch = -30.0F * wave; // mira hacia arriba, a la montaña
			case 3 -> { // escala: brazos arriba alternando
				rightArm = -150.0F + Mth.sin(progress * Mth.PI * 6.0F) * 25.0F;
				leftArm = -150.0F - Mth.sin(progress * Mth.PI * 6.0F) * 25.0F;
				bob += Mth.sin(progress * Mth.PI * 6.0F) * 0.8F;
			}
			default -> {
			}
		}
		switch (mood) {
			case WAVE -> rightArm = -160.0F + Mth.sin(moodSeconds * 12.0F) * 20.0F;
			case CELEBRATE -> {
				rightArm = -170.0F;
				bob -= Math.abs(Mth.sin(moodSeconds * 9.0F)) * 3.0F;
			}
			case SLEEP -> {
				eyesClosed = true;
				headPitch = 22.0F;
				rightArm = 0.0F;
				leftArm = 0.0F;
				hairLift = 6.0F;
			}
			case HIDE -> {
				rightArm = -80.0F;
				leftArm = -80.0F;
				headPitch = 15.0F;
				bob += Mth.sin(time * 3.0F) * 0.3F;
			}
			default -> {
			}
		}
		if (flying) {
			// Dash: se lanza hacia delante con los brazos atrás y el pelo azul estirado.
			lean = 60.0F;
			rightArm = 40.0F;
			leftArm = 40.0F;
			headPitch = -35.0F;
			hairLift = 70.0F + Mth.sin(time * 0.8F) * 8.0F;
			// Sombras del dash detrás de ella.
			for (int i = 1; i <= 2; i++) {
				poseStack.pushPose();
				poseStack.translate(0.0F, -4.0F / 16.0F, i * 4.5F / 16.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(lean));
				poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
				ghost.drawTranslucent(poseStack, collector, light);
				poseStack.popPose();
			}
		}

		poseStack.translate(0.0F, bob / 16.0F, 0.0F);
		if (lean != 0.0F) {
			poseStack.translate(0.0F, -6.0F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(lean));
			poseStack.translate(0.0F, 6.0F / 16.0F, 0.0F);
		}
		body.draw(poseStack, collector, light);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 3.4F / 16.0F, -8.8F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(side < 0 ? rightArm : leftArm));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * armsOut));
			arm.draw(poseStack, collector, light);
			poseStack.popPose();
		}

		poseStack.translate(0.0F, -9.8F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(headTurn));
		poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
		(blue ? headBlue : headRed).draw(poseStack, collector, light);
		if (eyesClosed) lids.draw(poseStack, collector, light);
		PetEmotes.blush(poseStack, collector, light, 1.9F, -1.8F, -3.1F);
		// Melena por la espalda: cinco tramos que se levantan con el viento y ondean.
		poseStack.pushPose();
		poseStack.translate(0.0F, -4.6F / 16.0F, 3.0F / 16.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(hairLift));
		Vox.Shape tail = blue ? tailBlue : tailRed;
		for (int i = 0; i < 5; i++) {
			float width = 1.0F - i * 0.12F;
			poseStack.pushPose();
			poseStack.scale(width, 1.0F, 1.0F);
			tail.draw(poseStack, collector, light);
			poseStack.popPose();
			poseStack.translate(0.0F, 1.9F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + Mth.sin(time * 0.25F - i * 0.8F) * (6.0F + (flying ? 10.0F : 0.0F))));
		}
		poseStack.popPose();
	}
}
