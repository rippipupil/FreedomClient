package com.freedomclient.mixin;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.stream.Stream;

/**
 * Menú de paquetes de recursos: solo se ven Minecraft, FreedomClient y los paquetes del jugador. Los internos (el de
 * los mods de Fabric, los de Continuity) se ocultan; siguen en la lista del juego, así que al pulsar "Listo" no se
 * desactivan.
 */
@Mixin(TransferableSelectionList.class)
public class TransferableSelectionListMixin {
	@ModifyVariable(method = "updateList", at = @At("HEAD"), argsOnly = true)
	private Stream<PackSelectionModel.Entry> freedomclient$hideInternalPacks(Stream<PackSelectionModel.Entry> entries) {
		return entries.filter(entry -> !com.freedomclient.pack.FreedomPack.isInternal(entry.getId()));
	}
}
