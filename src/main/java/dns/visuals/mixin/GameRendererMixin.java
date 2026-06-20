package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.render.EspRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: shrinks FOV while the Zoom module's key is held. Also captures the FOV for ESP nametags. Plus NoRender + AspectRatio hooks. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void dnsvisuals$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		Module zoom = ModuleManager.INSTANCE.find("Zoom");
		if (zoom == null || !zoom.isEnabled()) return;
		int key = zoom.keyVal("Key");
		if (key <= 0) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!InputUtil.isKeyPressed(mc.getWindow(), key)) return;
		double factor = zoom.numVal("Factor");
		if (factor < 1.0) factor = 1.0;
		cir.setReturnValue((float) (cir.getReturnValueF() / factor));
	}

	/**
	 * Capture the effective FOV (after any zoom adjustment) so EspRenderer can rebuild a matching
	 * projection matrix for nametag positioning. GameRenderer#getFov itself is private/inaccessible
	 * from outside, hence this capture.
	 */
	@Inject(method = "getFov", at = @At("RETURN"))
	private void dnsvisuals$captureFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		EspRenderer.lastFov = cir.getReturnValueF();
	}

	/** NoRender: skip the camera tilt that happens when you take damage. */
	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$noHurtCam(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && m.boolVal("HurtCam")) ci.cancel();
	}

	/** NoRender: skip drawing the first-person hand/held item. */
	@Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$noHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && m.boolVal("Hand")) ci.cancel();
	}

	/**
	 * AspectRatio: stretch the rendered world by scaling clip space. scaleLocal left-multiplies the
	 * projection so the final NDC is stretched (screen stretch), not a zoom. HUD uses a separate
	 * ortho projection so it is unaffected. require = 0: never break the build if the target shifts.
	 */
	@Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
	private void dnsvisuals$aspectRatio(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
		Module m = ModuleManager.INSTANCE.find("AspectRatio");
		if (m == null || !m.isEnabled()) return;
		float sx = (float) m.numVal("Stretch X");
		float sy = (float) m.numVal("Stretch Y");
		if (sx == 1f && sy == 1f) return;
		Matrix4f mat = cir.getReturnValue();
		mat.scaleLocal(sx, sy, 1f);
		cir.setReturnValue(mat);
	}
}
