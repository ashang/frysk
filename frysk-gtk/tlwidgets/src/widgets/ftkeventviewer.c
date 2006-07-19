/* Copyright (C) 2006 Red Hat, Inc.
   This file is part of the Red Hat EventViewer GTK+ widget.

   Red Hat EventViewer is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat EventViewer is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat EventViewer; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA. */

#define _GNU_SOURCE
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <math.h>
#include <sys/types.h>
#include <sys/time.h>
#include <gtk/gtk.h>
#include <cairo.h>
#include <glib-object.h>
#define DO_INITIALISE
#define INCLUDE_PRIVATE
#include "ftkeventviewer.h"

//#define USE_FTK_SIGNAL
#define _

GQuark ftk_quark;

static gboolean ftk_eventviewer_da_scroll (GtkWidget      *widget,
					   GdkEventScroll *event,
					   gpointer        user_data);

static gboolean ftk_eventviewer_expose (GtkWidget * widget,
					GdkEventExpose * event,
					gpointer data);

static gboolean ftk_eventviewer_da_expose (GtkWidget * widget,
					   GdkEventExpose * event,
					   gpointer data);
					   
static gboolean ftk_eventviewer_configure (GtkWidget * widget,
					   GdkEventConfigure * event,
					   gpointer data);

static gboolean ftk_eventviewer_style (GtkWidget * widget,
						GtkStyle * previous_style,
						gpointer data);

static gboolean ftk_eventviewer_legend_da_expose (GtkWidget * widget,
						  GdkEventExpose * event,
						  gpointer data);
#ifdef USE_OLD_STYLE
static void ftk_eventviewer_realize (GtkWidget * widget,
				     gpointer data);
#endif
static void ftk_eventviewer_destroy (GtkObject * widget,
				     gpointer data);

static void ftk_eventviewer_center_click (GtkButton *button,
					  gpointer   user_data);
#if 0
static void ftk_eventviewer_scale_toggle (GtkToggleButton * button,
					  gpointer data);
#endif

#ifndef USE_SLIDER_INTERVAL
static void ftk_eventviewer_spin_vc (GtkSpinButton *spinbutton,
				     gpointer       user_data);
#endif

static void ftk_eventviewer_scroll_cv (GtkRange     *range,
				       GtkScrollType scroll,
				       gdouble       value,
				       gpointer      user_data);

static gboolean ftk_ev_button_press_event (GtkWidget * widget,
					   GdkEventButton * event,
					   gpointer data);

static gboolean ftk_ev_motion_notify_event (GtkWidget * widget,
					    GdkEventMotion * event,
					    gpointer data);

static gboolean ftk_ev_legend_motion_notify_event (GtkWidget * widget,
						   GdkEventMotion * event,
						   gpointer data);

static gboolean ftk_ev_leave_notify_event (GtkWidget * widget,
					   GdkEventCrossing * event,
					   gpointer data);

#ifdef USE_SLIDER_INTERVAL
static gchar * ftk_eventviewer_slider_format (GtkScale *scale,
					      gdouble   arg1,
					      gpointer  user_data);

static gboolean ftk_eventviewer_slider_cv (GtkRange * range,
					   GtkScrollType scroll,
					   gdouble   value,
					   gpointer  user_data);
#endif
    
gboolean
ftk_eventviewer_preset_bg_rgb_e (FtkEventViewer * eventviewer, guint red, guint green, guint blue, GError ** err);
    
gboolean
ftk_eventviewer_preset_trace_rgb_e(FtkEventViewer * eventviewer,
				gint trace_index, guint red, guint green, guint blue,
				GError ** err);
				
gboolean
ftk_eventviewer_preset_marker_rgb_e(FtkEventViewer * eventviewer,
				gint marker_index, guint red, guint green, guint blue,
				GError ** err);
				
static inline double timeval_to_double (struct timeval * tv);
static inline void double_to_timeval (struct timeval * tv,
				      double time_d);

#define DEFAULT_BG_RED		65535
#define DEFAULT_BG_GREEN	65535
#define DEFAULT_BG_BLUE		65535

#define DEFAULT_TRACE_RED	    0
#define DEFAULT_TRACE_GREEN	    0
#define DEFAULT_TRACE_BLUE	    0

#define DEFAULT_TIE_RED		65535
#define DEFAULT_TIE_GREEN	    0
#define DEFAULT_TIE_BLUE	    0

#define DEFAULT_MIN_TRACE_LENGTH	100
#define DEFAULT_MIN_TRACE_HEIGHT	 30
#define DEFAULT_TRACE_OFFSET		 10
#define DEFAULT_INITIAL_WIDTH		100
#define DEFAULT_INITIAL_HEIGHT		 30
#define DEFAULT_INITIAL_LEGEND_HEIGHT	 30

#define DEFAULT_SPAN			 60.0
#define MINIMUM_SPAN			 1e-6
#define MAXIMUM_SPAN			 1e6

/*******************************************************/
/*                                                     */
/*                    object stuff                     */
/*                                                     */
/*******************************************************/

enum {
  FTK_EVENTVIEWER_SIGNAL,
  LAST_SIGNAL
};

static void  ftk_eventviewer_class_init (FtkEventViewerClass * klass,
					 gpointer class_data);
static void  ftk_eventviewer_init       (FtkEventViewer      * eventviewer);
static guint ftk_eventviewer_signals[LAST_SIGNAL] = { 0 };

static void
ftk_eventviewer_class_init (FtkEventViewerClass *klass,
			    gpointer class_data)
{
  ftk_eventviewer_signals[FTK_EVENTVIEWER_SIGNAL]
    = g_signal_new ("ftkeventviewer",
		    G_TYPE_FROM_CLASS (klass),
		    G_SIGNAL_RUN_FIRST | G_SIGNAL_ACTION,
		    G_STRUCT_OFFSET (FtkEventViewerClass, ftkeventviewer),
		    NULL, 
		    NULL,                
		    g_cclosure_marshal_VOID__VOID,
		    G_TYPE_NONE, 0);
}

static void
ftk_init_cr (FtkEventViewer * eventviewer)
{
  ftk_ev_symbols_initted (eventviewer) = TRUE;
  cairo_t * cr =
    gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);

  {
    int i;
    gsize br, bw;
    PangoFontDescription * desc;
    int width, height;

#define FONT "dingbats 10"    
    desc = pango_font_description_from_string (FONT);

    for (i = 0; i < db_symbols_count; i++) {
      
      db_symbols[i].utf8 =  g_convert (ftk_symbol_unicode (i), 2,
				       "utf-8", "unicode",
				       &br, &bw,
				       NULL);
      ftk_symbol_strlen (i) = bw;
      ftk_symbol_layout (i) = pango_cairo_create_layout (cr);
      pango_layout_set_font_description (ftk_symbol_layout (i), desc);
      pango_layout_set_text (ftk_symbol_layout (i),
			     ftk_symbol_utf8 (i),
			     ftk_symbol_strlen (i));
      pango_layout_get_pixel_size (ftk_symbol_layout (i), &width, &height);
      ftk_symbol_h_center (i) = width  >> 1;
      ftk_symbol_v_center (i) = height >> 1;
    }	
    pango_font_description_free (desc);

#define FONT_SMALL "dingbats 5"    
    desc = pango_font_description_from_string (FONT_SMALL);
    little_dot.utf8 = g_convert (little_dot.unicode, 2,
				 "utf-8", "unicode",
				 &br, &bw,
				 NULL);
    little_dot.strlen = bw;
    little_dot.layout = pango_cairo_create_layout (cr);
    pango_layout_set_font_description (little_dot.layout, desc);
    pango_layout_set_text (little_dot.layout,
			   little_dot.utf8,
			   little_dot.strlen);
    pango_layout_get_pixel_size (little_dot.layout, &width, &height);
    little_dot.h_center = width  >> 1;
    little_dot.v_center = height >> 1;

  }
  cairo_destroy (cr);
}

static void
set_up_colors (FtkEventViewer * eventviewer)
{
  int i;

  ftk_ev_color_values(eventviewer) = malloc (sizeof(default_color_set));
  memcpy (ftk_ev_color_values(eventviewer), default_color_set,
	  sizeof(default_color_set));

#define COLOR_RANDOMISER_ITERATIONS 30
  for (i = 0; i < COLOR_RANDOMISER_ITERATIONS; i++) {
    const GdkColor * tc;
    int fi = lrint (drand48() * (double)(colors_count - 1));
    int ti = lrint (drand48() * (double)(colors_count - 1));

    tc =  ftk_ev_color_value(eventviewer, fi);
    ftk_ev_color_value(eventviewer, fi) =
      ftk_ev_color_value(eventviewer, ti);
    ftk_ev_color_value(eventviewer, ti) = tc;
  }
}

static void
initialise_widget (FtkEventViewer * eventviewer)
{
  ftk_ev_next_glyph (eventviewer)	= 0;
  ftk_ev_next_color (eventviewer)	= 0;
  
  ftk_ev_min_time (eventviewer)		= HUGE_VAL;
  ftk_ev_max_time (eventviewer)		= -HUGE_VAL;
  ftk_ev_time_set (eventviewer)		= FALSE;
  
  ftk_ev_popup_window (eventviewer)	= NULL;
  ftk_ev_popup_type (eventviewer)	= FTK_POPUP_TYPE_NONE;
  ftk_ev_popup_trace (eventviewer)	= -1;
  ftk_ev_popup_marker (eventviewer)	= -1;

  gtk_widget_ensure_style(GTK_WIDGET (ftk_ev_vbox(eventviewer)));
  GtkStyle *style = gtk_widget_get_style (GTK_WIDGET (ftk_ev_vbox(eventviewer)));
  ftk_ev_bg_red(eventviewer)	= style->bg[0].red;
  ftk_ev_bg_green(eventviewer)	= style->bg[0].green;
  ftk_ev_bg_blue(eventviewer)	= style->bg[0].blue;
  ftk_ev_label_box_width(eventviewer)  = 0;
  ftk_ev_label_box_height(eventviewer) = 0;

  ftk_ev_trace_pool(eventviewer)      = NULL;
  ftk_ev_trace_pool_next(eventviewer) = 0;
  ftk_ev_trace_pool_max(eventviewer)  = 0;

  ftk_ev_trace_order(eventviewer)      = NULL;
  ftk_ev_trace_order_next(eventviewer) = 0;
  ftk_ev_trace_order_max(eventviewer)  = 0;

  ftk_ev_traces(eventviewer)     = NULL;
  ftk_ev_trace_next(eventviewer) = 0;
  ftk_ev_trace_max(eventviewer)  = 0;
  ftk_ev_trace_modified(eventviewer)  = FALSE;

  ftk_ev_ties(eventviewer)     = NULL;
  ftk_ev_tie_next(eventviewer) = 0;
  ftk_ev_tie_max(eventviewer)  = 0;
  ftk_ev_tie_modified(eventviewer)  = FALSE;

  ftk_ev_links(eventviewer)     = NULL;
  ftk_ev_link_next(eventviewer) = 0;
  ftk_ev_link_max(eventviewer)  = 0;
  
  ftk_ev_dlinks(eventviewer)     = NULL;
  ftk_ev_dlink_next(eventviewer) = 0;
  ftk_ev_dlink_max(eventviewer)  = 0;

  ftk_ev_markers (eventviewer)		= NULL;
  ftk_ev_markers_next (eventviewer)	= 0;
  ftk_ev_markers_max (eventviewer)	= 0;
  ftk_ev_markers_modified (eventviewer)	= FALSE;

  ftk_ev_drawable(eventviewer)  = FALSE;

  ftk_ev_span(eventviewer) = DEFAULT_SPAN;
  
  ftk_ev_widget_modified (eventviewer) = TRUE;
}

/*------------------------- Button Box --------------------------------------*/

/*
 * ---------------------------------------------------------------------------
 * |	Hold | Center|										|Interval ----|---|
 * ---------------------------------------------------------------------------
 */

static GtkWidget *
create_button_box (FtkEventViewer * eventviewer, GtkTooltips * eventviewer_tips)
{
  /*****************   button box  **************/
    
  GtkWidget * hbutton_box = gtk_hbox_new(FALSE, 5);
  ftk_ev_hbutton_box (eventviewer) = hbutton_box;
  
  /* Accessibility for button box. */
   AtkObject *obj;
   obj = gtk_widget_get_accessible (hbutton_box);
   atk_object_set_name(obj, _("Button Box"));
   atk_object_set_description (obj, _("Box to hold all the buttons."));
  
  
    
#if 0  /* fixme -- not yet implemented */
  {
    /* scale button */
      
    GtkWidget * scale_toggle_button
      = gtk_toggle_button_new_with_mnemonic ("_Scaled");

    ftk_ev_scale_toggle_button (eventviewer) = scale_toggle_button;
    
    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  ftk_ev_scale_toggle_button (eventviewer),
			  "Draw to scale or as an unscaled sequence.",
			  "private");
    
    g_signal_connect (GTK_OBJECT(scale_toggle_button),"toggled",
                      (GtkSignalFunc) ftk_eventviewer_scale_toggle, NULL);
    /* fixme -- make initial state configurable */
    gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (scale_toggle_button),
				  TRUE);
    gtk_box_pack_start (GTK_BOX (hbutton_box),
			scale_toggle_button,
			FALSE, FALSE, 0);
  }
#endif
    
  {
    /* hold button */
      
    GtkWidget * hold_toggle_button
      = gtk_toggle_button_new_with_mnemonic ("_Hold");

    ftk_ev_hold_toggle_button (eventviewer) = hold_toggle_button;
    
    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  ftk_ev_hold_toggle_button (eventviewer),
			  "Enable or disable auto-updates.",
			  "private");
			  
	/* Accessibility for hold button. */
   AtkObject *obj;
   obj = gtk_widget_get_accessible (hold_toggle_button);
   atk_object_set_name(obj, _("Auto-Updates"));
   atk_object_set_description (obj, _("Enable/disable automatic-updates."));
   
    
#if 0 /* fixme -- don't know if needed */
    g_signal_connect (GTK_OBJECT(scale_toggle_button),"toggled",
                      (GtkSignalFunc) ftk_eventviewer_scale_toggle, NULL);
