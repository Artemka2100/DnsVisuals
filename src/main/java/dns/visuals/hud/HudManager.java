package dns.visuals.hud;

import dns.visuals.gui.HudEditorScreen;
import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws enabled HUD modules. Each element has its own movable position.
 *
 * Two draw paths:
 *  - {@link #renderHud} is called from InGameHudMixin (TAIL of InGameHud#render) for the normal,
 *    no-screen case.
 *  - {@link #renderEditor} is called from {@link HudEditorScreen} (opened via {@code .hud}); it
 *    additionally paints drag handles and routes mouse drags to reposition elements.
 */
public final class HudManager {
	private HudManager() {}

	// element name -> live [x, y]
	private static final Map<String, int[]> POS = new HashMap<>();
	// element name -> default [x, y]
	private static final Map<String, int[]> DEFAULTS = new LinkedHashMap<>();
	static {
		DEFAULTS.put("Watermark", new int[]{4, 4});
		DEFAULTS.put("FPS", new int[]{4, 20});
		DEFAULTS.put("CPS", new int[]{4, 32});
		DEFAULTS.put("TPS", new int[]{4, 44});
		DEFAULTS.put("ArmorHUD", new int[]{4, 56});
		DEFAULTS.put("Coordinates", new int[]{4, 110});
		DEFAULTS.put("Ping", new int[]{4, 124});
		DEFAULTS.put("CooldownList", new int[]{4, 138});
		DEFAULTS.put("Effects", new int[]{4, 170});
		DEFAULTS.put("PlayerInfo", new int[]{-1, -1}); // centered lazily once screen size known
	}

	// last drawn screen bounds per element, for drag hit-testing
	private static final Map<String, int[]> bounds = new HashMap<>();
	private static String dragging = null;
	private static int dragDX, dragDY;

	private static int[] pos(String name) {
		int[] p = POS.get(name);
		if (p == null) {
			int[] d = DEFAULTS.getOrDefault(name, new int[]{4, 4});
			p = new int[]{d[0], d[1]};
			POS.put(name, p);
		}
		return p;
	}

	/** Normal HUD pass from InGameHudMixin. Skipped while any screen is open (the editor draws its own). */
	public static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.player == null) return;
		if (mc.currentScreen != null) return;
		draw(ctx, false);
	}

	/** Editor pass from HudEditorScreen: draws elements, drag handles and a hint. */
	public static void renderEditor(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;
		draw(ctx, true);
		int sh = mc.getWindow().getScaledHeight();
		ctx.drawText(mc.textRenderer, "HUD edit: drag elements, ESC to finish",
				4, sh - 12, 0xFFAAAAAA, true);
	}

	private static void draw(DrawContext ctx, boolean editing) {
		MinecraftClient mc = MinecraftClient.getInstance();
		int sw = mc.getWindow().getScaledWidth();
		int sh = mc.getWindow().getScaledHeight();

		int[] piDef = DEFAULTS.get("PlayerInfo");
		if (piDef[0] < 0) {
			piDef[0] = sw / 2 - 45;
			piDef[1] = sh / 2 + 18;
		}

		bounds.clear();
		for (Module m : ModuleManager.INSTANCE.byCategory(Category.HUD)) {
			if (!m.isEnabled() || m.hudRenderer == null) continue;

			// ArrayList positions itself with absolute screen coordinates -> no translation
			if (m.name.equals("ArrayList")) {
				m.hudRenderer.render(ctx, 0, 0, m);
				continue;
			}

			int[] p = pos(m.name);
			var matrices = ctx.getMatrices();
			matrices.pushMatrix();
			matrices.translate((float) p[0], (float) p[1]);
			int consumed = m.hudRenderer.render(ctx, 0, 0, m);
			matrices.popMatrix();

			if (consumed > 0) {
				bounds.put(m.name, new int[]{p[0], p[1], 110, consumed});
				if (editing) {
					// orange handle strip on the left edge of each element
					ctx.fill(p[0] - 3, p[1] - 2, p[0] - 1, p[1] + consumed + 1, 0xFFFF7A00);
				}
			}
		}
	}

	// ---- drag API called from HudEditorScreen ----
	public static boolean isEditing() {
		return MinecraftClient.getInstance().currentScreen instanceof HudEditorScreen;
	}

	public static boolean mouseClicked(double mx, double my, int button) {
		if (button != 0) return false;
		for (Map.Entry<String, int[]> e : bounds.entrySet()) {
			int[] b = e.getValue();
			if (mx >= b[0] - 4 && mx <= b[0] + b[2] && my >= b[1] - 2 && my <= b[1] + b[3]) {
				dragging = e.getKey();
				int[] p = pos(dragging);
				dragDX = (int) (mx - p[0]);
				dragDY = (int) (my - p[1]);
				return true;
			}
		}
		return false;
	}

	public static boolean mouseDragged(double mx, double my) {
		if (dragging == null) return false;
		int[] p = pos(dragging);
		p[0] = Math.max(0, (int) (mx - dragDX));
		p[1] = Math.max(0, (int) (my - dragDY));
		return true;
	}

	public static void mouseReleased() {
		dragging = null;
	}
}
