package com.freedomclient.cosmetic;

import com.freedomclient.cosmetic.vox.Vox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Mascota Neon al estilo Funko Pop pixel 3D: cabeza grande con ojos negros y brillo, flequillo a mechones, pelo azul
 * (oscuro, claro y con reflejos) con las puntas amarillas y sus dos moños altos acabados en punta como rayos, piel
 * morena, traje azul marino con la raya de energía cian, malla lavanda y cinturón de cobre, guantes con los nudillos
 * brillando y la batería a la espalda con su luz cian. Las partes de energía brillan también de noche.
 */
public final class NeonPetRenderer {
	private static final Vox.Palette PALETTE = new Vox.Palette("neon_pet",
			'H', 0xFF263AD2, 'h', 0xFF3A5BFF, 'L', 0xFF6EAAFF, 'Y', 0xFFF5C542, 's', 0xFFC98B62, 'S', 0xFFA86F4C,
			'E', 0xFF141018, 'W', 0xFFFFFFFF, 'P', 0xFFD8E0EA, 'n', 0xFF1B2350, 'N', 0xFF12173A, 'b', 0xFF3A7BFF,
			'v', 0xFFB4A0FF, 'q', 0xFFC8784A, 'G', 0xFF5A6070, 'g', 0xFF1E1E2A, 'c', 0xFF78EBFF, 'w', 0xFFF0F0F5,
			'm', 0xFF7A3A3A);

	private Vox.Shape body;
	private Vox.Shape head;
	private Vox.Shape awakeEyes;
	private Vox.Shape sleepingEyes;
	private Vox.Shape bun;
	private Vox.Shape arm;
	private Vox.Shape energy;
	private Vox.Shape armEnergy;

