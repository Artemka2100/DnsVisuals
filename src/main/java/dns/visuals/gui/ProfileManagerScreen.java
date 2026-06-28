package dns.visuals.gui;

import dns.visuals.mixin.MinecraftClientMixin;
import dns.visuals.util.AltManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * In-lobby Profile Manager (opened from a button on the title screen).
 *
 * Lets you create OFFLINE account profiles (type a nickname, press Create) and switch the active
 * account to any of them. Switching replaces {@link MinecraftClient}'s session via the
 * {@link MinecraftClientMixin} session accessor.
 *
 * LIMITATION: offline accounts only — these work on offline-mode/cracked servers and singleplayer,
 * but NOT on premium (online-mode) servers, because no Microsoft authentication is performed. The
 * currently-active nickname is shown at the top; a relog/title-screen refresh applies it cleanly.
 */
public class ProfileManagerScreen extends Screen {

	private final Screen parent;

	private int guiX, guiY;
	private final int guiW = 250;
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

	public ProfileManagerScreen(Screen parent) {
		super(Text.literal("DnsVisuals Profile Manager"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		guiX = (width - guiW) / 2;
		guiY = (height - guiH) / 2;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
		elements.clear();
		int accent = 0xFFFF7A00;

		this.renderBackground(ctx, mouseX, mouseY, tickDelta);
		ctx.fill(guiX, guiY, guiX + guiW, guiY + guiH, 0xEB101010);
		ctx.fill(guiX, guiY, guiX + guiW, guiY + 18, 0xFF0C0C0C);
		ctx.fill(guiX, guiY + 17, guiX + guiW, guiY + 18, accent);
		ctx.drawText(textRenderer, Text.literal("Profile Manager (offline)"), guiX + 8, guiY + 5, accent, false);

		// current account
		String current = MinecraftClient.getInstance().getSession().getUsername();
		ctx.drawText(textRenderer, Text.literal("Active: " + current), guiX + 8, guiY + 22, 0xFFAAAAAA, false);

		// name field + Create button
		int fieldY = guiY + 34;
		int btnW = 54;
		int fieldX2 = guiX + guiW - btnW - 10;
		ctx.fill(guiX + 8, fieldY, fieldX2, fieldY + 14, 0xFF1B1B1B);
		ctx.fill(guiX + 8, fieldY, fieldX2, fieldY + 1, 0xFF333333);
		String shown = nameBuffer.isEmpty() ? "type a nick..." : nameBuffer;
		int textColor = nameBuffer.isEmpty() ? 0xFF666666 : 0xFFFFFFFF;
		ctx.drawText(textRenderer, Text.literal(shown + "_"), guiX + 12, fieldY + 3, textColor, false);

		int btnX = fieldX2 + 4;
		ctx.fill(btnX, fieldY, btnX + btnW, fieldY + 14, accent & 0xC8FFFFFF);
		ctx.drawText(textRenderer, Text.literal("Create"), btnX + 9, fieldY + 3, 0xFFFFFFFF, false);
		elements.add(new Element(btnX, fieldY, btnX + btnW, fieldY + 14, this::createProfile));

		if (!status.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal(status), guiX + 8, fieldY + 18, 0xFF999999, false);
		}

		// profile list
		int listY = guiY + 58;
		int listY2 = guiY + guiH - 22;
		ctx.enableScissor(guiX + 8, listY, guiX + guiW - 8, listY2);
		List<String> profiles = AltManager.profiles();
		int y = listY - (int) scroll;
		if (profiles.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal("No accounts yet. Type a nick + Create."), guiX + 12, y + 4, 0xFF777777, false);
		}
		for (String name : profiles) {
			int rx1 = guiX + 8, rx2 = guiX + guiW - 8;
			boolean hover = mouseX >= rx1 && mouseX <= rx2 && mouseY >= y && mouseY <= y + rowH
					&& mouseY >= listY && mouseY <= listY2;
			boolean active = name.equalsIgnoreCase(current);
			ctx.fill(rx1, y, rx2, y + rowH - 2, hover ? 0xFF202020 : 0xFF161616);
			if (active) ctx.fill(rx1, y, rx1 + 2, y + rowH - 2, accent);
			ctx.drawText(textRenderer, Text.literal(name), rx1 + 6, y + 4, active ? accent : 0xFFCCCCCC, false);

			int delW = 20, swW = 50, gap = 4;
			int delX = rx2 - delW - 4;
			int swX = delX - swW - gap;
			ctx.fill(swX, y + 1, swX + swW, y + rowH - 3, accent & 0xAAFFFFFF);
			ctx.drawText(textRenderer, Text.literal("Switch"), swX + 6, y + 4, 0xFFFFFFFF, false);
			ctx.fill(delX, y + 1, delX + delW, y + rowH - 3, 0xFF802020);
			ctx.drawText(textRenderer, Text.literal("X"), delX + 7, y + 4, 0xFFFFFFFF, false);

			final String fn = name;
			elements.add(new Element(swX, y + 1, swX + swW, y + rowH - 3, () -> switchTo(fn)));
			elements.add(new Element(delX, y + 1, delX + delW, y + rowH - 3, () -> deleteProfile(fn)));
			y += rowH;
		}
		ctx.disableScissor();

		// Back button
		int backY = guiY + guiH - 18;
		ctx.fill(guiX + 8, backY, guiX + 68, backY + 14, 0xFF222222);
		ctx.drawText(textRenderer, Text.literal("Back"), guiX + 26, backY + 3, 0xFFCCCCCC, false);
		elements.add(new Element(guiX + 8, backY, guiX + 68, backY + 14, this::close));

		super.render(ctx, mouseX, mouseY, tickDelta);
	}

	private void createProfile() {
		if (nameBuffer.trim().isEmpty()) { status = "Enter a nick first."; return; }
		boolean ok = AltManager.add(nameBuffer);
		status = ok ? "Created '" + nameBuffer.trim() + "'." : "Invalid or duplicate nick.";
		if (ok) nameBuffer = "";
	}

	private void switchTo(String name) {
		try {
			MinecraftClient mc = MinecraftClient.getInstance();
			((MinecraftClientMixin) (Object) mc).dnsvisuals$setSession(AltManager.createOfflineSession(name));
			status = "Switched to '" + name + "' (offline). Rejoin servers to apply.";
		} catch (Throwable t) {
			status = "Failed to switch: " + t.getMessage();
		}
	}

	private void deleteProfile(String name) {
		boolean ok = AltManager.remove(name);
		status = ok ? "Removed '" + name + "'." : "Could not remove '" + name + "'.";
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
	public void close() {
		MinecraftClient.getInstance().setScreen(parent);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int key = input.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { createProfile(); return true; }
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
			return true;
		}
		Character c = mapKey(key);
		if (c != null && nameBuffer.length() < 16) {
			nameBuffer += c;
			return true;
		}
		return super.keyPressed(input);
	}

	/** Maps a GLFW key code to a safe nickname character, or null if not allowed. */
	private static Character mapKey(int key) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
			return (char) ('A' + (key - GLFW.GLFW_KEY_A));
		}
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
			return (char) ('0' + (key - GLFW.GLFW_KEY_0));
		}
		if (key == GLFW.GLFW_KEY_MINUS) return '_';
		return null;
	}
}