#endif
    /* fixme -- make initial state configurable */
    gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (hold_toggle_button),
				  FALSE);
    gtk_box_pack_start (GTK_BOX (hbutton_box),
			hold_toggle_button,
			FALSE, FALSE, 0);
  }
  
  {
    /* center button */
      
    GtkWidget * center_button
      = gtk_button_new_with_mnemonic ("_Center");

    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  center_button,
			  "Center the available data in the display.",
			  "private");
    
    g_signal_connect (GTK_OBJECT(center_button),"clicked",
                      (GtkSignalFunc) ftk_eventviewer_center_click,
		      eventviewer);

    gtk_box_pack_start (GTK_BOX (hbutton_box),
			center_button,
			FALSE, FALSE, 0);
		
   /* Accessibility for center button. */	
   
    AtkObject *obj;
    obj = gtk_widget_get_accessible (center_button);
    atk_object_set_name(obj, _("Center"));
    atk_object_set_description (obj, _("Center available data in the display."));
   
  }	

#ifdef USE_SLIDER_INTERVAL
  {
    /* interval slider */

    GtkRequisition requisition;

    GtkWidget * frame = gtk_frame_new (NULL);
    GtkWidget * hbox  = gtk_hbox_new (FALSE, 0);
    GtkWidget * label = gtk_label_new ("Interval");
    GtkObject * ival_adj
      = gtk_adjustment_new (log10 (ftk_ev_span (eventviewer)),
			    log10 (MINIMUM_SPAN),
			    log10 (MAXIMUM_SPAN),
			    0.1,
			    0.001,
			    0.01);
    GtkWidget * slider = gtk_hscale_new (GTK_ADJUSTMENT (ival_adj));
      
    ftk_ev_interval_scale (eventviewer) = slider;
    gtk_widget_size_request (slider, &requisition);
    gtk_widget_set_size_request (slider, 100, requisition.height);
    
    gtk_scale_set_digits (GTK_SCALE (slider), 1);
    g_signal_connect (GTK_OBJECT(slider),"format-value",
		      G_CALLBACK (ftk_eventviewer_slider_format),
		      eventviewer);

    g_signal_connect (GTK_OBJECT(slider),"change-value",
		      G_CALLBACK (ftk_eventviewer_slider_cv),
		      eventviewer);
      
    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  slider,
			  "Set display width in seconds.",
			  "private");
      
    gtk_container_add (GTK_CONTAINER(frame), hbox);
    gtk_box_pack_start (GTK_BOX (hbox), label, FALSE, FALSE, 10);
    gtk_box_pack_start (GTK_BOX (hbox), slider, FALSE, FALSE, 0);
    gtk_box_pack_end (GTK_BOX (hbutton_box),
		      frame,
		      FALSE, FALSE, 0);
		      
 	/* Accessibility for interval slider */
 	
 	{
 		AtkObject *obj, *atk_widget, *atk_label;
 		
 		obj = gtk_widget_get_accessible(frame);
 		atk_object_set_name(obj, _("Interval Frame"));
    	atk_object_set_description (obj, _("Frame to hold Interval Slider."));
    	
    	obj = gtk_widget_get_accessible(hbox);
 		atk_object_set_name(obj, _("Interval Box"));
    	atk_object_set_description (obj, _("Box to hold Interval Slider."));  	
 		
 		AtkRelationSet *relation_set;
        AtkRelation *relation;
        AtkObject *targets[1];

        atk_widget = gtk_widget_get_accessible (slider);
        atk_object_set_name(atk_widget, _("Interval slider"));
        atk_object_set_description(atk_widget, _("Logarithmic slider for time interval."));
        
        atk_label = gtk_widget_get_accessible (label);
        atk_object_set_name(atk_widget, _("Interval Label"));
        atk_object_set_description(atk_label, _("Label for the interval slider."));

        relation_set = atk_object_ref_relation_set (atk_label);
        targets[0] = atk_widget;

        relation = atk_relation_new (targets, 1, ATK_RELATION_LABEL_FOR);
        atk_relation_set_add (relation_set, relation);
        g_object_unref (G_OBJECT (relation));
 	}
  }
#endif

#ifndef USE_SLIDER_INTERVAL
  {
    /* interval spin button */

    GtkWidget * frame = gtk_frame_new (NULL);
    GtkWidget * hbox  = gtk_hbox_new (FALSE, 0);
    GtkWidget * label = gtk_label_new ("Interval");
    GtkObject * ival_adj
      = gtk_adjustment_new (ftk_ev_span (eventviewer),
			    MINIMUM_SPAN,
			    MAXIMUM_SPAN,
			    1.0,
			    ftk_ev_span (eventviewer),
			    ftk_ev_span (eventviewer));
    GtkWidget * ival_button
      = gtk_spin_button_new (GTK_ADJUSTMENT (ival_adj), /* adjustment */
			     1.0,		/* gdouble climb_rate */
			     4);		/* guint digits	*/
    g_signal_connect (GTK_OBJECT(ival_button),"value-changed",
                      (GtkSignalFunc) ftk_eventviewer_spin_vc,
		      eventviewer);

    ftk_ev_interval_button (eventviewer) = ival_button;
      
    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  ftk_ev_interval_button (eventviewer),
			  "Set display width in seconds.",
			  "private");

    gtk_container_add (GTK_CONTAINER(frame), hbox);
    gtk_box_pack_start (GTK_BOX (hbox), label, FALSE, FALSE, 10);
    gtk_box_pack_start (GTK_BOX (hbox), ival_button, FALSE, FALSE, 0);
    gtk_box_pack_end (GTK_BOX (hbutton_box),
		      frame,
		      FALSE, FALSE, 0);
  }
#endif

#ifdef USE_READOUT
  {
    /* center button */
      
    GtkWidget * frame   = gtk_frame_new ("Time");
    GtkWidget * readout = gtk_label_new ("");

    ftk_ev_readout (eventviewer) = readout;

    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  readout,
			  "Time in seconds.",
			  "private");

    gtk_container_set_border_width (GTK_CONTAINER (frame), 4);
    gtk_container_add (GTK_CONTAINER (frame), readout);
    gtk_box_pack_end (GTK_BOX (hbutton_box),
			frame,
			FALSE, FALSE, 0);
  }
#endif

  gtk_widget_show_all (hbutton_box);
  
  return hbutton_box;
}


/*------------------------------------ Legend Area --------------------------*/

/*Legend																	 
 * --------------------------------------------------------------------------
 * | {GlyphA} ObserverA {GlyphB} ObserverB .....							|
 * | ....																	|
 * --------------------------------------------------------------------------
 */
  
static GtkWidget *
create_legend_area (FtkEventViewer * eventviewer)
{
  GtkWidget * frame = gtk_frame_new ("Legend");
  GtkWidget * da = gtk_drawing_area_new();
  
  ftk_ev_legend_da (eventviewer) = GTK_DRAWING_AREA (da);
  gtk_drawing_area_size (ftk_ev_legend_da(eventviewer),
			 DEFAULT_INITIAL_WIDTH,
			 DEFAULT_INITIAL_LEGEND_HEIGHT);
  
  gtk_widget_set_events(GTK_WIDGET (da),
			GDK_POINTER_MOTION_MASK  |
			GDK_LEAVE_NOTIFY_MASK);

  g_signal_connect (GTK_OBJECT (da), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_legend_da_expose, eventviewer);

  g_signal_connect (GTK_OBJECT(da), "motion_notify_event",
		    G_CALLBACK (ftk_ev_legend_motion_notify_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "leave_notify_event",
		    G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
  
  ftk_ev_legend_frame (eventviewer) = frame;
  gtk_container_add (GTK_CONTAINER(frame), da);
  gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_IN);
  gtk_widget_show_all (frame);


  /* Legend Accessibility. */
  AtkObject *obj;
  obj = gtk_widget_get_accessible (frame);
  atk_object_set_name(obj, _("Legend Frame"));
  atk_object_set_description (obj, _("Frame to hold legend."));
  
  obj = gtk_widget_get_accessible (da);
  atk_object_set_name(obj, _("Legend Drawing Area"));
  atk_object_set_description (obj, _("Drawing Area to hold Legend."));

  return frame;
}

/************************************* Drawing Area **************************/
  
static GtkWidget *
create_drawing_area (FtkEventViewer * eventviewer)
{
  /*****************  drawing area **************/
  GtkWidget * scrolled_window;
  GtkWidget * frame = gtk_frame_new (NULL);
  GtkWidget * da = gtk_drawing_area_new();

  			/* atk stuff */
    AtkObject *obj;
    obj = gtk_widget_get_accessible (da);
    atk_object_set_name(obj, _("Drawing Area"));
    atk_object_set_description(obj, _("Main drawing area for event viewer"));
    
    obj = gtk_widget_get_accessible (frame);
    atk_object_set_name(obj, _("Drawing Frame"));
    atk_object_set_description(obj, _("The frame to hold the main drawing area"));
        
    /*fprintf (stderr, "initting atk %#08x\n", atk_obj);

    if (GTK_IS_ACCESSIBLE (atk_obj)) {
      fprintf (stderr, "accessible\n");
    } 
    */

  ftk_ev_da(eventviewer) = GTK_DRAWING_AREA (da);
  
  gtk_drawing_area_size (ftk_ev_da(eventviewer),
			 DEFAULT_INITIAL_WIDTH,
			 DEFAULT_INITIAL_HEIGHT);
  gtk_widget_show (da);

  ftk_ev_da_frame (eventviewer) = frame;
  
  gtk_widget_set_app_paintable(da, TRUE);
  
  gtk_widget_set_events(GTK_WIDGET (da),
			GDK_POINTER_MOTION_MASK  |
#if 0
			GDK_ENTER_NOTIFY_MASK |
#endif
			GDK_LEAVE_NOTIFY_MASK |
			GDK_SCROLL_MASK    |
			GDK_BUTTON_PRESS_MASK    |
			GDK_BUTTON_RELEASE_MASK);

  g_signal_connect (GTK_OBJECT (da), "scroll-event",
		    (GtkSignalFunc) ftk_eventviewer_da_scroll, eventviewer);

  g_signal_connect (GTK_OBJECT (da), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_da_expose, eventviewer);

  g_signal_connect (GTK_OBJECT(da), "button_press_event",
		    G_CALLBACK (ftk_ev_button_press_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "button_release_event",
		    G_CALLBACK (ftk_ev_button_press_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "motion_notify_event",
		    G_CALLBACK (ftk_ev_motion_notify_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "leave_notify_event",
		    G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
    
  g_signal_connect (GTK_OBJECT(da),"configure-event",
		    G_CALLBACK (ftk_eventviewer_configure), eventviewer);
		    
  g_signal_connect (GTK_OBJECT(da), "style-set",
  			G_CALLBACK (ftk_eventviewer_style), eventviewer);
  

#ifndef NOT_USE_SCROLL
  scrolled_window = gtk_scrolled_window_new (NULL, NULL);
  
  
  
  /* Scrolled Window Accessibility. */
  obj = gtk_widget_get_accessible (scrolled_window);
  atk_object_set_name(obj, _("Drawing Scrolled Window"));
  atk_object_set_description(obj, _("Scrolled Window to hold Drawing Area"));
  /* ------------------------------ */
   
  gtk_scrolled_window_add_with_viewport (GTK_SCROLLED_WINDOW (scrolled_window), da);
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolled_window),
				  GTK_POLICY_NEVER,
				  GTK_POLICY_ALWAYS);
  
  gtk_container_add (GTK_CONTAINER(frame), scrolled_window);
  gtk_widget_show (scrolled_window);
#else
  gtk_container_add (GTK_CONTAINER(frame), da);
#endif
  gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_IN);
  gtk_widget_show (frame);

  return frame;
}

GtkWidget *
create_scrollbar (FtkEventViewer * eventviewer)
{
  /**************** scrollbar *************/
    
  GtkObject * scroll_adj
    = gtk_adjustment_new (0.0,
			  0.0,
			  ftk_ev_span (eventviewer),
			  1.0,
			  ftk_ev_span (eventviewer),
			  ftk_ev_span (eventviewer)/4.0);
  GtkWidget * h_scroll = gtk_hscrollbar_new (GTK_ADJUSTMENT (scroll_adj));
  
   /* Horizontal ScrollBar Accessibility */
  
  AtkObject *obj;
  obj = gtk_widget_get_accessible (h_scroll);
  atk_object_set_name(obj, _("Main HScroll"));
  atk_object_set_description(obj, _("The main horizontal scrollbar"));
  
  gtk_range_set_update_policy (GTK_RANGE (h_scroll),
			       GTK_UPDATE_CONTINUOUS);
  gtk_widget_show (h_scroll);
  ftk_ev_scroll (eventviewer) = h_scroll;
  ftk_ev_scroll_adj (eventviewer) = GTK_ADJUSTMENT (scroll_adj);
  g_signal_connect (GTK_OBJECT(h_scroll),"change-value",
		    (GtkSignalFunc) ftk_eventviewer_scroll_cv, eventviewer);


  return h_scroll;
}

static void
ftk_eventviewer_init (FtkEventViewer * eventviewer)
{
  GtkTooltips * eventviewer_tips = gtk_tooltips_new ();
  GtkVBox * vbox = ftk_ev_vbox (eventviewer);
  
  AtkObject *obj;
  obj = gtk_widget_get_accessible (GTK_WIDGET (eventviewer));
  atk_object_set_name(obj,_("Main VBox"));
  atk_object_set_description(obj, _("The main event viewer VBox"));
  
  {
    struct timeval now;
    gettimeofday (&now, NULL);
    ftk_ev_zero (eventviewer) = timeval_to_double (&now);
    srand48 ((long int)(now.tv_usec));
  }

  set_up_colors (eventviewer);
  initialise_widget (eventviewer);
  
  g_signal_connect (GTK_OBJECT (eventviewer), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_expose, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer), "destroy",
		    GTK_SIGNAL_FUNC(ftk_eventviewer_destroy),
		    NULL);

  gtk_box_set_homogeneous (GTK_BOX (vbox), FALSE);
  gtk_box_set_spacing (GTK_BOX (vbox), 0);

  {
    GtkWidget * bb = create_button_box (eventviewer, eventviewer_tips);
    GtkWidget * la = create_legend_area (eventviewer);
    GtkWidget * da = create_drawing_area (eventviewer);
    GtkWidget * sb = create_scrollbar (eventviewer);
    
    gtk_box_pack_start (GTK_BOX (vbox), bb, FALSE, FALSE, 0);
    gtk_box_pack_start (GTK_BOX (vbox), la, FALSE, FALSE, 0);
    gtk_box_pack_start (GTK_BOX (vbox), da, TRUE, TRUE, 0);
    gtk_box_pack_start (GTK_BOX (vbox), sb, FALSE, FALSE, 0);
  }
}

/*******************************************************/
/*                                                     */
/*                         utilities                   */
/*                                                     */
/*******************************************************/

static inline double
timeval_to_double (struct timeval * tv)
{
  return ((double)(tv->tv_sec)) + (((double)(tv->tv_usec))/1.0e6);
}

static inline void
double_to_timeval (struct timeval * tv, double time_d)
{
  if (tv) {
    double int_part;
    tv->tv_sec = (time_t)floor (time_d);
    tv->tv_usec = lrint (1.0e6 * modf (time_d, &int_part));
  }
}

/*******************************************************/
/*                                                     */
/*                   drawing operations                */
/*                                                     */
/*******************************************************/

typedef struct {
  int x;
  int y;
} ftk_coord_s;

static int
coord_sort (a, b)
     ftk_coord_s * a;
     ftk_coord_s * b;
{
  return ((a->y == b->y) ? 0 : (a->y < b->y) ? -1 : 1);
}

#define FAKE_OFFSET 8  /* fixme */
#define FAKE_OFFSET_P 2  /* fixme */

static void
draw_dlink (FtkEventViewer * eventviewer, cairo_t * cr,
	    ftk_dlink_s * dlink,
	    gboolean flush_it)
{
  int i;
  gboolean kill_cr;

  if (1 < ftk_dlink_event_list_next (dlink)) {
    ftk_tie_s * tie = ftk_ev_tie (eventviewer, ftk_dlink_tie_index (dlink));
    ftk_coord_s * coords =
      alloca (ftk_dlink_event_list_next (dlink) * sizeof(ftk_coord_s));
    
    gdouble time_offset =
      gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));

    for (i = 0; i < ftk_dlink_event_list_next (dlink); i++) {
      int event_index = ftk_dlink_event_pair_event (dlink, i);
      int trace_index = ftk_dlink_event_pair_trace (dlink, i);
      ftk_trace_s * trace = ftk_ev_trace (eventviewer, trace_index);
      ftk_event_s * event = ftk_trace_event (trace, event_index);
      double etime =
	(ftk_event_time (event) - ftk_ev_zero (eventviewer)) - time_offset;
      double loc_d = etime / ftk_ev_span (eventviewer);
      int h_offset = ftk_ev_trace_origin (eventviewer) +
	lrint (((double)ftk_ev_trace_width (eventviewer)) * loc_d);

      coords[i].x = h_offset;
      coords[i].y = ftk_trace_vpos_d (trace);
    }

    qsort(coords, ftk_dlink_event_list_next(dlink),
	  sizeof(ftk_coord_s), coord_sort);
    
    if (NULL == cr) {
      cr = gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);
      kill_cr = TRUE;
    }
    else kill_cr = FALSE;
    
    cairo_save (cr);
    cairo_set_source_rgb (cr,
			  ((double)(ftk_tie_color_red (tie)))/65535.0,
			  ((double)(ftk_tie_color_green (tie)))/65535.0,
			  ((double)(ftk_tie_color_blue (tie)))/65535.0);

    cairo_new_path (cr);

    if (0 < ftk_tie_linewidth (tie))
      cairo_set_line_width (cr, ftk_tie_linewidth (tie));
	    
    if (0.0 < ftk_tie_linestyle (tie))
      cairo_set_dash (cr, &ftk_tie_linestyle (tie), 1, 0.0);
    
    for (i = 0; i < ftk_dlink_event_list_next (dlink); i++)
      cairo_line_to (cr, (double)(coords[i].x), (double)(FAKE_OFFSET + coords[i].y));
    cairo_stroke (cr);

    for (i = 0; i < ftk_dlink_event_list_next (dlink); i++) {
      cairo_move_to (cr,
		     (double)(coords[i].x - little_dot.h_center),
		     (double)(FAKE_OFFSET_P + coords[i].y + little_dot.v_center));
      pango_cairo_show_layout (cr, little_dot.layout);
      cairo_stroke (cr);
    }
    cairo_restore (cr);
    if (kill_cr)  cairo_destroy (cr);
  }
}

