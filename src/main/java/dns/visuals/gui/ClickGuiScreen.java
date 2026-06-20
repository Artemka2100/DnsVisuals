package dns.visuals.gui;

import dns.visuals.module.Category;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.KeybindSetting;
import dns.visuals.setting.ModeSetting;
import dns.visuals.setting.SliderSetting;
import dns.visuals.util.AnimationUtil;
import dns.visuals.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The black/orange ClickGUI. Left tab column + module list with expandable settings.
 *  - Left-click a module  -> toggle it (animated)
 *  - Right-click a module -> open/close its settings (animated)
 *  - Settings support: toggle, slider, mode (L=next / R=prev), color (R/G/B sliders), keybind.
 */
public class ClickGuiScreen extends Screen {

	private Category selected = Category.HUD;

	// panel geometry
	private int guiX, guiY, guiW, guiH;
	private final int tabW = 80;
	private final int rowH = 18;
	private double scroll = 0;

	// timing for animations
	private long lastNano = 0;

	// interaction state
	private SliderSetting draggingSlider = null;
	private double dragMinX, dragMaxX;
	private ColorSetting draggingColor = null;
	private char draggingChannel = 0; // 'r','g','b'
	private KeybindSetting listening = null;

	// per-frame hit-test elements
	private interface Click { void run(double mx); }
	private static final class Element {
		int x1, y1, x2, y2;
		Click onLeft, onRight;
		Element(int x1, int y1, int x2, int y2) { this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; }
		boolean has(double mx, double my) { return mx >= x1 && mx <= x2 && my >= y1 && my <= y2; }
	}
	private final List<Element> elements = new ArrayList<>();

	public ClickGuiScreen() {
		super(Text.literal("DnsVisuals"));
	}

	// ---- theme helpers (read from the Theme / Animations modules) ----
	private int accent() {
		Module theme = ModuleManager.INSTANCE.find("Theme");
		if (theme != null && theme.boolVal("Rainbow accent")) {
			return ColorUtil.rainbow(theme.numVal("Rainbow speed"), 0);
		}
		return theme != null ? theme.colorVal("Accent") : 0xFFFF7A00;
	}

	private int background() {
		Module theme = ModuleManager.INSTANCE.find("Theme");
		return theme != null ? theme.colorVal("Background") : 0xEB101010;
	}

	private double animSpeed() {
		Module a = ModuleManager.INSTANCE.find("Animations");
		return a != null ? a.numVal("Speed") : 12;
	}

	private double ease(double t) {
		Module a = ModuleManager.INSTANCE.find("Animations");
		String e = a != null ? a.modeVal("Easing") : "EaseOut";
		return switch (e) {
			case "EaseInOut" -> AnimationUtil.easeInOutQuad(t);
			case "Linear" -> AnimationUtil.clamp01(t);
			default -> AnimationUtil.easeOutCubic(t);
		};
	}

