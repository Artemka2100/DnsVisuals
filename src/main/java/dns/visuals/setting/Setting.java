package dns.visuals.setting;

/** Base class for every setting type. Each setting can serialize itself for the config file. */
public abstract class Setting {
	public final String name;
	public final String description;

	protected Setting(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/** Serialize the current value to a string for config saving. */
	public abstract String save();

	/** Restore the value from a saved string. */
	public abstract void load(String value);
}
