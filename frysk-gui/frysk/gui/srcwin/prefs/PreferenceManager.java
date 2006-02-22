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
		addPreference(new BooleanPreference(BooleanPreference.NAMES[0]), PreferenceManager.LNF_NODE);
	}

	public static void addPreference(FryskPreference preference, String node) {
		if(preferences.containsKey(preference.getName()))
			if(preference instanceof ColorPreference)
				getColorPreference(preference.name).appendTags((ColorPreference) preference);
			else
				return;
		
		preference.setModel(prefs.node(node));
		preferences.put(preference.getName(), preference);
	}
	
	public static boolean getBooleanPreferenceValue(int name){
		return ((BooleanPreference) getPreference(BooleanPreference.NAMES[name])).getCurrentValue();
	}
	
    public static BooleanPreference getBooleanPreference(int name){
        return (BooleanPreference) getPreference(BooleanPreference.NAMES[name]);
    }
    
	public static Color getColorPreferenceValue(int name){
		return ((ColorPreference) getPreference(ColorPreference.NAMES[name])).getCurrentColor();
	}
	
    public static ColorPreference getColorPreference(int name){
        return (ColorPreference) getPreference(ColorPreference.NAMES[name]);
    }
    
    private static ColorPreference getColorPreference(String name){
    	return (ColorPreference) getPreference(name);
    }
    
	public static SyntaxPreference getSyntaxPreference(int name){
		return (SyntaxPreference) getPreference(SyntaxPreference.NAMES[name]);
	}
	
	public static int getIntPreferenceValue(int name){
		return ((IntPreference) getPreference(IntPreference.NAMES[name])).getCurrentValue();
	}
	
    public static IntPreference getIntPreference(int name){
        return (IntPreference) getPreference(IntPreference.NAMES[name]);
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
