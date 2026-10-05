package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.BooleanSetting;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** ViewModel: mueve, gira y escala la mano y el objeto que ves en primera persona. */
public class ViewModelModule extends Module implements com.freedomclient.module.LivePreview {
	private final NumberSetting x = add(new NumberSetting("Position X", "Move the hands sideways.", 0, -1, 1, 0.05));
	private final NumberSetting y = add(new NumberSetting("Position Y", "Move the hands up or down.", 0, -1, 1, 0.05));
	private final NumberSetting z = add(new NumberSetting("Position Z", "Move the hands closer or further.", 0, -1, 1, 0.05));
	private final NumberSetting rotationX = add(new NumberSetting("Rotation X", "Tilt forward or back.", 0, -180, 180, 5, "°"));
	private final NumberSetting rotationY = add(new NumberSetting("Rotation Y", "Turn left or right.", 0, -180, 180, 5, "°"));
	private final NumberSetting rotationZ = add(new NumberSetting("Rotation Z", "Roll sideways.", 0, -180, 180, 5, "°"));
	private final NumberSetting scale = add(new NumberSetting("Scale", "Size of the hands and items.", 1, 0.3, 2, 0.05, "x"));
	private final NumberSetting swingSpeed = add(new NumberSetting("Swing speed",
			"How fast your hand swings when you attack or use items (only what you see; hits are not faster).", 1, 0.25, 3, 0.05, "x"));
	private final BooleanSetting noLowering = add(new BooleanSetting("No item lowering",
			"Your item doesn't go down when you switch items or after a hit.", false));
	private final BooleanSetting hideOffhand = add(new BooleanSetting("Hide offhand",
			"Don't draw the item in your off hand in first person (totems, shields…).", false));
	private final NumberSetting offhandX = add(new NumberSetting("Offhand X", "Move only the off hand sideways.", 0, -1, 1, 0.05));
	private final NumberSetting offhandY = add(new NumberSetting("Offhand Y", "Move only the off hand up or down.", 0, -1, 1, 0.05));

	public ViewModelModule() {
		super("ViewModel", "Change the position, rotation and size of your hands in first person.", Category.PVP, false);
	}

	/** Llamado desde ItemInHandRendererMixin para cada mano, justo después de guardar la matriz. */
	public static void apply(PoseStack poseStack, AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return;

		boolean mainHand = hand == InteractionHand.MAIN_HAND;
		HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

		ViewModelModule viewModel = manager.get(ViewModelModule.class);
		if (viewModel.isEnabled()) {
			poseStack.translate(viewModel.x.getFloat() * side, viewModel.y.getFloat(), viewModel.z.getFloat());
			if (!mainHand) poseStack.translate(viewModel.offhandX.getFloat() * side, viewModel.offhandY.getFloat(), 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(viewModel.rotationX.getFloat()));
			poseStack.mulPose(Axis.YP.rotationDegrees(viewModel.rotationY.getFloat() * side));
			poseStack.mulPose(Axis.ZP.rotationDegrees(viewModel.rotationZ.getFloat() * side));
			float s = viewModel.scale.getFloat();
			poseStack.scale(s, s, s);
		}

		manager.get(ShieldFixModule.class).apply(poseStack, player, stack);
	}

	private static ViewModelModule active() {
		ModuleManager manager = FreedomClient.getModuleManager();
		if (manager == null) return null;
		ViewModelModule module = manager.get(ViewModelModule.class);
		return module.isEnabled() ? module : null;
	}

	/** Duración del golpe de tu mano con la velocidad elegida (solo la animación que ves). */
	public static int swingDuration(int ticks) {
		ViewModelModule module = active();
		if (module == null) return ticks;
		return Math.max(1, Math.round(ticks / module.swingSpeed.getFloat()));
	}

	public static boolean noLowering() {
		ViewModelModule module = active();
		return module != null && module.noLowering.get();
	}

	public static boolean hideOffhand() {
		ViewModelModule module = active();
		return module != null && module.hideOffhand.get();
	}

	/** Vista previa en directo de los ajustes con esta cámara. */
	@Override
	public net.minecraft.client.CameraType previewCamera() {
		return net.minecraft.client.CameraType.FIRST_PERSON;
	}
}