	@Override
	protected void init() {
		guiW = 320;
		guiH = 220;
		guiX = (width - guiW) / 2;
		guiY = (height - guiH) / 2;
		lastNano = System.nanoTime();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private double delta() {
		long now = System.nanoTime();
		double d = (now - lastNano) / 1_000_000_000.0;
		lastNano = now;
		return Math.min(0.1, Math.max(0.0, d));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
		double dt = delta();
		Module clickGui = ModuleManager.INSTANCE.find("ClickGUI");
		if (clickGui == null || clickGui.boolVal("Background dim")) {
			this.renderBackground(ctx, mouseX, mouseY, tickDelta);
		}

		int accent = accent();
		int bg = background();

		// main panel
		ctx.fill(guiX, guiY, guiX + guiW, guiY + guiH, bg);
		// header
		ctx.fill(guiX, guiY, guiX + guiW, guiY + 18, 0xFF0C0C0C);
		ctx.drawText(textRenderer, Text.literal("Dns").append(Text.literal("Visuals")), guiX + 8, guiY + 5, accent, false);
		ctx.drawText(textRenderer, Text.literal(selected.title), guiX + tabW + 8, guiY + 5, 0xFF888888, false);
		ctx.fill(guiX, guiY + 17, guiX + guiW, guiY + 18, accent);

		// tab column
		ctx.fill(guiX, guiY + 18, guiX + tabW, guiY + guiH, 0xFF131313);
		int ty = guiY + 24;
		elements.clear();
		for (Category c : Category.values()) {
			boolean sel = c == selected;
			boolean hover = mouseX >= guiX && mouseX <= guiX + tabW && mouseY >= ty && mouseY <= ty + rowH;
			if (sel) {
				ctx.fill(guiX, ty, guiX + tabW, ty + rowH, 0xFF1E1E1E);
				ctx.fill(guiX, ty, guiX + 3, ty + rowH, accent); // accent strip
			} else if (hover) {
				ctx.fill(guiX, ty, guiX + tabW, ty + rowH, 0xFF181818);
			}
			ctx.drawText(textRenderer, Text.literal(c.title), guiX + 10, ty + 5, sel ? accent : 0xFFCCCCCC, false);
			final Category fc = c;
			Element e = new Element(guiX, ty, guiX + tabW, ty + rowH);
			e.onLeft = mx -> { selected = fc; scroll = 0; };
			elements.add(e);
			ty += rowH;
		}

		// module list (scissored)
		int listX = guiX + tabW;
		int listY = guiY + 18;
		int listX2 = guiX + guiW;
		int listY2 = guiY + guiH;
		ctx.enableScissor(listX, listY, listX2, listY2);

		int mx0 = listX + 6;
		int mxw = listX2 - listX - 12;
		int y = listY + 4 - (int) scroll;

		for (Module m : ModuleManager.INSTANCE.byCategory(selected)) {
			// update animations
			m.toggleAnim = AnimationUtil.approach(m.toggleAnim, m.isEnabled() ? 1 : 0, animSpeed(), dt);
			boolean rowHover = mouseX >= mx0 && mouseX <= mx0 + mxw && mouseY >= y && mouseY <= y + rowH && mouseY >= listY && mouseY <= listY2;
			m.hoverAnim = AnimationUtil.approach(m.hoverAnim, rowHover ? 1 : 0, animSpeed(), dt);
			m.openAnim = AnimationUtil.approach(m.openAnim, m.settingsOpen ? 1 : 0, animSpeed(), dt);

			// row background: blends toward accent as the module turns on
			int rowBg = ColorUtil.blend(0xFF161616, ColorUtil.withAlpha(accent, 60), ease(m.toggleAnim));
			if (rowHover) rowBg = ColorUtil.blend(rowBg, 0xFF2A2A2A, 0.5 * m.hoverAnim);
			ctx.fill(mx0, y, mx0 + mxw, y + rowH, rowBg);

			int nameColor = ColorUtil.blend(0xFFBBBBBB, accent, ease(m.toggleAnim));
			ctx.drawText(textRenderer, Text.literal(m.name), mx0 + 6, y + 5, nameColor, false);

			// little settings caret on the right
			ctx.drawText(textRenderer, Text.literal(m.settingsOpen ? "-" : "+"), mx0 + mxw - 10, y + 5, 0xFF777777, false);

			final Module fm = m;
			Element row = new Element(mx0, y, mx0 + mxw, y + rowH);
			row.onLeft = mx -> fm.toggle();
			row.onRight = mx -> fm.settingsOpen = !fm.settingsOpen;
			elements.add(row);
			y += rowH;

			// expanded settings
			if (m.openAnim > 0.01) {
				int fullH = settingsHeight(m);
				int shownH = (int) (fullH * ease(m.openAnim));
				int sectionTop = y;
				ctx.enableScissor(mx0, Math.max(listY, sectionTop), mx0 + mxw, Math.min(listY2, sectionTop + shownH));
				int sy = y;
				for (var s : m.settings) {
					sy = drawSetting(ctx, s, mx0 + 4, sy, mxw - 8, mouseX, mouseY);
				}
				ctx.disableScissor();
				y += shownH;
			}
			y += 2;
		}

		ctx.disableScissor();
		super.render(ctx, mouseX, mouseY, tickDelta);
	}

	private int settingsHeight(Module m) {
		int h = 0;
		for (var s : m.settings) h += settingRowHeight(s);
		return h + 2;
	}

	private int settingRowHeight(Object s) {
		if (s instanceof ColorSetting) return 14 + 3 * 9;
		return 14;
	}

	/** Draws one setting and registers its hit areas. Returns the new y. */
	private int drawSetting(DrawContext ctx, Object setting, int x, int y, int w, int mouseX, int mouseY) {
		int accent = accent();
		if (setting instanceof BooleanSetting b) {
			b.anim = AnimationUtil.approach(b.anim, b.value ? 1 : 0, animSpeed(), 0.016);
			ctx.drawText(textRenderer, Text.literal(b.name), x, y + 3, 0xFFAAAAAA, false);
			int pillW = 18, pillH = 8, px = x + w - pillW, py = y + 2;
			ctx.fill(px, py, px + pillW, py + pillH, ColorUtil.blend(0xFF444444, accent, b.anim));
			int knob = 6;
			int kx = (int) (px + 1 + (pillW - knob - 2) * b.anim);
			ctx.fill(kx, py + 1, kx + knob, py + 1 + knob, 0xFFFFFFFF);
			Element e = new Element(x, y, x + w, y + 13);
			e.onLeft = mx -> b.toggle();
			elements.add(e);
			return y + 14;
		}
		if (setting instanceof SliderSetting sl) {
			ctx.drawText(textRenderer, Text.literal(sl.name), x, y + 2, 0xFFAAAAAA, false);
			ctx.drawText(textRenderer, Text.literal(sl.display()), x + w - textRenderer.getWidth(sl.display()), y + 2, 0xFFFFFFFF, false);
			int barY = y + 11, barX1 = x, barX2 = x + w;
			ctx.fill(barX1, barY, barX2, barY + 2, 0xFF333333);
			int fillX = (int) (barX1 + (barX2 - barX1) * sl.getFraction());
			ctx.fill(barX1, barY, fillX, barY + 2, accent);
			ctx.fill(fillX - 1, barY - 2, fillX + 1, barY + 4, 0xFFFFFFFF);
			Element e = new Element(x, y, x + w, y + 13);
			final int fx1 = barX1, fx2 = barX2;
			e.onLeft = mx -> {
				sl.setFromFraction((mx - fx1) / (double) (fx2 - fx1));
				draggingSlider = sl; dragMinX = fx1; dragMaxX = fx2;
			};
			elements.add(e);
			return y + 14;
		}
		if (setting instanceof ModeSetting mo) {
			ctx.drawText(textRenderer, Text.literal(mo.name), x, y + 3, 0xFFAAAAAA, false);
			String v = mo.get();
			ctx.drawText(textRenderer, Text.literal(v), x + w - textRenderer.getWidth(v), y + 3, accent, false);
			Element e = new Element(x, y, x + w, y + 13);
			e.onLeft = mx -> mo.cycle(1);
			e.onRight = mx -> mo.cycle(-1);
			elements.add(e);
			return y + 14;
		}
		if (setting instanceof KeybindSetting kb) {
			ctx.drawText(textRenderer, Text.literal(kb.name), x, y + 3, 0xFFAAAAAA, false);
			String v = "[" + kb.keyName() + "]";
			ctx.drawText(textRenderer, Text.literal(v), x + w - textRenderer.getWidth(v), y + 3, listening == kb ? accent : 0xFFDDDDDD, false);
			Element e = new Element(x, y, x + w, y + 13);
			e.onLeft = mx -> { kb.listening = true; listening = kb; };
			elements.add(e);
			return y + 14;
		}
		if (setting instanceof ColorSetting col) {
			ctx.drawText(textRenderer, Text.literal(col.name), x, y + 3, 0xFFAAAAAA, false);
			ctx.fill(x + w - 14, y + 2, x + w, y + 10, 0xFF000000 | (col.argb() & 0xFFFFFF));
			int cy = y + 14;
			cy = drawChannel(ctx, col, 'r', "R", x, cy, w, mouseX);
			cy = drawChannel(ctx, col, 'g', "G", x, cy, w, mouseX);
			cy = drawChannel(ctx, col, 'b', "B", x, cy, w, mouseX);
			return cy;
		}
		return y + 14;
	}

	private int drawChannel(DrawContext ctx, ColorSetting col, char ch, String label, int x, int y, int w, int mouseX) {
		int val = ch == 'r' ? col.r : ch == 'g' ? col.g : col.b;
		int barX1 = x + 12, barX2 = x + w;
		ctx.drawText(textRenderer, Text.literal(label), x, y, 0xFF999999, false);
		ctx.fill(barX1, y + 3, barX2, y + 5, 0xFF333333);
		int fillX = (int) (barX1 + (barX2 - barX1) * (val / 255.0));
		int chColor = ch == 'r' ? 0xFFFF5555 : ch == 'g' ? 0xFF55FF55 : 0xFF5555FF;
		ctx.fill(barX1, y + 3, fillX, y + 5, chColor);
		ctx.fill(fillX - 1, y + 1, fillX + 1, y + 7, 0xFFFFFFFF);
		Element e = new Element(x, y, x + w, y + 8);
		final int fx1 = barX1, fx2 = barX2;
		e.onLeft = mx -> {
			int nv = (int) Math.round(255 * Math.max(0, Math.min(1, (mx - fx1) / (double) (fx2 - fx1))));
			if (ch == 'r') col.r = nv; else if (ch == 'g') col.g = nv; else col.b = nv;
			draggingColor = col; draggingChannel = ch; dragMinX = fx1; dragMaxX = fx2;
		};
		elements.add(e);
		return y + 9;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			Element e = elements.get(i);
			if (e.has(mouseX, mouseY)) {
				if (button == 0 && e.onLeft != null) { e.onLeft.run(mouseX); return true; }
				if (button == 1 && e.onRight != null) { e.onRight.run(mouseX); return true; }
				if (button == 0 || button == 1) return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (draggingSlider != null) {
			draggingSlider.setFromFraction((mouseX - dragMinX) / (dragMaxX - dragMinX));
			return true;
		}
		if (draggingColor != null) {
			int nv = (int) Math.round(255 * Math.max(0, Math.min(1, (mouseX - dragMinX) / (dragMaxX - dragMinX))));
			if (draggingChannel == 'r') draggingColor.r = nv;
			else if (draggingChannel == 'g') draggingColor.g = nv;
			else draggingColor.b = nv;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingSlider = null;
		draggingColor = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll = Math.max(0, scroll - vertical * 14);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (listening != null) {
			listening.key = (keyCode == GLFW.GLFW_KEY_ESCAPE) ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
			listening.listening = false;
			listening = null;
			return true;
		}
		Module clickGui = ModuleManager.INSTANCE.find("ClickGUI");
		int openKey = clickGui != null ? clickGui.keyVal("Open key") : GLFW.GLFW_KEY_RIGHT_SHIFT;
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == openKey) {
			this.close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
