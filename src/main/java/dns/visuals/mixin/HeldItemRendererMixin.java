package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
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

	/** CustomHand: offset and resize the entire first-person hand/item rendering. */
	@Inject(method = "renderItem", at = @At("HEAD"), require = 0)
	private void dnsvisuals$customHand(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("CustomHand");
		if (m == null || !m.isEnabled()) return;
		double x = m.numVal("X");
		double y = m.numVal("Y");
		double z = m.numVal("Z");
		double size = m.numVal("Size");
		if (x != 0 || y != 0 || z != 0) matrices.translate(x, y, z);
		if (size != 1.0) matrices.scale((float) size, (float) size, (float) size);
	}

	/** CustomSwing: cancel vanilla swing offset and apply a custom animation for the selected style. */
	@Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$customSwing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("CustomSwing");
		if (m == null || !m.isEnabled()) return;
		String style = m.modeVal("Style");
		if (style.equals("Vanilla")) return;
		int dir = arm == Arm.RIGHT ? 1 : -1;
		float sin = MathHelper.sin(swingProgress * (float) Math.PI);
		float sinSqrt = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
		switch (style) {
			case "Stab" -> {
				matrices.translate(dir * sinSqrt * 0.1f, 0f, -sin * 0.55f);
			}
			case "Slide" -> {
				matrices.translate(dir * sin * 0.35f, -sin * 0.1f, -sin * 0.1f);
			}
			case "Spin" -> {
				matrices.translate(dir * sinSqrt * 0.2f, sin * 0.1f, -sin * 0.2f);
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dir * sin * 360f));
			}
			default -> {
				return;
			}
		}
		ci.cancel();
	}
}
