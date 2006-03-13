package frysk.gui.common.prefs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

import frysk.gui.monitor.Saveable;

/**
 * A preference group is a collection of preferences that all
 * belong to the same category.
 *
 */
public class PreferenceGroup implements Saveable{
	private HashMap prefs;
	private HashMap subgroups;
	
	private String name;
	private int tabNum;
	
	/**
	 * Create a new Preference Group.
	 * @param name The name of the group.
	 */
	public PreferenceGroup(String name, int tabNum){
		this.name = name;
		this.tabNum = tabNum;
		this.prefs = new HashMap();
		this.subgroups = new HashMap();
	}
	
	/**
	 * @return An iterator to all the preferences in this group
	 */
	public Iterator getPreferences(){
		return prefs.values().iterator();
	}
	
	/**
	 * 
	 * @param name The name of the preference to look up
	 * @return The Preference, or null if no preference is found
	 */
	public FryskPreference getPreference(String name){
		return (FryskPreference) this.prefs.get(name);
	}
	
	/**
	 * 
	 * @return The name of this group
	 */
	public String getName(){
		return name;
	}

	/**
	 * Adds a new preference to this group
	 * @param preference The preference to add
	 */
	public void addPreference(FryskPreference preference){
		this.prefs.put(preference.getName(), preference);
	}
	
	/**
	 * Removes the preference from the group. If the preference is
	 * not in this group no action is performed
	 * @param preference The preference to remvoe
	 */
	public void removePreference(FryskPreference preference){
		if(this.prefs.containsValue(preference))
			this.prefs.remove(preference.getName());
	}
	
	/**
	 * Same as {@see #removePreference(FryskPreference)}, except
	 * removes the preference by name
	 * @param prefName The name of the preference to remove
	 */
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
	
	/**
	 * Saves the values of the preferences into the preference model.
	 * This propagates down through all Preferences in the group.
	 */
	public void save(Preferences prefs) {
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).save(prefs);
		
		// subgroups
		it = this.subgroups.values().iterator();
		
		while(it.hasNext())
			((PreferenceGroup) it.next()).save(prefs);
	}

	/**
	 * Replaces the current value of all preferences in the group
	 * with the values in the underlying preferences model.
	 */
	public void load(Preferences prefs) {
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).load(prefs);
		
		// subgroups
		it = this.subgroups.values().iterator();
		
		while(it.hasNext())
			((PreferenceGroup) it.next()).load(prefs);
	}
	
	/**
	 * Checks to see if a preference is in this group
	 * @param preference The preference to look for
	 * @return True iff the preference is in the group.
	 */
	public boolean contains(FryskPreference preference){
		return this.prefs.values().contains(preference);
	}
	
	/**
	 * Reverts all the preferences in this group back to the
	 * values in the model
	 *
	 */
	public void revertAll(){
		Iterator it = this.prefs.values().iterator();
		
		while(it.hasNext())
			((FryskPreference) it.next()).revert();
		
		// subgroups
		it = this.subgroups.values().iterator();
		
		while(it.hasNext())
			((PreferenceGroup) it.next()).revertAll();
	}
	
	/**
	 * Adds a new subgroup to this group
	 * @param subgroup The subgroup
	 */
	public void addSubgroup(PreferenceGroup subgroup){
		this.subgroups.put(subgroup.getName(), subgroup);
	}
	
	/**
	 * 
	 * @param name The name of the subgroup to fetch
	 * @return The subgroup, or null if no such subgroup exists
	 */
	public PreferenceGroup getSubgroup(String name){
		return (PreferenceGroup) this.subgroups.get(name);
	}
	
	/**
	 * 
	 * @return An iterator to all subgroups
	 */
	public Iterator getSubgroups(){
		return this.subgroups.values().iterator();
	}
	
	/**
	 * Removes the subgroup with the given name
	 * @param name
	 */
	public void removeSubgroup(String name){
		if(this.subgroups.containsKey(name))
			this.subgroups.remove(name);
	}
	
	/**
	 * Removes the provided subgroup.
	 * @param group The subgroup to remove
	 */
	public void removeSubgroup(PreferenceGroup group){
		this.removeSubgroup(group.getName());
	}

	/**
	 *
	 * @return The preference window tab number corresponding to this preference
	 */
	public int getTabNum() {
		return tabNum;
	}
}