static int
int_sort (a, b)
     int * a;
     int * b;
{
  return ((*a) == (*b)) ? 0 : (((*a) < (*b)) ? -1 : 1);
}

static void
draw_link (FtkEventViewer * eventviewer, cairo_t * cr,
	   ftk_link_s * link,
	   gboolean flush_it)
{
  gboolean kill_cr;
  gdouble time_offset =
    gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
  double link_time = (ftk_link_when (link) - ftk_ev_zero (eventviewer))
      - time_offset;

  if (link_time >= 0.0) {
    ftk_tie_s * tie = ftk_ev_tie (eventviewer, ftk_link_tie_index (link));
    double loc_d = link_time / ftk_ev_span (eventviewer);
    
    if ((loc_d >= 0.0) && (loc_d <= 1.0)) {
      int i;
      
      int h_offset = ftk_ev_trace_origin (eventviewer) +
	lrint (((double)ftk_ev_trace_width (eventviewer)) * loc_d);

      if (0 < ftk_link_trace_list_next(link)) {
	int * vps = alloca (ftk_link_trace_list_next(link) * sizeof (int));
  
	if (NULL == cr) {
	  cr = gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);
	  kill_cr = TRUE;
	}
	else kill_cr = FALSE;

	cairo_set_source_rgb (cr,
			      ((double)(ftk_tie_color_red (tie)))/65535.0,
			      ((double)(ftk_tie_color_green (tie)))/65535.0,
			      ((double)(ftk_tie_color_blue (tie)))/65535.0);
	
	for (i = 0; i < ftk_link_trace_list_next(link); i++) {
	  ftk_trace_s * trace =
	    ftk_ev_trace (eventviewer, ftk_link_trace(link, i));
	  vps[i] = ftk_trace_vpos_d (trace);

	  cairo_move_to (cr,
			 (double)(h_offset - little_dot.h_center),
			 (double)(FAKE_OFFSET_P + vps[i] + little_dot.v_center));
	  pango_cairo_show_layout (cr, little_dot.layout);
	  cairo_stroke (cr);
	}

	if (2 < ftk_link_trace_list_next(link)) {
	  qsort(vps, ftk_link_trace_list_next(link), sizeof(int), int_sort);
	}

	cairo_save (cr);
	cairo_move_to (cr, (double)(h_offset), (double)(FAKE_OFFSET + vps[0]));

	if (0 < ftk_tie_linewidth (tie))
	  cairo_set_line_width (cr, ftk_tie_linewidth (tie));
	    
	if (0.0 < ftk_tie_linestyle (tie))
	  cairo_set_dash (cr, &ftk_tie_linestyle (tie), 1, 0.0);
	
	cairo_line_to (cr, (double)(h_offset),
		       (double)(FAKE_OFFSET + vps[ftk_link_trace_list_next(link) - 1]));
	cairo_stroke (cr);
	cairo_restore (cr);
      
	if (kill_cr)  cairo_destroy (cr);
      }
    }
  }
}

/*******************************************************/
/*                                                     */
/*                     callbacks                       */
/*                                                     */
/*******************************************************/
static void
ftk_eventviewer_center_click (GtkButton *button,
			      gpointer   user_data)
{
  double slop;
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
  double span = ftk_ev_max_time (eventviewer) - ftk_ev_min_time (eventviewer);

  if (!ftk_ev_time_set (eventviewer) || (span == 0.0)) return;
  
  ftk_ev_span (eventviewer) = span;

  slop = 0.25 * ftk_ev_span (eventviewer);

  g_object_set (G_OBJECT (adj), "page-size", slop + ftk_ev_span (eventviewer),
		NULL);
  gtk_adjustment_set_value (adj, (ftk_ev_min_time (eventviewer)
				  - ftk_ev_zero (eventviewer)) - slop/2.0);

  gtk_range_set_value (GTK_RANGE (ftk_ev_interval_scale (eventviewer)),
		       log10 (slop + ftk_ev_span (eventviewer)));

  gtk_adjustment_changed (adj);

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
}

static gboolean
ftk_eventviewer_da_scroll (GtkWidget * widget,
			   GdkEventScroll * event,
			   gpointer user_data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  gboolean rc = FALSE;		/* false to propagate */

  switch ((int)(event->state)) {
  case 1:		/* shift */
    gtk_propagate_event (ftk_ev_scroll (eventviewer), (GdkEvent *)event);
    rc = TRUE;		/* inhibit prop	*/
    break;
  case 4:		/* ctrl */
    gtk_propagate_event (ftk_ev_interval_scale(eventviewer),  (GdkEvent *)event);
    rc = TRUE;		/* inhibit prop	*/
    break;
#if 0
  case 8:		/* alt */
    rc = TRUE;		/* inhibit prop	*/
    break;
#endif
  }

  return rc;
}

#ifdef USE_SLIDER_INTERVAL
static gchar *
ftk_eventviewer_slider_format (GtkScale *scale,
			       gdouble   arg1,
			       gpointer  user_data)
{
#if 1
  return g_strdup_printf ("%g", exp10 (arg1));
#else
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
  double lv = exp10 (arg1);
  
  ftk_ev_span(eventviewer) = lv;

  fprintf (stderr, "setting ps to %g\n", ftk_ev_span (eventviewer));

  g_object_set (G_OBJECT (adj), "page-size", ftk_ev_span (eventviewer), NULL);
  gtk_adjustment_changed (adj);
    
  draw_plot (eventviewer);
#endif
}

static gboolean
ftk_eventviewer_slider_cv (GtkRange * range,
			   GtkScrollType scroll,
			   gdouble   value,
			   gpointer  user_data)
{
  if ((value >= log10 (MINIMUM_SPAN)) && (value <= log10 (MAXIMUM_SPAN))) {
    FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
    GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
    double lv = exp10 (value);
  
    ftk_ev_span(eventviewer) = lv;

    g_object_set (G_OBJECT (adj), "page-size", ftk_ev_span (eventviewer), NULL);
    gtk_adjustment_changed (adj);

    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)),
			      NULL, eventviewer);
    
    return FALSE;
  }
  else return TRUE;	/* do no more */
}
#endif

#ifdef USE_READOUT
/* offset = adj_val				*/
/* mark = (time - zero) - offset		*/
/* loc_d = mark / span				*/
/* y = trace_origin +   trace_width * loc_d 	*/

/* y - origin = width * loc			*/
/* loc = (y - origin)/width			*/
/* mark = loc * span				*/
/* time - zero = mark + offset			*/

static void
set_readout (FtkEventViewer * eventviewer, double y)
{
  char * ro;
  double loc_d = (y - ((double)(ftk_ev_trace_origin (eventviewer)))) /
    ((double)(ftk_ev_trace_width (eventviewer)));
  double mark_time = loc_d *  ftk_ev_span (eventviewer);
  double tv = mark_time + gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
  fprintf (stderr, "%g %g\n",
	   ftk_ev_min_time (eventviewer) - ftk_ev_zero (eventviewer),
	   ftk_ev_max_time (eventviewer) - ftk_ev_zero (eventviewer));
  asprintf (&ro, "%g", tv);
  gtk_label_set_text (GTK_LABEL (ftk_ev_readout (eventviewer)), ro);
  free (ro);
}
#endif

static gboolean
ftk_ev_button_press_event (GtkWidget * widget,
			   GdkEventButton * event,
			   gpointer data)
{
  fprintf (stderr, "bp %d(%d) [%g, %g]\n",
	   (int)event->button,
	   (int)event->state,
	   (double)event->x,
	   (double)event->y);
  return TRUE;
}

static void
ftk_create_popup_window (FtkEventViewer * eventviewer, char * lbl, double dx)
{
  GtkWidget * window = gtk_window_new (GTK_WINDOW_POPUP);
  GtkWidget * frame = gtk_frame_new (NULL);
  GtkWidget * label = gtk_label_new (lbl);
  ftk_ev_popup_label (eventviewer) = label;

  ftk_ev_popup_window (eventviewer) = window;
  
  g_signal_connect (GTK_OBJECT (window), 
		    "destroy",
		    G_CALLBACK (gtk_widget_destroyed),
		    &ftk_ev_popup_window (eventviewer));
    
  gtk_window_set_decorated (GTK_WINDOW (window), TRUE);
  gtk_window_set_resizable (GTK_WINDOW (window), FALSE);
  gtk_window_set_modal (GTK_WINDOW (window), FALSE);
  gtk_window_set_keep_above (GTK_WINDOW (window), TRUE);
  gtk_widget_set_app_paintable (window, TRUE);
  gtk_container_set_border_width (GTK_CONTAINER (window), 2);
  gtk_widget_ensure_style (window);
  gtk_label_set_line_wrap (GTK_LABEL (label), TRUE);
  gtk_misc_set_alignment (GTK_MISC (label), 0.5, 0.5);
  
  gtk_widget_show (label);
  gtk_container_add (GTK_CONTAINER (frame), label);
  gtk_widget_show (frame);
  gtk_container_add (GTK_CONTAINER (window), frame);

  {
    GtkRequisition req;
    int offset;
    gint px, py;
    GtkWidget * widget = GTK_WIDGET (eventviewer);

    gdk_display_get_pointer (gdk_screen_get_display (gtk_widget_get_screen (widget)),
			     NULL, &px, &py, NULL);

    gtk_widget_size_request (frame, &req);

    offset = lrint ((dx/((double)(widget->allocation.width))) *
		    ((double)(req.width)));

    gtk_window_move (GTK_WINDOW (ftk_ev_popup_window (eventviewer)),
		     px - offset, py + 18);
    gtk_widget_show (ftk_ev_popup_window (eventviewer));
  }
}

static void
ftk_create_popup (FtkEventViewer * eventviewer, char * lbl, double dx)
{
  if (ftk_ev_popup_window (eventviewer))
    gtk_label_set_text (GTK_LABEL (ftk_ev_popup_label (eventviewer)), lbl);
  else 
    ftk_create_popup_window (eventviewer, lbl, dx);
}

#define MULTI_EVENT_INCR 4
typedef struct {
  int marker_idx;
  ftk_event_s * event;
  double time_d;
} ftk_multi_event_s;

