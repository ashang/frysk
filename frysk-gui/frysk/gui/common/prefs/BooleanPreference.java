package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

/**
 * BoleanPreference models a boolean-valued preference within Frysk
 *
 */
public class BooleanPreference extends FryskPreference {

	public interface BooleanPreferenceListener{
		void preferenceChanged(String prefName, boolean newValue);
	}

    protected boolean value;
	protected boolean fallback;
	
	protected Vector listeners;
	
	/**
	 * Creates a new BooleanPreference
	 * @param name The name of the preference
	 * @param fallback The default value of the preference
	 */
	public BooleanPreference(String name, boolean fallback) {
		this.name = name;
		this.fallback = fallback;
		this.listeners = new Vector();
	}

	/**
	 * 
	 * @return The current value of the preference
	 */
	public boolean getCurrentValue() {
		return this.value;
	}

	/**
	 * Sets the value of the preference. Note that this is not saved
	 * in the preferences model permanently until {@see #save(Preferences)}
	 * is called.
	 * @param val The new value
	 */
	public void setCurrentValue(boolean val) {
		value = val;
	}

	/**
	 * Saves the value into the model and notifies the attached
	 * listeners.
	 */
	public void save(Preferences prefs) {
		this.model.putBoolean(name, value);
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((BooleanPreferenceListener) it.next()).preferenceChanged(this.name, this.value);
	}

	/**
	 * Sets the preference to use the provided model and loads the value from it
	 */
	public void load(Preferences prefs) {
		this.model = prefs;
		this.revert();
	}
	
	/**
	 * Adds a listener that will be notified whenever the value
	 * of the preference is changed
	 * @param listener The object to notify
	 */
	public void addListener(BooleanPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.value);
	}

	/**
	 * Restores the preference from the value in the model.
	 */
	public void revert() {
		this.value = this.model.getBoolean(name, this.fallback);
	}
}
