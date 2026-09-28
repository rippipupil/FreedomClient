package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.CosmeticSlot;
import com.freedomclient.particle.EmberParticle;
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
 * Dark Scythe (Bee Swarm): guadaña negra de doble hoja con el filo rojo y el mango que acaba en morado, colgada de
 * lado a la espalda como la Soul Scythe, con brasas pixel 3D rosas y rojas saliendo de la parte de arriba.
 */
public class DarkScytheCosmetic extends VoxCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("dark_scythe",
			'k', 0xFF121216, 'K', 0xFF24242A, 'r', 0xFFC0182C, 'R', 0xFF7A0E1E, 'h', 0xFF1A1A20, 'p', 0xFF7A2FB8, 'P', 0xFF9A4AD8);
	/** Colores de las brasas: rosa, rojo, fucsia y rosa claro. */
	private static final int[] EMBERS = {0xFF6FA8, 0xFF3050, 0xE83CB8, 0xFF9AC8};
	/** k = hoja negra, K = negro de relieve, r/R = filo rojo, h = mango, p/P = final morado del mango. */
	private static final String[] SCYTHE = {
			"................k....",
			"...............kk....",
			"..k...........kK.....",
			"..kk.........kK......",
			"...kK.......kK.......",
			"....kK.....kKh.......",
			".....kK...kKhh.......",
			"...kkkkkkkkkhh.......",
			"..kkkkkkkkkkh........",
			".kkkKrrrrr..h........",
			".kkKrr......h........",
			"kkKrr.......h........",
			"kkKr........h........",
			"kkr.........h........",
			"kkr.........h........",
			"kkR.........h........",
			"kR..........h........",
			"kR..........h........",
			"R...........h........",
			"R...........h........",
			"............h........",
			"............h........",
			"............p........",
			"............P........",
			"............p........",
			"............P........",
			"............pp.......",
	};

	public final NumberSetting size = add(new NumberSetting("Size", "How big the scythe is.", 0.9, 0.5, 1.3, 0.05, "x"));
	public final ModeSetting side = add(new ModeSetting("Blade side", "Which shoulder the blade sticks out over.", "Left", "Left", "Right"));
	public final BooleanSetting embers = add(new BooleanSetting("Embers", "Little pixel 3D embers rising from the top of the scythe (third person only).", true));
	private final RandomSource random = RandomSource.create();
	private Vox.Shape scythe;

	public DarkScytheCosmetic() {
		super("Dark Scythe", "Bee Swarm: a black double-bladed scythe with a red edge and a purple handle end, with rising pixel embers.",
				CosmeticSlot.BACK);
	}

	@Override
	public void render(PlayerModel parent, PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state) {
		if (scythe == null) {
			// Hoja con grosor 2 y el mango de 1.5, centrada en el mango (columna 12, fila 13).
			scythe = new Vox.Shape(PALETTE).art(SCYTHE, -12.5F, -13.0F, -1.0F, 2.0F);
		}
		poseStack.pushPose();
		onBackDiagonal(parent, poseStack, state, size.getFloat(), side.is("Left"));
		scythe.draw(poseStack, collector, light);
		poseStack.popPose();
	}

	/** Brasas que salen de lo alto de la hoja (sobre el hombro), solo en tercera persona. */
	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!embers.get() || client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (client.options.getCameraType().isFirstPerson() || random.nextFloat() > 0.45F) return;
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double backX = Mth.sin(yaw), backZ = -Mth.cos(yaw);
		double rightX = -Mth.cos(yaw), rightZ = -Mth.sin(yaw);
		double bladeSide = side.is("Right") ? 1.0 : -1.0;
		double scale = size.get();
		double lateral = (0.3 + random.nextDouble() * 0.4) * bladeSide * scale;
		double height = 0.9 + (1.45 + random.nextDouble() * 0.35 - 0.9) * scale - (player.isCrouching() ? 0.3 : 0.0);
		double back = 0.28 + random.nextDouble() * 0.1;
		double x = player.getX() + rightX * lateral + backX * back;
		double y = player.getY() + height;
		double z = player.getZ() + rightZ * lateral + backZ * back;
		client.particleEngine.add(new EmberParticle(client.level, x, y, z,
				(random.nextDouble() - 0.5) * 0.01, 0.01 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.01,
				EMBERS[random.nextInt(EMBERS.length)], 0.05F + random.nextFloat() * 0.03F, 18 + random.nextInt(12)));
	}
}
