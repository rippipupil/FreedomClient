package com.freedomclient.mixin;

import com.freedomclient.module.visual.ShulkerPreviewModule;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	/** Añade la vista previa del contenido a los contenedores que no tienen ya una imagen en el tooltip. */
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void freedomclient$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (cir.getReturnValue().isEmpty()) {
			Optional<TooltipComponent> preview = ShulkerPreviewModule.preview((ItemStack) (Object) this);
			if (preview.isPresent()) cir.setReturnValue(preview);
		}
	}
}
