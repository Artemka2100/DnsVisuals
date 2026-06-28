package dns.visuals.mixin;

import dns.visuals.hud.HudManager;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom crosshair + HUD rendering hook. HudRenderCallback was removed in 1.21.x, so HUD is drawn at the tail of InGameHud#render. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void dnsvisuals$hud(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
		HudManager.renderHud(ctx, tickCounter);
	}

	private static Module dnsvisuals$noRender() {
		Module m = ModuleManager.INSTANCE.find("NoRender");
		return (m != null && m.isEnabled()) ? m : null;
	}

	/**
	 * NoRender "Overlays": skip the misc first-person screen overlays drawn by renderMiscOverlays
	 * (e.g. powder snow). The fire overlay is handled separately in InGameOverlayRendererMixin,
	 * because in 1.21.11 fire is NOT drawn here. require = 0 keeps the game loading even if the
	 * mapping ever changes (the toggle just goes inert).
	 */
	@Inject(method = "renderMiscOverlays", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noMiscOverlays(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
		Module m = dnsvisuals$noRender();
		if (m != null && m.boolVal("Overlays")) ci.cancel();
	}

	/** NoRender "Vignette": disable the dark screen-edge vignette overlay. */
	@Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noVignette(DrawContext ctx, Entity entity, CallbackInfo ci) {
		Module m = dnsvisuals$noRender();
		if (m != null && m.boolVal("Vignette")) ci.cancel();
	}

	/** NoRender "Spyglass": hide the spyglass scope overlay. */
	@Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noSpyglass(DrawContext ctx, float scale, CallbackInfo ci) {
		Module m = dnsvisuals$noRender();
		if (m != null && m.boolVal("Spyglass")) ci.cancel();
	}

	/** NoRender "Portal": hide the nether-portal screen tint. */
	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noPortal(DrawContext ctx, float nauseaStrength, CallbackInfo ci) {
		Module m = dnsvisuals$noRender();
		if (m != null && m.boolVal("Portal")) ci.cancel();
	}

	/** NoRender "Nausea": hide the nausea/warp overlay. */
	@Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void dnsvisuals$noNausea(DrawContext ctx, float nauseaStrength, CallbackInfo ci) {
		Module m = dnsvisuals$noRender();
		if (m != null && m.boolVal("Nausea")) ci.cancel();
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$crosshair(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
		Module ch = ModuleManager.INSTANCE.find("Crosshair");
		if (ch == null || !ch.isEnabled()) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!mc.options.getPerspective().isFirstPerson()) return;

		int cx = ctx.getScaledWindowWidth() / 2;
		int cy = ctx.getScaledWindowHeight() / 2;
		int color = ch.colorVal("Color");
		int size = (int) ch.numVal("Size");
		int gap = (int) ch.numVal("Gap");
		boolean outline = ch.boolVal("Outline");
		String type = ch.modeVal("Type");
		int oc = 0xFF000000;

		switch (type) {
			case "Dot" -> {
				if (outline) ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, oc);
				ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, color);
			}
			case "Circle" -> dnsvisuals$circle(ctx, cx, cy, Math.max(2, size), color, outline ? oc : 0);
			default -> {
				dnsvisuals$hline(ctx, cx - gap - size, cx - gap, cy, color, outline, oc);
				dnsvisuals$hline(ctx, cx + gap, cx + gap + size, cy, color, outline, oc);
				dnsvisuals$vline(ctx, cy - gap - size, cy - gap, cx, color, outline, oc);
				dnsvisuals$vline(ctx, cy + gap, cy + gap + size, cx, color, outline, oc);
			}
		}
		ci.cancel();
	}

	private void dnsvisuals$hline(DrawContext ctx, int x1, int x2, int y, int color, boolean outline, int oc) {
		if (outline) ctx.fill(x1 - 1, y - 1, x2 + 1, y + 2, oc);
		ctx.fill(x1, y, x2, y + 1, color);
	}

	private void dnsvisuals$vline(DrawContext ctx, int y1, int y2, int x, int color, boolean outline, int oc) {
		if (outline) ctx.fill(x - 1, y1 - 1, x + 2, y2 + 1, oc);
		ctx.fill(x, y1, x + 1, y2, color);
	}

	private void dnsvisuals$circle(DrawContext ctx, int cx, int cy, int r, int color, int oc) {
		for (int deg = 0; deg < 360; deg += 4) {
			double rad = Math.toRadians(deg);
			int px = cx + (int) Math.round(Math.cos(rad) * r);
			int py = cy + (int) Math.round(Math.sin(rad) * r);
			if (oc != 0) ctx.fill(px - 1, py - 1, px + 1, py + 1, oc);
			ctx.fill(px, py, px + 1, py + 1, color);
		}
	}
}
