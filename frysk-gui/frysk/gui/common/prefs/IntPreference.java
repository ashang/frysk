package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

public class IntPreference extends FryskPreference {

	public interface IntPreferenceListener{
		void preferenceChanged(String prefName, int newValue);
	}
	
    protected int currentValue;
	protected int min;
	protected int max;
	protected int fallback;
	
	protected Vector listeners;
	
	public IntPreference(String name, int min, int max, int fallback){
		this.name = name;
		this.min = min;
		this.max = max;
		this.fallback = fallback;
		this.listeners = new Vector();
	}
	
	public int getCurrentValue(){
		return this.currentValue;
	}
    
	public int getMinValue(){
		return this.min;
	}
	
	public int getMaxValue(){
		return this.max;
	}
	
    public void setCurrentValue(int newVal){
        this.currentValue = newVal;
    }

	public void save(Preferences prefs) {
		this.model.putInt(this.name, this.currentValue);
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((IntPreferenceListener) it.next()).preferenceChanged(this.name, this.currentValue);
	}

	public void load(Preferences prefs) {
		this.model = prefs;
		
		this.revert();
	}
	
	public void addListener(IntPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.currentValue);
	}

	public void revert() {
		this.currentValue = model.getInt(this.name, fallback);
		
		if(this.currentValue < this.min)
			this.currentValue = this.min;
		if(this.currentValue > this.max)
			this.currentValue = this.max;
	}

}