static char *
create_popup_marker_label (gboolean prepend_ts,
			   int * count_p,
			   FtkEventViewer * eventviewer,
			   ftk_event_s * revent,
			   ftk_trace_s * trace,
			   int marker_idx,
			   double time_d)
{
  char * lbl;
  int count;
  ftk_marker_s * marker = ftk_ev_marker (eventviewer, marker_idx);
  const char * trace_label = pango_layout_get_text (ftk_trace_label (trace));
  const char * marker_label = pango_layout_get_text (ftk_marker_label (marker));
  
  ftk_ev_popup_marker (eventviewer) = marker_idx;
  
  
  if (prepend_ts) {
    if (ftk_event_string (revent))
      count = asprintf (&lbl, "%g sec on trace %s\nEvent = %s\n%s",
			time_d,
			trace_label,
			marker_label,
			ftk_event_string (revent));
    else
      count = asprintf (&lbl, "%g sec on trace %s\nEvent = %s",	
			time_d,
			trace_label,
			marker_label);
  }
  else {
    if (ftk_event_string (revent))
      count = asprintf (&lbl, "Event = %s\n%s",	
			marker_label,
			ftk_event_string (revent));
    else
      count = asprintf (&lbl, "Event = %s",	
			marker_label);
  }

  if (count_p) *count_p = count;

  return lbl;
}

static char *
create_popup_label (FtkEventViewer * eventviewer,
		    ftk_popup_type_e pt,
		    ftk_trace_s * trace,
		    ftk_event_s * revent,
		    int marker_idx,
		    double time_d,
		    int me_next,
		    ftk_multi_event_s * me)
{
  char * lbl = NULL;
  ftk_marker_s * marker = ftk_ev_marker (eventviewer, marker_idx);
  
  switch (pt) {
  case FTK_POPUP_TYPE_NONE:
    break;
  case FTK_POPUP_TYPE_LABEL:
    if (ftk_trace_string (trace))
      asprintf (&lbl,
		"Trace: %s\n%s",
		pango_layout_get_text (ftk_trace_label (trace)),
		ftk_trace_string (trace));
    else
      asprintf (&lbl,
		"Trace: %s",
		pango_layout_get_text (ftk_trace_label (trace)));
    break;
  case FTK_POPUP_TYPE_MARKER:
    if (me) {
      int i;
      double time_d = -1.0;

      for (i = 0; i < me_next; i++) {
	int count;
	char * tlbl = create_popup_marker_label ((time_d != me[i].time_d)
						 ? TRUE : FALSE,
						 &count,
						 eventviewer,
						 me[i].event,
						 trace,
						 me[i].marker_idx,
						 me[i].time_d);
	time_d = me[i].time_d;

	if (tlbl && (count > 0)) {
	  if (lbl) asprintf (&lbl, "%s\n----\n%s", lbl, tlbl);
	  else lbl = tlbl;
	}
      }
    }
    else lbl =
	   create_popup_marker_label (TRUE, NULL, eventviewer, revent, trace,
				      marker_idx, time_d);
    break;
  case FTK_POPUP_TYPE_LEGEND:
    ftk_ev_popup_marker (eventviewer) = marker_idx;
    if (ftk_marker_string (marker))
      asprintf (&lbl,
		"Type = %s\n%s",
		pango_layout_get_text (ftk_marker_label (marker)),
		ftk_marker_string (marker));
    else 
      asprintf (&lbl,
		"Type = %s",
		pango_layout_get_text (ftk_marker_label (marker)));
    
    break;
  case FTK_POPUP_TYPE_TIME:
    asprintf (&lbl, "%g", time_d);
    break;
  }

  return lbl;
}

static void
handle_popup (FtkEventViewer * eventviewer,
	      GdkEventMotion * event,
	      ftk_popup_type_e pt,
	      int trace_idx,
	      int marker_idx,
	      double time_d,
	      ftk_trace_s * trace,
	      ftk_event_s * revent,
	      ftk_multi_event_s * multi_event,
	      int multi_event_next)
{
  char * lbl;
  
  if (FTK_POPUP_TYPE_NONE == ftk_ev_popup_type (eventviewer)) {
    if (FTK_POPUP_TYPE_NONE != pt) {	/* no existing popup, hit, create new popup */
      ftk_ev_popup_type (eventviewer) = pt;
      ftk_ev_popup_trace (eventviewer) = trace_idx;
      if ((lbl = create_popup_label (eventviewer, pt, trace, revent, marker_idx, time_d,
				     multi_event_next, multi_event)))
	ftk_create_popup (eventviewer, lbl, event->x);	
    }
  }
  else {
    if (FTK_POPUP_TYPE_NONE != pt) {	/* existing popup, hit */
      if ((ftk_ev_popup_trace (eventviewer) != trace_idx) ||	/* different trace	*/
	  (pt != ftk_ev_popup_type (eventviewer)) ||		/* or different type	*/
	  (pt == FTK_POPUP_TYPE_TIME) ||			/* or time		*/
	  ((pt == ftk_ev_popup_type (eventviewer)) &&		/* or same type and...	*/
	   ((pt == FTK_POPUP_TYPE_LEGEND) ||			/* either marker...	*/
	    (pt == FTK_POPUP_TYPE_MARKER)) &&			/* or legend...		*/
	   (ftk_ev_popup_marker (eventviewer) != marker_idx))) {   /* different idx	*/
	ftk_ev_popup_type (eventviewer) =  pt;
	ftk_ev_popup_trace (eventviewer) = trace_idx;
	ftk_ev_popup_marker (eventviewer) = marker_idx;
	if ((lbl = create_popup_label (eventviewer, pt, trace, revent, marker_idx,
				       time_d,
				       multi_event_next, multi_event)))
	  ftk_create_popup (eventviewer, lbl, event->x);	
      }
    }
    else {		/* existing popup, no hit, pop down */
      if (ftk_ev_popup_window (eventviewer))
	gtk_widget_destroy (ftk_ev_popup_window (eventviewer));
      ftk_ev_popup_type (eventviewer) =  FTK_POPUP_TYPE_NONE;
    }
  }
  if (multi_event) free (multi_event);
}

#define POPUP_TOLERANCE 6
static gboolean
ftk_ev_legend_motion_notify_event (GtkWidget * widget,
			    GdkEventMotion * event,
			    gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);

  {
    int marker_idx;
    ftk_popup_type_e pt;
    ftk_marker_s * marker;
    int pvpos = lrint (event->y) - 10;
    int phpos = lrint (event->x);

    pt = FTK_POPUP_TYPE_NONE;
    for (marker_idx = 0;
	 marker_idx < ftk_ev_markers_next (eventviewer);
	 marker_idx++) {
      marker = ftk_ev_marker (eventviewer, marker_idx);
      /* fixme -- replace the constants */
      if ((abs ((pvpos + 6) - ftk_marker_vpos (marker))  < POPUP_TOLERANCE) &&
	  ((abs ((phpos - 4) - ftk_marker_glyph_hpos (marker)) < POPUP_TOLERANCE) ||
	   ((phpos >=  ftk_marker_label_hpos (marker)) &&
	    (phpos <= ftk_marker_label_hpos (marker) +
	     ftk_marker_label_width(marker))))) {
	pt = FTK_POPUP_TYPE_LEGEND;
	break;
      }
    }

    handle_popup (eventviewer, event, pt,
		  0, marker_idx,
		  0.0, NULL, NULL,
		  NULL,
		  0);
  }

  return TRUE;
}

static gboolean
ftk_ev_motion_notify_event (GtkWidget * widget,
			    GdkEventMotion * event,
			    gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);

#ifdef USE_READOUT
  set_readout (eventviewer, event->x);
#endif

  {
    int i;
    int trace_idx;
    int marker_idx;
    ftk_trace_s * trace;
    ftk_marker_s * marker;
    ftk_popup_type_e pt;
    ftk_event_s * revent;
    double time_d = 0.0;
    /* fixme -- replace 12 w something based of font hgt */
    /* fixme maybe put in loop below and create running baseline */
    int pvpos = lrint (event->y) - 10;
    int phpos = lrint (event->x);
    ftk_multi_event_s * multi_event = NULL;
    int multi_event_next = 0;
    int multi_event_max  = 0;

    trace = NULL;
    marker = NULL;
    revent = NULL;
    trace_idx = -1;
    marker_idx = -1;
    pt = FTK_POPUP_TYPE_NONE;
    
    for (i = 0; i < ftk_ev_trace_next (eventviewer); i++) {
      ftk_trace_s * ltrace = ftk_ev_trace (eventviewer, i);

      if (!ftk_trace_valid (ltrace)) continue;
      if (abs (ftk_trace_vpos_d (ltrace) - pvpos) < POPUP_TOLERANCE) {
	trace = ltrace;
	trace_idx = i;
	if (phpos <= ftk_ev_label_box_width (eventviewer))
	  pt = FTK_POPUP_TYPE_LABEL;
	else {
	  int j;
	  int hit_count = 0;
	  ftk_event_s * qevent = NULL;
	  for (j = 0; j <  ftk_trace_event_next(ltrace); j++) {
	    ftk_event_s * pevent = ftk_trace_event (ltrace, j);
	    if ((abs (ftk_event_loc (pevent) - phpos) < POPUP_TOLERANCE) &&
		(trace_idx == i)) {		/* fixme -- always true ? */
	      if (1 == hit_count) {
		multi_event_max = MULTI_EVENT_INCR;
		multi_event = realloc (multi_event,
				       multi_event_max
				       * sizeof (ftk_multi_event_s));
		multi_event[0].marker_idx = marker_idx;
		multi_event[0].event = qevent;
		multi_event[0].time_d = time_d;
		multi_event_next = 1;
	      }
	      marker_idx = ftk_event_marker (pevent);
	      marker = ftk_ev_marker (eventviewer, marker_idx);
	      time_d = ftk_event_time (pevent) - ftk_ev_zero (eventviewer);
	      revent = pevent;
	      if (1 <= hit_count) {
		if (multi_event_next >= multi_event_max) {
		  multi_event_max += MULTI_EVENT_INCR;
		  multi_event = realloc (multi_event,
					 multi_event_max
					 * sizeof (ftk_multi_event_s));
		}
		multi_event[multi_event_next].marker_idx = marker_idx;
		multi_event[multi_event_next].event = pevent;
		multi_event[multi_event_next++].time_d = time_d;
	      }
	      pt = FTK_POPUP_TYPE_MARKER;
	      hit_count++;
	    }
	    qevent = pevent;
	  }
	  if (pt == FTK_POPUP_TYPE_NONE) {
	    double loc_d = (event->x - ((double)(ftk_ev_trace_origin (eventviewer)))) /
	      ((double)(ftk_ev_trace_width (eventviewer)));
	    double mark_time = loc_d *  ftk_ev_span (eventviewer);
	    time_d = mark_time + gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
	    pt = FTK_POPUP_TYPE_TIME;
	  }
	}
	break;
      }
    }

    handle_popup (eventviewer, event, pt,
		  trace_idx, marker_idx,
		  time_d, trace, revent,
		  multi_event,
		  multi_event_next);

  }

  return TRUE;
}

static gboolean
ftk_ev_leave_notify_event (GtkWidget * widget,
			   GdkEventCrossing * event,
			   gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);

  
  if (FTK_POPUP_TYPE_NONE != ftk_ev_popup_type (eventviewer)) {
    if (ftk_ev_popup_window (eventviewer)) {
      gtk_widget_destroy (ftk_ev_popup_window (eventviewer));
      // handled in gtk_widget_destroyed 
      // ftk_ev_popup_window (eventviewer) = NULL;
    }
    ftk_ev_popup_type (eventviewer) =  FTK_POPUP_TYPE_NONE;
  }
  return FALSE;
}

static void
draw_cairo_point (FtkEventViewer * eventviewer, cairo_t * cr,
		  ftk_trace_s * trace,
		  ftk_event_s * event, gboolean flush_it)
{
  //  int o_x, o_y;
  //  int d_w, d_h;
  gboolean kill_cr;

  ftk_event_loc (event) = -1;

  if (NULL == cr) {
    cr = gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);
    kill_cr = TRUE;
  }
  else kill_cr = FALSE;

  {
    gdouble time_offset = gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
    
    ftk_marker_s * marker
      = ftk_ev_marker (eventviewer, ftk_event_marker (event));
    double mark_time = (ftk_event_time (event) - ftk_ev_zero (eventviewer))
      - time_offset;
    if (mark_time >= 0.0) {
      double loc_d = mark_time / ftk_ev_span (eventviewer);

      if ((loc_d >= 0.0) && (loc_d <= 1.0)) {
	int h_offset = ftk_ev_trace_origin (eventviewer) +
	  lrint (((double)ftk_ev_trace_width (eventviewer)) * loc_d);
	int sym_idx = ftk_marker_glyph (marker);
	int v_offset = ftk_trace_vpos_d (trace) +
	  ((ftk_trace_label_dheight(trace) >> 1) -
	   ftk_symbol_v_center(sym_idx));

	cairo_set_source_rgb (cr,
			      (double)(ftk_marker_color_red (marker)) /
			      (double)65535,
			      (double)(ftk_marker_color_green (marker)) /
			      (double)65535,
			      (double)(ftk_marker_color_blue (marker)) /
			      (double)65535);

	cairo_move_to (cr,
		       (double)(h_offset - ftk_symbol_h_center(sym_idx)),
		       (double)(v_offset /*- ftk_symbol_v_center(sym_idx) */ ));
	
	pango_cairo_show_layout (cr, ftk_symbol_layout (sym_idx));
	cairo_stroke (cr);
	
	ftk_event_loc (event) = h_offset;
      }
    }
  }
  
  if (kill_cr)  cairo_destroy (cr);
}
  
#define LEGEND_MARGIN		10
#define LEGEND_GLYPH_SPACING	3

