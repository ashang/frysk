package frysk.gui.srcwin.prefs;

import java.util.HashMap;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;

public class PreferenceManager {
	private static HashMap preferences;

	private static Preferences prefs;

	static {
		preferences = new HashMap();
		prefs = Preferences.userRoot();

		/*
		 * initialize preferences that don't associate with the buffer here.
		 * We have to do this because these will be needed before the buffer
		 * is created.
		 */
		// Show Toolbar?
		preferences.put(BooleanPreference.NAMES[0],
				new BooleanPreference(BooleanPreference.NAMES[0]));
	}

	public static void addPreference(FryskPreference preference, String node) {
		if(preferences.containsKey(preference.getName()))
			return;
		
		preference.setModel(prefs.node(node));
		preferences.put(preference.getName(), preference);
	}
	
	public static boolean getBooleanPreference(int name){
		return ((BooleanPreference) getPreference(BooleanPreference.NAMES[name])).getCurrentValue();
	}
	
	public static Color getColorPreference(int name){
		return ((ColorPreference) getPreference(ColorPreference.NAMES[name])).getCurrentColor();
	}
	
	public static SyntaxPreference getSyntaxPreference(int name){
		return (SyntaxPreference) getPreference(SyntaxPreference.NAMES[name]);
	}
	
	public static int getIntPreference(int name){
		return ((IntPreference) getPreference(IntPreference.NAMES[name])).getCurrentValue();
	}
	
	public static FryskPreference getPreference(String name){
		return (FryskPreference) preferences.get(name);
	}

	/**
	 * Whether or not to show the toolbar
	 */
	public static final String LNF_NODE = "lnf";

	public static final String SYNTAX_NODE = "syntax";
}
