package com.freedomclient.cosmetic.vox;

import com.freedomclient.cosmetic.PetBehavior;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Jack-o'-Lantern (Halloween): calabaza que vuela a tu lado. Casi siempre está feliz con cara kawaii, pero de vez en
 * cuando (al azar) se vuelve malvada: ojos y boca tallados que brillan en rojo. El cambio parpadea un momento.
 */
public class JackOLanternCosmetic extends FollowPetCosmetic {
	private static final Vox.Palette PALETTE = new Vox.Palette("jack_o_lantern",
			'o', 0xFFF08A24, 'O', 0xFFD06E14, 'g', 0xFF5A7A2A, 'l', 0xFF6FB83A,
			'k', 0xFF3A1A08, 'w', 0xFFFFFFFF, 'p', 0xFFF48AA6, 'r', 0xFFFF2A2A, 'R', 0xFFB01010);
	private final RandomSource random = RandomSource.create();
	private Vox.Shape pumpkin;
	private Vox.Shape happy;
	private Vox.Shape evil;
	private boolean isEvil;
	private int ticksToSwitch = 400;
	private int flicker;

	public JackOLanternCosmetic() {
		super("Jack-o'-Lantern", "Halloween: a flying pumpkin with a cute kawaii face that sometimes turns evil with glowing red eyes.");
	}

	@Override
	public void onTick(Minecraft client) {
		super.onTick(client);
		if (flicker > 0) flicker--;
		// Al azar: feliz casi todo el rato (20-50 s) y malvada un ratito (6-12 s).
		if (--ticksToSwitch <= 0) {
			isEvil = !isEvil;
			flicker = 16;
			ticksToSwitch = isEvil ? 120 + random.nextInt(120) : 400 + random.nextInt(600);
		}
	}

	/** Para las pruebas: pone la cara malvada o la feliz sin parpadeo. */
	public void forceEvil(boolean evil) {
		isEvil = evil;
		flicker = 0;
		ticksToSwitch = 400;
	}

	private void build() {
		pumpkin = new Vox.Shape(PALETTE);
		// Gajos verticales: franjas alternas de delante a atrás, más bajas en los bordes para que salga redonda.
		for (int x = -4; x <= 4; x++) {
			boolean edge = Math.abs(x) == 4;
			float inset = edge ? 1.0F : Math.abs(x) == 3 ? 0.4F : 0.0F;
			pumpkin.box((x & 1) == 0 ? 'o' : 'O', x - 0.5F, -8.0F + inset, -4.5F + inset, 1.0F, 8.0F - inset * 2, 9.0F - inset * 2);
		}
		pumpkin.box('g', -0.8F, -10.0F, -0.8F, 1.6F, 2.2F, 1.6F).box('l', 0.8F, -9.0F, -0.6F, 2.6F, 0.4F, 1.6F);
		happy = new Vox.Shape(PALETTE).art(new String[] {
				".kk...kk.",
				".wk...wk.",
				".........",
				"p..k.k..p",
				"....k....",
		}, -4.5F, -6.2F, -4.9F, 0.4F);
		evil = new Vox.Shape(PALETTE).art(new String[] {
				"rr.....rr",
				"Rrr...rrR",
				".Rr...rR.",
				".........",
				"r.r.r.r.r",
				"RrRrRrRrR",
				".R.R.R.R.",
		}, -4.5F, -6.8F, -4.9F, 0.4F);
	}

	@Override
	protected void renderPet(PoseStack poseStack, SubmitNodeCollector collector, int light, float time, PetBehavior.Mood mood, float moodSeconds) {
		if (pumpkin == null) build();
		boolean showEvil = flicker > 0 ? (flicker / 2) % 2 == 0 ? isEvil : !isEvil : isEvil;
		float bob = Mth.sin(time * 0.12F) * 1.0F;
		poseStack.translate(0.0F, bob / 16.0F, 0.0F);
		poseStack.translate(0.0F, -4.0F / 16.0F, 0.0F);
		// Feliz se mece; malvada tiembla un poco.
		if (showEvil) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 2.2F) * 2.5F));
		} else {
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.1F) * 8.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.04F) * 15.0F));
		}
		poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
		pumpkin.draw(poseStack, collector, light);
		if (showEvil) {
			evil.drawGlow(poseStack, collector);
		} else {
			happy.draw(poseStack, collector, light);
		}
	}
}