static gboolean
ftk_eventviewer_legend_da_expose(GtkWidget * dwidge, GdkEventExpose * event,
				 gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  
  if (!ftk_ev_symbols_initted (eventviewer)) ftk_init_cr (eventviewer);
  
  /* compute legend extents and baselines */
  
  if (ftk_ev_markers_modified (eventviewer) ||
      ftk_ev_widget_modified(eventviewer)) {
    int l_h_pos = LEGEND_MARGIN;
    int dww = (int)(dwidge->allocation.width);
    int legend_width;
    int i;
    gint width = 0;
    gint height = 0;
    int legend_v_pos = 0;
    
    ftk_ev_markers_modified (eventviewer) = FALSE;
    //    ftk_ev_widget_modified (eventviewer) = FALSE;

    for ( i = 0; i < ftk_ev_markers_next (eventviewer); i++) {
      gint gwidth, gheight;
      ftk_marker_s * marker = ftk_ev_marker (eventviewer, i);

      /* fixme -- maybe use marker_label_modified */
      width   = ftk_marker_label_width(marker);
      height  = ftk_marker_label_height(marker);

      pango_layout_get_pixel_size (ftk_symbol_layout (ftk_marker_glyph (marker)),
				   &gwidth, &gheight);

      legend_width = gwidth + LEGEND_GLYPH_SPACING + width;
      if ((l_h_pos + legend_width + LEGEND_MARGIN) > dww) {
	l_h_pos  = LEGEND_MARGIN;
	legend_v_pos += height;
      }

      ftk_marker_glyph_hpos (marker) = l_h_pos;
      ftk_marker_label_hpos (marker) = l_h_pos + gwidth
	+ LEGEND_GLYPH_SPACING ;
      ftk_marker_vpos (marker) = legend_v_pos;
      l_h_pos += gwidth + LEGEND_GLYPH_SPACING + width + LEGEND_MARGIN;
    }
    
    ftk_ev_legend_height(eventviewer) = legend_v_pos + height;

    if ((ftk_ev_legend_height(eventviewer) > (int)(dwidge->allocation.height)) ||
	(ftk_ev_legend_height(eventviewer) < ((int)(dwidge->allocation.height) - 12))) {
      gtk_widget_set_size_request (GTK_WIDGET (ftk_ev_legend_da (eventviewer)),
				   (int)(dwidge->allocation.width),
				   ftk_ev_legend_height(eventviewer));
    }
  }
  
  {	/* draw legend */
    int i;

    cairo_t * cr = gdk_cairo_create (dwidge->window);
      
    for ( i = 0; i < ftk_ev_markers_next (eventviewer); i++) {
      ftk_marker_s * marker = ftk_ev_marker (eventviewer, i);

      cairo_set_source_rgb (cr,
			    (double)(ftk_marker_color_red (marker)) /
			    (double)65535,
			    (double)(ftk_marker_color_green (marker)) /
			    (double)65535,
			    (double)(ftk_marker_color_blue (marker)) /
			    (double)65535);
	
      cairo_move_to (cr,
		     (double)(ftk_marker_glyph_hpos(marker)),
		     (double)(ftk_marker_vpos(marker)));
      pango_cairo_show_layout (cr,
			       ftk_symbol_layout (ftk_marker_glyph (marker)));
      cairo_stroke (cr);
	
      cairo_move_to (cr,
		     (double)(ftk_marker_label_hpos(marker)),
		     (double)(ftk_marker_vpos(marker)));
      pango_cairo_show_layout (cr, ftk_marker_label (marker));
      cairo_stroke (cr);
    }

    cairo_destroy (cr);
  }

  return FALSE;
}

static gboolean
ftk_eventviewer_da_expose(GtkWidget * dwidge, GdkEventExpose * event,
			  gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  
  if (!ftk_ev_symbols_initted (eventviewer)) ftk_init_cr (eventviewer);

  /* compute label extents and baselines */
  if (ftk_ev_trace_modified (eventviewer)) {
    int i;
    int max_label_width  = 0;
    int total_label_height = 0;
    
    ftk_ev_trace_modified (eventviewer) = FALSE;

    for (i = 0; i < ftk_ev_trace_order_next(eventviewer); i++) {
      ftk_trace_s * trace = ftk_ev_trace(eventviewer,
					 ftk_ev_trace_order_ety (eventviewer, i));
      
      if (ftk_trace_valid(trace)) {
	if (ftk_trace_label_modified(trace)) {
	  gint width, height;

	  ftk_trace_label_modified(trace) = FALSE;

	  pango_layout_get_pixel_size (ftk_trace_label(trace),
				       &width, &height);

	  ftk_trace_label_dwidth(trace)  = width;
	  ftk_trace_label_dheight(trace) = height;
	}
	ftk_trace_vpos_d(trace) = total_label_height;
	total_label_height += ftk_trace_label_dheight(trace);
	max_label_width     = MAX (max_label_width,
				   ftk_trace_label_dwidth(trace));
      }
    }
    ftk_ev_label_box_width (eventviewer)  = max_label_width;
    ftk_ev_label_box_height (eventviewer) = total_label_height;
    ftk_ev_da_height(eventviewer) = ftk_ev_label_box_height (eventviewer);
  }
  
  if ((ftk_ev_da_height(eventviewer) > (int)(dwidge->allocation.height)) ||
      (ftk_ev_da_height(eventviewer) < ((int)(dwidge->allocation.height) - 12))) {
    gtk_widget_set_size_request (GTK_WIDGET (ftk_ev_da (eventviewer)),
				 (int)(dwidge->allocation.width),
				 ftk_ev_da_height(eventviewer));
  }
  
  {
    cairo_t * cr = gdk_cairo_create (dwidge->window);

    int dww = (int)(dwidge->allocation.width);
    int dwh = (int)(dwidge->allocation.height);
  
    cairo_rectangle (cr, 0, 0, dww, dwh);
    cairo_set_source_rgb (cr,
			  (double)ftk_ev_bg_red (eventviewer)/65535.0,
			  (double)ftk_ev_bg_green (eventviewer)/65535.0,
			  (double)ftk_ev_bg_blue (eventviewer)/65535.0);
			  
    cairo_fill_preserve (cr);
    cairo_clip (cr);
    cairo_stroke (cr);

    {		/* draw labels and baselines */
      int i;

#define LABEL_GAP 10
      ftk_ev_trace_origin (eventviewer) =
	ftk_ev_label_box_width (eventviewer) + LABEL_GAP;
      ftk_ev_trace_width (eventviewer)  =
	(dww - LABEL_GAP) - ftk_ev_trace_origin (eventviewer);

      for (i = 0; i < ftk_ev_trace_order_next(eventviewer); i++) {
	ftk_trace_s * trace = ftk_ev_trace(eventviewer,
					   ftk_ev_trace_order_ety (eventviewer, i));
	if (ftk_trace_valid (trace)) {
	  cairo_set_source_rgb (cr,
				(double)(ftk_trace_color_red (trace))/(double)65535,
				(double)(ftk_trace_color_green (trace))/(double)65535,
				(double)(ftk_trace_color_blue (trace))/(double)65535);
	  cairo_move_to (cr,
			 (double)(ftk_ev_label_box_width (eventviewer) -
				  ftk_trace_label_dwidth (trace)),
			 (double)(ftk_trace_vpos_d(trace)));
	  pango_cairo_show_layout (cr, ftk_trace_label(trace));
	  cairo_stroke (cr);

	  {
	    double vp = (double)((ftk_trace_label_dheight(trace) >> 1) +
				 ftk_trace_vpos_d(trace));

	    if ((0.0 < ftk_trace_linewidth (trace)) ||
		(0.0 < ftk_trace_linestyle (trace))) {
	      cairo_save (cr);
	    
	      if (0.0 < ftk_trace_linewidth (trace))
		cairo_set_line_width (cr, ftk_trace_linewidth (trace));
	    
	      if (0.0 < ftk_trace_linestyle (trace))
		cairo_set_dash (cr, &ftk_trace_linestyle (trace), 1, 0.0);
	    }
	    
	    cairo_move_to (cr, (double)(ftk_ev_trace_origin (eventviewer)),
			   vp);
	    cairo_line_to (cr, (double)(ftk_ev_trace_origin (eventviewer) +
					ftk_ev_trace_width (eventviewer)), vp);
	  
	    cairo_stroke (cr);
	  
	    if (0.0 < ftk_trace_linewidth (trace)) cairo_restore (cr);
	  }
	

	}
      }
    }

    cairo_rectangle (cr,
		     ftk_ev_trace_origin (eventviewer),
		     0,
		     ftk_ev_trace_width (eventviewer),
		     dwh);
    cairo_clip (cr);

    {				/* draw points */
      int i,j;

    for (i = 0; i < ftk_ev_trace_order_next(eventviewer); i++) {
      ftk_trace_s * trace = ftk_ev_trace(eventviewer,
					 ftk_ev_trace_order_ety (eventviewer, i));
      if (!ftk_trace_valid (trace)) continue;
	
	for (j = 0; j < ftk_trace_event_next(trace); j++) {
	  ftk_event_s * event = ftk_trace_event (trace, j);
	
	  draw_cairo_point (eventviewer, cr, trace, event, FALSE);
	}
      }
    }
    
    {				/* draw links */
      int i;

      for (i = 0; i < ftk_ev_link_next(eventviewer); i++) {
	ftk_link_s * link = ftk_ev_link (eventviewer, i);
	
	draw_link (eventviewer, cr, link, FALSE);
      }
    }
    {				/* draw dlinks */
      int i;

      for (i = 0; i < ftk_ev_dlink_next(eventviewer); i++) {
	ftk_dlink_s * dlink = ftk_ev_dlink (eventviewer, i);
	
	draw_dlink (eventviewer, cr, dlink, FALSE);
      }
    }

    
    cairo_destroy (cr);
  }

  return FALSE;		/* other handlers ? */
}

static gboolean
ftk_eventviewer_expose(GtkWidget * widget, GdkEventExpose * event,
		       gpointer data)
{
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (widget), FALSE);
  //  g_return_val_if_fail (event != NULL, FALSE);

  if (GTK_WIDGET_DRAWABLE (widget)) {
    FtkEventViewer * eventviewer = FTK_EVENTVIEWER (widget);


    if (event) {
      gtk_container_propagate_expose (GTK_CONTAINER (eventviewer),
				      ftk_ev_hbutton_box (eventviewer),	
				      event);
      gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_legend_frame (eventviewer)),
				      event);
      gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_da_frame (eventviewer)),
				      event);
      gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_scroll (eventviewer)),
				      event);
    }

    //    draw_plot (eventviewer);
  }

  return TRUE;	/* last handler */
}

static gboolean
ftk_eventviewer_configure(GtkWidget * widget, GdkEventConfigure * event,
			     gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  
  ftk_ev_widget_modified(eventviewer) = TRUE;
  return TRUE;	/* last handler */
}

static gboolean
ftk_eventviewer_style (GtkWidget * widget, GtkStyle *previous_style,
				gpointer data)
{
	FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
	
	GtkDrawingArea *da = ftk_ev_da(eventviewer);
	GtkStyle * style = gtk_widget_get_style(GTK_WIDGET(da));
	
	GdkColor * backgrounds = style->bg;
	ftk_eventviewer_preset_bg_rgb_e(eventviewer, backgrounds->red, 
	backgrounds->green, backgrounds->blue, NULL);
		
	GdkColor * foregrounds = style->fg;
	
	for (gint i = 0; i < ftk_ev_trace_next(eventviewer); i++) {
		if (ftk_trace_valid (ftk_ev_trace (eventviewer, i))) {
			ftk_eventviewer_preset_trace_rgb_e(eventviewer, i, 
			foregrounds->red, foregrounds->green,
			 foregrounds->blue, NULL);
		}
    }

	for (gint i = 0; i < ftk_ev_markers_next(eventviewer); i++) {
		ftk_eventviewer_preset_marker_rgb_e(eventviewer, i, 
		foregrounds->red, foregrounds->green, 
		foregrounds->blue, NULL);
	}

	if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (da))) {
	    ftk_eventviewer_da_expose(GTK_WIDGET(da), NULL, eventviewer);
	}
	return TRUE;
}

static void
ftk_eventviewer_destroy(GtkObject * widget,
			gpointer data)
{
  int i;
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (widget);
  g_return_if_fail (FTK_IS_EVENTVIEWER (widget));

  for (i = 0; i < ftk_ev_trace_next(eventviewer); i++) {
    ftk_trace_s * trace = ftk_ev_trace(eventviewer, i);
      
    if (ftk_trace_label(trace)) g_object_unref (ftk_trace_label (trace));
    if (ftk_trace_string(trace)) free (ftk_trace_string (trace));
    if (ftk_trace_gc(trace)) g_object_unref (ftk_trace_gc (trace));
    if (ftk_trace_events (trace))  free (ftk_trace_events(trace));
  }
  if (ftk_ev_traces (eventviewer))  free (ftk_ev_traces(eventviewer));

  for (i = 0; i < ftk_ev_tie_next(eventviewer); i++) {
    ftk_tie_s * tie = ftk_ev_tie(eventviewer, i);

    if (ftk_tie_gc(tie)) g_object_unref (ftk_tie_gc (tie));
  }
  if (ftk_ev_ties (eventviewer))    free (ftk_ev_ties(eventviewer));

  for (i = 0; i < ftk_ev_markers_next(eventviewer); i++) {
    ftk_marker_s * marker = ftk_ev_marker(eventviewer, i);
      
    if (ftk_marker_gc(marker)) g_object_unref (ftk_marker_gc (marker));
    if (ftk_marker_string (marker)) free (ftk_marker_string (marker));
  }
  if (ftk_ev_markers (eventviewer)) free (ftk_ev_markers (eventviewer));

  for (i = 0; i < ftk_ev_link_next(eventviewer); i++) {
    ftk_link_s * link = ftk_ev_link(eventviewer, i);

    if (ftk_link_trace_list (link)) free (ftk_link_trace_list (link));
  }
  if (ftk_ev_links (eventviewer))   free (ftk_ev_links(eventviewer));

  for (i = 0; i < ftk_ev_dlink_next(eventviewer); i++) {
    ftk_dlink_s * dlink = ftk_ev_dlink(eventviewer, i);

    if (ftk_dlink_event_pair_list (dlink))
      free (ftk_dlink_event_pair_list (dlink));
  }
  if (ftk_ev_dlinks (eventviewer))  free (ftk_ev_dlinks(eventviewer));

  if (ftk_ev_color_values(eventviewer))
    free (ftk_ev_color_values(eventviewer));

  for (i = 0; i < db_symbols_count; i++) {
    /* fixme -- this causes a segv */
    //    g_object_unref (ftk_symbol_layout(i));
    g_free (ftk_symbol_utf8(i));
  }
}



#if 0
static void
ftk_eventviewer_scale_toggle(GtkToggleButton * button,
			     gpointer data)
{
  g_return_if_fail (GTK_IS_TOGGLE_BUTTON (button));

  /* fixme -- not yet implemented */
  //  gboolean active = gtk_toggle_button_get_active (button);
}
#endif

