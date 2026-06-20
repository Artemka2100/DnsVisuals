package dns.visuals.module;

import dns.visuals.DnsVisuals;
import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.KeybindSetting;
import dns.visuals.setting.ModeSetting;
import dns.visuals.setting.SliderSetting;
import dns.visuals.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Holds and builds every module. 5 categories, 5 modules each, varied setting types. */
public class ModuleManager {
	public static final ModuleManager INSTANCE = new ModuleManager();

	private final List<Module> modules = new ArrayList<>();

	public List<Module> all() {
		return modules;
	}

	public List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (m.category == c) out.add(m);
		return out;
	}

	public Module find(String name) {
		for (Module m : modules) if (m.name.equalsIgnoreCase(name)) return m;
		return null;
	}

	private Module reg(Module m) {
		modules.add(m);
		return m;
	}

	private static TextRenderer tr() {
		return MinecraftClient.getInstance().textRenderer;
	}

	public void init() {
		if (!modules.isEmpty()) return;
		buildHud();
		buildRender();
		buildInterface();
		buildChat();
		buildMisc();
	}

	// =========================== HUD ===========================
	private void buildHud() {
		// 1. Watermark
		reg(new Module("Watermark", "Client name on screen", Category.HUD)
				.add(new ModeSetting("Style", "Text style", "Full", "Full", "Compact"))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Show version", "Append version", true))
				.add(new BooleanSetting("Rainbow", "Animated color", false))
				.add(new ColorSetting("Color", "Accent color", 255, 122, 0, 255))
				.add(new SliderSetting("Rainbow speed", "Hue cycle speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.hud((ctx, x, y, self) -> {
					boolean compact = self.modeVal("Style").equals("Compact");
					String text = compact ? "DnsV" : "DnsVisuals";
					if (self.boolVal("Show version")) text += " v1.2";
					int color = self.boolVal("Rainbow")
							? ColorUtil.rainbow(self.numVal("Rainbow speed"), 0)
							: self.colorVal("Color");
					ctx.drawText(tr(), text, x, y, color, self.boolVal("Shadow"));
					return tr().fontHeight;
				}));

		// 2. FPS
		reg(new Module("FPS", "Frames per second", Category.HUD)
				.add(new BooleanSetting("Label", "Show 'FPS:' prefix", true))
				.add(new BooleanSetting("Brackets", "Wrap in [ ]", false))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ModeSetting("Color mode", "Static or dynamic", "Dynamic", "Static", "Dynamic"))
				.add(new ColorSetting("Color", "Static color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					int fps = MinecraftClient.getInstance().getCurrentFps();
					String text = (self.boolVal("Label") ? "FPS: " : "") + fps;
					if (self.boolVal("Brackets")) text = "[" + text + "]";
					int color;
					if (self.modeVal("Color mode").equals("Dynamic")) {
						color = fps >= 120 ? 0xFF55FF55 : fps >= 60 ? 0xFFFFFF55 : 0xFFFF5555;
					} else {
						color = self.colorVal("Color");
					}
					ctx.drawText(tr(), text, x, y, color, self.boolVal("Shadow"));
					return tr().fontHeight;
				}));

		// 3. CPS
		reg(new Module("CPS", "Clicks per second", Category.HUD)
				.add(new ModeSetting("Button", "Which button", "Both", "Left", "Right", "Both"))
				.add(new BooleanSetting("Label", "Show 'CPS:' prefix", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.add(new BooleanSetting("Rainbow", "Animated color", false))
				.hud((ctx, x, y, self) -> {
					String mode = self.modeVal("Button");
					String val;
					if (mode.equals("Left")) val = String.valueOf(DnsVisuals.CPS.left());
					else if (mode.equals("Right")) val = String.valueOf(DnsVisuals.CPS.right());
					else val = DnsVisuals.CPS.left() + " | " + DnsVisuals.CPS.right();
					String text = (self.boolVal("Label") ? "CPS: " : "") + val;
					int color = self.boolVal("Rainbow") ? ColorUtil.rainbow(1.0, 0) : self.colorVal("Color");
					ctx.drawText(tr(), text, x, y, color, self.boolVal("Shadow"));
					return tr().fontHeight;
				}));

		// 4. TPS
		reg(new Module("TPS", "Estimated server ticks", Category.HUD)
				.add(new BooleanSetting("Label", "Show 'TPS:' prefix", true))
				.add(new SliderSetting("Decimals", "Decimal places", 1, 0, 2, 1, ""))
				.add(new BooleanSetting("Color by value", "Green/yellow/red", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Static color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					double tps = DnsVisuals.TPS.get();
					int dec = (int) self.numVal("Decimals");
					String num = String.format("%." + dec + "f", tps);
					String text = (self.boolVal("Label") ? "TPS: " : "") + num;
					int color = self.colorVal("Color");
					if (self.boolVal("Color by value")) {
						color = tps >= 19 ? 0xFF55FF55 : tps >= 15 ? 0xFFFFFF55 : 0xFFFF5555;
					}
					ctx.drawText(tr(), text, x, y, color, self.boolVal("Shadow"));
					return tr().fontHeight;
				}));

		// 5. ArmorHUD
		reg(new Module("ArmorHUD", "Your armor durability", Category.HUD)
				.add(new ModeSetting("Direction", "Layout", "Vertical", "Vertical", "Horizontal"))
				.add(new BooleanSetting("Durability", "Show value", true))
				.add(new BooleanSetting("Percent", "Show as percent", false))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					ClientPlayerEntity p = MinecraftClient.getInstance().player;
					if (p == null) return 0;
					String[] names = {"Boots", "Legs", "Chest", "Helmet"};
					boolean horizontal = self.modeVal("Direction").equals("Horizontal");
					int color = self.colorVal("Color");
					boolean shadow = self.boolVal("Shadow");
					int dx = x, dy = y, used = 0;
					EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
					for (int i = 3; i >= 0; i--) {
						ItemStack piece = p.getEquippedStack(slots[i]);
						String line;
						if (piece.isEmpty()) {
							line = names[i] + ": -";
						} else if (self.boolVal("Durability") && piece.isDamageable()) {
							int left = piece.getMaxDamage() - piece.getDamage();
							if (self.boolVal("Percent")) {
								int pct = (int) Math.round(100.0 * left / piece.getMaxDamage());
								line = names[i] + ": " + pct + "%";
							} else {
								line = names[i] + ": " + left + "/" + piece.getMaxDamage();
							}
						} else {
							line = names[i] + ": \u221e";
						}
						if (horizontal) {
							ctx.drawText(tr(), line, dx, y, color, shadow);
							dx += tr().getWidth(line) + 8;
							used = tr().fontHeight;
						} else {
							ctx.drawText(tr(), line, x, dy, color, shadow);
							dy += tr().fontHeight + 1;
							used += tr().fontHeight + 1;
						}
					}
					return used;
				}));
	}

	// =========================== RENDER ===========================
	private void buildRender() {
		reg(new Module("Crosshair", "Custom static crosshair", Category.RENDER)
				.add(new ModeSetting("Type", "Shape", "Cross", "Cross", "Dot", "Circle"))
				.add(new SliderSetting("Size", "Pixel size", 5, 1, 12, 1, "px"))
				.add(new SliderSetting("Gap", "Center gap", 2, 0, 8, 1, "px"))
				.add(new BooleanSetting("Outline", "Black outline", true))
				.add(new ColorSetting("Color", "Crosshair color", 255, 255, 255, 255)));

		reg(new Module("ViewBobbing", "Adjust hand/view bob", Category.RENDER)
				.add(new SliderSetting("Amount", "Bobbing intensity", 100, 0, 100, 5, "%"))
				.add(new BooleanSetting("Hand", "Bob the hand", true))
				.add(new BooleanSetting("Camera", "Bob the camera", true))
				.add(new ModeSetting("Mode", "Bob style", "Vanilla", "Vanilla", "Smooth", "Off"))
				.add(new BooleanSetting("Sprinting only", "Only while sprinting", false)));

		reg(new Module("Zoom", "Optifine-style zoom", Category.RENDER)
				.add(new KeybindSetting("Key", "Hold to zoom", GLFW.GLFW_KEY_C))
				.add(new SliderSetting("Factor", "Zoom amount", 3, 1.5, 8, 0.5, "x"))
				.add(new BooleanSetting("Smooth", "Smooth transition", true))
				.add(new BooleanSetting("Cinematic", "Cinematic camera", false))
				.add(new SliderSetting("Speed", "Transition speed", 1.0, 0.2, 3.0, 0.1, "x")));

		reg(new Module("BlockOutline", "Custom block selection box", Category.RENDER)
				.add(new SliderSetting("Width", "Line width", 2, 1, 5, 0.5, "px"))
				.add(new BooleanSetting("Rainbow", "Animated color", false))
				.add(new SliderSetting("Rainbow speed", "Hue speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.add(new ColorSetting("Color", "Outline color", 0, 0, 0, 160))
				.add(new BooleanSetting("Fill", "Fill the box", false)));

		reg(new Module("SkyColor", "Recolor the sky (cosmetic)", Category.RENDER)
				.add(new BooleanSetting("Override", "Force custom sky", true))
				.add(new ColorSetting("Color", "Sky color", 30, 30, 60, 255))
				.add(new SliderSetting("Blend", "Blend with vanilla", 60, 0, 100, 5, "%"))
				.add(new BooleanSetting("Affect fog", "Tint fog too", true))
				.add(new ModeSetting("When", "Apply during", "Always", "Always", "Day", "Night")));

		reg(new Module("Hitboxes", "Entity boxes + hit color & particles", Category.RENDER)
				.add(new ColorSetting("Color", "Normal box color", 255, 255, 255, 160))
				.add(new ColorSetting("Hit color", "Color while hitting", 255, 60, 60, 220))
				.add(new SliderSetting("Range", "Max distance", 24, 4, 64, 1, "m"))
				.add(new SliderSetting("Hit flash", "Hit color duration", 300, 50, 1000, 50, "ms"))
				.add(new BooleanSetting("Players only", "Only show players", false))
				.add(new BooleanSetting("Particles", "Particles on hit", true))
				.add(new ModeSetting("Particle", "Particle type", "Crit", "Crit", "Flame", "Heart", "Cloud", "Angry"))
				.add(new SliderSetting("Particle count", "Particles per hit", 16, 1, 50, 1, "")));
	}

	// =========================== INTERFACE ===========================
	private void buildInterface() {
		reg(new Module("ClickGUI", "This menu", Category.INTERFACE)
				.add(new KeybindSetting("Open key", "Key to open menu", GLFW.GLFW_KEY_RIGHT_SHIFT))
				.add(new SliderSetting("Scale", "Menu scale", 1.0, 0.7, 1.5, 0.05, "x"))
				.add(new BooleanSetting("Background dim", "Darken screen", false))
				.add(new BooleanSetting("Hover glow", "Highlight on hover", true))
				.add(new ModeSetting("Layout", "Tab placement", "Left", "Left", "Top")));

		reg(new Module("Theme", "Colors of the client", Category.INTERFACE)
				.add(new ColorSetting("Accent", "Primary accent", 255, 122, 0, 255))
				.add(new ColorSetting("Background", "Panel background", 16, 16, 16, 235))
				.add(new BooleanSetting("Rainbow accent", "Cycle accent hue", false))
				.add(new SliderSetting("Rainbow speed", "Hue speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.add(new ModeSetting("Base", "Background tone", "Black", "Black", "Dark", "Gray")));

		reg(new Module("Animations", "GUI motion feel", Category.INTERFACE)
				.add(new SliderSetting("Speed", "Animation speed", 12, 4, 30, 1, ""))
				.add(new BooleanSetting("Toggle slide", "Slide on toggle", true))
				.add(new BooleanSetting("Settings expand", "Animate settings", true))
				.add(new BooleanSetting("Hover fade", "Fade highlight", true))
				.add(new ModeSetting("Easing", "Curve", "EaseOut", "EaseOut", "EaseInOut", "Linear")));

		reg(new Module("Notifications", "Toasts on actions", Category.INTERFACE)
				.add(new BooleanSetting("On toggle", "Notify on module toggle", true))
				.add(new SliderSetting("Duration", "Seconds on screen", 2.0, 0.5, 6.0, 0.5, "s"))
				.add(new ModeSetting("Position", "Corner", "Top right", "Top right", "Bottom right", "Bottom left"))
				.add(new BooleanSetting("Slide in", "Slide animation", true))
				.add(new ColorSetting("Color", "Accent strip", 255, 122, 0, 255)));

		reg(new Module("HudEditor", "Arrange HUD elements", Category.INTERFACE)
				.add(new KeybindSetting("Key", "Open editor", GLFW.GLFW_KEY_RIGHT_CONTROL))
				.add(new BooleanSetting("Snap to grid", "Snap movement", true))
				.add(new SliderSetting("Grid size", "Grid pixels", 4, 1, 16, 1, "px"))
				.add(new BooleanSetting("Show outlines", "Outline elements", true))
				.add(new BooleanSetting("Lock", "Lock positions", false)));
	}

	// =========================== CHAT ===========================
	private void buildChat() {
		reg(new Module("ChatTimestamps", "Prefix messages with time", Category.CHAT)
				.add(new ModeSetting("Format", "Clock format", "HH:mm", "HH:mm", "HH:mm:ss", "hh:mm a"))
				.add(new BooleanSetting("Brackets", "Wrap in [ ]", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Timestamp color", 150, 150, 150, 255))
				.add(new BooleanSetting("24h", "24-hour clock", true)));

		reg(new Module("BetterChat", "Chat quality of life", Category.CHAT)
				.add(new BooleanSetting("Infinite history", "Keep more lines", true))
				.add(new SliderSetting("History", "Stored lines", 500, 100, 2000, 50, ""))
				.add(new BooleanSetting("Anti spam", "Stack duplicates", true))
				.add(new BooleanSetting("Smooth scroll", "Smooth scrolling", true))
				.add(new BooleanSetting("Copy on click", "Click to copy line", true)));

		reg(new Module("ChatPrefix", "Personal client prefix", Category.CHAT)
				.add(new ModeSetting("Symbol", "Prefix symbol", ".", ".", "!", ">", "-"))
				.add(new BooleanSetting("Enabled", "Use prefix", true))
				.add(new ColorSetting("Color", "Prefix color", 255, 122, 0, 255))
				.add(new BooleanSetting("Bold", "Bold prefix", true))
				.add(new SliderSetting("Repeat", "Symbol count", 1, 1, 3, 1, "")));

		reg(new Module("AutoText", "Quick chat macros (your own messages)", Category.CHAT)
				.add(new KeybindSetting("Key", "Send macro", GLFW.GLFW_KEY_G))
				.add(new ModeSetting("Message", "Preset text", "gg", "gg", "glhf", "gl", "wp"))
				.add(new SliderSetting("Delay", "Cooldown", 1.0, 0, 5, 0.5, "s"))
				.add(new BooleanSetting("Enabled", "Active", true))
				.add(new ColorSetting("Color", "Confirm color", 85, 255, 85, 255)));

		reg(new Module("ClearChat", "Clear chat with a key", Category.CHAT)
				.add(new KeybindSetting("Key", "Clear key", GLFW.GLFW_KEY_DELETE))
				.add(new BooleanSetting("Confirm", "Ask before clearing", false))
				.add(new BooleanSetting("Fade", "Fade-out animation", true))
				.add(new ModeSetting("Scope", "What to clear", "All", "All", "Visible"))
				.add(new BooleanSetting("Notify", "Show toast", true)));
	}

	// =========================== MISC ===========================
	private void buildMisc() {
		reg(new Module("FpsLimit", "Cap framerate to save power", Category.MISC)
				.add(new SliderSetting("Limit", "Max FPS", 120, 30, 260, 5, ""))
				.add(new BooleanSetting("When unfocused", "Lower when alt-tabbed", true))
				.add(new SliderSetting("Unfocused FPS", "FPS when unfocused", 15, 5, 60, 5, ""))
				.add(new BooleanSetting("Show in HUD", "Display cap", false))
				.add(new ModeSetting("Mode", "Cap behavior", "Smooth", "Smooth", "Hard")));

		reg(new Module("WindowTitle", "Custom game window title", Category.MISC)
				.add(new ModeSetting("Title", "Window text", "DnsVisuals", "DnsVisuals", "Minecraft", "Custom"))
				.add(new BooleanSetting("Show FPS", "Append FPS", true))
				.add(new BooleanSetting("Show version", "Append MC version", false))
				.add(new BooleanSetting("Dynamic", "Live-update", true))
				.add(new SliderSetting("Update rate", "Updates/sec", 2, 1, 10, 1, "")));

		reg(new Module("SoundControl", "Quick volume helper", Category.MISC)
				.add(new SliderSetting("Master", "Master volume", 100, 0, 100, 5, "%"))
				.add(new BooleanSetting("Music", "Background music", true))
				.add(new BooleanSetting("Click sound", "Sound on GUI click", true))
				.add(new SliderSetting("Click volume", "GUI click volume", 50, 0, 100, 5, "%"))
				.add(new ModeSetting("Click pitch", "Click tone", "Normal", "Low", "Normal", "High")));

		reg(new Module("AutoSave", "Persist your config", Category.MISC)
				.add(new KeybindSetting("Save key", "Save now", GLFW.GLFW_KEY_UNKNOWN))
				.add(new BooleanSetting("Auto save", "Save on close", true))
				.add(new SliderSetting("Interval", "Autosave minutes", 5, 1, 30, 1, "m"))
				.add(new BooleanSetting("Backup", "Keep backup copy", true))
				.add(new BooleanSetting("Notify", "Toast on save", true)));

		reg(new Module("CustomSplash", "Custom main-menu splash text", Category.MISC)
				.add(new BooleanSetting("Enabled", "Override splash", true))
				.add(new ModeSetting("Text", "Splash preset", "DnsVisuals!", "DnsVisuals!", "Hello :)", "GG"))
				.add(new BooleanSetting("Rainbow", "Animated color", true))
				.add(new SliderSetting("Rainbow speed", "Hue speed", 1.0, 0.1, 4, 0.1, "x"))
				.add(new ColorSetting("Color", "Static color", 255, 255, 85, 255)));
	}
}
