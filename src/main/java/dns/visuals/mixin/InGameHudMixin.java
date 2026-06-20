package dns.visuals.mixin;

import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom crosshair: replaces the vanilla crosshair when the Crosshair module is on. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void dnsvisuals$crosshair(DrawContext ctx, CallbackInfo ci) {
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
		for (int deg = 0; deg < 360; deg += 6) {
			double rad = Math.toRadians(deg);
			int px = cx + (int) Math.round(Math.cos(rad) * r);
			int py = cy + (int) Math.round(Math.sin(rad) * r);
			if (oc != 0) ctx.fill(px - 1, py - 1, px + 1, py + 1, oc);
			ctx.fill(px, py, px + 1, py + 1, color);
		}
	}
}
