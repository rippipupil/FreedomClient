package com.freedomclient.module.visual;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.ui.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Pone la insignia FC delante de tu nombre (sobre tu cabeza y en la lista de jugadores). */
public class FcNametagModule extends Module {
	private final BooleanSetting nametag = add(new BooleanSetting("Above head", "Show the FC badge in your name tag (F5).", true));
	private final BooleanSetting tabList = add(new BooleanSetting("Tab list", "Show the FC badge in the player list.", true));

	public FcNametagModule() {
		super("FC Nametag", "Shows the FreedomClient FC badge next to your name.", Category.VISUAL, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}

	private static FcNametagModule active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		FcNametagModule module = manager.get(FcNametagModule.class);
		return module.isEnabled() ? module : null;
	}

	private static Component badge(Component name) {
		return Component.empty().append(UiText.fcBadge()).append(" ").append(name);
	}

	public static Component decorateNametag(Entity entity, Component name) {
		FcNametagModule module = active();
		if (module == null || !module.nametag.get() || entity != Minecraft.getInstance().player) return name;
		return badge(name);
	}

	public static Component decorateTabName(UUID playerId, Component name) {
		FcNametagModule module = active();
		Minecraft client = Minecraft.getInstance();
		if (module == null || !module.tabList.get() || client.player == null || !client.player.getUUID().equals(playerId)) return name;
		return badge(name);
	}
}
