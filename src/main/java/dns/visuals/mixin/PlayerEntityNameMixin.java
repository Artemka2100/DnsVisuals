package dns.visuals.mixin;

import dns.visuals.util.DvNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prefixes "DV" on a player's display name (used for the in-world nametag and many chat routes) when
 * that player is a known DnsVisuals user and the DVTag module is enabled.
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityNameMixin {

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void dnsvisuals$dvTag(CallbackInfoReturnable<Text> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		Text decorated = DvNetwork.decorate(cir.getReturnValue(), self.getUuid());
		if (decorated != null) cir.setReturnValue(decorated);
	}
}
