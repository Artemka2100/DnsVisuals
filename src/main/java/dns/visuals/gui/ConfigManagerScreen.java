package dns.visuals.gui;

import dns.visuals.config.ConfigManager;
import dns.visuals.module.Module;
import dns.visuals.module.ModuleManager;
import dns.visuals.util.ColorUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Config Manager screen (opened with the chat command {@code .config}).
 *
 * Lets you create named configs (snapshotting the current module state), load them back, and
 * delete them. Configs are stored by {@link ConfigManager} in config/dnsvisuals/<name>.txt.
 *
 * Text entry for the new-config name is handled directly in {@link #keyPressed(KeyInput)} by mapping
 * GLFW key codes to characters. This avoids depending on the exact charTyped(...) signature, which
 * differs between Minecraft versions, and keeps the screen self-contained.
 */
public class ConfigManagerScreen extends Screen {

	private int guiX, guiY;
	private final int guiW = 240;
	private final int guiH = 220;
	private final int rowH = 16;
	private double scroll = 0;

	private String nameBuffer = "";
	private String status = "";

	private interface ClickAction { void run(); }
	private static final class Element {
		int x1, y1, x2, y2;
		ClickAction onClick;
		Element(int x1, int y1, int x2, int y2, ClickAction a) {
			this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.onClick = a;
		}
		boolean has(double mx, double my) { return mx >= x1 && mx <= x2 && my >= y1 && my <= y2; }
	}
	private final List<Element> elements = new ArrayList<>();

	public ConfigManagerScreen() {
		super(Text.literal("DnsVisuals Config Manager"));
	}

	private int accent() {
		Module theme = ModuleManager.INSTANCE.find("Theme");
		if (theme != null && theme.boolVal("Rainbow accent")) {
			return ColorUtil.rainbow(theme.numVal("Rainbow speed"), 0);
		}
		return theme != null ? theme.colorVal("Accent") : 0xFFFF7A00;
	}

	@Override
	protected void init() {
		guiX = (width - guiW) / 2;
		guiY = (height - guiH) / 2;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
		elements.clear();
		int accent = accent();

		ctx.fill(0, 0, width, height, 0x66000000);
		ctx.fill(guiX, guiY, guiX + guiW, guiY + guiH, 0xEB101010);
		// header
		ctx.fill(guiX, guiY, guiX + guiW, guiY + 18, 0xFF0C0C0C);
		ctx.fill(guiX, guiY + 17, guiX + guiW, guiY + 18, accent);
		ctx.drawText(textRenderer, Text.literal("Config Manager"), guiX + 8, guiY + 5, accent, false);

		// name field + Create button
		int fieldY = guiY + 24;
		int btnW = 54;
		int fieldX2 = guiX + guiW - btnW - 10;
		ctx.fill(guiX + 8, fieldY, fieldX2, fieldY + 14, 0xFF1B1B1B);
		ctx.fill(guiX + 8, fieldY, fieldX2, fieldY + 1, 0xFF333333);
		String shown = nameBuffer.isEmpty() ? "type a name..." : nameBuffer;
		int textColor = nameBuffer.isEmpty() ? 0xFF666666 : 0xFFFFFFFF;
		ctx.drawText(textRenderer, Text.literal(shown + "_"), guiX + 12, fieldY + 3, textColor, false);

		int btnX = fieldX2 + 4;
		ctx.fill(btnX, fieldY, btnX + btnW, fieldY + 14, ColorUtil.withAlpha(accent, 200));
		ctx.drawText(textRenderer, Text.literal("Create"), btnX + 9, fieldY + 3, 0xFFFFFFFF, false);
		elements.add(new Element(btnX, fieldY, btnX + btnW, fieldY + 14, this::createConfig));

		// status line
		if (!status.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal(status), guiX + 8, fieldY + 18, 0xFF999999, false);
		}

		// list of saved configs
		int listY = guiY + 48;
		int listY2 = guiY + guiH - 8;
		ctx.enableScissor(guiX + 8, listY, guiX + guiW - 8, listY2);
		List<String> configs = ConfigManager.list();
		int y = listY - (int) scroll;
		if (configs.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal("No saved configs yet."), guiX + 12, y + 4, 0xFF777777, false);
		}
		for (String name : configs) {
			int rx1 = guiX + 8, rx2 = guiX + guiW - 8;
			boolean hover = mouseX >= rx1 && mouseX <= rx2 && mouseY >= y && mouseY <= y + rowH
					&& mouseY >= listY && mouseY <= listY2;
			ctx.fill(rx1, y, rx2, y + rowH - 2, hover ? 0xFF202020 : 0xFF161616);
			ctx.drawText(textRenderer, Text.literal(name), rx1 + 4, y + 4, 0xFFCCCCCC, false);

			// Load / Delete buttons on the right
			int delW = 20, loadW = 34, gap = 4;
			int delX = rx2 - delW - 4;
			int loadX = delX - loadW - gap;
			ctx.fill(loadX, y + 1, loadX + loadW, y + rowH - 3, ColorUtil.withAlpha(accent, 170));
			ctx.drawText(textRenderer, Text.literal("Load"), loadX + 6, y + 4, 0xFFFFFFFF, false);
			ctx.fill(delX, y + 1, delX + delW, y + rowH - 3, 0xFF802020);
			ctx.drawText(textRenderer, Text.literal("X"), delX + 7, y + 4, 0xFFFFFFFF, false);

			final String fn = name;
			elements.add(new Element(loadX, y + 1, loadX + loadW, y + rowH - 3, () -> loadConfig(fn)));
			elements.add(new Element(delX, y + 1, delX + delW, y + rowH - 3, () -> deleteConfig(fn)));
			y += rowH;
		}
		ctx.disableScissor();

		ctx.drawText(textRenderer, Text.literal("Type a name + Create to snapshot current settings. ESC to close."),
				guiX + 8, guiY + guiH - 6, 0xFF555555, false);

		super.render(ctx, mouseX, mouseY, tickDelta);
	}

	private void createConfig() {
		if (nameBuffer.trim().isEmpty()) { status = "Enter a name first."; return; }
		String saved = ConfigManager.saveNamed(nameBuffer);
		status = "Saved config '" + saved + "'.";
		nameBuffer = "";
	}

	private void loadConfig(String name) {
		boolean ok = ConfigManager.loadNamed(name);
		status = ok ? "Loaded '" + name + "'." : "Config '" + name + "' not found.";
	}

	private void deleteConfig(String name) {
		boolean ok = ConfigManager.deleteNamed(name);
		status = ok ? "Deleted '" + name + "'." : "Could not delete '" + name + "'.";
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			Element e = elements.get(i);
			if (e.has(click.x(), click.y()) && e.onClick != null) {
				e.onClick.run();
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll = Math.max(0, scroll - vertical * 14);
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int key = input.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			createConfig();
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
			return true;
		}
		Character c = mapKey(key);
		if (c != null && nameBuffer.length() < 32) {
			nameBuffer += c;
			return true;
		}
		return super.keyPressed(input);
	}

	/** Maps a GLFW key code to a safe filename character, or null if not allowed. */
	private static Character mapKey(int key) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
			return (char) ('a' + (key - GLFW.GLFW_KEY_A));
		}
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
			return (char) ('0' + (key - GLFW.GLFW_KEY_0));
		}
		if (key == GLFW.GLFW_KEY_SPACE) return ' ';
		if (key == GLFW.GLFW_KEY_MINUS) return '-';
		return null;
	}
}
