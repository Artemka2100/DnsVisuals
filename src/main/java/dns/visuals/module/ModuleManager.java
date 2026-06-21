package dns.visuals.module;

import dns.visuals.DnsVisuals;
import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.KeybindSetting;
import dns.visuals.setting.ModeSetting;
import dns.visuals.setting.SliderSetting;
import dns.visuals.util.AttackTracker;
import dns.visuals.util.ColorUtil;
import dns.visuals.util.CooldownTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Holds and builds every module. */
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
		// 1. Watermark -> "DnsVisuals v1.2 | Developer | beta-test"
		reg(new Module("Watermark", "Client name banner", Category.HUD)
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Background", "Panel behind text", true))
				.add(new BooleanSetting("Rainbow", "Animate name color", false))
				.add(new SliderSetting("Rainbow speed", "Hue cycle speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.add(new ColorSetting("Color", "Name color", 255, 122, 0, 255))
				.add(new ColorSetting("Separator", "Separator color", 130, 130, 130, 255))
				.add(new ModeSetting("Tag", "Right-side tag", "beta-test", "beta-test", "release", "private", "dev"))
				.hud((ctx, x, y, self) -> {
					boolean shadow = self.boolVal("Shadow");
					int name = self.boolVal("Rainbow")
							? ColorUtil.rainbow(self.numVal("Rainbow speed"), 0)
							: self.colorVal("Color");
					int sep = self.colorVal("Separator");
					int white = 0xFFFFFFFF;
					String p1 = "DnsVisuals v1.2";
					String p2 = "Developer";
					String p3 = self.modeVal("Tag");
					String s = " | ";
					int sw = tr().getWidth(s);
					int total = tr().getWidth(p1) + sw + tr().getWidth(p2) + sw + tr().getWidth(p3);
					int pad = 3;
					int h = tr().fontHeight;
					if (self.boolVal("Background")) {
						ctx.fill(x - pad, y - pad, x + total + pad, y + h + pad, 0x90000000);
						ctx.fill(x - pad, y - pad, x - pad + 1, y + h + pad, name);
					}
					int cx = x;
					ctx.drawText(tr(), p1, cx, y, name, shadow); cx += tr().getWidth(p1);
					ctx.drawText(tr(), s, cx, y, sep, shadow); cx += sw;
					ctx.drawText(tr(), p2, cx, y, white, shadow); cx += tr().getWidth(p2);
					ctx.drawText(tr(), s, cx, y, sep, shadow); cx += sw;
					ctx.drawText(tr(), p3, cx, y, white, shadow);
					return h + (self.boolVal("Background") ? pad : 0);
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

		// 6. ArrayList -> enabled modules, left/right alignable (self-positions)
		reg(new Module("ArrayList", "List of enabled modules", Category.HUD)
				.add(new ModeSetting("Align", "Screen side", "Right", "Left", "Right"))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Background", "Bar behind text", true))
				.add(new BooleanSetting("Rainbow", "Rainbow gradient", true))
				.add(new SliderSetting("Rainbow speed", "Hue speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.add(new ColorSetting("Color", "Static color", 255, 122, 0, 255))
				.hud((ctx, x, y, self) -> {
					MinecraftClient mc = MinecraftClient.getInstance();
					int screenW = mc.getWindow().getScaledWidth();
					boolean right = self.modeVal("Align").equals("Right");
					boolean shadow = self.boolVal("Shadow");
					List<String> names = new ArrayList<>();
					for (Module m : all()) {
						if (m.isEnabled() && m.category != Category.INTERFACE) names.add(m.name);
					}
					names.sort((a, b) -> tr().getWidth(b) - tr().getWidth(a));
					int dy = 2;
					int i = 0;
					for (String n : names) {
						int w = tr().getWidth(n);
						int tx = right ? screenW - w - 3 : 3;
						int color = self.boolVal("Rainbow")
								? ColorUtil.rainbow(self.numVal("Rainbow speed"), i * 40)
								: self.colorVal("Color");
						if (self.boolVal("Background")) {
							ctx.fill(tx - 2, dy - 1, tx + w + 2, dy + tr().fontHeight, 0x90000000);
							int stripX = right ? tx + w + 1 : tx - 2;
							ctx.fill(stripX, dy - 1, stripX + 1, dy + tr().fontHeight, color);
						}
						ctx.drawText(tr(), n, tx, dy, color, shadow);
						dy += tr().fontHeight + 1;
						i++;
					}
					return 0;
				}));

		// 7. Effects -> active potion effects
		reg(new Module("Effects", "Active potion effects", Category.HUD)
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Show level", "Append level", true))
				.add(new BooleanSetting("Show time", "Append duration", true))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					ClientPlayerEntity p = MinecraftClient.getInstance().player;
					if (p == null) return 0;
					int color = self.colorVal("Color");
					boolean shadow = self.boolVal("Shadow");
					int dy = y;
					for (StatusEffectInstance e : p.getStatusEffects()) {
						StringBuilder sb = new StringBuilder(
								Text.translatable(e.getEffectType().value().getTranslationKey()).getString());
						if (self.boolVal("Show level")) sb.append(' ').append(e.getAmplifier() + 1);
						if (self.boolVal("Show time")) {
							int secs = e.getDuration() / 20;
							sb.append("  ").append(secs / 60).append(':').append(String.format("%02d", secs % 60));
						}
						ctx.drawText(tr(), sb.toString(), x, dy, color, shadow);
						dy += tr().fontHeight + 1;
					}
					return dy == y ? 0 : (dy - y);
				}));

		// 8. PlayerInfo -> shows last entity you hit (positioned by HudManager)
		reg(new Module("PlayerInfo", "Target panel when you hit someone", Category.HUD)
				.add(new SliderSetting("Duration", "Seconds shown", 4, 1, 10, 1, "s"))
				.add(new BooleanSetting("Health bar", "Show health bar", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.add(new ColorSetting("Accent", "Accent strip", 255, 122, 0, 255))
				.hud((ctx, x, y, self) -> {
					long ms = (long) (self.numVal("Duration") * 1000);
					LivingEntity t = AttackTracker.recentLiving(ms);
					if (t == null) return 0;
					boolean shadow = self.boolVal("Shadow");
					int color = self.colorVal("Color");
					float hp = t.getHealth();
					float max = Math.max(1f, t.getMaxHealth());
					String line = t.getName().getString() + "  "
							+ String.format("%.1f", hp) + "/" + String.format("%.1f", max);
					boolean bar = self.boolVal("Health bar");
					int w = Math.max(tr().getWidth(line), 70) + 8;
					int h = tr().fontHeight + (bar ? 9 : 0) + 6;
					ctx.fill(x, y, x + w, y + h, 0x90000000);
					ctx.fill(x, y, x + 2, y + h, self.colorVal("Accent"));
					ctx.drawText(tr(), line, x + 5, y + 3, color, shadow);
					if (bar) {
						int by = y + tr().fontHeight + 5;
						int bw = w - 10;
						float frac = Math.max(0f, Math.min(1f, hp / max));
						int hc = frac > 0.5f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
						ctx.fill(x + 5, by, x + 5 + bw, by + 3, 0xFF333333);
						ctx.fill(x + 5, by, x + 5 + (int) (bw * frac), by + 3, hc);
					}
					return h;
				}));

		// 9. Coordinates -> XYZ position
		reg(new Module("Coordinates", "Your XYZ position", Category.HUD)
				.add(new BooleanSetting("Label", "Show 'XYZ:' prefix", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Dimension coords", "Show other-dimension XZ", false))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					MinecraftClient mc = MinecraftClient.getInstance();
					ClientPlayerEntity p = mc.player;
					if (p == null) return 0;
					int px = (int) Math.floor(p.getX());
					int py = (int) Math.floor(p.getY());
					int pz = (int) Math.floor(p.getZ());
					boolean shadow = self.boolVal("Shadow");
					int color = self.colorVal("Color");
					String text = (self.boolVal("Label") ? "XYZ: " : "") + px + ", " + py + ", " + pz;
					ctx.drawText(tr(), text, x, y, color, shadow);
					int used = tr().fontHeight;
					if (self.boolVal("Dimension coords")) {
						boolean nether = mc.world != null && mc.world.getRegistryKey() == World.NETHER;
						int ox, oz;
						String tag;
						if (nether) { ox = px * 8; oz = pz * 8; tag = "OW"; }
						else { ox = px / 8; oz = pz / 8; tag = "Nether"; }
						String line2 = tag + ": " + ox + ", " + oz;
						ctx.drawText(tr(), line2, x, y + used + 1, 0xFFAAAAAA, shadow);
						used += tr().fontHeight + 1;
					}
					return used;
				}));

		// 10. Ping -> latency to the server
		reg(new Module("Ping", "Your latency to the server", Category.HUD)
				.add(new BooleanSetting("Label", "Show 'Ping:' prefix", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Color by value", "Green/yellow/red", true))
				.add(new ColorSetting("Color", "Static color", 255, 255, 255, 255))
				.hud((ctx, x, y, self) -> {
					MinecraftClient mc = MinecraftClient.getInstance();
					int ping = 0;
					if (mc.getNetworkHandler() != null && mc.player != null) {
						var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
						if (e != null) ping = e.getLatency();
					}
					String text = (self.boolVal("Label") ? "Ping: " : "") + ping + "ms";
					int color = self.colorVal("Color");
					if (self.boolVal("Color by value")) {
						color = ping < 60 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : 0xFFFF5555;
					}
					ctx.drawText(tr(), text, x, y, color, self.boolVal("Shadow"));
					return tr().fontHeight;
				}));

		// 11. CooldownList -> item cooldowns as text (no icons)
		reg(new Module("CooldownList", "Item cooldowns (text only)", Category.HUD)
				.add(new BooleanSetting("Label", "Show 'Cooldowns:' header", true))
				.add(new BooleanSetting("Shadow", "Text shadow", true))
				.add(new BooleanSetting("Hide when empty", "Hide if nothing cooling", true))
				.add(new ColorSetting("Color", "Text color", 255, 255, 255, 255))
				.add(new ColorSetting("Accent", "Header color", 255, 122, 0, 255))
				.hud((ctx, x, y, self) -> {
					java.util.List<String> lines = CooldownTracker.poll();
					if (lines.isEmpty() && self.boolVal("Hide when empty")) return 0;
					boolean shadow = self.boolVal("Shadow");
					int dy = y;
					if (self.boolVal("Label")) {
						ctx.drawText(tr(), "Cooldowns:", x, dy, self.colorVal("Accent"), shadow);
						dy += tr().fontHeight + 1;
					}
					if (lines.isEmpty()) {
						ctx.drawText(tr(), "none", x, dy, self.colorVal("Color"), shadow);
						dy += tr().fontHeight + 1;
					} else {
						for (String l : lines) {
							ctx.drawText(tr(), l, x, dy, self.colorVal("Color"), shadow);
							dy += tr().fontHeight + 1;
						}
					}
					return dy - y;
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
				.add(new SliderSetting("Box size", "Grow/shrink box (visual)", 0, -0.3, 0.6, 0.05, "m"))
				.add(new SliderSetting("Hit flash", "Hit color duration", 300, 50, 1000, 50, "ms"))
				.add(new BooleanSetting("Players only", "Only show players", false))
				.add(new BooleanSetting("Particles", "Particles on hit", true))
				.add(new ModeSetting("Particle", "Particle type", "Crit", "Crit", "Flame", "Heart", "Cloud", "Angry"))
				.add(new SliderSetting("Particle count", "Particles per hit", 16, 1, 50, 1, "")));

		// ESP -> highlight visible players/entities
		reg(new Module("ESP", "Highlight players/entities in sight", Category.RENDER)
				.add(new ModeSetting("Mode", "Render style", "Outline", "Outline", "Fill", "Both"))
				.add(new BooleanSetting("Visible only", "Only when in line of sight", true))
				.add(new BooleanSetting("Players only", "Only players", true))
				.add(new BooleanSetting("Nametag", "Name above entity", true))
				.add(new BooleanSetting("Health in name", "Append health to name", true))
				.add(new SliderSetting("Range", "Max distance", 48, 4, 128, 1, "m"))
				.add(new SliderSetting("Line width", "Outline width", 1.5, 0.5, 4, 0.5, "px"))
				.add(new ColorSetting("Color", "ESP color", 255, 80, 80, 255))
				.add(new SliderSetting("Fill opacity", "Fill alpha", 40, 0, 150, 5, "")));

		// Chams -> solid color FILL on players/mobs (depth-respecting; does not show through walls)
		reg(new Module("Chams", "Solid color fill on players/mobs", Category.RENDER)
				.add(new ModeSetting("Mode", "Render style", "Fill", "Fill", "Outline", "Both"))
				.add(new ModeSetting("Color mode", "How the color is chosen", "Static", "Static", "Rainbow", "Health", "Distance"))
				.add(new SliderSetting("Rainbow speed", "Hue speed", 1.0, 0.1, 4.0, 0.1, "x"))
				.add(new BooleanSetting("Players", "Color other players", true))
				.add(new BooleanSetting("Mobs", "Color mobs/animals", false))
				.add(new BooleanSetting("Self", "Color your own model (3rd person)", false))
				.add(new BooleanSetting("Visible only", "Only when in line of sight", true))
				.add(new ColorSetting("Players color", "Player fill color", 255, 120, 120, 255))
				.add(new ColorSetting("Mobs color", "Mob fill color", 120, 200, 255, 255))
				.add(new SliderSetting("Opacity", "Fill alpha", 255, 40, 255, 5, "")));

		// NoRender -> disable selected visual effects
		reg(new Module("NoRender", "Disable selected visual effects", Category.RENDER)
				.add(new BooleanSetting("HurtCam", "No damage camera tilt", true))
				.add(new BooleanSetting("Hand", "Hide first-person hand", false))
				.add(new BooleanSetting("Fire", "Hide first-person fire overlay", true))
				.add(new BooleanSetting("Overlays", "Hide all misc screen overlays (fire/powder snow)", false))
				.add(new BooleanSetting("Vignette", "Disable the dark screen-edge vignette", false))
				.add(new BooleanSetting("Spyglass", "Hide the spyglass scope overlay", false))
				.add(new BooleanSetting("Portal", "Hide the nether portal screen tint", false))
				.add(new BooleanSetting("Nausea", "Hide the nausea/portal warp overlay", false))
				.add(new BooleanSetting("Effect particles", "Hide colored potion swirl particles on entities", false))
				.add(new BooleanSetting("Speed FOV", "Stop speed/sprint from changing your FOV", false))
				.add(new BooleanSetting("Blindness", "Remove the blindness darkness/fog effect", false)));

		// CustomSwing -> replace the hand swing animation (logic in HeldItemRendererMixin)
		reg(new Module("CustomSwing", "Custom hand swing animation", Category.RENDER)
				.add(new ModeSetting("Style", "Swing animation", "Vanilla", "Vanilla", "Stab", "Slide", "Spin", "Push", "Pull", "Wave", "Tap", "Circle", "Exaggerated"))
				.add(new SliderSetting("Intensity", "Animation strength", 1.0, 0.1, 2.0, 0.1, "x")));

		// CustomHand -> reposition/scale the first-person hand (logic in HeldItemRendererMixin)
		reg(new Module("CustomHand", "Reposition the first-person hand", Category.RENDER)
				.add(new SliderSetting("Size", "Hand scale", 1.0, 0.5, 2.0, 0.05, "x"))
				.add(new BooleanSetting("Per hand", "Offset each hand separately", false))
				.add(new SliderSetting("X", "Horizontal offset (both)", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Y", "Vertical offset (both)", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Z", "Depth offset (both)", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Main X", "Main hand horizontal", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Main Y", "Main hand vertical", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Main Z", "Main hand depth", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Off X", "Off hand horizontal", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Off Y", "Off hand vertical", 0, -1.0, 1.0, 0.02, ""))
				.add(new SliderSetting("Off Z", "Off hand depth", 0, -1.0, 1.0, 0.02, "")));
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
				.add(new ModeSetting("Base", "Background tone", "Black", "Black", "Dark", "Gray"))
				.add(new ModeSetting("Menu style", "GUI layout", "Panel", "Panel", "Columns")));

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

		// AutoTool -> switch to the best tool when mining
		reg(new Module("AutoTool", "Switch to the best tool when mining", Category.MISC)
				.add(new BooleanSetting("Switch back", "Restore previous slot", true))
				.add(new BooleanSetting("Only hotbar", "Use hotbar slots only", true))
				.add(new BooleanSetting("Prefer silk touch", "Prefer Silk Touch", false))
				.add(new BooleanSetting("Prefer fortune", "Prefer Fortune", false))
				.add(new BooleanSetting("Avoid low durability", "Skip nearly-broken tools", true)));

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