#ifndef USE_SLIDER_INTERVAL
static void
ftk_eventviewer_spin_vc (GtkSpinButton *spinbutton,
			 gpointer       user_data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);

  ftk_ev_span (eventviewer) = gtk_spin_button_get_value (spinbutton);

  g_object_set (G_OBJECT (adj), "page-size", ftk_ev_span (eventviewer), NULL);
  gtk_adjustment_changed (adj);
    
  draw_plot (eventviewer);
}
#endif

static void
ftk_eventviewer_scroll_cv(GtkRange     *range,
			  GtkScrollType scroll,
			  gdouble       value,
			  gpointer      user_data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  GtkAdjustment * adj = gtk_range_get_adjustment (range);
  gtk_adjustment_set_value (adj, value);

  ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
}

static gint
do_append (FtkEventViewer * eventviewer,
	   gint trace_index,
	   gint marker,
	   gchar * string,
	   double now_d)
{
  ftk_event_s * event;
  int event_nr = -1;
  ftk_trace_s * trace = ftk_ev_trace (eventviewer, trace_index);

#define FTK_TRACE_EVENT_INCR	64
  {
    if (ftk_trace_event_next(trace) >= ftk_trace_event_max(trace)) {
      ftk_trace_event_max(trace) += FTK_TRACE_EVENT_INCR;
      ftk_trace_events(trace)
	= realloc (ftk_trace_events(trace),
		   ftk_trace_event_max(trace) * sizeof(ftk_event_s));
    }
    event_nr = ftk_trace_event_next(trace)++;
    event = ftk_trace_event (trace, event_nr);
  }

  ftk_event_marker (event) = marker;
  ftk_event_string (event) = string ? strdup (string) : NULL;
  ftk_event_time (event) = ftk_ev_now(eventviewer) = now_d;
  ftk_event_loc (event) = -1;
  
  ftk_trace_min_time (trace) = fmin (ftk_trace_min_time (trace), now_d);
  ftk_trace_max_time (trace) = fmax (ftk_trace_max_time (trace), now_d);
  ftk_trace_time_set (trace) = TRUE;
  
  ftk_ev_min_time (eventviewer) = fmin (ftk_ev_min_time (eventviewer), now_d);
  ftk_ev_max_time (eventviewer) = fmax (ftk_ev_max_time (eventviewer), now_d);
  ftk_ev_time_set (eventviewer) = TRUE;
  
  {
    gdouble upper;
    gdouble nt = ftk_event_time (event) - ftk_ev_zero (eventviewer);
    GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
    
    g_object_get (G_OBJECT (adj), "upper", &upper, NULL);
    if (upper < nt) {
      g_object_set (G_OBJECT (adj),
		    "upper", nt,
		    NULL);
      gtk_adjustment_changed (adj);
      if (!gtk_toggle_button_get_active ( GTK_TOGGLE_BUTTON (ftk_ev_hold_toggle_button(eventviewer))))
	gtk_adjustment_set_value (adj, nt - ftk_ev_span (eventviewer));
    }
  }

   if (GTK_WIDGET_DRAWABLE (eventviewer))
     draw_cairo_point (eventviewer, NULL, trace, event, TRUE);

   return event_nr;
}

/*******************************************************/
/*                                                     */
/*                         api                         */
/*                                                     */
/*******************************************************/

GType
ftk_eventviewer_get_type ()
{
  static GType eventviewer_type = 0;

  if (!eventviewer_type) {
    static const GTypeInfo eventviewer_info = {
      sizeof (FtkEventViewerClass),		/* class_size		*/
      NULL, 					/* base_init		*/
      NULL,					/* base_finalize	*/
      (GClassInitFunc) ftk_eventviewer_class_init,	/* class_init	*/
      NULL, 					/* class_finalize	*/
      NULL, 					/* class_data		*/
      sizeof (FtkEventViewer),			/* instance size	*/
      0,					/* n_preallocs		*/
      (GInstanceInitFunc) ftk_eventviewer_init,	/* instance_init	*/
    };

    eventviewer_type = g_type_register_static (GTK_TYPE_VBOX,
					       "Gtk_EventViewer",
					       &eventviewer_info, 0);
  }

  return eventviewer_type;
}

GtkWidget*
ftk_eventviewer_new ()
{
  FtkEventViewer * eventviewer
    = g_object_new (ftk_eventviewer_get_type (),
		    NULL);

  
  
  return GTK_WIDGET (eventviewer);
}


/*
 *
 *	resize the widget
 *
 */

/**
 * ftk_eventviewer_new creates and initialises new
 * eventviewer instance.  The returned GtkWidget pointer is in fact a
 * pointer to a FtkEventViewer structure which should be considered
 * opaque.  The returned widget pointer should be cast to an FtkEventViewer
 * pointer using the FTK_EVENTVIEWER() macro and used as the first
 * argument to all of the following functions.
 */


gboolean
ftk_eventviewer_resize_e (FtkEventViewer * eventviewer,
			  gint width, gint height,
			  GError ** err)
{
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }

  gtk_widget_set_size_request (GTK_WIDGET (eventviewer), width, height);

  ftk_ev_widget_modified (eventviewer) = TRUE;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);

  return TRUE;
}

gboolean
ftk_eventviewer_resize (FtkEventViewer * eventviewer,
			gint width, gint height)
{
  return ftk_eventviewer_resize_e (eventviewer, width,  height, NULL);
}


/*
 *
 *	setting bg rgb
 *
 */
 
gboolean
ftk_eventviewer_preset_bg_rgb_e (FtkEventViewer * eventviewer, guint red, guint green, guint blue, GError ** err)
{
 if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkEventViewer color.");
    return FALSE;
  }
  
  /*XXX: Remove Print statement. */
  fprintf(stderr, "Background Color red: %d, green: %d blue: %d\n", red, green, blue);

  ftk_ev_bg_red(eventviewer)		= red;
  ftk_ev_bg_green(eventviewer)		= green;
  ftk_ev_bg_blue(eventviewer)		= blue;	
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_bg_rgb_e (FtkEventViewer * eventviewer,
			      guint red, guint green, guint blue,
			      GError ** err)
{
  if (!ftk_eventviewer_preset_bg_rgb_e (eventviewer, red, green, blue, err)) {
  	return FALSE;
  }

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_bg_rgb (FtkEventViewer * eventviewer,
			    guint red, guint green, guint blue)
{
  return ftk_eventviewer_set_bg_rgb_e (eventviewer,
				       red, green, blue,
				       NULL);
}

gboolean
ftk_eventviewer_set_bg_color_e (FtkEventViewer * eventviewer,
				GdkColor * color, GError ** err)
{
return ftk_eventviewer_set_bg_rgb_e (eventviewer,
				       color->red,
				       color->green,
				       color->blue,
				       err);				       
}

gboolean
ftk_eventviewer_set_bg_color (FtkEventViewer * eventviewer,
			      GdkColor * color)
{
  
 return ftk_eventviewer_set_bg_color_e (eventviewer,
				       color,
				       NULL);				       
}

gboolean
ftk_eventviewer_set_bg_default(FtkEventViewer * eventviewer)
{
  
  GdkColor *backgrounds = ftk_eventviewer_get_bg_default(eventviewer);
  return ftk_eventviewer_set_bg_color (eventviewer,
				       backgrounds);				
}

GdkColor * 
ftk_eventviewer_get_bg_default(FtkEventViewer *eventviewer)
{
  gtk_widget_ensure_style(GTK_WIDGET(ftk_ev_da(eventviewer)));
  GtkStyle *style = gtk_widget_get_style(GTK_WIDGET(ftk_ev_da(eventviewer)));
  
  if (NULL == style) {
  	fprintf(stderr, "Style was null");
  }
  
  GdkColor *backgrounds = style->bg;
  
  if (NULL == backgrounds) {
  	fprintf(stderr, "Backgrounds was null");
  }
  
  return backgrounds;
}

GdkColor * 
ftk_eventviewer_get_fg_default(FtkEventViewer *eventviewer)
{
  gtk_widget_ensure_style(GTK_WIDGET(ftk_ev_da(eventviewer)));
  GtkStyle *style = gtk_widget_get_style(GTK_WIDGET(ftk_ev_da(eventviewer)));
  
  if (NULL == style) {
  	fprintf(stderr, "Style was null");
  }
  
  GdkColor *foregrounds = style->fg;
  
  if (NULL == foregrounds) {
  	fprintf(stderr, "Backgrounds was null");
  }
 
  return foregrounds;
}

gboolean
ftk_eventviewer_set_timebase_e	(FtkEventViewer * eventviewer,
				 double span,
				 GError ** err)
{
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((span < MINIMUM_SPAN) || (span > MAXIMUM_SPAN)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_SPAN,	/* error code */
		 "Invalid FtkEventViewer timebase span.");
    return FALSE;
  }
  
  ftk_ev_span(eventviewer) = span;
#ifndef USE_SLIDER_INTERVAL  
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (ftk_ev_interval_button (eventviewer)),
			     span);
#endif

#ifdef USE_SLIDER_INTERVAL  
  gtk_range_set_value (GTK_RANGE (ftk_ev_interval_scale (eventviewer)), log10 (span));
#endif

  {
    GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
    g_object_set (G_OBJECT (adj), "upper", ftk_ev_span (eventviewer), NULL);
    gtk_adjustment_changed (adj);
  }

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_timebase	(FtkEventViewer * eventviewer,
				 double span)
{
  return ftk_eventviewer_set_timebase_e	(eventviewer, span, NULL);
}

gint
ftk_eventviewer_add_trace_e (FtkEventViewer * eventviewer,
			     char * label,
			     char * string,
			     GError ** err)
{
  int tag = -1;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }

  {
    char * t_label;
    ftk_trace_s * trace = NULL;
#define FTK_EV_TRACE_INCR	8

    if (ftk_ev_trace_pool (eventviewer) &&
	(0 < ftk_ev_trace_pool_next (eventviewer))) {
      tag = ftk_ev_trace_pool_ety(eventviewer,
				  --ftk_ev_trace_pool_next (eventviewer ));
    }
    if (-1 == tag) {
      if (ftk_ev_trace_max(eventviewer) <= ftk_ev_trace_next(eventviewer)) {
	ftk_ev_trace_max(eventviewer) += FTK_EV_TRACE_INCR;
	ftk_ev_traces(eventviewer)
	  = realloc (ftk_ev_traces(eventviewer),
		     ftk_ev_trace_max(eventviewer) * sizeof(ftk_trace_s));
      }
      tag = ftk_ev_trace_next(eventviewer)++;
    }

    if (ftk_ev_trace_order_next (eventviewer) >= ftk_ev_trace_order_max (eventviewer)) {
#define FTK_TRACE_ORDER_INCR 4
      ftk_ev_trace_order_max (eventviewer) += FTK_TRACE_ORDER_INCR;
      ftk_ev_trace_order(eventviewer)
	= realloc (ftk_ev_trace_order(eventviewer),
		   ftk_ev_trace_order_max (eventviewer) * sizeof(int));
    }
    ftk_ev_trace_order_ety(eventviewer, ftk_ev_trace_order_next(eventviewer)++) = tag;
    
    trace = ftk_ev_trace (eventviewer, tag);
    ftk_trace_gc (trace)		= NULL;
    ftk_trace_vpos_d (trace)		= 0.0;
    ftk_trace_linestyle (trace)		= -1.0;
    ftk_trace_linewidth (trace)		= -1.0;
    ftk_trace_color_red (trace)		= DEFAULT_TRACE_RED;
    ftk_trace_color_green (trace)	= DEFAULT_TRACE_GREEN;
    ftk_trace_color_blue (trace)	= DEFAULT_TRACE_BLUE;
    ftk_trace_event_next(trace) 	= 0;
    ftk_trace_event_max(trace)  	= 0;
    ftk_trace_events(trace)     	= NULL;
    ftk_trace_min_time (trace)		= HUGE_VAL;
    ftk_trace_max_time (trace)		= -HUGE_VAL;
    
    
    if (label) asprintf (&t_label, "%s :%2d", label, tag);
    else       asprintf (&t_label, ":%2d", tag);

    ftk_trace_label(trace) =
      gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
    free (t_label);
    ftk_trace_string (trace) = string ? strdup (string) : NULL;
    ftk_trace_label_modified (trace)	= TRUE;
    ftk_trace_valid (trace)    = TRUE;
    
    ftk_ev_trace_modified (eventviewer) = TRUE;

    if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
      ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
				eventviewer);
  }

  return tag;
}

gint
ftk_eventviewer_add_trace (FtkEventViewer * eventviewer,
			   char * label,
			   char * string)
{
  return ftk_eventviewer_add_trace_e (eventviewer, label, string, NULL);
}

