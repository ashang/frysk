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

package frysk.gui.srcwin.prefs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.common.prefs.ColorPreference;

public class SyntaxPreference extends ColorPreference {

	public interface SyntaxPreferenceListener{
		void preferenceChanged(String name, Color newColor, Weight newWeight, Style newStyle);
	}

	protected Weight currentWeight;
	protected Style currentStyle;
	
	protected Weight defaultWeight;
	protected Style defaultStyle;

	protected LinkedList syntaxListeners;
	
	public SyntaxPreference(String name, Color fallback, Weight defaultWeight, Style defaultStyle) {
		super(name, fallback);
		
		this.defaultStyle = defaultStyle;
		this.defaultWeight = defaultWeight;
		this.syntaxListeners = new LinkedList();
	}

	public Style getCurrentStyle() {
		return currentStyle;
	}

	public void toggleItalics() {
		if (this.currentStyle.equals(Style.ITALIC))
			this.currentStyle = Style.NORMAL;
		else
			this.currentStyle = Style.ITALIC;
	}

	public Weight getCurrentWeight() {
		return currentWeight;
	}

	public void toggleBold() {
		if (this.currentWeight.equals(Weight.BOLD))
			this.currentWeight = Weight.NORMAL;
		else
			this.currentWeight = Weight.BOLD;
	}

	public void save(Preferences prefs) {
		super.save(prefs);

		this.model.putInt(name + "_weight", this.currentWeight.getValue());
		this.model.putInt(name + "_style", this.currentStyle.getValue());
		
		Iterator it = this.syntaxListeners.iterator();
		while(it.hasNext())
			((SyntaxPreferenceListener) it.next()).preferenceChanged(name, currentColor, currentWeight, currentStyle);
	}
	
	public void addListener(SyntaxPreferenceListener listener){
		this.syntaxListeners.add(listener);
		listener.preferenceChanged(this.getName(), this.currentColor, this.currentWeight, this.currentStyle);
	}
	
	public void revert(){
		super.revert();
		this.currentWeight = Weight.intern(this.model.getInt(name + "_weight",
				this.defaultWeight.getValue()));

		this.currentStyle = Style.intern(this.model.getInt(name + "_style",
				this.defaultStyle.getValue()));
	}
}
