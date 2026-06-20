package dns.visuals.setting;

import java.util.Arrays;
import java.util.List;

/** A multiple-choice setting that cycles through string options. */
public class ModeSetting extends Setting {
	public final List<String> modes;
	public int index;

	public ModeSetting(String name, String description, String def, String... options) {
		super(name, description);
		this.modes = Arrays.asList(options);
		int i = modes.indexOf(def);
		this.index = i < 0 ? 0 : i;
	}

	public String get() {
		return modes.get(index);
	}

	public boolean is(String value) {
		return get().equalsIgnoreCase(value);
	}

	public void cycle(int dir) {
		index = (index + dir + modes.size()) % modes.size();
	}

	@Override
	public String save() {
		return get();
	}

	@Override
	public void load(String v) {
		int i = modes.indexOf(v);
		if (i >= 0) index = i;
	}
}
