package dns.visuals.mixin;

import dns.visuals.render.Chams;
import dns.visuals.render.ChamsColorHolder;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Solid-fill chams.
 *
 * During updateRenderState we still have the entity, so we compute the chams fill color and stash it
 * onto the render state (via ChamsColorHolder). During getMixColor we read it back and, if set,
 * override the model tint color, which colors the whole model. Depth is respected by the normal
 * render layer, so it does not show through walls.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void dnsvisuals$chamsUpdate(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
		if (state instanceof ChamsColorHolder holder) {
			holder.dnsvisuals$setChamsColor(Chams.fillColor(entity));
		}
	}

	@Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true)
	private void dnsvisuals$chamsMix(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		if (state instanceof ChamsColorHolder holder) {
			int color = holder.dnsvisuals$getChamsColor();
			if (color != 0) {
				cir.setReturnValue(color);
			}
		}
	}
}
