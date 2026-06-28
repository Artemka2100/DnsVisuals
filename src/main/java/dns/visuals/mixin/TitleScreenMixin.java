package dns.visuals.mixin;

import dns.visuals.gui.ProfileManagerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "DV Profiles" button to the main title screen (top-left) that opens the offline
 * {@link ProfileManagerScreen}. Placed away from the central vanilla button stack to avoid overlap.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

	protected TitleScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dnsvisuals$addProfilesButton(CallbackInfo ci) {
		Screen self = this;
		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("DV Profiles"),
				btn -> this.client.setScreen(new ProfileManagerScreen(self))
		).dimensions(6, 6, 100, 20).build());
	}
}
