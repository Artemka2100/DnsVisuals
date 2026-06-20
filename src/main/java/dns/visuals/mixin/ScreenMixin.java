package dns.visuals.mixin;

import dns.visuals.hud.HudManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes mouse input to the HUD editor while the chat screen is open, so HUD elements can be
 * dragged to new positions. We mix into the base {@link Screen} (which defines these methods) and
 * guard for {@link ChatScreen}; this avoids the fragile Fabric ScreenMouseEvents API, whose
 * callback signatures changed in 1.21.11.
 */
@Mixin(Screen.class)
public class ScreenMixin {

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ChatScreen)) return;
		if (HudManager.mouseClicked(click.x(), click.y(), click.button())) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$mouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ChatScreen)) return;
		if (HudManager.mouseDragged(click.x(), click.y())) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"))
	private void dnsvisuals$mouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ChatScreen)) return;
		HudManager.mouseReleased();
	}
}
