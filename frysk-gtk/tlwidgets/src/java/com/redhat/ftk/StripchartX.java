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
public class StripchartX extends Widget 
{

    static {
	System.loadLibrary ("ftk");
	System.loadLibrary ("ftkjni");
    }

    public StripchartX() {
	super(ftk_stripchartx_new());
    }
	
    /**
     * Construct a Stripchart using a handle to a native resource.
     */
    public StripchartX(Handle handle) {
	super(handle);
    }
    
    /**
     * Internal static factory method to be used by Java-Gnome only.
     */
    public static StripchartX getStripchartX(Handle handle) {
        if (handle == null) {
            return null;
        }
        
        StripchartX obj = (StripchartX) GObject.getGObjectFromHandle(handle);
        
        if (obj == null) {
            obj = new StripchartX(handle);
        }
        
        return obj;
    }

    /**
     * Retrieve the runtime type used by the GLib library.
     */
    public static Type getType() {
	return new Type(ftk_stripchartx_get_type());
    }

    /**
     * Set stripchart size
     */
    public void resize(int width, int height) {
	ftk_stripchartx_resize (getHandle(), width, height);
    }

    /**
     * Set bg color
     */
    public void setBackgroundRGB(int red, int green, int blue) {
	ftk_stripchartx_set_bg_rgb (getHandle(), red, green, blue);
    }

    /**
     * Set readout color
     */
    public void setReadoutRGB(int red, int green, int blue) {
	ftk_stripchartx_set_readout_rgb (getHandle(), red, green, blue);
    }

    /**
     * Set chart color
     */
    public void setChartRGB(int red, int green, int blue) {
	ftk_stripchartx_set_chart_rgb (getHandle(), red, green, blue);
    }

    /**
     * Set an event color
     */
    /********** removed pro-tem *************
    public void setEventRGB(int type, int red, int green, int blue) {
	ftk_stripchartx_set_event_rgb (getHandle(), type,
				      red, green, blue);
    }
    **************************************/

    /**
     * Set an event title
     */
    /********** removed pro-tem *************
    public void setEventTitle(int type, String title) {
	ftk_stripchartx_set_event_title (getHandle(), type, title);
    }
    **************************************/

    /**
     * Create new event
     */
    public int createEvent(String title, int red, int green, int blue) {
	return ftk_stripchartx_new_event (getHandle(), title, red, green, blue);
    }

    /**
     * Set stripchart update interval
     */
    public void setUpdate(int update) {
	ftk_stripchartx_set_update (getHandle(), update);
    }

    /**
     * Set stripchart range interval
     */
    public void setRange(int range) {
	ftk_stripchartx_set_range (getHandle(), range);
    }

    /**
     * Set stripchart append event
     */
    public void appendEvent(int type) {
	ftk_stripchartx_append_event (getHandle(), type);
    }


    native static final protected int
	ftk_stripchartx_get_type ();
    native static final protected Handle
	ftk_stripchartx_new ();
    /********** removed pro-tem *************
    native static final protected void
	ftk_stripchartx_set_event_rgb (Handle sc,
				      int type,
				      int red, int green, int blue);
    native static final protected void
	ftk_stripchartx_set_event_title(Handle sc,
				       int type,
				       String title);
    **************************************/
    native static final protected int
	ftk_stripchartx_new_event (Handle sc, String title,
				  int red, int green, int blue);
    native static final protected void
	ftk_stripchartx_resize (Handle sc, int width, int height);
    native static final protected void
	ftk_stripchartx_set_bg_rgb (Handle sc,
				   int red, int green, int blue);
    native static final protected void
	ftk_stripchartx_set_readout_rgb (Handle sc,
					int red, int green, int blue);
    native static final protected void
	ftk_stripchartx_set_chart_rgb (Handle sc,
				      int red, int green, int blue);
    native static final protected void
	ftk_stripchartx_set_update (Handle sc, int update);
    native static final protected void
	ftk_stripchartx_set_range (Handle sc, int range);
    native static final protected void
	ftk_stripchartx_append_event (Handle sc, int type);

}

