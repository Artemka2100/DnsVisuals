package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side Time and Weather overrides (visual only).
 *
 * Targets the common {@link World} class but every injection bails out unless this is the client
 * world ({@code isClient()}), so the integrated server's logic (mob spawning, real weather, etc.)
 * is never touched. Only what you see locally changes.
 */
@Mixin(World.class)
public abstract class WorldMixin {

	private boolean dnsvisuals$client() {
		return ((World) (Object) this).isClient();
	}

	private static Module dnsvisuals$mod(String name) {
		Module m = ModuleManager.INSTANCE.find(name);
		return (m != null && m.isEnabled()) ? m : null;
	}

	@Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$setTime(CallbackInfoReturnable<Long> cir) {
		if (!dnsvisuals$client()) return;
		Module m = dnsvisuals$mod("Time");
		if (m == null) return;
		long t;
		switch (m.modeVal("Time")) {
			case "Morning" -> t = 0L;
			case "Noon" -> t = 6000L;
			case "Sunset" -> t = 12000L;
			case "Night" -> t = 14000L;
			case "Midnight" -> t = 18000L;
			default -> t = 1000L; // Day
		}
		cir.setReturnValue(t);
	}

	@Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$rain(float delta, CallbackInfoReturnable<Float> cir) {
		if (!dnsvisuals$client()) return;
		Module m = dnsvisuals$mod("Weather");
		if (m == null) return;
		cir.setReturnValue(m.modeVal("Weather").equals("Clear") ? 0f : 1f);
	}

	@Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$thunder(float delta, CallbackInfoReturnable<Float> cir) {
		if (!dnsvisuals$client()) return;
		Module m = dnsvisuals$mod("Weather");
		if (m == null) return;
		cir.setReturnValue(m.modeVal("Weather").equals("Thunder") ? 1f : 0f);
	}
}
