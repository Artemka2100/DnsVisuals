package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NoRender "Effect particles": LivingEntity#tickStatusEffects spawns the colored swirling potion
 * particles around every entity by calling World#addParticleClient on the client world. Redirecting
 * that single call lets us skip only the visual spawn. Server-side effect logic (durations,
 * attributes, expiry) lives in the ServerWorld branch and is never touched, so gameplay is safe even
 * in singleplayer.
 */
@Mixin(LivingEntity.class)
public class LivingEntityParticleMixin {

	@Redirect(
			method = "tickStatusEffects",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"),
			require = 0
	)
	private void dnsvisuals$noEffectParticles(World world, ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && m.boolVal("Effect particles")) return;
		world.addParticleClient(parameters, x, y, z, velocityX, velocityY, velocityZ);
	}
}
