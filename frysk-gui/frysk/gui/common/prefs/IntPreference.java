// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

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
