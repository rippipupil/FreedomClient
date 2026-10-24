package com.freedomclient.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Arreglo del AAC de jcodec (JAAD) para el Music Player: su ruido de PNS hace {@code randomState *= 1664525 + 1013904223},
 * que por la precedencia multiplica por un número par. A las 32 vueltas el estado es 0, la energía de la banda es 0, sale
 * 0 / 0 = NaN y el NaN se queda en el solapamiento del filtro: desde ahí toda la canción suena en silencio. Con el
 * multiplicador impar del LCG ({@code 1664525}) el estado nunca llega a 0.
 */
@Mixin(targets = "net.sourceforge.jaad.aac.syntax.ICStream", remap = false)
public class JaadNoiseMixin {
	@ModifyConstant(method = "decodeSpectralData", constant = @Constant(intValue = 1015568748))
	private int freedomclient$oddMultiplier(int multiplier) {
		return 1664525;
	}
}
