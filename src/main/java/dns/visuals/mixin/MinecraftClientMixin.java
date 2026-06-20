package dns.visuals.mixin;

import dns.visuals.render.Chams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forces the vanilla entity outline on for chams player targets (see-through colored silhouette). */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$chamsOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (Chams.shouldGlow(entity)) {
			cir.setReturnValue(true);
		}
	}
}
