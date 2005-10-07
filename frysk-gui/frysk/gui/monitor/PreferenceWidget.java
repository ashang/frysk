/*
 * Created on Oct 6, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import org.gnu.gtk.Frame;


/**
 * Generic Preference widget, which provides a base class for preference pages
 * that can be added to the preference window. New preference widgets should inherent
 * from this and add themselves the the Preference Window.
 * */
public class PreferenceWidget extends Frame{
	
	public PreferenceWidget(String name) {
		super(name);
		this.showAll();
	}
	
}
