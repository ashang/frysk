package frysk.gui.srcwin.prefs;

import java.util.prefs.Preferences;

public abstract class FryskPreference {

	protected Preferences model;
	
	protected String name;
	
	public abstract void saveValues();
	
	protected abstract void setModel(Preferences prefs);
	
	public String getName(){
		return name;
	}
}