gboolean
ftk_eventviewer_delete_trace_e	(FtkEventViewer * eventviewer,
				 gint trace_index,
				 GError ** err)
{
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  {
    int i;

    for (i = 0; i < ftk_ev_trace_order_next (eventviewer); i++) {
      if (ftk_ev_trace_order_ety (eventviewer, i) == trace_index) {
	if (i < (ftk_ev_trace_order_next (eventviewer) - 1)) {
	  memmove (&ftk_ev_trace_order_ety (eventviewer, i),
		   &ftk_ev_trace_order_ety (eventviewer, i + 1),
		   ((ftk_ev_trace_order_next(eventviewer) - i) - 1) *
		   sizeof(int));
	  ftk_ev_trace_order_next (eventviewer)--;
	  i--;
	}
      }
    }
  }

  {
    ftk_trace_s * trace = ftk_ev_trace (eventviewer, trace_index);

    if (ftk_trace_label(trace)) {
      g_object_unref (ftk_trace_label (trace));
      ftk_trace_label (trace) = NULL;
    }
    if (ftk_trace_string(trace)) {
      free (ftk_trace_string (trace));
      ftk_trace_string (trace) = NULL;
    }
    if (ftk_trace_gc(trace)) {
      g_object_unref (ftk_trace_gc (trace));
      ftk_trace_gc (trace) = NULL;
    }
    if (ftk_trace_events(trace)) {
      free (ftk_trace_events (trace));
      ftk_trace_events (trace) = NULL;
    }
    ftk_trace_event_max (trace) = 0;
    ftk_trace_event_next (trace) = 0;
    ftk_trace_valid (trace) = FALSE;

    if (ftk_ev_trace_pool_next(eventviewer) >=
	ftk_ev_trace_pool_max(eventviewer)) {
#define FTK_EVENt_POOL_INCR 4
      ftk_ev_trace_pool_max(eventviewer) += FTK_EVENt_POOL_INCR;
      ftk_ev_trace_pool(eventviewer) =
	realloc (ftk_ev_trace_pool(eventviewer),
		 ftk_ev_trace_pool_max(eventviewer) * sizeof(int));
    }
    ftk_ev_trace_pool_ety(eventviewer, ftk_ev_trace_pool_next(eventviewer)++) =
      trace_index;
  }

  /* delete ftk_ev_dlinks(eventviewer) */
  if (ftk_ev_dlinks (eventviewer)) {
    int i;

    for (i = 0; i < ftk_ev_dlink_next (eventviewer); i++) {
      ftk_dlink_s * dlink = ftk_ev_dlink (eventviewer, i);

      if (ftk_dlink_event_pair_list(dlink)) {
	int j;

	for (j = 0; j < ftk_dlink_event_list_next(dlink); j++) {
	  int ti = ftk_dlink_event_pair_trace(dlink, j);
	  if (trace_index == ti) {
	    if (j < (ftk_dlink_event_list_next (dlink) - 1))
	      memmove (&ftk_dlink_event_pair(dlink, j),
		       &ftk_dlink_event_pair(dlink, j+1),
		       ((ftk_dlink_event_list_next(dlink) - j) - 1) *
		       sizeof(ftk_event_pair_s));
	    ftk_dlink_event_list_next (dlink)--;
	    j--;
	  }
	}
      }

      if (1 >= ftk_dlink_event_list_next (dlink)) {
	if (ftk_dlink_event_pair_list(dlink))
	  free(ftk_dlink_event_pair_list(dlink));
	if (i < (ftk_ev_dlink_next (eventviewer) - 1)) 
	  memmove (ftk_ev_dlink(eventviewer, i),
		   ftk_ev_dlink(eventviewer, i+1),
		   (ftk_ev_dlink_next (eventviewer) - 1)
		   * sizeof(ftk_dlink_s));
	ftk_ev_dlink_next (eventviewer)--;
      }
      
    }
  }
  
  /* delete ftk_ev_links(eventviewer) */
  if (ftk_ev_links (eventviewer)) {
    int i;

    for (i = 0; i < ftk_ev_link_next (eventviewer); i++) {
      ftk_link_s * link = ftk_ev_link (eventviewer, i);

      if (ftk_link_trace_list(link)) {
	int j;

	for (j = 0; j < ftk_link_trace_list_next(link); j++) {
	  int ti = ftk_link_trace(link, j);
	  if (trace_index == ti) {
	    if (j < (ftk_link_trace_list_next (link) - 1))
	      memmove (&ftk_link_trace(link, j),
		       &ftk_link_trace(link, j+1),
		       ((ftk_link_trace_list_next(link) - j) - 1) *
		       sizeof(gint));
	    ftk_link_trace_list_next (link)--;
	    j--;
	  }
	}
      }

      if (1 >= ftk_link_trace_list_next (link)) {
	if (ftk_link_trace_list(link)) free(ftk_link_trace_list(link));
	if (i < (ftk_ev_link_next (eventviewer) - 1)) 
	  memmove (ftk_ev_link(eventviewer, i),
		   ftk_ev_link(eventviewer, i+1),
		   (ftk_ev_link_next (eventviewer) - 1) * sizeof(ftk_link_s));
	ftk_ev_link_next (eventviewer)--;
      }
    }
  }

  ftk_ev_trace_modified(eventviewer) = TRUE;
  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)),
			      NULL, eventviewer);

  return TRUE;
}

gboolean
ftk_eventviewer_delete_trace	(FtkEventViewer * eventviewer,
				 gint trace_idx)
{
  return ftk_eventviewer_delete_trace_e	(eventviewer, trace_idx, NULL);
}

/*
 *
 *	setting trace rgb
 *
 */
 
gboolean
ftk_eventviewer_preset_trace_rgb_e(FtkEventViewer * eventviewer,
				gint trace_index, guint red, guint green, guint blue,
				GError ** err)
{
	ftk_trace_s * trace;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkEventViewer color.");
    return FALSE;
  }

  trace = ftk_ev_trace (eventviewer, trace_index);
  ftk_trace_color_red (trace)		= red;
  ftk_trace_color_green (trace)		= green;
  ftk_trace_color_blue (trace)		= blue;
  ftk_ev_trace_modified (eventviewer)	= TRUE;
	
  return TRUE;
}

gboolean
ftk_eventviewer_set_trace_rgb_e (FtkEventViewer * eventviewer,
				 gint trace_index,
				 guint red, guint green, guint blue,
				 GError ** err)
{
  if (!ftk_eventviewer_preset_trace_rgb_e (eventviewer, trace_index, red, green, blue, err)) {
  	return FALSE;
  }
  
  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_trace_rgb (FtkEventViewer * eventviewer,
			       gint trace,
			       guint red, guint green, guint blue)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer, trace,
					  red, green, blue,
					  NULL);
}

gboolean
ftk_eventviewer_set_trace_color_e (FtkEventViewer * eventviewer,
				   gint trace,
				   GdkColor * color, GError ** err)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer,
					  trace,
					  color->red,
					  color->green,
					  color->blue,
					  err);
}

gboolean
ftk_eventviewer_set_trace_color (FtkEventViewer * eventviewer,
				 gint trace,
				 GdkColor * color)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer,
					  trace,
					  color->red,
					  color->green,
					  color->blue,
					  NULL);
}

/*
 *
 *	setting trace label
 *
 */

gboolean
ftk_eventviewer_set_trace_label_e (FtkEventViewer * eventviewer,
				   gint trace_index,
				   char * label,
				   GError ** err)
{
  ftk_trace_s * trace;
  char * t_label;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  trace = ftk_ev_trace (eventviewer, trace_index);
  if (label) asprintf (&t_label, "%s :%2d", label, trace_index);
  else       asprintf (&t_label, ":%2d", trace_index);
  ftk_trace_label_modified (trace)	= TRUE;
  ftk_ev_trace_modified (eventviewer)	= TRUE;
  if (ftk_trace_label(trace)) g_object_unref (ftk_trace_label(trace));
  ftk_trace_label(trace) =
    gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
  free (t_label);

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_trace_label (FtkEventViewer * eventviewer,
				 gint trace,
				 char * label)
{
  return ftk_eventviewer_set_trace_label_e (eventviewer, trace,
					    label,
					    NULL);
}


/*
 *
 *	setting trace linestyle
 *
 */

gboolean
ftk_eventviewer_set_trace_linestyle_e (FtkEventViewer * eventviewer,
				       gint trace_index,
				       gint lw,
				       gint ls,
				       GError ** err)
{
  ftk_trace_s * trace;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }
  
  trace = ftk_ev_trace (eventviewer, trace_index);
  ftk_trace_linestyle (trace)		= (double)ls;
  ftk_trace_linewidth (trace)		= (double)lw;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_trace_linestyle (FtkEventViewer * eventviewer,
				     gint trace,
				     gint lw,
				     gint ls)
{
  return ftk_eventviewer_set_trace_linestyle_e (eventviewer, trace,
					  lw, ls,
					  NULL);
}

/*
 * Create a new marker
 */

gint
ftk_eventviewer_marker_new_e (FtkEventViewer * eventviewer,
			      FtkGlyph glyph,
			      char * label,
			      char * string,
			      GError ** err)
{
  ftk_marker_s * marker;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }
  
  if ((glyph != FTK_GLYPH_AUTOMATIC) && ((glyph < 0) || (glyph >= FTK_GLYPH_LAST))) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_EV_ERROR_INVALID_GLYPH,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return -1;
  }

#define FTK_MARKER_INCR 8
  
  if (ftk_ev_markers_max (eventviewer) <= ftk_ev_markers_next (eventviewer)) {
    ftk_ev_markers_max (eventviewer) += FTK_MARKER_INCR;
    ftk_ev_markers (eventviewer)
      = realloc (ftk_ev_markers (eventviewer),
		 ftk_ev_markers_max (eventviewer) * sizeof(ftk_marker_s));
  }
  
  marker = ftk_ev_marker (eventviewer, ftk_ev_markers_next (eventviewer));
  ftk_marker_gc (marker)    = NULL;
  ftk_marker_label (marker) =
    gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), label);
  ftk_marker_label_modified(marker) = TRUE;
  if (glyph == FTK_GLYPH_AUTOMATIC) {
    int ci = ftk_ev_next_color (eventviewer)++;
    ftk_ev_next_color (eventviewer) %= colors_count;

    ftk_marker_glyph (marker) = ftk_ev_next_glyph (eventviewer)++;
    ftk_ev_next_glyph (eventviewer) %= db_symbols_count;

    ftk_marker_color (marker) = *ftk_ev_color_value(eventviewer, ci);
  }
  else {
    ftk_marker_glyph (marker) = glyph;
    ftk_marker_color_red (marker)   = 0;
    ftk_marker_color_green (marker) = 0;
    ftk_marker_color_blue (marker)  = 0;
  }
  
  pango_layout_get_pixel_size (ftk_marker_label(marker),
			       &ftk_marker_label_width(marker),
			       &ftk_marker_label_height(marker));

  ftk_marker_string (marker) = string ? strdup (string) : NULL;
  ftk_ev_markers_modified (eventviewer) = TRUE;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_legend_da_expose(GTK_WIDGET(ftk_ev_legend_da(eventviewer)),
				     NULL, eventviewer);

  return ftk_ev_markers_next (eventviewer)++;
}

gint
ftk_eventviewer_marker_new (FtkEventViewer * eventviewer,
			    FtkGlyph glyph,
			    char * label,
			    char * string)
{
  return ftk_eventviewer_marker_new_e (eventviewer, glyph, label, string, NULL);
}

/*
 *
 *	setting marker rgb
 *
 */
 
gboolean
ftk_eventviewer_preset_marker_rgb_e (FtkEventViewer * eventviewer,
				  gint marker_index,
				  guint red, guint green, guint blue,
				  GError ** err)
{
 ftk_marker_s * marker;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((marker_index < 0) ||
      (marker_index >= ftk_ev_markers_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkEventViewer color.");
    return FALSE;
  }

  marker = ftk_ev_marker (eventviewer, marker_index);
  ftk_marker_color_red (marker)		= red;
  ftk_marker_color_green (marker)	= green;
  ftk_marker_color_blue (marker)	= blue;
  ftk_ev_markers_modified (eventviewer)	= TRUE;
	
  return TRUE;
}

gboolean
ftk_eventviewer_set_marker_rgb_e (FtkEventViewer * eventviewer,
				  gint marker_index,
				  guint red, guint green, guint blue,
				  GError ** err)
{
 
 if (!ftk_eventviewer_preset_marker_rgb_e(eventviewer, marker_index, red, green, blue, err)) {
 	return FALSE;
 }
 
 if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_marker_rgb (FtkEventViewer * eventviewer,
				gint marker,
				guint red, guint green, guint blue)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer, marker,
					   red, green, blue,
					   NULL);
}

gboolean
ftk_eventviewer_set_marker_color_e (FtkEventViewer * eventviewer,
				    gint marker,
				    GdkColor * color, GError ** err)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer,
					   marker,
					   color->red,
					   color->green,
					   color->blue,
					   err);
}

gboolean
ftk_eventviewer_set_marker_color (FtkEventViewer * eventviewer,
				  gint marker,
				  GdkColor * color)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer,
					   marker,
					   color->red,
					   color->green,
					   color->blue,
					   NULL);
}

GdkColor *
ftk_eventviewer_get_marker_color_e	(FtkEventViewer * eventviewer,
					 gint marker_index,
					 GError ** err)
{
  GdkColor * color;
  ftk_marker_s * marker;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return NULL;
  }
  
  if ((marker_index < 0) ||
      (marker_index >= ftk_ev_markers_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return NULL;
  }
  
  marker = ftk_ev_marker (eventviewer, marker_index);
  color = g_malloc (sizeof(GdkColor));
  memcpy (color, &ftk_marker_color (marker), sizeof(GdkColor));

  return color;
}

GdkColor *
ftk_eventviewer_get_marker_color	(FtkEventViewer * eventviewer,
					 gint marker)
{
  return ftk_eventviewer_get_marker_color_e (eventviewer, marker, NULL);
}







gint
ftk_eventviewer_tie_new_e (FtkEventViewer * eventviewer,
#ifdef USE_TIE_LABEL
			   char * label,
#endif
			   GError ** err)
{
  int tag = -1;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }

  {
#ifdef USE_TIE_LABEL
    char * t_label;
#endif
    ftk_tie_s * tie;
#define FTK_EV_TIE_INCR	8
    
    if (ftk_ev_tie_max(eventviewer) <= ftk_ev_tie_next(eventviewer)) {
      ftk_ev_tie_max(eventviewer) += FTK_EV_TIE_INCR;
      ftk_ev_ties(eventviewer)
	= realloc (ftk_ev_ties(eventviewer),
		   ftk_ev_tie_max(eventviewer) * sizeof(ftk_tie_s));
    }
    tie = ftk_ev_tie (eventviewer, tag = ftk_ev_tie_next(eventviewer)++);
    ftk_tie_gc (tie)		= NULL;
    ftk_tie_vpos_d (tie)	= 0.0;
    ftk_tie_linestyle (tie)		= -1.0;
    ftk_tie_linewidth (tie)		= -1.0;
    ftk_tie_color_red (tie)		= DEFAULT_TIE_RED;
    ftk_tie_color_green (tie)		= DEFAULT_TIE_GREEN;
    ftk_tie_color_blue (tie)		= DEFAULT_TIE_BLUE;
    
#ifdef USE_TIE_LABEL
    if (label) asprintf (&t_label, "%s :%2d", label, tag);
    else       asprintf (&t_label, ":%2d", tag);
      
    ftk_tie_label(tie) =
      gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
    free (t_label);
    ftk_tie_label_modified (tie)	= TRUE;
#else
    ftk_tie_label(tie) = NULL;
#endif
    
    ftk_ev_tie_modified (eventviewer) = TRUE;
  }

  return tag;
}

