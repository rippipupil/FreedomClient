package com.freedomclient.mixin;

import com.freedomclient.pack.FreedomPack;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
	@Shadow
	@Final
	@Mutable
	private Set<RepositorySource> sources;

	/** Añade el paquete único de FreedomClient a los paquetes de recursos del cliente (no a los de datos de los mundos). */
	@Inject(method = "<init>", at = @At("TAIL"))
	private void freedomclient$addPack(RepositorySource[] args, CallbackInfo ci) {
		boolean client = false;
		for (RepositorySource source : sources) client |= source instanceof ClientPackSource;
		if (!client) return;
		Set<RepositorySource> all = new LinkedHashSet<>(sources);
		all.add(FreedomPack.SOURCE);
		sources = all;
	}
}
