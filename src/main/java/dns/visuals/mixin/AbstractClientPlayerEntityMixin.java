package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NoRender "Speed FOV": speed/sprint (and other movement effects) zoom the camera out via
 * AbstractClientPlayerEntity#getFovMultiplier. Forcing the multiplier to 1.0 for the local player
 * removes that distracting FOV change. We bail out while the player is using an item so spyglass /
 * bow zoom still work normally.
 */
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

	@Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true, require = 0)
	private void dnsvisuals$noSpeedFov(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
		AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
		if (self != MinecraftClient.getInstance().player) return;
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m == null || !m.isEnabled() || !m.boolVal("Speed FOV")) return;
		if (self.isUsingItem()) return;
		cir.setReturnValue(1.0F);
	}
}
