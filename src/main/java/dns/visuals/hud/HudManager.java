package dns.visuals.hud;

import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Draws all enabled HUD modules in a vertical stack at the top-left of the screen. */
public final class HudManager {
	private HudManager() {}

	public static void register() {
		HudRenderCallback.EVENT.register(HudManager::onRenderHud);
	}

	private static void onRenderHud(DrawContext ctx, float tickDelta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.player == null || mc.currentScreen != null) return;

		int x = 4;
		int y = 4;
		for (Module m : ModuleManager.INSTANCE.byCategory(Category.HUD)) {
			if (!m.isEnabled() || m.hudRenderer == null) continue;

			double scale = 1.0;
			if (m.get("Scale") != null) scale = m.numVal("Scale");

			var matrices = ctx.getMatrices();
			matrices.push();
			matrices.translate(x, y, 0);
			if (scale != 1.0) matrices.scale((float) scale, (float) scale, 1f);

			int consumed = m.hudRenderer.render(ctx, 0, 0, m);

			matrices.pop();
			y += (int) (consumed * scale) + 3;
		}
	}
}
