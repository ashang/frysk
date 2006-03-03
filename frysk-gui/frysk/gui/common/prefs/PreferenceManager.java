package frysk.gui.common.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

public class PreferenceManager {
	
	private static HashMap preferenceGroups;
	
	private static Preferences prefs;

	// Static preference groups
	public static PreferenceGroup sourceWinGroup = new PreferenceGroup("Source Window");
	public static PreferenceGroup syntaxHighlightingGroup = new PreferenceGroup("Syntax Highlighting");
	
	static {
		preferenceGroups = new HashMap();
		
		prefs = Preferences.userRoot();
	}
	
	public static void addPreferenceGroup(PreferenceGroup newGroup){
		preferenceGroups.put(newGroup.getName(), newGroup);
		newGroup.load(prefs);
	}

	public static Iterator getPreferenceGroups(){
		return preferenceGroups.values().iterator();
	}
	
	public static void setPreferenceModel(Preferences newModel){
		prefs = newModel;
		Iterator it = getPreferenceGroups();
		while(it.hasNext())
			((PreferenceGroup) it.next()).load(newModel);
	}
	
	public static void saveAll(){
		Iterator it = getPreferenceGroups();
		while(it.hasNext())
			((PreferenceGroup) it.next()).save(prefs);
	}
	
}
