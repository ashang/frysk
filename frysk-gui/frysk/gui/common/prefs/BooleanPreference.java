package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

public class BooleanPreference extends FryskPreference {

	public interface BooleanPreferenceListener{
		void preferenceChanged(String prefName, boolean newValue);
	}

    protected boolean value;
	protected boolean fallback;
	
	protected Vector listeners;
	
	public BooleanPreference(String name, boolean fallback) {
		this.name = name;
		this.fallback = fallback;
		this.listeners = new Vector();
	}

	public boolean getCurrentValue() {
		return this.value;
	}

	public void setCurrentValue(boolean val) {
		value = val;
	}

	public void save(Preferences prefs) {
		this.model.putBoolean(name, value);
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((BooleanPreferenceListener) it.next()).preferenceChanged(this.name, this.value);
	}

	public void load(Preferences prefs) {
		this.model = prefs;
		this.revert();
	}
	
	public void addListener(BooleanPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.value);
	}

	public void revert() {
		this.value = this.model.getBoolean(name, this.fallback);
	}
}
