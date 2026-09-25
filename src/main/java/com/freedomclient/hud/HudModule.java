package com.freedomclient.hud;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.HudPositionSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Elemento del HUD que se puede mover y escalar en el editor. Se dibuja siempre en (0, 0) sin escalar. */
public abstract class HudModule extends Module {
	private final HudPositionSetting position;

	protected HudModule(String name, String description, boolean enabledByDefault, HudPosition defaultPosition) {
		super(name, description, Category.HUD, enabledByDefault);
		this.position = add(new HudPositionSetting(defaultPosition));
	}

	/** Ancho en px sin escalar. {@code preview} es true en el editor, donde se muestran datos de ejemplo. */
	public abstract int getWidth(Minecraft client, boolean preview);

	public abstract int getHeight(Minecraft client, boolean preview);

	/** Dibuja el elemento con su esquina superior izquierda en (0, 0). */
	public abstract void render(GuiGraphics graphics, Minecraft client, boolean preview);

	/** Si hay algo que mostrar ahora mismo (por ejemplo, el contador de efectos sin efectos activos no se dibuja). */
	public boolean shouldRender(Minecraft client) {
		return true;
	}

	public HudPosition getPosition() {
		return position.get();
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
