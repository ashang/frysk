package frysk.gui.srcwin.prefs;

import java.util.HashMap;
import java.util.prefs.Preferences;

public class BooleanPreference extends FryskPreference {

	private static HashMap defaultValues;

	public static String[] NAMES = { "Show Toolbar", "Show Line Numbers",
			"Show Executable Marks" };

	static {
		defaultValues = new HashMap();

		// Whether to show the toolbar
		defaultValues.put(NAMES[0], new Boolean(false));
		defaultValues.put(NAMES[1], new Boolean(true));
		defaultValues.put(NAMES[2], new Boolean(true));
	}

	public static final int TOOLBAR = 0;
	public static final int LINE_NUMS = 1;
	public static final int EXEC_MARKS = 2;

	private static boolean getDefaultValue(String name) {
		return ((Boolean) defaultValues.get(name)).booleanValue();
	}

	private boolean value;

	protected BooleanPreference(String name) {
		this.name = name;
	}

	public BooleanPreference(int name) {
		this(NAMES[name]);
	}

	public boolean getCurrentValue() {
		return this.value;
	}

	public void setCurrentValue(boolean val) {
		value = val;
	}

	public void saveValues() {
		this.model.putBoolean(name, value);
	}

	protected void setModel(Preferences prefs) {
		this.model = prefs;
		this.value = this.model.getBoolean(name, getDefaultValue(name));
	}
}
