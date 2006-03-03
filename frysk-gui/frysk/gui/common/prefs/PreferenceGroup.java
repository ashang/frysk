package frysk.gui.common.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

import frysk.gui.monitor.Saveable;

public class PreferenceGroup implements Saveable{
	private HashMap prefs;
	private String name;
	
	public PreferenceGroup(String name){
		this.name = name;
		this.prefs = new HashMap();
	}
	
	public Iterator getPreferences(){
		return prefs.values().iterator();
	}
	
	public FryskPreference getPreference(String name){
		return (FryskPreference) this.prefs.get(name);
	}
	
	public String getName(){
		return name;
	}

	public void addPreference(FryskPreference preference){
		this.prefs.put(preference.getName(), preference);
	}
	
	public void removePreference(FryskPreference preference){
		this.prefs.remove(preference.getName());
	}
	
	public void removePreference(String prefName){
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext()){
			FryskPreference pref = (FryskPreference) it.next();
			if(pref.getName().equals(prefName)){
				this.prefs.remove(pref);
				break;
			}
		}
	}
	
	public void save(Preferences prefs) {
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).save(prefs);
	}

	public void load(Preferences prefs) {
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).load(prefs);
	}
	
	public boolean contains(FryskPreference preference){
		return this.prefs.values().contains(preference);
	}
}
