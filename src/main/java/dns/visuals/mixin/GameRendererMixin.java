package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: shrinks FOV while the Zoom module's key is held. */
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
}
