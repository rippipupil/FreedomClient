package com.freedomclient.cosmetic;

import com.freedomclient.particle.AuraParticle;
import com.freedomclient.particle.PixelParticles;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ModeSetting;
import com.freedomclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;

/** Aura: lucecitas pixel alrededor de ti, en varios estilos. */
public class AuraCosmetic extends CosmeticModule {
	public final ModeSetting style = add(new ModeSetting("Style", "Look of the aura.", "Angel light",
			"Angel light", "Devil embers", "Sky sparkles", "Starry", "Hearts", "Feathers", "Music"));
	public final NumberSetting amount = add(new NumberSetting("Amount", "How many lights float around you.", 3, 1, 8, 1));
	public final BooleanSetting firstPerson = add(new BooleanSetting("Show in first person", "Also show the aura in first person.", false));

	private final RandomSource random = RandomSource.create();

	public AuraCosmetic() {
		super("Aura", "Little pixel lights floating around you: angel light, devil embers, sky sparkles, stars, hearts, feathers or notes.");
	}

	@Override
	public void onTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (client.level == null || player == null || player.isInvisible() || client.isPaused()) return;
		if (!firstPerson.get() && client.options.getCameraType().isFirstPerson()) return;
		// Cada tick hay una probabilidad de que salga una luz nueva, según la cantidad elegida.
		if (random.nextFloat() > amount.getFloat() / 8.0F) return;

		double height = 0.1 + random.nextDouble() * 1.7;
		AuraParticle particle = switch (style.get()) {
			// Brasas: salen de los pies y suben girando despacio.
			case "Devil embers" -> new AuraParticle(client.level, player, PixelParticles.sprite("ember"), 0.5 + random.nextDouble() * 0.3,
					0.05, 0.05F, 0.035, false, 0.06F, 45);
			case "Sky sparkles" -> new AuraParticle(client.level, player, PixelParticles.sprite("sky_spark"), 0.75, height, 0.08F, 0.0, true, 0.06F, 40);
			case "Starry" -> new AuraParticle(client.level, player, PixelParticles.sprite("star"), 0.9, height, 0.04F, 0.0, true, 0.07F, 50);
			case "Hearts" -> new AuraParticle(client.level, player, PixelParticles.sprite("heart"), 0.6, 0.3 + random.nextDouble(), 0.03F, 0.02, false, 0.07F, 45);
			case "Feathers" -> new AuraParticle(client.level, player, PixelParticles.sprite("feather"), 0.8, 1.8, 0.06F, -0.03, false, 0.08F, 55);
			case "Music" -> new AuraParticle(client.level, player, PixelParticles.sprite("note"), 0.7, height, 0.06F, 0.01, false, 0.07F, 40);
			// Luz de ángel: chispas doradas que giran y suben un poco.
			default -> new AuraParticle(client.level, player, PixelParticles.sprite("spark"), 0.7, height, 0.07F, 0.012, true, 0.06F, 40);
		};
		client.particleEngine.add(particle);
	}
}
