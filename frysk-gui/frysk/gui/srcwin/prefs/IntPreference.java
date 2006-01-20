package frysk.gui.srcwin.prefs;

import java.util.HashMap;
import java.util.prefs.Preferences;

public class IntPreference extends FryskPreference {

	protected static HashMap defaultValues;
	
	public static final String[] NAMES = {"Visible Levels of Inline Code"};
	
    public static final int INLINE_LEVELS = 0;
    
	static{
		defaultValues = new HashMap();
		
		defaultValues.put(NAMES[INLINE_LEVELS], new Integer(2));
	}
	
	public static int getDefaultValue(String name){
		return ((Integer) defaultValues.get(name)).intValue();
	}
	
	protected int currentValue;
	
	protected IntPreference(String name){
		this.name = name;
	}
	
	public IntPreference(int name){
		this(NAMES[name]);
	}
	
	public int getCurrentValue(){
		return this.currentValue;
	}
    
    public void setCurrentValue(int newVal){
        this.currentValue = newVal;
    }
	
	public void saveValues() {
		this.model.putInt(this.name, this.currentValue);
	}

	protected void setModel(Preferences prefs) {
		this.model = prefs;
		
		this.currentValue = model.getInt(this.name, getDefaultValue(this.name));
	}

}
