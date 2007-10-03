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

import org.gnu.gdk.Color;

/**
 * ColorPreference models a color-values preference for Frysk
 * 
 */
public class ColorPreference extends FryskPreference {

	public interface ColorPreferenceListener{
		void preferenceChanged(String prefName, Color newColor);
	}

    protected Color currentColor;
	protected Color fallback;
	
	protected Vector listeners;
	
	/**
	 * Creates a new ColorPreference
	 * @param name The name of the preference
	 * @param fallback The default color to use
	 */
	public ColorPreference(String name, Color fallback) {
		this.name = name;
		this.fallback = fallback;
		this.listeners = new Vector();
	}
	
	/**
	 * Sets the current color for this preference. Note that this is
	 * not saved into the model until {@see #save(Preferences)} is called.
	 * @param currentColor The new color
	 */
	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}

	/**
	 * 
	 * @return The current value of this preference
	 */
	public Color getCurrentColor() {
		return this.currentColor;
	}

	/**
	 * Saves the value of this preference into the preference model and
	 * notify all attached listeners
	 */
	public void save(Preferences prefs) {
		this.model.putInt(name + "_R", this.currentColor.getRed());
		this.model.putInt(name + "_G", this.currentColor.getGreen());
		this.model.putInt(name + "_B", this.currentColor.getBlue());
		
		Iterator it = this.listeners.iterator();
		while(it.hasNext())
			((ColorPreferenceListener) it.next()).preferenceChanged(this.name, this.currentColor);
	}

	/**
	 * Sets the preference to use the provided model and loads the values from
	 * it.
	 */
	public void load(Preferences prefs) {
		this.model = prefs;
		
		this.revert();
	}

	/**
	 * Adds a listener to this preference that will get notified whenever
	 * the value of this preference changes
	 * @param listener The object to notify when the preference changes.
	 */
	public void addListener(ColorPreferenceListener listener){
		this.listeners.add(listener);
		listener.preferenceChanged(this.name, this.currentColor);
	}

	/**
	 * Restores the current value of this preference from the model.
	 */
	public void revert() {
		int r = model.getInt(name + "_R", fallback.getRed());
		int g = model.getInt(name + "_G", fallback.getGreen());
		int b = model.getInt(name + "_B", fallback.getBlue());
		this.currentColor = new Color(r, g, b);
	}
}
