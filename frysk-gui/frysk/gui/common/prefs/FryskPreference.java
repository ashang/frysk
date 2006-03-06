package frysk.gui.common.prefs;

import java.util.prefs.Preferences;

import frysk.gui.monitor.Saveable;

/**
 * FryskPreference represents a user-settable preference
 *
 */
public abstract class FryskPreference implements Saveable{

	protected Preferences model;
	
	protected String name;
	
	public String getName(){
		return name;
	}
	
	public abstract void revert();
}
