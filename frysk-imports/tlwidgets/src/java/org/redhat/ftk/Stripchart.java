/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */

package com.redhat.ftk;

import org.gnu.glib.GObject;
import org.gnu.glib.Type;
import org.gnu.glib.Handle;

/**
 * The Stripchart widget is used for creating custom user interface elements.
 * It's essentially a blank widget you can draw on.
 */
public class Stripchart extends Widget 
{
	public Stripchart() {
		super(ftk_stripchart_new());
	}
	
	/**
	 * Construct a Stripchart using a handle to a native resource.
	 */
	public Stripchart(Handle handle) {
	    super(handle);
	}
    
    /**
     * Internal static factory method to be used by Java-Gnome only.
     */
    public static Stripchart getStripchart(Handle handle) {
        if (handle == null) {
            return null;
        }
        
        Stripchart obj = (Stripchart) GObject.getGObjectFromHandle(handle);
        
        if (obj == null) {
            obj = new Stripchart(handle);
        }
        
        return obj;
    }

	/**
	 * Retrieve the runtime type used by the GLib library.
	 */
	public static Type getType() {
		return new Type(ftk_stripchart_get_type());
	}


    native static final protected int ftk_stripchart_get_type ();
    native static final protected Handle ftk_stripchart_new ();

}

