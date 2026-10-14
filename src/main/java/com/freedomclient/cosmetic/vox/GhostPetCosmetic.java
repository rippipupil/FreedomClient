package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticPreview;
import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.cosmetic.PetBehavior;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Ghostie: el fantasmita rosa de FreedomPlayer en pixel 3D, que vuela dando vueltas a tu alrededor a la altura de la
 * cabeza y cambia de cara cada pocos segundos. Saluda con el bracito al entrar, gira encima de tu cabeza en las kills,
 * se duerme a tu lado (en blanco, con "z z") si estás AFK, se esconde detrás temblando con poca vida, se asusta si te
 * pegan y vuela detrás de ti con élitros; contenta da saltitos, sonrojada pone UwU y suelta corazones.
 * <p>
 * Todo sale del diseño (scripts/textures/ghost/ghost.html) con las cuentas de {@link GhostMotion}, comprobadas número a
 * número. La vuelta va fija al mundo (no gira contigo); los sitios de cada estado van respecto a hacia donde miras.
 */
public class GhostPetCosmetic extends VoxCosmetic {
	public final ModeSetting color = add(new ModeSetting("Color",
			"Automatic: pink when happy, white when surprised, sleepy or sad (like in FreedomPlayer).", "Automatic", "Automatic", "Pink", "White"));
	public final NumberSetting size = add(new NumberSetting("Size", "Size of the ghost.", GhostMotion.SCALE, 0.5, 1.2, 0.05, "x"));

	private static final Vox.Palette FX = new Vox.Palette("ghost_fx", 'r', 0xFFFF4F7A, 'R', 0xFFD8325A, 'w', 0xFFFFD0DC, 'z', 0xFFF4A6CB);
	private static final String[] HEART = {".rr.rr.", "rwrrrrr", "rrrrrrr", ".rrrrR.", "..rrR..", "...R..."};
	private static final String[] Z = {"zzz", "..z", ".z.", "z..", "zzz"};
	private static Vox.Shape heart, zee;

	// Estado entre fotogramas: el estado de ahora, desde cuándo, de dónde venía y el último golpe.
	private GhostMotion.Mood mood = GhostMotion.Mood.IDLE;
	private double moodAt = -1;
	private final GhostMotion.Pose from = new GhostMotion.Pose(), last = new GhostMotion.Pose(), pose = new GhostMotion.Pose();
	private boolean hasFrom, hasLast;
	private long hurtAt = -100_000;
	private int lastHurtTime;
	private final double[] limbs = new double[3];

	public GhostPetCosmetic() {
		super("Ghostie", "A pink pixel ghost from FreedomPlayer that flies around you changing faces. Waves, celebrates kills, "
				+ "sleeps when you are AFK, hides when you are low and gets scared when you are hit.", CosmeticSlot.PET);
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;
		// Te acaban de pegar: el tiempo de daño sube.
		if (player.hurtTime > lastHurtTime) hurtAt = System.currentTimeMillis();
		lastHurtTime = player.hurtTime;
	}

	/** El estado de ahora con las prioridades del juego: élitros, poca vida, golpe y después los de PetBehavior. */
	private GhostMotion.Mood currentMood() {
		if (PetBehavior.flying()) return GhostMotion.Mood.FLY;
		PetBehavior.Mood m = PetBehavior.mood();
		if (m == PetBehavior.Mood.HIDE) return GhostMotion.Mood.HIDE;
		if (System.currentTimeMillis() - hurtAt < 1600) return GhostMotion.Mood.HURT;
		return switch (m) {
			case WAVE -> GhostMotion.Mood.WAVE;
			case CELEBRATE -> GhostMotion.Mood.CELEBRATE;
			case SLEEP -> GhostMotion.Mood.SLEEP;
			default -> GhostMotion.Mood.IDLE;
		};
	}

