package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Niko Scarf (OneShot): bufanda gruesa morada y azul alrededor del cuello, con dos puntas que salen hacia atrás y se
 * curvan hacia arriba. Cada punta acaba en cuatro trozos sueltos que se van haciendo pequeños, como la del dibujo.
 * Las puntas tienen física: se levantan al correr, suben al caer, se quedan atrás al girar y ondean con el viento.
 */
public class NikoScarfCosmetic extends VoxCosmetic {
	static final Vox.Palette PALETTE = new Vox.Palette("niko_scarf",
			'p', 0xFF7A5CD6, 'b', 0xFF3322B0, 'h', 0xFF9C84F0, 'd', 0xFF1E1470);
	/** Largo de cada tramo de la punta, en píxeles del modelo. */
	private static final float SEGMENT = 2.0F;
	private static final int PIECES = 4;

	private Vox.Shape wrap;
	private Vox.Shape segment;
	private final Vox.Shape[] pieces = new Vox.Shape[PIECES];

	// Física de las puntas (grados), cada tick; al dibujar se interpola.
	private float lift = 25.0F;
	private float liftVelocity;
	private float previousLift = 25.0F;
	private float sway;
	private float swayVelocity;
	private float previousSway;
	private float lastBodyRot = Float.NaN;

	public NikoScarfCosmetic() {
		super("Niko Scarf", "OneShot: Niko's thick purple-blue scarf. Its two tails fly back behind you, end in four loose pieces "
				+ "and move with you: they rise when you run or fall and trail behind when you turn.", CosmeticSlot.NECK);
	}

	private void build() {
		// Vuelta al cuello: 1 px de grosor y 3.2 de alto, con una franja clara arriba y el nudo detrás.
		wrap = new Vox.Shape(PALETTE)
				.box('p', -5.0F, -1.2F, -3.2F, 10.0F, 3.2F, 1.1F)
				.box('b', -5.0F, -1.2F, 2.1F, 10.0F, 3.2F, 1.1F)
				.box('p', -5.0F, -1.2F, -2.1F, 1.1F, 3.2F, 4.2F)
				.box('b', 3.9F, -1.2F, -2.1F, 1.1F, 3.2F, 4.2F)
				.box('h', -5.0F, -1.4F, -3.35F, 10.0F, 0.7F, 0.4F)
				.box('d', -5.0F, 1.6F, -3.3F, 10.0F, 0.5F, 0.3F)
				.box('b', -2.2F, -0.6F, 3.0F, 4.4F, 2.6F, 1.2F)
				.box('p', -1.4F, -0.2F, 3.9F, 2.8F, 1.8F, 0.6F);
		// Tramo de la punta colgando hacia +y: cara clara por fuera y oscura por dentro.
		segment = new Vox.Shape(PALETTE)
				.box('p', -1.5F, 0.0F, -0.6F, 3.0F, SEGMENT + 0.2F, 0.8F)
				.box('b', -1.5F, 0.0F, 0.2F, 3.0F, SEGMENT + 0.2F, 0.5F)
				.box('h', -1.5F, 0.0F, -0.7F, 0.5F, SEGMENT + 0.2F, 0.2F);
		for (int i = 0; i < PIECES; i++) {
			float size = 2.6F - i * 0.45F;
			pieces[i] = new Vox.Shape(PALETTE)
					.box('p', -size / 2.0F, 0.0F, -0.5F, size, size, 0.7F)
					.box('b', -size / 2.0F, size * 0.55F, 0.2F, size, size * 0.45F, 0.4F);
		}
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		previousLift = lift;
		previousSway = sway;
		if (player == null) {
			lastBodyRot = Float.NaN;
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		float turn = Float.isNaN(lastBodyRot) ? 0.0F : Mth.wrapDegrees(player.yBodyRot - lastBodyRot);
		// Un salto de golpe (teletransporte, girar la cámara de repente) no es un giro de verdad.
		if (Math.abs(turn) > 40.0F) turn = 0.0F;
		lastBodyRot = player.yBodyRot;

		// Quieta cuelga un poco hacia atrás; corriendo casi en horizontal; cayendo sube y saltando baja.
		float targetLift = 22.0F + (float) Math.min(62.0, speed * 240.0) + (float) Mth.clamp(-motion.y * 90.0, -12.0, 34.0);
		// Al girar, las puntas se quedan hacia el lado contrario.
		float targetSway = Mth.clamp(-turn * 1.2F, -18.0F, 18.0F);
		liftVelocity = (liftVelocity + (targetLift - lift) * 0.22F) * 0.72F;
		lift += liftVelocity;
		swayVelocity = (swayVelocity + (targetSway - sway) * 0.2F) * 0.7F;
		sway += swayVelocity;
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (wrap == null) build();
		float time = state.ageInTicks;
		float partial = time % 1.0F;
		float walk = Math.min(1.0F, state.walkAnimationSpeed);
		float baseLift = Mth.lerp(partial, previousLift, lift);
		float baseSway = Mth.lerp(partial, previousSway, sway);

		poseStack.pushPose();
		parent.body.translateAndRotate(poseStack);
		// Con peto se agranda para quedar por fuera (el peto sobresale 1 px del cuerpo).
		if (!state.chestEquipment.isEmpty()) poseStack.scale(1.18F, 1.0F, 1.4F);
		wrap.draw(poseStack, collector, light);
		for (int tail = 0; tail < 2; tail++) {
			float side = tail == 0 ? -1.0F : 1.0F;
			// La punta izquierda es un tramo más larga, como en la bufanda de Niko.
			int segments = tail == 0 ? 5 : 4;
			poseStack.pushPose();
			poseStack.translate(side * 1.3F / 16.0F, 0.6F / 16.0F, 4.2F / 16.0F);
			// Abiertas en V hacia los lados y hacia atrás.
			poseStack.mulPose(Axis.ZP.rotationDegrees(baseSway + side * (5.0F + walk * 4.0F)));
			poseStack.mulPose(Axis.YP.rotationDegrees(side * 8.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(baseLift + tail * 6.0F));
			for (int i = 0; i < segments; i++) {
				segment.draw(poseStack, collector, light);
				poseStack.translate(0.0F, SEGMENT / 16.0F, 0.0F);
				// Se va curvando hacia arriba y ondea como una tela, más al correr.
				float wave = Mth.sin(time * (0.22F + walk * 0.18F) - i * 0.9F + tail * 1.7F) * (4.0F + walk * 9.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(11.0F + walk * 5.0F + wave));
				poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.13F - i * 0.7F + tail) * 2.0F));
			}
			// Cuatro trozos sueltos al final, cada vez más pequeños y un poco torcidos.
			for (int i = 0; i < PIECES; i++) {
				poseStack.translate(0.0F, (0.9F + (i == 0 ? 0.3F : 0.0F)) / 16.0F, 0.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(9.0F + Mth.sin(time * 0.3F - i * 1.1F + tail * 2.0F) * (5.0F + walk * 8.0F)));
				poseStack.pushPose();
				float jitter = Mth.sin(time * 0.4F + i * 2.3F + tail) * 4.0F;
				poseStack.mulPose(Axis.ZP.rotationDegrees((i % 2 == 0 ? 16.0F : -12.0F) * side + jitter));
				pieces[i].draw(poseStack, collector, light);
				poseStack.popPose();
				poseStack.translate(0.0F, (2.6F - i * 0.45F) / 16.0F, 0.0F);
			}
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