	private void build() {
		body = new Vox.Shape(PALETTE)
				// Piernas con malla y raya azul, zapatillas blancas con la suela y un toque amarillo.
				.box('n', -2.8F, -4.0F, -1.4F, 2.6F, 3.0F, 2.8F)
				.box('n', 0.2F, -4.0F, -1.4F, 2.6F, 3.0F, 2.8F)
				.box('b', -2.0F, -4.0F, -1.5F, 0.6F, 3.0F, 0.2F)
				.box('b', 1.4F, -4.0F, -1.5F, 0.6F, 3.0F, 0.2F)
				.box('w', -2.9F, -1.2F, -1.8F, 2.8F, 1.2F, 3.3F)
				.box('w', 0.1F, -1.2F, -1.8F, 2.8F, 1.2F, 3.3F)
				.box('Y', -2.9F, -0.4F, -1.85F, 2.8F, 0.4F, 0.3F)
				.box('Y', 0.1F, -0.4F, -1.85F, 2.8F, 0.4F, 0.3F)
				// Torso: top azul marino, malla lavanda en la cintura y cinturón de cobre.
				.box('n', -3.0F, -9.0F, -2.0F, 6.0F, 3.2F, 4.0F)
				.box('N', -3.05F, -9.0F, -2.05F, 6.1F, 0.8F, 4.1F)
				.box('v', -2.9F, -5.8F, -1.9F, 5.8F, 1.0F, 3.8F)
				.box('q', -3.05F, -4.8F, -2.05F, 6.1F, 0.8F, 4.1F)
				.box('Y', -0.5F, -4.85F, -2.15F, 1.0F, 0.9F, 0.2F)
				// La batería a la espalda: carcasa gris con placas de cobre.
				.box('G', -2.0F, -8.6F, 2.0F, 4.0F, 3.4F, 1.4F)
				.box('q', -2.2F, -8.8F, 2.2F, 0.6F, 3.8F, 1.0F)
				.box('q', 1.6F, -8.8F, 2.2F, 0.6F, 3.8F, 1.0F);
		// Lo que brilla: la raya del pecho (cian y amarilla en zigzag) y la luz de la batería.
		energy = new Vox.Shape(PALETTE)
				.art(new String[] {
						"c....c",
						".c..Y.",
						"..cY..",
				}, -3.0F, -8.2F, -2.15F, 0.2F)
				.box('c', -0.8F, -7.6F, 3.4F, 1.6F, 1.6F, 0.2F);
		head = new Vox.Shape(PALETTE)
				.box('s', -5.0F, -9.0F, -4.5F, 10.0F, 9.0F, 9.0F)
				.box('S', -5.0F, -0.8F, -4.55F, 10.0F, 0.8F, 0.1F)
				// Casquete de pelo con reflejos claros y la melena de atrás con las puntas amarillas.
				.box('h', -5.5F, -10.2F, -4.9F, 11.0F, 3.2F, 10.4F)
				.box('L', -3.5F, -10.3F, -3.0F, 1.0F, 0.2F, 5.0F)
				.box('L', 2.0F, -10.3F, -2.0F, 1.0F, 0.2F, 4.0F)
				.box('H', -5.6F, -7.2F, 4.0F, 11.2F, 5.4F, 1.6F)
				.box('Y', -5.6F, -1.8F, 4.0F, 11.2F, 1.6F, 1.6F)
				.box('L', -3.0F, -6.5F, 5.65F, 1.0F, 3.0F, 0.1F)
				.box('L', 1.5F, -6.0F, 5.65F, 1.0F, 3.0F, 0.1F)
				// Mechones a los lados de la cara, con la punta amarilla.
				.box('h', -6.0F, -7.6F, -4.5F, 1.0F, 5.0F, 3.0F)
				.box('Y', -6.0F, -2.6F, -4.5F, 1.0F, 1.2F, 3.0F)
				.box('h', 5.0F, -7.6F, -4.5F, 1.0F, 5.0F, 3.0F)
				.box('Y', 5.0F, -2.6F, -4.5F, 1.0F, 1.2F, 3.0F)
				// Pinza de la nariz.
				.box('P', -0.6F, -3.4F, -4.75F, 1.2F, 0.6F, 0.3F);
		// Flequillo a mechones que cae sobre la frente.
		head.art(new String[] {
				"hhLhhhhLhh",
				"hhhhHhhhhh",
				"HhhHh.hHhH",
				".H..H..H..",
		}, -5.0F, -9.2F, -5.1F, 0.7F);
		awakeEyes = new Vox.Shape(PALETTE)
				.box('E', -3.8F, -5.4F, -4.75F, 2.2F, 2.2F, 0.3F)
				.box('E', 1.6F, -5.4F, -4.75F, 2.2F, 2.2F, 0.3F)
				.box('W', -3.4F, -5.1F, -4.85F, 0.7F, 0.7F, 0.2F)
				.box('W', 2.0F, -5.1F, -4.85F, 0.7F, 0.7F, 0.2F)
				.box('m', -0.6F, -2.2F, -4.7F, 1.2F, 0.4F, 0.2F);
		sleepingEyes = new Vox.Shape(PALETTE)
				.box('E', -3.8F, -4.2F, -4.75F, 2.2F, 0.5F, 0.3F)
				.box('E', 1.6F, -4.2F, -4.75F, 2.2F, 0.5F, 0.3F);
		// Moño con su punta en rayo: azul oscuro, claro y la punta amarilla.
		bun = new Vox.Shape(PALETTE)
				.box('h', -1.6F, -3.2F, -1.6F, 3.2F, 3.2F, 3.2F)
				.box('L', -0.8F, -3.3F, -1.7F, 1.0F, 0.3F, 1.0F)
				.box('H', -0.7F, -5.2F, -0.7F, 1.4F, 2.2F, 1.4F)
				.box('L', -0.5F, -6.6F, -0.3F, 1.0F, 1.6F, 1.0F)
				.box('Y', -0.35F, -7.8F, 0.1F, 0.7F, 1.4F, 0.7F);
		// Brazo con la piel del hombro, la manga con raya y el guante oscuro.
		arm = new Vox.Shape(PALETTE)
				.box('s', -1.0F, 0.0F, -1.0F, 2.0F, 1.2F, 2.0F)
				.box('n', -1.0F, 1.2F, -1.0F, 2.0F, 1.8F, 2.0F)
				.box('b', -1.05F, 1.8F, -1.05F, 2.1F, 0.4F, 2.1F)
				.box('g', -1.1F, 3.0F, -1.1F, 2.2F, 2.2F, 2.2F);
		armEnergy = new Vox.Shape(PALETTE).box('c', -0.9F, 4.3F, -1.25F, 1.8F, 0.5F, 0.2F).box('c', -1.25F, 3.4F, -0.5F, 0.2F, 1.0F, 1.0F);
	}

