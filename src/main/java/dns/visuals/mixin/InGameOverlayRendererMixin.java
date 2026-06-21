package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NoRender "Fire": the first-person fire overlay is drawn by
 * InGameOverlayRenderer#renderFireOverlay (a private static method called from renderOverlays when
 * the player is on fire), NOT by InGameHud#renderMiscOverlays. Cancelling this method at HEAD is
 * what actually removes the burning overlay. Either "Fire" or "Overlays" suppresses it.
 */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

	@Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
	private static void dnsvisuals$noFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		if (m != null && m.isEnabled() && (m.boolVal("Fire") || m.boolVal("Overlays"))) {
			ci.cancel();
		}
	}
}
