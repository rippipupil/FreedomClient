package com.freedomclient.cosmetic.vox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/**
 * Niko (OneShot): Niko sentado en tu hombro con su sombrero de gato, cara redondita y clara, grandes ojos amarillos de
 * gato con la pupila rasgada, boquita ":3" y el pelo negro alborotado; la gabardina con solapas y cinturón, la
 * bufanda y el Sol (la bombilla con su casquillo) en brazos.
 * Hace tonterías: ladea la cabeza, da saltitos, gira, se aplasta como un pancake, levanta el Sol y se tambalea.
 */
public class NikoPetCosmetic extends ShoulderPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("niko_pet",
			's', 0xFFF3E6DC, 'S', 0xFFD9C6BA, 'H', 0xFF1E1822, 'c', 0xFF5A3128, 'C', 0xFF46251E, 'l', 0xFF7A4A38,
			'p', 0xFF5B3C9E, 'P', 0xFF3E4FB8, 'y', 0xFFFFD447, 'Y', 0xFFE8A92A, 'k', 0xFF120C18, 'w', 0xFFFFFFFF,
			'm', 0xFFE08AB0, 'K', 0xFF1C1620, 'G', 0xFF8A8C94, 'g', 0xFF5E6068);
	private Vox.Shape body;
	private Vox.Shape head;
	private Vox.Shape arm;
	private Vox.Shape hat;
	private Vox.Shape sun;
	private Vox.Shape socket;
	private Vox.Shape bigSun;

	public NikoPetCosmetic() {
		super("Niko", "OneShot: Niko sitting on your shoulder with the Sun in their arms, being silly: head tilts, hops, spins, squishes and wobbles.");
	}

	private void build() {
		body = new Vox.Shape(PALETTE)
				// Piernas cortas colgando del hombro, con zapatos oscuros.
				.box('C', -2.3F, -1.8F, -3.2F, 1.7F, 1.6F, 3.2F)
				.box('C', 0.6F, -1.8F, -3.2F, 1.7F, 1.6F, 3.2F)
				.box('K', -2.4F, -1.9F, -3.9F, 1.9F, 1.8F, 1.0F)
				.box('K', 0.5F, -1.9F, -3.9F, 1.9F, 1.8F, 1.0F)
				// Gabardina larga que se abre abajo, con solapas, cinturón y botones.
				.box('c', -3.0F, -2.8F, -2.2F, 6.0F, 1.4F, 4.4F)
				.box('c', -2.6F, -6.8F, -1.9F, 5.2F, 4.2F, 3.8F)
				.box('C', -0.25F, -6.6F, -2.0F, 0.5F, 4.0F, 0.2F)
				.box('l', -1.9F, -6.8F, -2.05F, 1.2F, 2.0F, 0.2F)
				.box('l', 0.7F, -6.8F, -2.05F, 1.2F, 2.0F, 0.2F)
				.box('C', -2.65F, -4.2F, -1.95F, 5.3F, 0.6F, 3.9F)
				.box('Y', -0.9F, -5.6F, -2.1F, 0.5F, 0.5F, 0.2F)
				.box('Y', -0.9F, -3.4F, -2.1F, 0.5F, 0.5F, 0.2F)
				// Bufanda morada y azul con las dos puntas separadas por detrás.
				.box('p', -2.9F, -7.6F, -2.2F, 5.8F, 1.2F, 4.4F)
				.box('P', -2.9F, -7.0F, -2.25F, 5.8F, 0.4F, 4.5F)
				.box('p', 0.6F, -6.6F, 2.0F, 1.2F, 3.4F, 0.5F)
				.box('P', 1.9F, -6.8F, 2.0F, 1.0F, 2.6F, 0.5F);
		head = new Vox.Shape(PALETTE)
				// Cabeza redondita: un cubo con las aristas recortadas.
				.box('s', -3.0F, -5.6F, -3.0F, 6.0F, 5.2F, 6.0F)
				.box('s', -2.6F, -6.0F, -2.6F, 5.2F, 6.0F, 5.2F)
				.box('S', -2.6F, -0.4F, -2.65F, 5.2F, 0.4F, 0.1F)
				// Pelo negro alborotado: casquete, nuca, mechones de punta a los lados y flequillo.
				.box('H', -3.2F, -6.4F, -3.2F, 6.4F, 1.4F, 6.4F)
				.box('H', -3.2F, -5.2F, 2.0F, 6.4F, 4.4F, 1.2F)
				.box('H', -3.6F, -5.0F, -1.6F, 0.8F, 2.4F, 3.0F)
				.box('H', 2.8F, -5.0F, -1.6F, 0.8F, 2.4F, 3.0F)
				.box('H', -4.1F, -4.2F, -0.6F, 0.6F, 1.0F, 1.2F)
				.box('H', 3.5F, -4.2F, -0.6F, 0.6F, 1.0F, 1.2F);
		head.art(new String[] {
				"HHHHHH",
				"H.HH.H",
				"..H...",
		}, -3.0F, -5.2F, -3.35F, 0.4F);
		// Ojos grandes y redondos de gato: amarillos, pupila rasgada y un brillo; nariz rosa y boquita ":3".
		for (int side = -1; side <= 1; side += 2) {
			float cx = side * 1.45F;
			head.box('y', cx - 1.0F, -3.9F, -3.3F, 2.0F, 2.2F, 0.4F)
					.box('y', cx - 0.7F, -4.2F, -3.3F, 1.4F, 2.8F, 0.4F)
					.box('k', cx - 0.2F, -3.9F, -3.45F, 0.4F, 2.2F, 0.2F)
					.box('w', cx - 0.8F, -3.8F, -3.45F, 0.5F, 0.5F, 0.2F);
		}
		head.box('m', -0.3F, -1.5F, -3.25F, 0.6F, 0.4F, 0.3F)
				.art(new String[] {
						"k.k.k",
						".k.k.",
				}, -1.25F, -1.05F, -3.3F, 0.2F, 0.5F);
		arm = new Vox.Shape(PALETTE).box('c', -0.8F, 0.0F, -0.8F, 1.6F, 3.4F, 1.6F).box('l', -0.85F, 2.8F, -0.85F, 1.7F, 0.5F, 1.7F)
				.box('s', -0.7F, 3.3F, -0.7F, 1.4F, 0.9F, 1.4F);
		hat = NikoHatCosmetic.build();
		sun = SunBackpackCosmetic.sun(1.8F);
		bigSun = SunBackpackCosmetic.sun(4.6F);
		// Casquillo de rosca de la bombilla.
		socket = new Vox.Shape(PALETTE)
				.box('G', -0.9F, 1.4F, -0.9F, 1.8F, 1.4F, 1.8F)
				.box('g', -1.0F, 1.8F, -1.0F, 2.0F, 0.3F, 2.0F)
				.box('g', -1.0F, 2.3F, -1.0F, 2.0F, 0.3F, 2.0F)
				.box('K', -0.5F, 2.8F, -0.5F, 1.0F, 0.5F, 1.0F);
	}

	@Override
	protected int emotes() {
		return 6;
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, int emote, float progress, boolean sleeping) {
		if (body == null) build();
		float wave = Mth.sin(progress * Mth.PI);
		float headTilt = sleeping ? 20.0F : Mth.sin(time * 0.06F) * 6.0F;
		float hop = 0.0F;
		float spin = 0.0F;
		float squish = 0.0F;
		float wobble = 0.0F;
		float sunUp = 0.0F;
		switch (emote) {
			case 0 -> headTilt = Mth.sin(progress * Mth.PI * 4.0F) * 30.0F * wave; // ladea la cabeza de lado a lado
			case 1 -> hop = Math.abs(Mth.sin(progress * Mth.PI * 4.0F)) * 2.2F; // saltitos
			case 2 -> spin = progress * 720.0F; // gira dos veces
			case 3 -> squish = wave; // pancake
			case 4 -> sunUp = wave; // levanta el Sol
			case 5 -> wobble = Mth.sin(progress * Mth.PI * 6.0F) * 16.0F * wave; // se tambalea
			default -> {
			}
		}
		boolean flying = com.freedomclient.cosmetic.PetBehavior.flying();
		if (flying) {
			// Volando con élitros: Niko va montado encima del Sol, la bombilla gigante, que gira despacio y brilla.
			// Todo sube por encima del hombro para que la bombilla no quede dentro del brazo.
			poseStack.translate(0.0F, -9.6F / 16.0F, 0.0F);
			poseStack.pushPose();
			poseStack.translate(0.0F, 4.4F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
			bigSun.drawGlow(poseStack, collector);
			poseStack.scale(2.5F, 2.5F, 2.5F);
			socket.draw(poseStack, collector, light);
			poseStack.popPose();
			wobble = Mth.sin(time * 0.25F) * 6.0F;
			headTilt = -wobble;
		}
		poseStack.translate(0.0F, -hop / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(spin));
		poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
		poseStack.scale(1.0F + squish * 0.35F, 1.0F - squish * 0.45F, 1.0F + squish * 0.35F);
		body.draw(poseStack, collector, light);
		// Brazos hacia delante sujetando el Sol (o levantándolo por encima de la cabeza).
		// Volando se agarra a la bombilla con los brazos hacia abajo.
		float armAngle = flying ? -20.0F : -70.0F - sunUp * 100.0F;
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 2.9F / 16.0F, -6.4F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(armAngle));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * -12.0F));
			arm.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		if (!flying) {
			poseStack.pushPose();
			poseStack.translate(0.0F, (-5.0F - sunUp * 9.0F) / 16.0F, (-4.2F + sunUp * 3.0F) / 16.0F);
			sun.drawGlow(poseStack, collector);
			socket.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.translate(0.0F, -7.4F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(headTilt));
		head.draw(poseStack, collector, light);
		PetEmotes.blush(poseStack, collector, light, 2.2F, -1.5F, -3.4F);
		poseStack.translate(0.0F, -6.3F / 16.0F, 0.0F);
		poseStack.scale(0.62F, 0.62F, 0.62F);
		hat.draw(poseStack, collector, light);
	}
}
