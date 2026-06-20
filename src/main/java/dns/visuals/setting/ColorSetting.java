package dns.visuals.setting;

/** An RGBA color setting, edited via three R/G/B sliders in the GUI. */
public class ColorSetting extends Setting {
	public int r, g, b, a;

	public ColorSetting(String name, String description, int r, int g, int b, int a) {
		super(name, description);
		this.r = clamp(r);
		this.g = clamp(g);
		this.b = clamp(b);
		this.a = clamp(a);
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	public int argb() {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	@Override
	public String save() {
		return r + "," + g + "," + b + "," + a;
	}

	@Override
	public void load(String v) {
		String[] parts = v.split(",");
		if (parts.length == 4) {
			try {
				r = clamp(Integer.parseInt(parts[0]));
				g = clamp(Integer.parseInt(parts[1]));
				b = clamp(Integer.parseInt(parts[2]));
				a = clamp(Integer.parseInt(parts[3]));
			} catch (NumberFormatException ignored) {}
		}
	}
}
