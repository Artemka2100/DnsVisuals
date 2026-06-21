package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NoRender "Blindness": removes the visuals of the blindness effect. Cancelling
 * applyStartEndModifier stops the closing-in dark fog, and neutralizing applyDarknessModifier
 * (returning the incoming darkness unchanged) stops the screen from fading to black. Only the
 * camera owner's view is affected, and no status-effect state is modified.
 */
@Mixin(BlindnessEffectFogModifier.class)
public class BlindnessFogMixin {

	@Inject(method = "applyStartEndModifier", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noBlindnessFog(FogData data, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && m.boolVal("Blindness")) {
			ci.cancel();
		}
	}

	@Inject(method = "applyDarknessModifier", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noBlindnessDarkness(LivingEntity cameraEntity, float darkness, float tickProgress, CallbackInfoReturnable<Float> cir) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && m.boolVal("Blindness")) {
			cir.setReturnValue(darkness);
		}
	}
}
