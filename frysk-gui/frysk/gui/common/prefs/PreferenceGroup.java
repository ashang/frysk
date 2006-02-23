package frysk.gui.common.prefs;

import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import frysk.gui.monitor.Saveable;

public class PreferenceGroup implements Saveable{
	private Vector prefs;
	private String name;
	
	public PreferenceGroup(String name){
		this.name = name;
		this.prefs = new Vector();
	}
	
	public Iterator getPreferences(){
		return prefs.iterator();
	}
	
	public String getName(){
		return name;
	}

	public void addPreference(FryskPreference preference){
		this.prefs.add(preference);
	}
	
	public void removePreference(FryskPreference preference){
		this.prefs.remove(preference);
	}
	
	public void removePreference(String prefName){
		Iterator it = this.prefs.iterator();
		
		while(it.hasNext()){
			FryskPreference pref = (FryskPreference) it.next();
			if(pref.getName().equals(prefName)){
				this.prefs.remove(pref);
				break;
			}
		}
	}
	
	public void save(Preferences prefs) {
		Iterator it = this.prefs.iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).save(prefs);
	}

	public void load(Preferences prefs) {
		Iterator it = this.prefs.iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).load(prefs);
	}
}
