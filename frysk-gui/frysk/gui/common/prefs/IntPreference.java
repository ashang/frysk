package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

/**
 * IntPreference represents a preference that takes integer values within a
 * contiguous range.
 *
 */
public class IntPreference extends FryskPreference {

	public interface IntPreferenceListener{
		void preferenceChanged(String prefName, int newValue);
	}
	
    protected int currentValue;
	protected int min;
	protected int max;
	protected int fallback;
	
	protected Vector listeners;

	/**
	 * Creates a new IntPreference. Note that if the default value is outside
	 * the bounds set by min and max it will be set to the closest bound.
	 * @param name The name of the preference
	 * @param min The minimum value
	 * @param max The maximum value
	 * @param fallback The default value.
	 */
	public IntPreference(String name, int min, int max, int fallback){
		this.name = name;
		this.min = min;
		this.max = max;
		this.fallback = fallback;
		this.listeners = new Vector();
	}
	
	/**
	 * 
	 * @return The current value of the preference
	 */
	public int getCurrentValue(){
		return this.currentValue;
	}
    
	/**
	 * 
	 * @return The minimum value of the preference
	 */
	public int getMinValue(){
		return this.min;
	}
	
	/**
	 * 
	 * @return The maximum value of the preference
	 */
	public int getMaxValue(){
		return this.max;
	}
	
	/**
	 * Sets the value of this preference. Note that if this value is 
	 * outside of the min,max for this preference it will be set to the
	 * closest bound
	 * @param newVal The new value for this preference.
	 */
    public void setCurrentValue(int newVal){
        this.currentValue = newVal;
    }

    /**
     * Saves the current value of this preference into the model and
     * notifies all listeners
     */
	public void save(Preferences prefs) {
		this.model.putInt(this.name, this.currentValue);
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((IntPreferenceListener) it.next()).preferenceChanged(this.name, this.currentValue);
	}

	/**
	 * Loads the value of this preference from the provided model
	 */
	public void load(Preferences prefs) {
		this.model = prefs;
		
		this.revert();
	}
	
	/**
	 * Adds a new listener to this preference. The listener will be notified whenever
	 * this preference saves information into the preference model.
	 * @param listener The IntPreferenceListener
	 */
	public void addListener(IntPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.currentValue);
	}

	/**
	 * Replaces the current value of the preference with the one in the model
	 */
	public void revert() {
		this.currentValue = model.getInt(this.name, fallback);
		
		if(this.currentValue < this.min)
			this.currentValue = this.min;
		if(this.currentValue > this.max)
			this.currentValue = this.max;
	}

}
