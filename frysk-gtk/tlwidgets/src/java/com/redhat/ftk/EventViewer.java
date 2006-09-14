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

import org.gnu.gdk.Color;
import org.gnu.glib.GObject;
import org.gnu.glib.Type;
import org.gnu.glib.Struct;
import org.gnu.glib.Handle;
import org.gnu.gtk.VBox;

/**
 * The EventViewer widget is used for displaying events on multiple timelines.
 */
public class EventViewer extends VBox 
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
    public boolean resize(int width, int height) {
	return ftk_eventviewer_resize (getHandle(), width, height);
    }

    /**
     * Set bg color
     */
    public boolean setBackgroundRGB(int red, int green, int blue) {
	return ftk_eventviewer_set_bg_rgb (getHandle(), red, green, blue);
    }

    /**
     * Set the background color using a Color
     */
    public boolean setBackgroundColor(Color color) {
    return ftk_eventviewer_set_bg_color (getHandle(), color.getHandle());
    }
    
    /*
     * Set the background color to the default theme color for the background.
     */
    public boolean setBackgroundDefault() {
      return ftk_eventviewer_set_bg_default(getHandle());
    }
    
    /*
     * Get the default background colors.
     */
    public Color[] getBackgroundDefault() {
      return ftk_eventviewer_get_bg_default(getHandle());
    }
    
    public Color[] getForegroundDefault() {
      return ftk_eventviewer_get_fg_default(getHandle());
    }
    
    /*
    * Set show grid
    */
    public boolean setShowGrid(boolean sg) {
	    return ftk_eventviewer_set_show_grid (getHandle(), sg);
    }
    
    /*
    * Is the grid showing?
    */
    public boolean isShowGrid() {
	    return ftk_eventviewer_is_show_grid(getHandle());
    }
    
    /*
    * Set the grid size.
    */
    public boolean setGridSize(double gs) {
	    return ftk_eventviewer_set_grid_size(getHandle(), gs);
    }
    
    /*
    * Get the grid size.
    */
    public double getGridSize() {
	    return ftk_eventviewer_get_grid_size(getHandle());
    }
    
    /*
     *  Set the grid color.
     */
    public boolean setGridColor(Color col) {
	    return ftk_eventviewer_set_grid_color(getHandle(), col.getHandle());
    }
    
    /*
     * Get the grid color.
     */
    public Color getGridColor() {
	    return ftk_eventviewer_get_grid_color(getHandle());
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
    public int addTrace(String label, String desc) {
	return ftk_eventviewer_add_trace (getHandle(), label, desc);
    }
    
    /*
     * Remove trace
     */
    public boolean deleteTrace(int trace) {
      return ftk_eventviewer_delete_trace (getHandle(), trace);
    }
    
    /*
     * Get selected traces
     */
    public int[] getSelectedTraces() {
      return ftk_eventviewer_get_selected_traces (getHandle());
    }
    /**
     * Set trace rgb
     */
    public boolean setTraceRGB(int trace, int red, int green, int blue) {
	return ftk_eventviewer_set_trace_rgb (getHandle(), trace,
					      red, green, blue);
    }
    
    /**
     * Set trace using a color.
     */
    public boolean setTraceColor(int trace, Color color) {
    return ftk_eventviewer_set_trace_color(getHandle(), trace,
					   color.getHandle());
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
     * Add a new marker.
     * @param glyph the symbol for this marker
     * @param label the label for this marker.
     * @param desc A description for this marker.
     * @return
     */
    public int addMarker(int glyph, String label, String desc) {
	return ftk_eventviewer_marker_new (getHandle(), glyph, label, desc);
    }

    /**
     * Set marker rgb
     */
    public boolean setMarkerRGB(int marker, int red, int green, int blue) {
	return ftk_eventviewer_set_marker_rgb (getHandle(), marker,
					       red, green, blue);
    }

    /**
     * Set the marker using a Color
     */
    public boolean setMarkerColor(int marker, Color color) {
    	return ftk_eventviewer_set_marker_color(getHandle(), marker, 
    			color.getHandle());
    }
    
    /**
    * Set the marker symbol size
    */
    public boolean setMarkerSymbolSize(int marker, int symbol_size) {
	    return ftk_eventviewer_set_marker_symbol_size(getHandle(), marker, symbol_size);
    }
    
    /**
     * Append event
     */
    public boolean  appendEvent(int trace, int marker, String desc) {
	return ftk_eventviewer_append_event (getHandle(), trace, marker, desc);
    }

    
    /**
     * Add a tie
     */
    public int appendTie() {
      return ftk_eventviewer_tie_new(getHandle());
    }
    
    /**
     * Set tie color
     */
    public boolean setTieRGB(int tie, int red, int green, int blue) {
      return ftk_eventviewer_set_tie_rgb(getHandle(), tie, red, green, blue);
    }
    
    /**
     * Set tie color
     */
    public boolean setTieColor(int tie, Color color) {
      return ftk_eventviewer_set_tie_color(getHandle(), tie,
					   color.getHandle());
    }
    
    /**
     * Set tie linestyle
     */
    public boolean setTieLinestyle(int tie, int width, int style) {
    return ftk_eventviewer_set_tie_linestyle (getHandle(), tie,
                            width, style);
    }
    
    /**
     * Append simultaneous events.
     */
    public boolean appendSimultaneousEvents(int tie,
    SimultaneousEvent[] events) {
      return ftk_eventviewer_append_simultaneous_events_array(getHandle(),
      tie, events.length, events);
    }

    native static final protected int
	ftk_eventviewer_get_type ();

    native static final protected Handle
	ftk_eventviewer_new ();

    native static final protected boolean
        ftk_eventviewer_resize (Handle sc, int width, int height);
    

    native static final protected boolean
	ftk_eventviewer_set_bg_rgb (Handle sc,
				    int red, int green, int blue);
    
    native static final protected boolean
    ftk_eventviewer_set_bg_color (Handle sc, Handle color);

    native static final protected boolean
    ftk_eventviewer_set_bg_default (Handle sc);
      
    native static final protected Color[]
    ftk_eventviewer_get_bg_default(Handle sc);
    
    native static final protected Color[]
    ftk_eventviewer_get_fg_default(Handle sc);
    
    native static final protected boolean
	    ftk_eventviewer_set_show_grid(Handle sc, boolean sg);
    
    native static final protected boolean
	    ftk_eventviewer_is_show_grid(Handle sc);
    
    native static final protected boolean
	    ftk_eventviewer_set_grid_size(Handle sc, double gs);
    
    native static final protected double
	    ftk_eventviewer_get_grid_size(Handle sc);
    
    native static final protected boolean
	    ftk_eventviewer_set_grid_color(Handle sc, Handle col);
    
    native static final protected Color
	    ftk_eventviewer_get_grid_color(Handle sc);
    
    native static final protected boolean
	ftk_eventviewer_set_timebase (Handle sc, double span);

    native static final protected int
	ftk_eventviewer_add_trace (Handle sc, String label, String desc);

    native static final protected boolean
	ftk_eventviewer_set_trace_rgb (Handle sc, int trace,
				       int red, int green, int blue);
    
    native static final protected boolean
    ftk_eventviewer_set_trace_color (Handle sc, int trace, Handle color);

    native static final protected boolean
	ftk_eventviewer_set_trace_label (Handle sc, int trace,
					 String label);

    native static final protected boolean
	ftk_eventviewer_set_trace_linestyle (Handle sc, int trace,
					     int lw, int ls);

    native static final protected int
	ftk_eventviewer_marker_new (Handle sc, int glyph, String label,
				    String desc);

    native static final protected boolean
	ftk_eventviewer_set_marker_rgb (Handle sc, int marker,
					int red, int green, int blue);
    
    native static final protected boolean
    ftk_eventviewer_set_marker_color (Handle sc, int marker, Handle color);
    
    native static final protected boolean
    ftk_eventviewer_set_marker_symbol_size (Handle sc, int marker, int sym_size);
    
    native static final protected boolean
	ftk_eventviewer_append_event (Handle sc, int trace, int marker,
				      String desc);
    
    native static final protected int
    ftk_eventviewer_tie_new (Handle sc);
    
    native static final protected boolean
    ftk_eventviewer_set_tie_rgb (Handle sc, int tie, int red, int green,
				 int blue);
    
    native static final protected boolean
    ftk_eventviewer_set_tie_color (Handle sc, int tie, Handle color);

    native static final protected boolean
    ftk_eventviewer_set_tie_linestyle (Handle sc, int tie, int linewidth,
				       int linestyle);

    native static final protected boolean
    ftk_eventviewer_append_simultaneous_events_array (Handle sc, int tie,
    int arrayCount, SimultaneousEvent[] events);
    
    native static final protected boolean
    ftk_eventviewer_delete_trace (Handle sc, int trace_idx);
    
    native static final protected int[]
    ftk_eventviewer_get_selected_traces (Handle sc);
                
    native static final protected boolean
    ftk_eventviewer_tie_event_array (Handle sc, int tie_index, int count,
                 EventPair[] events);
}

