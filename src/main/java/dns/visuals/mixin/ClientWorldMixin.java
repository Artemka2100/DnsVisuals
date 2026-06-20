package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SkyColor module: blends a custom sky color into the value returned by ClientWorld#getSkyColor.
 * The vanilla method returns a packed 0xRRGGBB int; we lerp it towards the configured color by the
 * "Blend" percentage. "When" restricts the override to Day/Night based on the (possibly Time-module
 * overridden) world time.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void dnsvisuals$skyColor(Vec3d pos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
		Module m = ModuleManager.INSTANCE.find("SkyColor");
		if (m == null || !m.isEnabled() || !m.boolVal("Override")) return;

		String when = m.modeVal("When");
		if (when != null && !when.equals("Always")) {
			long t = ((World) (Object) this).getTimeOfDay() % 24000L;
			if (t < 0L) t += 24000L;
			boolean day = t < 12000L;
			if (when.equals("Day") && !day) return;
			if (when.equals("Night") && day) return;
		}

		int custom = m.colorVal("Color");
		int vanilla = cir.getReturnValueI();
		double b = Math.max(0.0, Math.min(1.0, m.numVal("Blend") / 100.0));

		int vr = (vanilla >> 16) & 0xFF, vg = (vanilla >> 8) & 0xFF, vb = vanilla & 0xFF;
		int cr = (custom >> 16) & 0xFF, cg = (custom >> 8) & 0xFF, cb = custom & 0xFF;

		int rr = (int) (vr * (1.0 - b) + cr * b);
		int rg = (int) (vg * (1.0 - b) + cg * b);
		int rb = (int) (vb * (1.0 - b) + cb * b);

		cir.setReturnValue((rr << 16) | (rg << 8) | rb);
	}
}
