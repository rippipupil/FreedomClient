package com.freedomclient.mixin;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.stream.Stream;

/**
 * Menú de paquetes de recursos: los paquetes internos de FreedomClient (Gap Counter, Better Grass, colores del
 * brillo...) no se enseñan uno a uno, solo la entrada "FreedomClient"; los controlan los mods desde el menú del
 * cliente. Siguen en la lista del juego, así que al pulsar "Listo" no se desactivan.
 */
@Mixin(TransferableSelectionList.class)
public class TransferableSelectionListMixin {
	@ModifyVariable(method = "updateList", at = @At("HEAD"), argsOnly = true)
	private Stream<PackSelectionModel.Entry> freedomclient$hideInternalPacks(Stream<PackSelectionModel.Entry> entries) {
		return entries.filter(entry -> {
			String id = entry.getId();
			return !id.contains("freedomclient") || id.endsWith("fc_resources");
		});
	}
}
