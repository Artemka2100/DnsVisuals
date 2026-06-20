package dns.visuals.mixin;

import dns.visuals.render.ChamsColorHolder;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Attaches a chams fill color to every living entity's render state. Written during
 * updateRenderState (LivingEntityRendererMixin) and read back in getMixColor.
 */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements ChamsColorHolder {
	@Unique
	private int dnsvisuals$chamsColor = 0;

	@Override
	public int dnsvisuals$getChamsColor() {
		return this.dnsvisuals$chamsColor;
	}

	@Override
	public void dnsvisuals$setChamsColor(int color) {
		this.dnsvisuals$chamsColor = color;
	}
}
