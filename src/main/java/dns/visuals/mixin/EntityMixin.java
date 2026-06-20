package dns.visuals.mixin;

import dns.visuals.render.Chams;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides the outline (team) color used for the glowing silhouette of chams player targets. */
@Mixin(Entity.class)
public class EntityMixin {

	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$chamsColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (Chams.shouldGlow(self)) {
			cir.setReturnValue(Chams.glowColor(self));
		}
	}
}