gint
ftk_eventviewer_tie_new (FtkEventViewer * eventviewer
#ifdef USE_TIE_LABEL
			 , char * label
#endif
			 )
{
#ifdef USE_TIE_LABEL
  return ftk_eventviewer_tie_new_e (eventviewer, label, NULL);
#else
  return ftk_eventviewer_tie_new_e (eventviewer, NULL);
#endif
}

/*
 *
 *	setting tie rgb
 *
 */

gboolean
ftk_eventviewer_set_tie_rgb_e (FtkEventViewer * eventviewer,
				 gint tie_index,
				 guint red, guint green, guint blue,
				 GError ** err)
{
  ftk_tie_s * tie;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((tie_index < 0) || (tie_index >= ftk_ev_tie_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TIE,	/* error code */
		 "Invalid FtkEventViewer tie.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkEventViewer color.");
    return FALSE;
  }

  tie = ftk_ev_tie (eventviewer, tie_index);
  ftk_tie_color_red (tie)		= red;
  ftk_tie_color_green (tie)		= green;
  ftk_tie_color_blue (tie)		= blue;
  ftk_ev_tie_modified (eventviewer)	= TRUE;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_tie_rgb (FtkEventViewer * eventviewer,
			       gint tie,
			       guint red, guint green, guint blue)
{
  return ftk_eventviewer_set_tie_rgb_e (eventviewer, tie,
					  red, green, blue,
					  NULL);
}

gboolean
ftk_eventviewer_set_tie_color_e (FtkEventViewer * eventviewer,
				   gint tie,
				   GdkColor * color, GError ** err)
{
  return ftk_eventviewer_set_tie_rgb_e (eventviewer,
					  tie,
					  color->red,
					  color->green,
					  color->blue,
					  err);
}

gboolean
ftk_eventviewer_set_tie_color (FtkEventViewer * eventviewer,
				 gint tie,
				 GdkColor * color)
{
  return ftk_eventviewer_set_tie_rgb_e (eventviewer,
					  tie,
					  color->red,
					  color->green,
					  color->blue,
					  NULL);
}

#ifdef USE_TIE_LABEL
/*
 *
 *	setting tie label
 *
 */

gboolean
ftk_eventviewer_set_tie_label_e (FtkEventViewer * eventviewer,
				   gint tie_index,
				   char * label,
				   GError ** err)
{
  ftk_tie_s * tie;
  char * t_label;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((tie_index < 0) || (tie_index >= ftk_ev_tie_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TIE,	/* error code */
		 "Invalid FtkEventViewer tie.");
    return FALSE;
  }

  tie = ftk_ev_tie (eventviewer, tie_index);
  if (label) asprintf (&t_label, "%s :%2d", label, tie_index);
  else       asprintf (&t_label, ":%2d", tie_index);
  ftk_ev_tie_modified (eventviewer)	= TRUE;
  if (ftk_tie_label(tie)) g_object_unref (ftk_tie_label(tie));
  ftk_tie_label(tie) =
    gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
  free (t_label);
  ftk_tie_label_modified (tie)	= TRUE;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_tie_label (FtkEventViewer * eventviewer,
				 gint tie,
				 char * label)
{
  return ftk_eventviewer_set_tie_label_e (eventviewer, tie,
					    label,
					    NULL);
}
#endif  /* USE_TIE_LABEL */


/*
 *
 *	setting tie linestyle
 *
 */

gboolean
ftk_eventviewer_set_tie_linestyle_e (FtkEventViewer * eventviewer,
				     gint tie_index,
				     gint lw,
				     gint ls,
				     GError ** err)
{
  ftk_tie_s * tie;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((tie_index < 0) || (tie_index >= ftk_ev_tie_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TIE,	/* error code */
		 "Invalid FtkEventViewer tie.");
    return FALSE;
  }

  tie = ftk_ev_tie (eventviewer, tie_index);
  ftk_tie_linewidth (tie)		= (double)lw;
  ftk_tie_linestyle (tie)		= (double)ls;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_tie_linestyle (FtkEventViewer * eventviewer,
				     gint tie,
				     gint lw,
				     gint ls)
{
  return ftk_eventviewer_set_tie_linestyle_e (eventviewer, tie,
					  lw, ls,
					  NULL);
}

gint
ftk_eventviewer_append_event_e (FtkEventViewer * eventviewer,
				gint trace_index,
				gint marker,
				gchar * string,
				GError ** err)
{
  int rc = -1;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return -1;
  }

  if ((marker < 0) || (marker >= ftk_ev_markers_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return -1;
  }

  {
    struct timeval now;
   
    gettimeofday (&now, NULL);

    rc = do_append (eventviewer, trace_index, marker, string,
		    timeval_to_double (&now));
  }

  return rc;
}

gint
ftk_eventviewer_append_event   (FtkEventViewer * eventviewer,
				gint trace,
				gint marker,
				gchar * string)
{
  return ftk_eventviewer_append_event_e (eventviewer, trace, marker,
					 string, NULL);
}

static gboolean
do_simultaneous_append (FtkEventViewer * eventviewer,
			double now_d,
			ftk_link_s * link,
			gint trace_index,
			gint marker_index,
			gchar *string,
			GError ** err)
{
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  if ((marker_index < 0) ||
      (marker_index >= ftk_ev_markers_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return FALSE;
  }

  do_append (eventviewer, trace_index, marker_index, string, now_d);
  if (link) {
    if (ftk_link_trace_list_next(link) >=
	ftk_link_trace_list_max(link)) {
#define FTK_LINK_TRACE_INCR	4
      ftk_link_trace_list_max(link) +=  FTK_LINK_TRACE_INCR;
      ftk_link_trace_list(link) =
	realloc (ftk_link_trace_list(link),
		 ftk_link_trace_list_max(link) * sizeof(int));
    }
    ftk_link_trace(link, ftk_link_trace_list_next(link)++) =
      trace_index;
  }
  return TRUE;
}

static ftk_link_s *
create_link(FtkEventViewer * eventviewer, gint tie_index, double now_d)
{
  static ftk_link_s * link;
  
  if (ftk_ev_link_next(eventviewer) >= ftk_ev_link_max(eventviewer)) {
#define FTK_EV_LINK_INCR	4
    ftk_ev_link_max(eventviewer) +=  FTK_EV_LINK_INCR;
    ftk_ev_links(eventviewer) =
      realloc (ftk_ev_links(eventviewer),
	       ftk_ev_link_max(eventviewer) * sizeof(ftk_link_s));
  }
  link = ftk_ev_link(eventviewer, ftk_ev_link_next(eventviewer)++);
  ftk_link_when(link)			= now_d;
  ftk_link_tie_index(link)		= tie_index;
  ftk_link_trace_list(link)		= NULL;
  ftk_link_trace_list_next(link)	= 0;
  ftk_link_trace_list_max(link)	= 0;

  return link;
}

static gboolean
do_simultaneous (FtkEventViewer * eventviewer, gint tie_index, 
		 GError ** err, va_list ap)
{
  gboolean rc = TRUE;
  struct timeval now;
  double now_d;
  ftk_link_s * link;
   
  gettimeofday (&now, NULL);
  now_d = timeval_to_double (&now);
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }

  link = (-1 != tie_index) ? create_link(eventviewer, tie_index, now_d) : NULL;

  while(1) {
    gint trace_index;
    gint marker_index;
    gchar * string;

    trace_index = va_arg (ap, int);
    if (-1 == trace_index) break;
    
    marker_index = va_arg (ap, int);
    string       = va_arg (ap, char *);

    rc = do_simultaneous_append (eventviewer,
				 now_d,
				 link,
				 trace_index,
				 marker_index,
				 string,
				 err);
    if (FALSE == rc) break;
  }

  if (link && GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    draw_link (eventviewer, NULL, link, TRUE);

  return rc;
}

gboolean
ftk_eventviewer_append_simultaneous_events (FtkEventViewer * eventviewer,
					    gint tie_index, ...)
{

  gboolean rc;
  va_list ap;
  
  va_start (ap, tie_index);
  rc = do_simultaneous (eventviewer, tie_index, NULL, ap);
  va_end (ap);

  return rc;
}

gboolean
ftk_eventviewer_append_simultaneous_events_e (FtkEventViewer * eventviewer,
					      gint tie_index,
					      GError ** err, ...)
{

  gboolean rc;
  va_list ap;
  
  va_start (ap, err);
  rc = do_simultaneous (eventviewer, tie_index, err, ap);
  va_end (ap);

  return rc;
}

gboolean
ftk_eventviewer_append_simultaneous_event_array_e (FtkEventViewer * eventviewer,
						   gint tie_index,
						   gint array_count,
						   ftk_simultaneous_events_s * events_array,
						   GError ** err)
{
  int i;
  struct timeval now;
  double now_d;
  ftk_link_s * link;
  gboolean rc = TRUE;
   
  gettimeofday (&now, NULL);
  now_d = timeval_to_double (&now);
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  link = (-1 != tie_index) ? create_link(eventviewer, tie_index, now_d) : NULL;

  for (i = 0; i <  array_count; i++) {
    rc = do_simultaneous_append (eventviewer,
				 now_d,
				 link,
				 events_array[i].trace,
				 events_array[i].marker,
				 events_array[i].string,
				 err);
    if (FALSE == rc) break;
  }

  return rc;
}

gboolean
ftk_eventviewer_append_simultaneous_event_array (FtkEventViewer * eventviewer,
						 gint tie_index,
						 gint array_count,
						 ftk_simultaneous_events_s * events_array)
{
  return ftk_eventviewer_append_simultaneous_event_array_e (eventviewer,
							    tie_index,
							    array_count,
							    events_array,
							    NULL);
}

static gboolean
do_event_dlink (FtkEventViewer * eventviewer, ftk_dlink_s * dlink,
		ftk_event_pair_s * event_pair, GError ** err)
{
  gint trace_index = event_pair->trace_idx;
  gint event_index = event_pair->event_idx;
  ftk_trace_s * trace;

  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer)) ||
      !ftk_trace_valid (ftk_ev_trace (eventviewer, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }
  
  trace = ftk_ev_trace (eventviewer, trace_index);
  
  if ((event_index < 0) ||
      (event_index >= ftk_trace_event_next (trace))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT,	/* error code */
		 "Invalid FtkEventViewer event.");
    return FALSE;
  }

  if (ftk_dlink_event_list_next(dlink) >=
      ftk_dlink_event_list_max(dlink)) {
#define FTK_LINK_EVENT_INCR	4
    ftk_dlink_event_list_max(dlink) +=  FTK_LINK_EVENT_INCR;
    ftk_dlink_event_pair_list(dlink) =
      realloc (ftk_dlink_event_pair_list(dlink),
	       ftk_dlink_event_list_max(dlink)
	       * sizeof(ftk_event_pair_s));
  }
  ftk_dlink_event_pair_trace(dlink, ftk_dlink_event_list_next(dlink)) =
    trace_index;
  ftk_dlink_event_pair_event(dlink, ftk_dlink_event_list_next(dlink)++) =
    event_index;

  return TRUE;
}

static ftk_dlink_s *
create_dlink (FtkEventViewer * eventviewer, gint tie_index)
{
  ftk_dlink_s * dlink;
  
  if (ftk_ev_dlink_next(eventviewer) >= ftk_ev_dlink_max(eventviewer)) {
#define FTK_EV_DLINK_INCR	4
    ftk_ev_dlink_max(eventviewer) +=  FTK_EV_DLINK_INCR;
    ftk_ev_dlinks(eventviewer) =
      realloc (ftk_ev_dlinks(eventviewer),
	       ftk_ev_dlink_max(eventviewer) * sizeof(ftk_dlink_s));
  }
  dlink = ftk_ev_dlink(eventviewer, ftk_ev_dlink_next(eventviewer)++);
  ftk_dlink_tie_index(dlink)		= tie_index;
  ftk_dlink_event_pair_list(dlink)	= NULL;
  ftk_dlink_event_list_next(dlink)	= 0;
  ftk_dlink_event_list_max(dlink)	= 0;

  return dlink;
}

static gboolean
do_dlink_ties (FtkEventViewer * eventviewer, gint tie_index, 
	       GError ** err, va_list ap)
{
  gboolean rc = TRUE;
  ftk_dlink_s * dlink;
   
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
   
  if (0 > tie_index) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TIE,	/* error code */
		 "Invalid FtkEventViewer tie.");
    return FALSE;
  }

  dlink = create_dlink (eventviewer, tie_index);

  while(1) {
    ftk_event_pair_s event_pair;

    event_pair.trace_idx = va_arg (ap, gint);
    if (-1 == event_pair.trace_idx) break;
    event_pair.event_idx = va_arg (ap, gint);

    rc = do_event_dlink (eventviewer, dlink, &event_pair, err);
    if (FALSE == rc) break;
  }

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer)))
    draw_dlink (eventviewer, NULL, dlink, TRUE);

  return rc;
}

gboolean
ftk_eventviewer_tie_events_e (FtkEventViewer * eventviewer,
			      gint tie_index,
			      GError ** err, ...)
{

  gboolean rc;
  va_list ap;
  
  va_start (ap, err);
  rc = do_dlink_ties (eventviewer, tie_index, err, ap);
  va_end (ap);

  return rc;
}

gboolean
ftk_eventviewer_tie_events (FtkEventViewer * eventviewer,
			    gint tie_index, ...)
{

  gboolean rc;
  va_list ap;
  
  va_start (ap, tie_index);
  rc = do_dlink_ties (eventviewer, tie_index, NULL, ap);
  va_end (ap);

  return rc;
}

gboolean
ftk_eventviewer_tie_event_array_e (FtkEventViewer * eventviewer,
				   gint tie_index,
				   gint count,
				   ftk_event_pair_s * events,
				   GError ** err)
{
  int i;
  ftk_dlink_s * dlink;
  gboolean rc = TRUE;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
   
  if (0 > tie_index) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TIE,	/* error code */
		 "Invalid FtkEventViewer tie.");
    return FALSE;
  }
  
  dlink = create_dlink (eventviewer, tie_index);

  for (i = 0; i < count; i++) {
    rc = do_event_dlink (eventviewer, dlink, &events[i], err);
    if (FALSE == rc) break;
  }

  return rc;
}

gboolean
ftk_eventviewer_tie_event_array (FtkEventViewer * eventviewer,
				 gint tie_index, gint count,
				 ftk_event_pair_s * events)
{
  return ftk_eventviewer_tie_event_array_e (eventviewer, tie_index, count,
					    events, NULL);
}
