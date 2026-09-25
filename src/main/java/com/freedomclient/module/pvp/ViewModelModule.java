package com.freedomclient.module.pvp;

import com.freedomclient.FreedomClient;
import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.module.ModuleManager;
import com.freedomclient.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** ViewModel: mueve, gira y escala la mano y el objeto que ves en primera persona. */
public class ViewModelModule extends Module {
	private final NumberSetting x = add(new NumberSetting("Position X", "Move the hands sideways.", 0, -1, 1, 0.05));
	private final NumberSetting y = add(new NumberSetting("Position Y", "Move the hands up or down.", 0, -1, 1, 0.05));
	private final NumberSetting z = add(new NumberSetting("Position Z", "Move the hands closer or further.", 0, -1, 1, 0.05));
	private final NumberSetting rotationX = add(new NumberSetting("Rotation X", "Tilt forward or back.", 0, -180, 180, 5, "°"));
	private final NumberSetting rotationY = add(new NumberSetting("Rotation Y", "Turn left or right.", 0, -180, 180, 5, "°"));
	private final NumberSetting rotationZ = add(new NumberSetting("Rotation Z", "Roll sideways.", 0, -180, 180, 5, "°"));
	private final NumberSetting scale = add(new NumberSetting("Scale", "Size of the hands and items.", 1, 0.3, 2, 0.05, "x"));

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
			poseStack.mulPose(Axis.XP.rotationDegrees(viewModel.rotationX.getFloat()));
			poseStack.mulPose(Axis.YP.rotationDegrees(viewModel.rotationY.getFloat() * side));
			poseStack.mulPose(Axis.ZP.rotationDegrees(viewModel.rotationZ.getFloat() * side));
			float s = viewModel.scale.getFloat();
			poseStack.scale(s, s, s);
		}

		manager.get(ShieldFixModule.class).apply(poseStack, player, stack);
	}
}
