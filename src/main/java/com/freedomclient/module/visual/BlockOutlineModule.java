package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.ColorSetting;
import com.freedomclient.setting.NumberSetting;
import com.freedomclient.util.ColorUtil;

/** ColorCubeBorders: color y grosor del contorno del bloque al que apuntas. */
public class BlockOutlineModule extends Module {
	private final ColorSetting color = add(new ColorSetting("Color", "Color of the block outline.", 0xFFF2C94C, true));
	private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", "Animated rainbow outline.", false));
	private final NumberSetting width = add(new NumberSetting("Thickness", "Thickness of the outline lines.", 2.5, 1, 8, 0.5));

	public BlockOutlineModule() {
		super("Block Outline", "Change the color and thickness of the outline of the block you look at (ColorCubeBorders).", Category.VISUAL, true);
		color.visibleWhen(() -> !rainbow.get());
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static BlockOutlineModule active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		BlockOutlineModule module = manager.get(BlockOutlineModule.class);
		return module.isEnabled() ? module : null;
	}

	public static int color(int vanilla) {
		BlockOutlineModule module = active();
		if (module == null) return vanilla;
		return module.rainbow.get() ? ColorUtil.rainbow(0) : module.color.get();
	}

	public static float width(float vanilla) {
		BlockOutlineModule module = active();
		return module == null ? vanilla : module.width.getFloat();
	}
}