	public void render(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, NeonPetCosmetic module) {
		Vec3 position = module.follower.modelPosition(state.x, state.y, state.z, state.bodyRot, state.scale, state.ageInTicks % 1.0F);
		if (position == null) return;
		if (body == null) build();
		float time = state.ageInTicks;
		PetBehavior.Mood mood = PetBehavior.mood();
		float moodTime = PetBehavior.moodSeconds();

		// Flota, balancea la cabeza grande como un Funko y mueve un poco los brazos (ángulos en grados).
		float bob = Mth.sin(time * 0.12F) * 1.0F;
		float rightArmX = Mth.sin(time * 0.12F) * 11.0F;
		float leftArmX = -Mth.sin(time * 0.12F) * 11.0F;
		float rightArmZ = 9.0F;
		float leftArmZ = -9.0F;
		float headYaw = Mth.sin(time * 0.035F) * 17.0F;
		float headRoll = Mth.sin(time * 0.08F) * 3.5F;
		float headPitch = 0.0F;
		float shake = 0.0F;
		boolean sleeping = false;
		// Los moños se mecen un poco, como si les pasara la corriente.
		float bunSway = Mth.sin(time * 0.2F) * 4.0F;

		switch (mood) {
			case WAVE -> {
				rightArmX = -160.0F;
				rightArmZ = 12.0F + Mth.sin(moodTime * 12.0F) * 20.0F;
				headYaw = -17.0F;
			}
			case CELEBRATE -> {
				// Puño arriba y saltitos, como su pose.
				rightArmX = -172.0F;
				rightArmZ = 14.0F;
				leftArmX = -57.0F;
				bob -= Math.abs(Mth.sin(moodTime * 9.0F)) * 3.0F;
				bunSway = Mth.sin(time * 0.9F) * 10.0F;
			}
			case SLEEP -> {
				headPitch = 23.0F;
				headYaw = 0.0F;
				bob = Mth.sin(time * 0.05F) * 0.5F;
				rightArmX = 0.0F;
				leftArmX = 0.0F;
				sleeping = true;
				bunSway = 0.0F;
			}
			case HIDE -> {
				headPitch = 17.0F;
				shake = Mth.sin(time * 3.0F) * 0.3F;
				rightArmX = -69.0F;
				leftArmX = -69.0F;
			}
			default -> {
			}
		}

		boolean flying = PetBehavior.flying();
		if (flying) {
			// Volando con élitros: sprint eléctrico de Neon, brazos echados atrás y los moños al viento.
			rightArmX = 65.0F;
			leftArmX = 65.0F;
			rightArmZ = 14.0F;
			leftArmZ = -14.0F;
			headPitch = -15.0F;
			headYaw = 0.0F;
			bunSway = Mth.sin(time * 1.2F) * 14.0F;
		}

		poseStack.pushPose();
		poseStack.translate(position.x + shake / 16.0F, position.y + bob / 16.0F, position.z);
		float size = module.size.getFloat();
		poseStack.scale(size, size, size);
		poseStack.translate(0.0F, -com.freedomclient.cosmetic.vox.PetEmotes.hop() / 16.0F, 0.0F);
		com.freedomclient.cosmetic.vox.PetEmotes.hearts(poseStack, collector, -20.0F);
		if (flying) {
			poseStack.translate(0.0F, -9.0F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(55.0F));
			poseStack.translate(0.0F, 9.0F / 16.0F, 0.0F);
			com.freedomclient.cosmetic.vox.PetEmotes.speedLines(poseStack, collector, light, time, -9.0F);
		}
		body.draw(poseStack, collector, light);
		energy.drawGlow(poseStack, collector);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 4.0F / 16.0F, -9.0F / 16.0F, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(side < 0 ? rightArmX : leftArmX));
			poseStack.mulPose(Axis.ZP.rotationDegrees(side < 0 ? rightArmZ : leftArmZ));
			arm.draw(poseStack, collector, light);
			armEnergy.drawGlow(poseStack, collector);
			poseStack.popPose();
		}
		poseStack.translate(0.0F, -9.0F / 16.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
		poseStack.mulPose(Axis.ZP.rotationDegrees(headRoll));
		poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
		head.draw(poseStack, collector, light);
		(sleeping ? sleepingEyes : awakeEyes).draw(poseStack, collector, light);
		for (int side = -1; side <= 1; side += 2) {
			poseStack.pushPose();
			poseStack.translate(side * 3.6F / 16.0F, -9.6F / 16.0F, 1.0F / 16.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(side * (26.0F + bunSway)));
			poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F));
			bun.draw(poseStack, collector, light);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
