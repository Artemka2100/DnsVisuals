package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CustomSwing: speeds up (or slows) the local player's hand-swing animation by scaling its duration. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
	private void dnsvisuals$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != MinecraftClient.getInstance().player) return;
		Module m = ModuleManager.INSTANCE.find("CustomSwing");
		if (m == null || !m.isEnabled()) return;
		double speed = m.numVal("Speed");
		if (speed <= 0) return;
		int base = cir.getReturnValueI();
		int dur = (int) Math.max(1, Math.round(base / speed));
		cir.setReturnValue(dur);
	}
}
