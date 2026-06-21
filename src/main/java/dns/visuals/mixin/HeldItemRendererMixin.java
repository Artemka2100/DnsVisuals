package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CustomHand: translate/scale the whole first-person hand. CustomSwing: replace the swing animation. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

	/**
	 * CustomHand: offset and resize the entire first-person hand/item rendering.
	 * HeldItemRenderer has multiple renderItem overloads, so we pin the full descriptor of the
	 * first-person overload to avoid matching the wrong method. require = 0 keeps a future
	 * signature change from crashing the client (the feature just goes inert instead).
	 *
	 * Size always applies. The global X/Y/Z offset applies only when "Per hand" is OFF; when it is
	 * ON, per-arm offsets are applied in {@link #dnsvisuals$handPerArm} instead.
	 */
	@Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), require = 0)
	private void dnsvisuals$customHand(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("CustomHand");
		if (m == null || !m.isEnabled()) return;
		double size = m.numVal("Size");
		if (size != 1.0) matrices.scale((float) size, (float) size, (float) size);
		if (!m.boolVal("Per hand")) {
			double x = m.numVal("X");
			double y = m.numVal("Y");
			double z = m.numVal("Z");
			if (x != 0 || y != 0 || z != 0) matrices.translate(x, y, z);
		}
	}

	/**
	 * CustomHand "Per hand": apply a separate offset for the main hand vs. the off hand. This rides on
	 * applySwingOffset (HEAD), which is part of the per-arm item transform pipeline and runs for each
	 * arm with the correct {@link Arm}. require = 0 keeps it inert if the mapping ever changes.
	 */
	@Inject(method = "applySwingOffset", at = @At("HEAD"), require = 0)
	private void dnsvisuals$handPerArm(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("CustomHand");
		if (m == null || !m.isEnabled() || !m.boolVal("Per hand")) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;
		boolean main = arm == mc.player.getMainArm();
		double x = m.numVal(main ? "Main X" : "Off X");
		double y = m.numVal(main ? "Main Y" : "Off Y");
		double z = m.numVal(main ? "Main Z" : "Off Z");
		if (x != 0 || y != 0 || z != 0) matrices.translate(x, y, z);
	}

	/** CustomSwing: cancel vanilla swing offset and apply a custom animation for the selected style. */
	@Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$customSwing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("CustomSwing");
		if (m == null || !m.isEnabled()) return;
		String style = m.modeVal("Style");
		if (style.equals("Vanilla")) return;
		float k = (float) m.numVal("Intensity");
		int dir = arm == Arm.RIGHT ? 1 : -1;
		float sin = MathHelper.sin(swingProgress * (float) Math.PI);
		float sinSqrt = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		switch (style) {
			case "Stab" -> {
				matrices.translate(dir * sinSqrt * 0.1f * k, 0f, -sin * 0.55f * k);
			}
			case "Slide" -> {
				matrices.translate(dir * sin * 0.35f * k, -sin * 0.1f * k, -sin * 0.1f * k);
			}
			case "Spin" -> {
				matrices.translate(dir * sinSqrt * 0.2f * k, sin * 0.1f * k, -sin * 0.2f * k);
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dir * sin * 360f * k));
			}
			case "Push" -> {
				matrices.translate(0f, 0f, -sin * 0.6f * k);
			}
			case "Pull" -> {
				matrices.translate(0f, sin * 0.25f * k, sin * 0.3f * k);
			}
			case "Wave" -> {
				matrices.translate(dir * sin * 0.4f * k, sin * 0.05f * k, 0f);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dir * sin * 25f * k));
			}
			case "Tap" -> {
				matrices.translate(0f, -sin * 0.3f * k, 0f);
			}
			case "Circle" -> {
				float ang = swingProgress * (float) Math.PI * 2f;
				matrices.translate(dir * MathHelper.cos(ang) * 0.2f * k, MathHelper.sin(ang) * 0.2f * k, -0.1f * k);
			}
			case "Exaggerated" -> {
				matrices.translate(dir * sin * 0.5f * k, -sin * 0.2f * k, -sin * 0.3f * k);
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-sin * 40f * k));
			}
			default -> {
				return;
			}
		}
		ci.cancel();
	}
}
