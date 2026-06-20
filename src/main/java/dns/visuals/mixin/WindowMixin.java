package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS Limit: overrides the framerate cap when the FpsLimit module is on. */
@Mixin(Window.class)
public class WindowMixin {
	@Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$fpsLimit(CallbackInfoReturnable<Integer> cir) {
		Module fps = ModuleManager.INSTANCE.find("FpsLimit");
		if (fps == null || !fps.isEnabled()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		int limit = (int) fps.numVal("Limit");
		if (fps.boolVal("When unfocused") && !mc.isWindowFocused()) {
			limit = (int) fps.numVal("Unfocused FPS");
		}
		if (limit < 1) limit = 1;
		cir.setReturnValue(limit);
	}
}
