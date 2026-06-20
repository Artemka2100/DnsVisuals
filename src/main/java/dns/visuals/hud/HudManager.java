package dns.visuals.hud;

import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Draws all enabled HUD modules in a vertical stack at the top-left of the screen.
 * Called from InGameHudMixin (TAIL of InGameHud#render); HudRenderCallback was removed in 1.21.x.
 */
public final class HudManager {
	private HudManager() {}

	public static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.player == null || mc.currentScreen != null) return;

		int x = 4;
		int y = 4;
		for (Module m : ModuleManager.INSTANCE.byCategory(Category.HUD)) {
			if (!m.isEnabled() || m.hudRenderer == null) continue;

			double scale = 1.0;
			if (m.get("Scale") != null) scale = m.numVal("Scale");

			var matrices = ctx.getMatrices();
			matrices.pushMatrix();
			matrices.translate((float) x, (float) y);
			if (scale != 1.0) matrices.scale((float) scale, (float) scale);

			int consumed = m.hudRenderer.render(ctx, 0, 0, m);

			matrices.popMatrix();
			y += (int) (consumed * scale) + 3;
		}
	}
}
