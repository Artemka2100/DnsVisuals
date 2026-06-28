package dns.visuals.mixin;

import dns.visuals.util.DvNetwork;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prefixes "DV" in the tab list for players known to be running DnsVisuals. */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

	@Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
	private void dnsvisuals$dvTag(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
		if (entry == null || entry.getProfile() == null) return;
		Text decorated = DvNetwork.decorate(cir.getReturnValue(), entry.getProfile().id());
		if (decorated != null) cir.setReturnValue(decorated);
	}
}
