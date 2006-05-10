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
 * The EventViewer widget is used for displaying events on multiple timelines.
 */
public class EventViewer extends Widget 
{

    static {
	System.loadLibrary ("ftk");
	System.loadLibrary ("ftkjni");
    }

    public EventViewer() {
	super(ftk_eventviewer_new());
    }
	
    /**
     * Construct an eventviewer using a handle to a native resource.
     */
    public EventViewer(Handle handle) {
	super(handle);
    }
    
    /**
     * Internal static factory method to be used by Java-Gnome only.
     */
    public static EventViewer getEventViewer(Handle handle) {
        if (handle == null) {
            return null;
        }
        
        EventViewer obj = (EventViewer) GObject.getGObjectFromHandle(handle);
        
        if (obj == null) {
            obj = new EventViewer(handle);
        }
        
        return obj;
    }

    /**
     * Retrieve the runtime type used by the GLib library.
     */
    public static Type getType() {
	return new Type(ftk_eventviewer_get_type());
    }

    /**
     * Set eventviewer size
     */
    /*    not yet implemented 
    public return resize(int width, int height) {
	return ftk_eventviewer_resize (getHandle(), width, height);
    }
    */

    /**
     * Set bg color
     */
    public boolean setBackgroundRGB(int red, int green, int blue) {
	return ftk_eventviewer_set_bg_rgb (getHandle(), red, green, blue);
    }

    /**
     * Set viewable window (seconds)
     */
    public boolean setTimebase(double span) {
	return ftk_eventviewer_set_timebase (getHandle(), span);
    }

    /**
     * Add new trace
     */
    public int addTrace(String label) {
	return ftk_eventviewer_add_trace (getHandle(), label);
    }

    /**
     * Set trace rgb
     */
    public boolean setTraceRGB(int trace, int red, int green, int blue) {
	return ftk_eventviewer_set_trace_rgb (getHandle(), trace,
					      red, green, blue);
    }

    /**
     * Set trace label
     */
    public boolean setTraceLabel(int trace, String label) {
	return ftk_eventviewer_set_trace_label (getHandle(), trace, label);
    }

    /**
     * Set trace linestyle
     */
    public boolean setTraceLinestyle(int trace, int width, int style) {
	return ftk_eventviewer_set_trace_linestyle (getHandle(), trace,
						    width, style);
    }

    /**
     * Add new marker
     */
    public int addMarker(int glyph, String label) {
	return ftk_eventviewer_marker_new (getHandle(), glyph, label);
    }

    /**
     * Set marker rgb
     */
    public boolean setMarkerRGB(int marker, int red, int green, int blue) {
	return ftk_eventviewer_set_marker_rgb (getHandle(), marker,
					       red, green, blue);
    }

    /**
     * Append event
     */
    public boolean  appendEvent(int trace, int marker) {
	return ftk_eventviewer_append_event (getHandle(), trace, marker);
    }


    native static final protected int
	ftk_eventviewer_get_type ();

    native static final protected Handle
	ftk_eventviewer_new ();

    /* not yet implemented 
    native static final protected boolean
        ftk_eventviewer_resize (Handle sc, int width, int height);
    */

    native static final protected boolean
	ftk_eventviewer_set_bg_rgb (Handle sc,
				    int red, int green, int blue);

    native static final protected boolean
	ftk_eventviewer_set_timebase (Handle sc, double span);

    native static final protected int
	ftk_eventviewer_add_trace (Handle sc, String label);

    native static final protected boolean
	ftk_eventviewer_set_trace_rgb (Handle sc, int trace,
				       int red, int green, int blue);

    native static final protected boolean
	ftk_eventviewer_set_trace_label (Handle sc, int trace,
					 String label);

    native static final protected boolean
	ftk_eventviewer_set_trace_linestyle (Handle sc, int trace,
					     int lw, int ls);

    native static final protected int
	ftk_eventviewer_marker_new (Handle sc, int glyph, String label);

    native static final protected boolean
	ftk_eventviewer_set_marker_rgb (Handle sc, int marker,
					int red, int green, int blue);
    
    native static final protected boolean
	ftk_eventviewer_append_event (Handle sc, int trace, int marker);

}

