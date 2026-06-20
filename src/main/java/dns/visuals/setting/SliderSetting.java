package dns.visuals.setting;

/** A numeric slider setting with min/max/step. */
public class SliderSetting extends Setting {
	public double value;
	public final double min;
	public final double max;
	public final double step;
	public final String unit;

	public SliderSetting(String name, String description, double value, double min, double max, double step, String unit) {
		super(name, description);
		this.value = value;
		this.min = min;
		this.max = max;
		this.step = step;
		this.unit = unit == null ? "" : unit;
	}

	public double get() {
		return value;
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	public double getFraction() {
		if (max == min) return 0;
		return (value - min) / (max - min);
	}

	public void setFromFraction(double f) {
		f = Math.max(0, Math.min(1, f));
		double raw = min + (max - min) * f;
		if (step > 0) raw = Math.round(raw / step) * step;
		value = Math.max(min, Math.min(max, raw));
	}

	public String display() {
		if (step >= 1) return getInt() + unit;
		return String.format("%.2f%s", value, unit);
	}

	@Override
	public String save() {
		return Double.toString(value);
	}

	@Override
	public void load(String v) {
		try { value = Double.parseDouble(v); } catch (NumberFormatException ignored) {}
	}
}