	private static GhostMotion.Emotion emotion() {
		return switch (PetBehavior.emotion()) {
			case HAPPY -> GhostMotion.Emotion.HAPPY;
			case BLUSH -> GhostMotion.Emotion.BLUSH;
			case HEARTS -> GhostMotion.Emotion.HEARTS;
			default -> GhostMotion.Emotion.NONE;
		};
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		boolean preview = CosmeticPreview.of(state) != null;
		double t = state.ageInTicks / 20.0;
		GhostMotion.Mood m = preview ? GhostMotion.Mood.IDLE : currentMood();
		GhostMotion.Emotion e = preview ? GhostMotion.Emotion.NONE : emotion();
		// Al cambiar de estado se mezcla desde donde estaba (la vista previa del menú no toca este estado).
		if (!preview && (m != mood || moodAt < 0)) {
			mood = m;
			moodAt = t;
			hasFrom = hasLast;
			from.copy(last);
		}
		if (preview) m = GhostMotion.Mood.IDLE;
		else m = mood;
		double s = preview ? 0 : Math.max(0, t - moodAt);
		poseStack.pushPose();
		if (preview) {
			// En la tarjeta del menú, la vuelta más pequeña para que quepa.
			VoidKeysCosmetic.ringFrame(null, 0.0F, poseStack);
			poseStack.translate(0.0F, 16.0F / 16.0F, 0.0F);
			poseStack.scale(0.55F, 0.55F, 0.55F);
			poseStack.translate(0.0F, -16.0F / 16.0F, 0.0F);
		} else {
			// Fijo al mundo: se deshace el giro del cuerpo.
			VoidKeysCosmetic.ringFrame(null, state.bodyRot, poseStack);
		}
		GhostMotion.pose(m, t, s, null, pose);
		if (GhostMotion.anchor(m, t, s, new GhostMotion.Pose())) {
			// Los sitios de cada estado van respecto a hacia donde miras: se giran como el cuerpo.
			double phi = preview ? 0 : -Math.toRadians(state.bodyRot);
			double x = pose.x * Math.cos(phi) + pose.z * Math.sin(phi), z = -pose.x * Math.sin(phi) + pose.z * Math.cos(phi);
			pose.set(x, pose.y, z, pose.yaw + phi, pose.pitch, pose.roll);
		}
		if (hasFrom && s < GhostMotion.BLEND && !preview) {
			double k = GhostMotion.smooth(s / GhostMotion.BLEND);
			pose.set(from.x + (pose.x - from.x) * k, from.y + (pose.y - from.y) * k, from.z + (pose.z - from.z) * k,
					from.yaw + Math.atan2(Math.sin(pose.yaw - from.yaw), Math.cos(pose.yaw - from.yaw)) * k, from.pitch + (pose.pitch - from.pitch) * k,
					from.roll + (pose.roll - from.roll) * k);
		}
		if (!preview) {
			last.copy(pose);
			hasLast = true;
		}

		int face = GhostMotion.face(m, e, t, s);
		boolean white = color.is("Automatic") ? com.freedomclient.ui.GhostArt.whiteFace(face) : color.is("White");
		GhostMotion.limbs(m, e, t, s, PetBehavior.emotionSeconds(), limbs);
		float scale = size.getFloat();

		poseStack.pushPose();
		poseStack.translate((float) pose.x / 16.0F, (float) (pose.y + limbs[2]) / 16.0F, (float) pose.z / 16.0F);
		poseStack.mulPose(Axis.YP.rotation((float) pose.yaw));
		poseStack.scale(scale, scale, scale);
		poseStack.mulPose(Axis.XP.rotation((float) pose.pitch));
		poseStack.mulPose(Axis.ZP.rotation((float) pose.roll));
		GhostModel.draw(poseStack, collector, light, face, white, limbs[0], limbs[1]);
		poseStack.popPose();

		if (heart == null) {
			heart = new Vox.Shape(FX);
			for (int r = 0; r < HEART.length; r++) for (int c = 0; c < HEART[r].length(); c++) {
				char ch = HEART[r].charAt(c);
				if (ch != '.') heart.box(ch, c - 3.5F, -r, -0.5F, 1, 1, 1);
			}
			zee = new Vox.Shape(FX);
			for (int r = 0; r < Z.length; r++) for (int c = 0; c < Z[r].length(); c++) if (Z[r].charAt(c) != '.') zee.box('z', c * 0.5F, -r * 0.5F, -0.1F, 0.5F, 0.5F, 0.2F);
		}
		// Corazones que suben (3,5 s) y la "z z" de dormir, como en el diseño.
		double es = PetBehavior.emotionSeconds();
		if (e == GhostMotion.Emotion.HEARTS && es < 3.5) {
			for (int i = 0; i < 3; i++) {
				double u = (es * 0.6 + i / 3.0) % 1;
				fx(poseStack, collector, light, heart, pose.x + (i - 1) * 3, pose.y + 14 + u * 8, pose.z, pose.yaw, 0.35 * (1 - u * 0.5));
			}
		}
		if (m == GhostMotion.Mood.SLEEP) {
			for (int i = 0; i < 2; i++) {
				double u = (t * 0.5 + i * 0.5) % 1;
				fx(poseStack, collector, light, zee, pose.x + 4 + u * 3, pose.y + 12 + u * 6, pose.z, pose.yaw, 1 + u);
			}
		}
		poseStack.popPose();
	}

	private static void fx(PoseStack poseStack, SubmitNodeCollector collector, int light, Vox.Shape shape, double x, double y, double z, double yaw, double s) {
		poseStack.pushPose();
		poseStack.translate((float) x / 16.0F, (float) y / 16.0F, (float) z / 16.0F);
		poseStack.mulPose(Axis.YP.rotation((float) yaw));
		poseStack.scale((float) s, (float) s, (float) s);
		shape.draw(poseStack, collector, light);
		poseStack.popPose();
	}
}
