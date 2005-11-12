/*
 * Java-Gnome Bindings Library
 *
 * Copyright 1998-2004 the Java-Gnome Team, all rights reserved.
 *
 * The Java-Gnome bindings library is free software distributed under
 * the terms of the GNU Library General Public License version 2.
 */

//package org.gnu.gtk;
package com.redhat.ftk;

import org.gnu.glib.GObject;
import org.gnu.glib.Type;
import org.gnu.glib.Struct;
import org.gnu.glib.Handle;
import org.gnu.gtk.Widget;

/**
 * The Stripchart widget is used for creating custom user interface elements.
 * It's essentially a blank widget you can draw on.
 */
public class Stripchart extends Widget 
{

    static {
	System.loadLibrary ("ftk");
    }

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

    /**
     * Set stripchart size
     */
    public void resize(int width, int height) {
	ftk_stripchart_resize (getHandle(), width, height);
    }

    /**
     * Set an event color
     */
    public void setEventRGB(int type, int red, int green, int blue) {
	ftk_stripchart_set_event_rgb (getHandle(), type,
				      red, green, blue);
    }

    /**
     * Set an event title
     */
    public void setEventTitle(int type, String title) {
	ftk_stripchart_set_event_title (getHandle(), type, title);
    }

    /**
     * Set stripchart update interval
     */
    public void setUpdate(int update) {
	ftk_stripchart_set_update (getHandle(), update);
    }

    /**
     * Set stripchart range interval
     */
    public void setRange(int range) {
	ftk_stripchart_set_range (getHandle(), range);
    }

    /**
     * Set stripchart append event
     */
    public void appendEvent(int type) {
	ftk_stripchart_append_event (getHandle(), type);
    }


    native static final protected int
	ftk_stripchart_get_type ();
    native static final protected Handle
	ftk_stripchart_new ();
    native static final protected void
	ftk_stripchart_set_event_rgb (Handle sc,
				      int type,
				      int red, int green, int blue);
    native static final protected void
	ftk_stripchart_set_event_title(Handle sc,
				       int type,
				       String title);
    native static final protected void
	ftk_stripchart_resize (Handle sc, int width, int height);
    native static final protected void
	ftk_stripchart_set_update (Handle sc, int update);
    native static final protected void
	ftk_stripchart_set_range (Handle sc, int range);
    native static final protected void
	ftk_stripchart_append_event (Handle sc, int type);

}

