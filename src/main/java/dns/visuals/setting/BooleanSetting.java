package dns.visuals.setting;

/** A simple on/off toggle setting. */
public class BooleanSetting extends Setting {
	public boolean value;
	public double anim = 0; // 0..1 animation for the toggle pill

	public BooleanSetting(String name, String description, boolean value) {
		super(name, description);
		this.value = value;
		this.anim = value ? 1 : 0;
	}

	public boolean get() {
		return value;
	}

	public void toggle() {
		value = !value;
	}

	@Override
	public String save() {
		return Boolean.toString(value);
	}

	@Override
	public void load(String v) {
		value = Boolean.parseBoolean(v);
		anim = value ? 1 : 0;
	}
}
