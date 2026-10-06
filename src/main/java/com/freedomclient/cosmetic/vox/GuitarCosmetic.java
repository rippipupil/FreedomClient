package com.freedomclient.cosmetic.vox;

import com.freedomclient.particle.GlowParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Electric Guitar (Música): guitarra eléctrica roja de doble cutaway a la espalda, en diagonal como las guadañas.
 * Cuerpo rojo con golpeador blanco, tres pastillas, puente cromado, mástil de arce con diapasón oscuro y la pala
 * crema con sus clavijas. De vez en cuando suelta notas musicales.
 */
public class GuitarCosmetic extends MusicGuitarCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("guitar",
			'r', 0xFFC8182A, 'R', 0xFF8E0E1C, 'h', 0xFFE84050, 'w', 0xFFF4F2EC, 'W', 0xFFCFCBC2, 'k', 0xFF1E1A1E,
			's', 0xFFD8DCE4, 'S', 0xFF9AA0AC, 'n', 0xFFE6C48A, 'N', 0xFFC49A5E, 'f', 0xFF4A2A1C, 'c', 0xFFF2E3C2);
	/** Cuerpo visto de frente: 16 de ancho, la fila 0 justo debajo del mástil. */
	private static final String[] BODY = {
			"..rR........Rr..",
			".rrrR......Rrrr.",
			".rrrrR....Rrrrr.",
			".rrrrrrrrrrrrrr.",
			"rrrrrrrrrrrrrrrr",
			"rrrrrrrrrrrrrrrr",
			"rrrrrrrrrrrrrrrr",
			".rrrrrrrrrrrrrr.",
			"..rrrrrrrrrrrr..",
			".rrrrrrrrrrrrrr.",
			"rrrrrrrrrrrrrrrr",
			"rrrrrrrrrrrrrrrr",
			"RrrrrrrrrrrrrrrR",
			".RrrrrrrrrrrrrR.",
			"..RRrrrrrrrrRR..",
			"....RRRRRRRR....",
	};
	/** Golpeador blanco con su borde, encima del cuerpo. */
	private static final String[] GUARD = {
			"..WWWW..",
			".WwwwwW.",
			"WwwwwwwW",
			"Wwwwwwww",
			"Wwwwwwww",
			"WwwwwwwW",
			".Wwwwwww",
			"..WwwwwW",
			"...WwwW.",
	};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the guitar is.", 0.75, 0.5, 1.1, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Neck side", "Which shoulder the neck sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting notes = add(new BooleanSetting("Music notes", "Little music notes float out of the guitar now and then.", true));

	private final RandomSource random = RandomSource.create();
	private Vox.Shape guitar;

	public GuitarCosmetic() {
		super("Electric Guitar", "Music: a red electric guitar on your back with a white pickguard, maple neck and chrome bridge. Plays little music notes.");
	}

	private void build() {
		// La cara de delante (golpeador y cuerdas) mira hacia +z, hacia fuera de la espalda.
		guitar = new Vox.Shape(PALETTE)
				.art(BODY, -8.0F, 0.0F, -1.2F, 2.4F)
				.art(GUARD, -4.0F, 3.0F, 1.2F, 0.3F)
				// Tres pastillas y el puente.
				.box('k', -2.2F, 4.0F, 1.4F, 4.4F, 0.8F, 0.3F)
				.box('k', -2.4F, 6.0F, 1.4F, 4.4F, 0.8F, 0.3F)
				.box('k', -2.6F, 8.2F, 1.4F, 4.4F, 0.8F, 0.3F)
				.box('s', -2.6F, 10.4F, 1.3F, 5.0F, 1.2F, 0.4F)
				.box('S', -2.6F, 11.4F, 1.3F, 5.0F, 0.3F, 0.4F)
				// Potenciómetros y selector.
				.box('s', 2.6F, 9.2F, 1.4F, 0.9F, 0.9F, 0.3F)
				.box('s', 3.6F, 10.6F, 1.4F, 0.9F, 0.9F, 0.3F)
				.box('w', 3.2F, 7.6F, 1.4F, 1.0F, 0.5F, 0.3F)
				// Mástil de arce con el diapasón oscuro y los trastes.
				.box('n', -1.0F, -16.0F, -0.6F, 2.0F, 16.6F, 1.6F)
				.box('f', -0.9F, -16.0F, 1.0F, 1.8F, 16.0F, 0.3F)
				// Pala crema con las seis clavijas a un lado.
				.box('c', -1.0F, -21.6F, -0.5F, 2.2F, 5.6F, 1.4F)
				.box('c', 1.2F, -21.2F, -0.5F, 1.2F, 4.0F, 1.4F)
				.box('N', -1.0F, -21.9F, -0.5F, 2.6F, 0.4F, 1.4F);
		for (int i = 0; i < 8; i++) guitar.box('S', -0.9F, -15.0F + i * 1.9F, 1.25F, 1.8F, 0.2F, 0.15F);
		for (int i = 0; i < 6; i++) guitar.box('s', 2.4F, -21.0F + i * 0.7F, 0.0F, 0.7F, 0.4F, 0.4F);
		// Seis cuerdas finas desde la pala hasta el puente.
		for (int i = 0; i < 3; i++) guitar.box('s', -0.6F + i * 0.5F, -16.0F, 1.4F, 0.15F, 27.0F, 0.1F);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (guitar == null) build();
		poseStack.pushPose();
		float scale = size.getFloat() * 0.62F;
		if (inHands(state)) {
			inHands(parent, poseStack, state, scale, scale, 1.2F);
		} else {
			onBackDiagonal(parent, poseStack, state, scale, side.is("Left"), 1.2F);
		}
		// El cuerpo de la guitarra queda en el centro de la espalda y el mástil asoma por encima del hombro.
		poseStack.translate(0.0F, -7.0F / 16.0F, 0.0F);
		guitar.draw(poseStack, collector, light);
		poseStack.popPose();
	}

	/** Notas musicales que salen de la guitarra y suben despacio (solo en tercera persona). */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!notes.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || random.nextFloat() > 0.06F) return;
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw);
		double backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		double lateral = (random.nextDouble() - 0.5) * 0.5;
		double x = player.getX() + rightX * lateral + backX * 0.4;
		double y = player.getY() + 0.9 + random.nextDouble() * 0.5;
		double z = player.getZ() + rightZ * lateral + backZ * 0.4;
		GlowParticle note = new GlowParticle(client.level, x, y, z, (random.nextDouble() - 0.5) * 0.01, 0.025, (random.nextDouble() - 0.5) * 0.01,
				PixelParticles.sprite("note"), 0.07F, 0.09F, 30 + random.nextInt(15));
		client.particleEngine.add(note.thirdPersonOnly());
	}
}
