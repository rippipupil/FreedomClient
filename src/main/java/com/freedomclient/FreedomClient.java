package com.freedomclient;

import com.freedomclient.config.Config;
import com.freedomclient.cosmetic.AngelCosmeticsLayer;
import com.freedomclient.hud.CombatTracker;
import com.freedomclient.hud.HudRenderer;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.module.hud.AppleSkinModule;
import com.freedomclient.module.hud.PotionEffectsHud;
import com.freedomclient.module.pvp.AttackIndicatorModule;
import com.freedomclient.module.pvp.BetterCrosshairModule;
import com.freedomclient.module.pvp.CenteredCrosshairModule;
import com.freedomclient.module.utility.AnnouncementsModule;
import com.freedomclient.module.utility.ChatFilterModule;
import com.freedomclient.module.utility.LogCleanerModule;
import com.freedomclient.module.visual.BetterGrassModule;
import com.freedomclient.module.visual.CustomScreensModule;
import com.freedomclient.module.visual.ShulkerPreviewModule;
import com.freedomclient.waypoint.WaypointsModule;
import com.freedomclient.ui.menu.FreedomMenuScreen;
import com.freedomclient.ui.scene.FreedomTitleScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreedomClient implements ClientModInitializer {
	public static final String MOD_ID = "freedomclient";
	public static final String NAME = "FreedomClient";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private static KeyMapping.Category keyCategory;
	private static ModuleManager moduleManager;

	@Override
	public void onInitializeClient() {
		keyCategory = KeyMapping.Category.register(id("general"));

		KeyMapping menuKey = registerKey("clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT);

		moduleManager = new ModuleManager();
		Config.load(moduleManager);
		moduleManager.get(LogCleanerModule.class).clean();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new FreedomMenuScreen());
				}
			}

			// Respaldo por si el menú principal de vanilla se abre por un camino que el mixin no cubre.
			if (client.screen instanceof TitleScreen && !(client.screen instanceof FreedomTitleScreen)
					&& CustomScreensModule.mainMenuEnabled()) {
				client.setScreen(new FreedomTitleScreen());
			}

			CombatTracker.tick(client);
			moduleManager.onTick(client);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> moduleManager.onShutdown(client));
		BetterGrassModule.registerPack();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> moduleManager.get(BetterGrassModule.class).syncWithPacks(client));

		registerHud();
		registerCosmetics();

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			Minecraft client = Minecraft.getInstance();
			if (player == client.player) {
				CombatTracker.onAttack(client.player, entity, hitResult);
			}
			return InteractionResult.PASS;
		});

		registerChat();

		TooltipComponentCallback.EVENT.register(data ->
				data instanceof ShulkerPreviewModule.Contents contents ? new ShulkerPreviewModule.PreviewComponent(contents) : null);

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
				moduleManager.get(AppleSkinModule.class).appendTooltip(stack, lines));

		LOGGER.info("{} loaded with {} mods", NAME, moduleManager.getModules().size());
	}

	private static void registerChat() {
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
				moduleManager.get(ChatFilterModule.class).allowChat(message, sender));
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
				overlay || moduleManager.get(ChatFilterModule.class).allowGame(message));
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
				moduleManager.get(AnnouncementsModule.class).modify(message, overlay));
	}

	private static void registerHud() {
		// Los waypoints van primero para quedar por debajo del resto del HUD.
		HudElementRegistry.addFirst(id("waypoints"), (graphics, deltaTracker) ->
				moduleManager.get(WaypointsModule.class).render(graphics, Minecraft.getInstance()));
		HudElementRegistry.addLast(id("hud"), (graphics, deltaTracker) -> HudRenderer.render(graphics));

		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, id("appleskin"), (graphics, deltaTracker) ->
				moduleManager.get(AppleSkinModule.class).renderOverlay(graphics, Minecraft.getInstance()));

		// Mira propia (Better Crosshair) o la de vanilla, opcionalmente centrada al píxel exacto.
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (graphics, deltaTracker) ->
				withoutVanillaAttackIndicator(() -> renderCrosshair(vanilla, graphics, deltaTracker)));

		// Custom Attack Indicator: el propio se dibuja encima y el de vanilla se oculta en la mira y en la barra.
		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, vanilla -> (graphics, deltaTracker) ->
				withoutVanillaAttackIndicator(() -> vanilla.render(graphics, deltaTracker)));
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, id("attack_indicator"), (graphics, deltaTracker) -> {
			AttackIndicatorModule indicator = moduleManager.get(AttackIndicatorModule.class);
			if (indicator.isEnabled()) indicator.render(graphics, Minecraft.getInstance());
		});

		// Oculta los iconos de efectos de vanilla cuando el HUD de efectos propio lo pide.
		HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, vanilla -> (graphics, deltaTracker) -> {
			if (!moduleManager.get(PotionEffectsHud.class).hidesVanillaEffects()) {
				vanilla.render(graphics, deltaTracker);
			}
		});
	}

	/** Ejecuta un dibujado de vanilla con su indicador de ataque desactivado si el Custom Attack Indicator está activo. */
	private static void withoutVanillaAttackIndicator(Runnable render) {
		if (!moduleManager.get(AttackIndicatorModule.class).isEnabled()) {
			render.run();
			return;
		}
		OptionInstance<AttackIndicatorStatus> option = Minecraft.getInstance().options.attackIndicator();
		AttackIndicatorStatus saved = option.get();
		option.set(AttackIndicatorStatus.OFF);
		try {
			render.run();
		} finally {
			option.set(saved);
		}
	}

	private static void renderCrosshair(HudElement vanilla, GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		BetterCrosshairModule crosshair = moduleManager.get(BetterCrosshairModule.class);
		if (crosshair.isEnabled()) {
			if (crosshair.shouldRender(client)) crosshair.render(graphics, client);
			return;
		}
		if (moduleManager.get(CenteredCrosshairModule.class).isEnabled()) {
			// Vanilla dibuja en (ancho - 15) / 2 con división entera; se corrige el medio píxel perdido.
			float offsetX = (graphics.guiWidth() - 15) / 2.0F - (graphics.guiWidth() - 15) / 2;
			float offsetY = (graphics.guiHeight() - 15) / 2.0F - (graphics.guiHeight() - 15) / 2;
			graphics.pose().pushMatrix();
			graphics.pose().translate(offsetX, offsetY);
			vanilla.render(graphics, deltaTracker);
			graphics.pose().popMatrix();
			return;
		}
		vanilla.render(graphics, deltaTracker);
	}

	@SuppressWarnings("unchecked")
	private static void registerCosmetics() {
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new AngelCosmeticsLayer((RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) avatar));
			}
		});
	}

	public static KeyMapping registerKey(String name, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, defaultKey, keyCategory));
	}

	public static ModuleManager getModuleManager() {
		return moduleManager;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
