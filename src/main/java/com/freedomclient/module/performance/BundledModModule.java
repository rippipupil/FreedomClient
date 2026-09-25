package com.freedomclient.module.performance;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.util.ModIcons;
import net.minecraft.resources.Identifier;

/** Tarjeta informativa de un mod de rendimiento incluido en el cliente, que siempre está activo. */
public class BundledModModule extends Module {
	private final String modId;

	public BundledModModule(String name, String modId, String description) {
		super(name, description, Category.PERFORMANCE, true);
		this.modId = modId;
	}

	@Override
	public boolean canToggle() {
		return false;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public Identifier getIcon() {
		return ModIcons.get(modId, super.getIcon());
	}
}
