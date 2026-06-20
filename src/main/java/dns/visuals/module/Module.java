package dns.visuals.module;

import dns.visuals.setting.BooleanSetting;
import dns.visuals.setting.ColorSetting;
import dns.visuals.setting.KeybindSetting;
import dns.visuals.setting.ModeSetting;
import dns.visuals.setting.Setting;
import dns.visuals.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A toggleable feature. Modules are created in {@link ModuleManager} and configured with a
 * fluent API (add settings, attach an optional HUD renderer).
 *
 * Left-click in the GUI toggles the module; right-click expands its settings.
 */
public class Module {
	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting> settings = new ArrayList<>();

	private boolean enabled = false;

	// GUI animation state (0..1)
	public double toggleAnim = 0;
	public double hoverAnim = 0;
	public double openAnim = 0;
	public boolean settingsOpen = false;

	public HudRenderer hudRenderer = null;

	protected final MinecraftClient mc = MinecraftClient.getInstance();

	public Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	public Module add(Setting s) {
		settings.add(s);
		return this;
	}

	public Module hud(HudRenderer renderer) {
		this.hudRenderer = renderer;
		return this;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean e) {
		if (enabled == e) return;
		enabled = e;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	// ---- convenience setting lookups (used by HUD renderers) ----

	public Setting get(String n) {
		for (Setting s : settings) if (s.name.equalsIgnoreCase(n)) return s;
		return null;
	}

	public boolean boolVal(String n) {
		return get(n) instanceof BooleanSetting b && b.value;
	}

	public double numVal(String n) {
		return get(n) instanceof SliderSetting s ? s.value : 0;
	}

	public String modeVal(String n) {
		return get(n) instanceof ModeSetting m ? m.get() : "";
	}

	public int colorVal(String n) {
		return get(n) instanceof ColorSetting c ? c.argb() : 0xFFFFFFFF;
	}

	public int keyVal(String n) {
		return get(n) instanceof KeybindSetting k ? k.key : -1;
	}

	/** Implemented via lambda for HUD modules. Draws starting at (x, y); returns consumed height. */
	public interface HudRenderer {
		int render(DrawContext ctx, int x, int y, Module self);
	}
}
