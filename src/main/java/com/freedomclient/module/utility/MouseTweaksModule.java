package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.util.ModIcons;
import net.minecraft.resources.Identifier;

/**
 * Mouse Tweaks (incluido en el cliente): mejores controles del inventario. Se puede apagar para los servidores que
 * lo prohíben; apagado, el inventario funciona como en vanilla (ver MouseTweaksMainMixin).
 */
public class MouseTweaksModule extends Module {
	private static MouseTweaksModule instance;

	public MouseTweaksModule() {
		super("Mouse Tweaks", "Better inventory controls: drag to move items, scroll to move stacks. Turn it off on servers that forbid it.",
				Category.UTILITY, true);
		instance = this;
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	@Override
	public Identifier getIcon() {
		return ModIcons.get("mousetweaks", super.getIcon());
	}

	/** Si Mouse Tweaks tiene que ignorar el ratón (el módulo está apagado). */
	public static boolean blocked() {
		return instance != null && !instance.isEnabled();
	}
}
