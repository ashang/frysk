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
#include <gtk/gtkwidget.h>
#ifdef SCROLLBAR
#include <gtk/gtkmarshalers.h>
#endif
#include <cairo.h>
#include <glib-object.h>
#define DO_INITIALISE
#define INCLUDE_PRIVATE
#include "ftkeventviewer.h"

//#define USE_FTK_SIGNAL
#define _
#define I_

GQuark ftk_quark;

G_DEFINE_TYPE (FtkDrawingArea, ftk_drawing_area, GTK_TYPE_DRAWING_AREA);
G_DEFINE_TYPE (FtkLegend, ftk_legend, GTK_TYPE_DRAWING_AREA);

/************************************************************
 *                                                          *
 * 		Callback Definitions.       	                    *
 * 							                                *
 ************************************************************/
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

static gboolean ftk_eventviewer_scroll_cv (GtkRange     *range,
				       GtkScrollType scroll,
				       gdouble       value,
				       gpointer      user_data);
				       
static gboolean
ftk_eventviewer_scroll_adj_val_changed (GtkAdjustment *adjustment,
                                            gpointer       user_data);

static gboolean ftk_ev_button_press_event (GtkWidget * widget,
					   GdkEventButton * event,
					   gpointer data);
					   
static gboolean ftk_ev_third_button_press_event (GtkWidget * widget,
					   GdkEventButton * event,
					   gpointer data);					   

static gboolean ftk_ev_button_release_event (GtkWidget * widget,
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
    
    
/************************************************************
 *                                                          *
 * 		 Internal method definitions                        *
 *							                                *
 ************************************************************/
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
		
/************************************************************
 *                                                          *
 *       Time Conversion Method Definitions                 *
 *                                                          *
 ************************************************************/
static inline double timeval_to_double (struct timeval * tv);
static inline void double_to_timeval (struct timeval * tv,
				      double time_d);
				      
/************************************************************
 *                                                          *
 *       Accessibility Definitions                          *
 *                                                          *
 ************************************************************/
static AtkObject *
ftk_drawing_area_get_accessible (GtkWidget *widget);

static AtkObject *
ftk_trace_get_accessible (GtkWidget *widget);

static AtkObject *
ftk_legend_get_accessible (GtkWidget * widget);
  
/************************************************************
 *                                                          *
 *       Default Definitions                                *
 *                                                          *
 ************************************************************/
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
static void ftk_drawing_area_init (FtkDrawingArea	*da);
static void ftk_drawing_area_set_scroll_adjustments(FtkDrawingArea *da, GtkAdjustment *hadj, GtkAdjustment *vadj);

#ifdef SCROLLBAR
enum
{
  SET_SCROLL_ADJUSTMENTS,
  LAST_SCROLL_SIGNAL
};

static guint signals[LAST_SCROLL_SIGNAL] = { 0 };
#endif

GtkType ftk_drawing_area_get_type(void);	
GtkWidget* ftk_drawing_area_new();

static void
ftk_drawing_area_class_init (FtkDrawingAreaClass *klass)
{
	GtkWidgetClass * widget_class;
	
	
  
	widget_class = (GtkWidgetClass *) klass;
	widget_class->get_accessible = ftk_drawing_area_get_accessible;
	#ifdef SCROLLBAR
	GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
	
	signals[SET_SCROLL_ADJUSTMENTS] =
    g_signal_new (I_("set_scroll_adjustments"),
		  G_OBJECT_CLASS_TYPE (gobject_class),
		  G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
		  G_STRUCT_OFFSET (GtkTextViewClass, set_scroll_adjustments),
		  NULL, NULL,
		  _gtk_marshal_VOID__OBJECT_OBJECT,
		  G_TYPE_NONE, 2,
		  GTK_TYPE_ADJUSTMENT,
		  GTK_TYPE_ADJUSTMENT);  
	widget_class->set_scroll_adjustments_signal = signals[SET_SCROLL_ADJUSTMENTS];
	#endif
	klass->set_scroll_adjustments = ftk_drawing_area_set_scroll_adjustments;
}		

static void
ftk_drawing_area_init (FtkDrawingArea * da)
{
	ftk_da_trace_pool(da)      = NULL;
  ftk_da_trace_pool_next(da) = 0;
  ftk_da_trace_pool_max(da)  = 0;

  ftk_da_trace_order(da)      = NULL;
  ftk_da_trace_order_next(da) = 0;
  ftk_da_trace_order_max(da)  = 0;

  ftk_da_traces(da)     = NULL;
  ftk_da_trace_next(da) = 0;
  ftk_da_trace_max(da)  = 0;
  ftk_da_trace_modified(da)  = FALSE;
}

GtkWidget*
ftk_drawing_area_new ()
{
	FtkDrawingArea * da
	= g_object_new (ftk_drawing_area_get_type(),
			NULL);
			
	return GTK_WIDGET (da);
}

static void
ftk_da_value_changed (GtkAdjustment *adj, FtkDrawingArea *da)
{
}

static void
ftk_drawing_area_set_scroll_adjustments (FtkDrawingArea   *da,
                                      GtkAdjustment *hadj,
                                      GtkAdjustment *vadj)
{
  gboolean need_adjust = FALSE;

  if (hadj)
    g_return_if_fail (GTK_IS_ADJUSTMENT (hadj));
  else
    hadj = GTK_ADJUSTMENT (gtk_adjustment_new (0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
  if (vadj)
    g_return_if_fail (GTK_IS_ADJUSTMENT (vadj));
  else
    vadj = GTK_ADJUSTMENT (gtk_adjustment_new (0.0, 0.0, 0.0, 0.0, 0.0, 0.0));

  if (da->hadjustment && (da->hadjustment != hadj))
    {
      g_signal_handlers_disconnect_by_func (da->hadjustment,
					    ftk_da_value_changed,
					    da);
      g_object_unref (da->hadjustment);
    }

  if (da->vadjustment && (da->vadjustment != vadj))
    {
      g_signal_handlers_disconnect_by_func (da->vadjustment,
					    ftk_da_value_changed,
					    da);
      g_object_unref (da->vadjustment);
    }

  if (da->hadjustment != hadj)
    {
      da->hadjustment = hadj;
      g_object_ref_sink (da->hadjustment);
      
      g_signal_connect (da->hadjustment, "value_changed",
                        G_CALLBACK (ftk_da_value_changed),
			da);
      need_adjust = TRUE;
    }

  if (da->vadjustment != vadj)
    {
      da->vadjustment = vadj;
      g_object_ref_sink (da->vadjustment);
      
      g_signal_connect (da->vadjustment, "value_changed",
                        G_CALLBACK (ftk_da_value_changed),
			da);
      need_adjust = TRUE;
    }

  if (need_adjust)
  	ftk_da_value_changed (NULL, da);
}



static void ftk_legend_init (FtkLegend *legend);

GtkType ftk_legend_get_type(void);
GtkWidget * ftk_legend_new();

static void
ftk_legend_class_init (FtkLegendClass *klass)
{
	GtkWidgetClass * widget_class;
	
	widget_class = (GtkWidgetClass *) klass;
	widget_class->get_accessible = ftk_legend_get_accessible;
}		

static void
ftk_legend_init (FtkLegend * legend)
{
	ftk_legend_markers (legend)		= NULL;
  ftk_legend_markers_next (legend)	= 0;
  ftk_legend_markers_max (legend)	= 0;
  ftk_legend_markers_modified (legend)	= FALSE;
}

GtkWidget*
ftk_legend_new ()
{
	FtkLegend * legend
	= g_object_new (ftk_legend_get_type(),
			NULL);
			
	return GTK_WIDGET (legend);
}

static GtkWidgetClass *parent_class = NULL;
static GtkWidgetClass *trace_parent_class = NULL;

enum {
  FTK_EVENTVIEWER_SIGNAL,
  LAST_SIGNAL
};

static void  ftk_trace_class_init (FtkTraceClass * klass,
					gpointer class_data);
static void  ftk_trace_init (FtkTrace *trace);
//static GtkWidget*  ftk_trace_new();

static void
ftk_trace_class_init (FtkTraceClass *klass,
			gpointer class_data)
{
	GtkWidgetClass * widget_class;
	
	widget_class = (GtkWidgetClass *) klass;
	
	widget_class->get_accessible = ftk_trace_get_accessible;
	trace_parent_class = gtk_type_class (gtk_widget_get_type ());
}

static void
ftk_trace_init (FtkTrace * trace)
{
}		

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

  GtkWidgetClass *widget_class;
  	    
  widget_class = (GtkWidgetClass *) klass;
  
  parent_class = gtk_type_class (gtk_widget_get_type ());  		    
}

static void
ftk_init_cr (FtkEventViewer * eventviewer)
{
  ftk_ev_symbols_initted (eventviewer) = TRUE;
  cairo_t * cr =
    gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);

  {
    gint i;
    gsize br, bw;
    PangoFontDescription * desc;
    gint width, height;

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

//static void
//set_up_colors (FtkEventViewer * eventviewer)
//{
//  gint i;
//
//  ftk_ev_color_values(eventviewer) = malloc (sizeof(default_color_set));
//  memcpy (ftk_ev_color_values(eventviewer), default_color_set,
//	  sizeof(default_color_set));
//
//#define COLOR_RANDOMISER_ITERATIONS 30
//  for (i = 0; i < COLOR_RANDOMISER_ITERATIONS; i++) {
//    const GdkColor * tc;
//    gint fi = lrint (drand48() * (double)(colors_count - 1));
//    gint ti = lrint (drand48() * (double)(colors_count - 1));
//
//    tc =  ftk_ev_color_value(eventviewer, fi);
//    ftk_ev_color_value(eventviewer, fi) =
//      ftk_ev_color_value(eventviewer, ti);
//    ftk_ev_color_value(eventviewer, ti) = tc;
//  }
//}

static void
initialise_widget (FtkEventViewer * eventviewer)
{
	
  ftk_ev_hold_activated (eventviewer) = FALSE;
  
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
  ftk_ev_bg_color(eventviewer) = style->bg[GTK_STATE_NORMAL];

  ftk_ev_label_box_width(eventviewer)  = 0;
  ftk_ev_label_box_height(eventviewer) = 0;  

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

  ftk_ev_drawable(eventviewer)  = FALSE;

  ftk_ev_span(eventviewer) = DEFAULT_SPAN;
  
  ftk_ev_widget_modified (eventviewer) = TRUE;
  
  ftk_ev_selected_trace (eventviewer) = -1;
}


static void
create_button_legend_box (FtkEventViewer * eventviewer)
{
	GtkWidget * button_legend_box = gtk_hbox_new(FALSE, 5);
	ftk_ev_button_legend_box (eventviewer) = button_legend_box;
	
	AtkObject *obj;
	obj = gtk_widget_get_accessible (button_legend_box);
	atk_object_set_name(obj, _("Button Legend Box"));
	atk_object_set_description(obj, _("Box to hold the buttons and legend."));
}

/*------------------------- Button Box --------------------------------------*/

/*
 * ---------------------------------------------------------------------------
 * |	Hold | Center|                                     |Interval ----|---|
 * ---------------------------------------------------------------------------
 */

static void
create_button_box (FtkEventViewer * eventviewer, GtkTooltips * eventviewer_tips)
{
  /*****************   button box  **************/
    
  GtkWidget * hbutton_box = gtk_hbox_new(FALSE, 5);
  GtkWidget * vbutton_box = gtk_vbox_new(FALSE, 5);
  
  gtk_box_pack_end (GTK_BOX (hbutton_box),
			vbutton_box,
			FALSE, FALSE, 0);
  
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
   

    /* fixme -- make initial state configurable */
    gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (hold_toggle_button),
				  ftk_ev_hold_activated(eventviewer));
    gtk_box_pack_start (GTK_BOX (vbutton_box),
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

    gtk_box_pack_start (GTK_BOX (vbutton_box),
			center_button,
			FALSE, FALSE, 0);
		
    /* Accessibility for center button. */	
   
    AtkObject *obj;
    obj = gtk_widget_get_accessible (center_button);
    atk_object_set_name(obj, _("Center"));
    atk_object_set_description (obj, _("Center available data in the display."));
   
  }	

  {
    /* ginterval slider */

    GtkRequisition requisition;

    GtkWidget * frame = gtk_frame_new ("Interval");
    
    gtk_frame_set_shadow_type(GTK_FRAME(frame), GTK_SHADOW_ETCHED_IN);
    
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
      
    gtk_container_add (GTK_CONTAINER(frame), slider);
    
    gtk_box_pack_start (GTK_BOX (hbutton_box),
		      frame,
		      FALSE, FALSE, 0);
		      
    /* Accessibility for interval slider */
 	
    {
      AtkObject *obj;
 		
      obj = gtk_widget_get_accessible(frame);
      atk_object_set_name(obj, _("Interval Frame"));
      atk_object_set_description (obj, _("Frame to hold Interval Slider."));
    	
     
      obj = gtk_widget_get_accessible (slider);
      atk_object_set_name(obj, _("Interval slider"));
      atk_object_set_description(obj, _("Logarithmic slider for time ginterval."));
        
      
    }
  }

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

}


/*------------------------------------ Legend Area --------------------------*/

/*Legend
 * --------------------------------------------------------------------------
 * | {GlyphA} ObserverA {GlyphB} ObserverB .....                            |
 * | ....                                                                   |
 * --------------------------------------------------------------------------
 */
  
static void
create_legend_area (FtkEventViewer * eventviewer)
{
  GtkWidget * frame = gtk_frame_new ("Legend");
  GtkWidget * legend = ftk_legend_new();
  
  ftk_ev_legend_da (eventviewer) = FTK_LEGEND (legend);
  gtk_drawing_area_size (GTK_DRAWING_AREA(ftk_ev_legend_da(eventviewer)),
			 DEFAULT_INITIAL_WIDTH,
			 DEFAULT_INITIAL_LEGEND_HEIGHT);
  
  gtk_widget_set_events(GTK_WIDGET (legend),
			GDK_POINTER_MOTION_MASK  |
			GDK_LEAVE_NOTIFY_MASK);

  g_signal_connect (GTK_OBJECT (legend), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_legend_da_expose, eventviewer);

  g_signal_connect (GTK_OBJECT(legend), "motion_notify_event",
		    G_CALLBACK (ftk_ev_legend_motion_notify_event), eventviewer);

  g_signal_connect (GTK_OBJECT(legend), "leave_notify_event",
		    G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
		     
  ftk_ev_legend_frame (eventviewer) = frame;
  gtk_container_add (GTK_CONTAINER(frame), legend);
  gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_ETCHED_IN);

  /* Legend Accessibility. */
  AtkObject *obj;
  obj = gtk_widget_get_accessible (frame);
  atk_object_set_name(obj, _("Legend Frame"));
  atk_object_set_description (obj, _("Frame to hold legend."));
  
  obj = gtk_widget_get_accessible (legend);
  atk_object_set_name(obj, _("Legend Drawing Area"));
  atk_object_set_description (obj, _("Drawing Area to hold Legend."));

  //return frame;
}

/************************************* Drawing Area **************************/
  
static void
create_drawing_area (FtkEventViewer * eventviewer)
{
  /*****************  drawing area **************/
  GtkWidget * scrolled_window;
  GtkWidget * frame = gtk_frame_new (NULL);
  
  gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_NONE);
  GtkWidget * da = ftk_drawing_area_new();
  
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

  ftk_ev_da(eventviewer) = FTK_DRAWING_AREA(da);
  
  gtk_drawing_area_size (GTK_DRAWING_AREA(ftk_ev_da(eventviewer)),
			 DEFAULT_INITIAL_WIDTH,
			 DEFAULT_INITIAL_HEIGHT);
  //gtk_widget_show (da);

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

  g_signal_connect (GTK_OBJECT(da), "button_press_event",
		    G_CALLBACK (ftk_ev_third_button_press_event), eventviewer);
		    
  g_signal_connect (GTK_OBJECT(da), "button_release_event",
		    G_CALLBACK (ftk_ev_button_release_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "motion_notify_event",
		    G_CALLBACK (ftk_ev_motion_notify_event), eventviewer);

  g_signal_connect (GTK_OBJECT(da), "leave_notify_event",
		    G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
    
  g_signal_connect (GTK_OBJECT(da),"configure-event",
		    G_CALLBACK (ftk_eventviewer_configure), eventviewer);
		    
  g_signal_connect (GTK_OBJECT(da), "style-set",
		    G_CALLBACK (ftk_eventviewer_style), eventviewer);
  
  scrolled_window = gtk_scrolled_window_new (NULL, NULL);
  ftk_ev_da_scrolled_window(eventviewer) = GTK_SCROLLED_WINDOW (scrolled_window);
  
  
  /* Scrolled Window Accessibility. */
  obj = gtk_widget_get_accessible (scrolled_window);
  atk_object_set_name(obj, _("Drawing Scrolled Window"));
  atk_object_set_description(obj, _("Scrolled Window to hold Drawing Area"));
  /* ------------------------------ */
  
  #ifdef SCROLLBAR 
  gtk_container_add(GTK_CONTAINER(scrolled_window), da);
  # else
  gtk_scrolled_window_add_with_viewport (GTK_SCROLLED_WINDOW (scrolled_window), da);
  #endif
  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (scrolled_window),
  #ifdef SCROLLBAR
				  GTK_POLICY_ALWAYS,
  #else
  				  GTK_POLICY_NEVER,
  #endif				  			  
				  GTK_POLICY_ALWAYS);
  
  gtk_container_add (GTK_CONTAINER(frame), scrolled_window);
 
  gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_NONE);
 
}

void
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
  
  #ifdef SCROLLBAR	  
  GtkScrolledWindow * da_scrolled_window = ftk_ev_da_scrolled_window(eventviewer);
  gtk_scrolled_window_set_hadjustment(da_scrolled_window, GTK_ADJUSTMENT (scroll_adj));
  GtkWidget * h_scroll = gtk_scrolled_window_get_hscrollbar(da_scrolled_window);
  #endif
  
  GtkWidget * h_scroll = gtk_hscrollbar_new(GTK_ADJUSTMENT(scroll_adj));

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
  g_signal_connect (GTK_RANGE(h_scroll),"change-value",
		    (GtkSignalFunc) ftk_eventviewer_scroll_cv, eventviewer);
		    
  g_signal_connect (scroll_adj, "value-changed",
  			(GtkSignalFunc) ftk_eventviewer_scroll_adj_val_changed, eventviewer);

}

static gboolean
ftk_eventviewer_scroll_adj_val_changed (GtkAdjustment *adjustment,
                                            gpointer       user_data) 
{
	return TRUE; //inhibit propagation
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

  //set_up_colors (eventviewer);
  initialise_widget (eventviewer);
  
  g_signal_connect (GTK_OBJECT (eventviewer), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_expose, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer), "destroy",
		    GTK_SIGNAL_FUNC(ftk_eventviewer_destroy),
		    NULL);

  gtk_box_set_homogeneous (GTK_BOX (vbox), FALSE);
  gtk_box_set_spacing (GTK_BOX (vbox), 0);

  create_button_legend_box(eventviewer);
    create_button_box (eventviewer, eventviewer_tips);
    create_legend_area (eventviewer);
    create_drawing_area (eventviewer);
    create_scrollbar (eventviewer);
    
    gtk_box_pack_end (GTK_BOX (ftk_ev_button_legend_box(eventviewer)), 
    					ftk_ev_hbutton_box(eventviewer), FALSE, FALSE, 0);
    gtk_box_pack_start (GTK_BOX (ftk_ev_button_legend_box(eventviewer)), 
    					ftk_ev_legend_frame(eventviewer), TRUE, TRUE, 0);
    

	gtk_box_pack_start (GTK_BOX (vbox), ftk_ev_button_legend_box(eventviewer), 
    					FALSE, FALSE, 0);
    					 					
  	gtk_box_pack_start (GTK_BOX (vbox), ftk_ev_da_frame(eventviewer), 
    					TRUE, TRUE, 0);
    					
    #ifndef SCROLLBAR
    gtk_box_pack_start (GTK_BOX(vbox), ftk_ev_scroll(eventviewer), FALSE, 
    FALSE, 0);
    
    #endif					
    					
    gtk_widget_show_all(GTK_WIDGET(eventviewer));
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
  gint x;
  gint y;
} ftk_coord_s;


/* Return 0 if a.y == b.y, -1 if a.y < b.y, 1 if a.y > b.y. */
static gint
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
	FtkDrawingArea * da = ftk_ev_da(eventviewer);
  gint i;
  gboolean kill_cr;

  if (1 < ftk_dlink_event_list_next (dlink)) {
    ftk_tie_s * tie = ftk_ev_tie (eventviewer, ftk_dlink_tie_index (dlink));
    ftk_coord_s * coords =
      alloca (ftk_dlink_event_list_next (dlink) * sizeof(ftk_coord_s));
    
    gdouble time_offset =
      gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));

    for (i = 0; i < ftk_dlink_event_list_next (dlink); i++) {
      gint event_index = ftk_dlink_event_pair_event (dlink, i);
      gint trace_index = ftk_dlink_event_pair_trace (dlink, i);
      FtkTrace * trace = ftk_da_trace (da, trace_index);
      ftk_event_s * event = ftk_trace_event (trace, event_index);
      double etime =
	(ftk_event_time (event) - ftk_ev_zero (eventviewer)) - time_offset;
      double loc_d = etime / ftk_ev_span (eventviewer);
      gint h_offset = ftk_da_trace_origin (da) +
	lrint (((double)ftk_da_trace_width (da)) * loc_d);

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
    gdk_cairo_set_source_color(cr, &ftk_tie_color(tie));
    //    cairo_set_source_rgb (cr,
    //			  ((double)(ftk_tie_color_red (tie)))/65535.0,
    //			  ((double)(ftk_tie_color_green (tie)))/65535.0,
    //			  ((double)(ftk_tie_color_blue (tie)))/65535.0);

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

static gint
int_sort (a, b)
     gint * a;
     gint * b;
{
  return ((*a) == (*b)) ? 0 : (((*a) < (*b)) ? -1 : 1);
}

static void
draw_link (FtkEventViewer * eventviewer, cairo_t * cr,
	   ftk_link_s * link,
	   gboolean flush_it)
{
	FtkDrawingArea *da = ftk_ev_da(eventviewer);
  gboolean kill_cr;
  gdouble time_offset =
    gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
  double link_time = (ftk_link_when (link) - ftk_ev_zero (eventviewer))
    - time_offset;

  if (link_time >= 0.0) {
    ftk_tie_s * tie = ftk_ev_tie (eventviewer, ftk_link_tie_index (link));
    double loc_d = link_time / ftk_ev_span (eventviewer);
    
    if ((loc_d >= 0.0) && (loc_d <= 1.0)) {
      gint i;
      
      gint h_offset = ftk_da_trace_origin (da) +
	lrint (((double)ftk_da_trace_width (da)) * loc_d);

      if (0 < ftk_link_trace_list_next(link)) {
	int * vps = alloca (ftk_link_trace_list_next(link) * sizeof (int));
  
	if (NULL == cr) {
	  cr = gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);
	  kill_cr = TRUE;
	}
	else kill_cr = FALSE;

	gdk_cairo_set_source_color(cr, &ftk_tie_color (tie));
	//	cairo_set_source_rgb (cr,
	//			      ((double)(ftk_tie_color_red (tie)))/65535.0,
	//			      ((double)(ftk_tie_color_green (tie)))/65535.0,
	//			      ((double)(ftk_tie_color_blue (tie)))/65535.0);
	
	for (i = 0; i < ftk_link_trace_list_next(link); i++) {
	  FtkTrace * trace =
	    ftk_da_trace (da, ftk_link_trace(link, i));
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

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
		{
      		ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
			eventviewer);
	  	}	
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

  //return rc;
  return TRUE;
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
  double loc_d = (y - ((double)(ftk_da_trace_origin (da)))) /
    ((double)(ftk_da_trace_width (da)));
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


/* 
 * Highlighting a trace: User should click and release button on the same trace.
 * Clicking on a trace and releasing elsewhere is considered a cancel.
 */
static gboolean
ftk_ev_button_press_event (GtkWidget * widget,
			   GdkEventButton * event,
			   gpointer data)
{
	if (1  != event->button) {
		return FALSE;
	}
  
  FtkEventViewer *eventviewer = FTK_EVENTVIEWER(data);
FtkDrawingArea * da = ftk_ev_da(eventviewer);

 /* If a trace was selected toggle its selected value. */
  for (gint trace = 0; trace < ftk_da_trace_next(da); trace++) 
    {
      FtkTrace *temp_trace = ftk_da_trace(da, trace);
      //FIXME: HARDCODED VALUES
       double vp = (double)((ftk_trace_label_dheight(temp_trace) >> 1) +
				 ftk_trace_vpos_d(temp_trace));
      
#define SELECT_TOLERANCE 6
     
      if ( abs ((double) event->y - vp) < SELECT_TOLERANCE)
		{
			ftk_trace_modified (temp_trace) = TRUE;
			return FALSE; /* Continue handling event. */
		}
	  
    }
	
  /* If no trace was selected, set all trace selected values to false. */
  for (gint trace = 0; trace < ftk_da_trace_next(da); trace++)
  {
  	FtkTrace *temp_trace = ftk_da_trace(da, trace);
  	ftk_trace_selected (temp_trace) = FALSE;
  }
   
   if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) {
      			ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
				eventviewer);
	  		}
	  			  		
  return FALSE; /* Continue handling event. */
}

static gboolean
ftk_ev_button_release_event (GtkWidget * widget, 
							GdkEventButton *event,
							gpointer data)
{

	if (1 != event->button) {
		return FALSE;
	}
  /* Select a trace. */
  FtkEventViewer *eventviewer = FTK_EVENTVIEWER(data);
FtkDrawingArea * da = ftk_ev_da(eventviewer);

 /* If a trace was selected toggle its selected value. */
  for (gint trace = 0; trace < ftk_da_trace_next(da); trace++) 
    {
      FtkTrace *temp_trace = ftk_da_trace(da, trace);
      //FIXME: HARDCODED VALUES
       double vp = (double)((ftk_trace_label_dheight(temp_trace) >> 1) +
				 ftk_trace_vpos_d(temp_trace));
      
     if (ftk_trace_modified(temp_trace))
     {
     	ftk_trace_modified(temp_trace) = FALSE;
     	ftk_trace_selected (temp_trace) = !ftk_trace_selected (temp_trace);			
      if  (abs ((double) event->y - vp) < SELECT_TOLERANCE) 
      {
      	if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
		{
      		ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
			eventviewer);
	  	}	  		
	    return FALSE; /* Continue handling event. */
      } else {
      	ftk_trace_selected(temp_trace) = !ftk_trace_selected(temp_trace);
      }
     } 
    }
	
  return FALSE; /* Continue handling event. */
}		

static gboolean
ftk_ev_third_button_press_event	(GtkWidget * widgeet,
								 GdkEventButton *event,
								 gpointer data)
{
	
	if (3 != event->button) {
		return FALSE;
	}

/* Select a trace. */
  FtkEventViewer *eventviewer = FTK_EVENTVIEWER(data);
FtkDrawingArea * da = ftk_ev_da(eventviewer);

 /* If a trace was selected toggle its selected value. */
  for (gint trace = 0; trace < ftk_da_trace_next(da); trace++) 
    {
      FtkTrace *temp_trace = ftk_da_trace(da, trace);
      //FIXME: HARDCODED VALUES
       double vp = (double)((ftk_trace_label_dheight(temp_trace) >> 1) +
				 ftk_trace_vpos_d(temp_trace));
		
	   if ( abs ((double) event->y - vp) < SELECT_TOLERANCE)
		{
			ftk_trace_selected(temp_trace) = TRUE;
		}
		else
		{
			ftk_trace_selected(temp_trace) = FALSE;
		}				 
    }
    
    if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
		{
      		ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
			eventviewer);
	  	}	
	  	
	return FALSE;  					
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
    gint offset;
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
  gint marker_idx;
  ftk_event_s * event;
  double time_d;
} ftk_multi_event_s;

static char *
create_popup_marker_label (gboolean prepend_ts,
			   gint * count_p,
			   FtkEventViewer * eventviewer,
			   ftk_event_s * revent,
			   FtkTrace * trace,
			   gint marker_idx,
			   double time_d)
{
  char * lbl;
  gint count;
  FtkLegend * legend = ftk_ev_legend_da(eventviewer);
  ftk_marker_s * marker = ftk_legend_marker (legend, marker_idx);
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
		    FtkTrace * trace,
		    ftk_event_s * revent,
		    gint marker_idx,
		    double time_d,
		    gint me_next,
		    ftk_multi_event_s * me)
{
  char * lbl = NULL;
  FtkLegend * legend = ftk_ev_legend_da(eventviewer);
  ftk_marker_s * marker = ftk_legend_marker (legend, marker_idx);
  
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
      gint i;
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
	      gint trace_idx,
	      gint marker_idx,
	      double time_d,
	      FtkTrace * trace,
	      ftk_event_s * revent,
	      ftk_multi_event_s * multi_event,
	      gint multi_event_next)
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
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);

  {
    gint marker_idx;
    ftk_popup_type_e pt;
    ftk_marker_s * marker;
    gint pvpos = lrint (event->y) - 10;
    gint phpos = lrint (event->x);

    pt = FTK_POPUP_TYPE_NONE;
    for (marker_idx = 0;
	 marker_idx < ftk_legend_markers_next (legend);
	 marker_idx++) {
      marker = ftk_legend_marker (legend, marker_idx);
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
  FtkLegend * legend = ftk_ev_legend_da(eventviewer);
  FtkDrawingArea * da = ftk_ev_da(eventviewer);
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);

#ifdef USE_READOUT
  set_readout (eventviewer, event->x);
#endif

  {
    gint i;
    gint trace_idx;
    gint marker_idx;
    FtkTrace * trace;
    ftk_marker_s * marker;
    ftk_popup_type_e pt;
    ftk_event_s * revent;
    double time_d = 0.0;
    /* fixme -- replace 12 w something based of font hgt */
    /* fixme maybe put in loop below and create running baseline */
    gint pvpos = lrint (event->y) - 10;
    gint phpos = lrint (event->x);
    ftk_multi_event_s * multi_event = NULL;
    gint multi_event_next = 0;
    gint multi_event_max  = 0;

    trace = NULL;
    marker = NULL;
    revent = NULL;
    trace_idx = -1;
    marker_idx = -1;
    pt = FTK_POPUP_TYPE_NONE;
    
    for (i = 0; i < ftk_da_trace_next (da); i++) {
      FtkTrace * ltrace = ftk_da_trace (da, i);

      if (!ftk_trace_valid (ltrace)) continue;
      if (abs (ftk_trace_vpos_d (ltrace) - pvpos) < POPUP_TOLERANCE) {
	trace = ltrace;
	trace_idx = i;
	if (phpos <= ftk_ev_label_box_width (eventviewer))
	  pt = FTK_POPUP_TYPE_LABEL;
	else {
	  gint j;
	  gint hit_count = 0;
	  ftk_event_s * qevent = NULL;
	  for (j = 0; j <  ftk_trace_event_next(ltrace); j++) {
	    ftk_event_s * pevent = ftk_trace_event (ltrace, j);
	    if ((abs (ftk_event_hloc (pevent) - phpos) < POPUP_TOLERANCE) &&
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
	      marker = ftk_legend_marker (legend, marker_idx);
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
	    double loc_d = (event->x - ((double)(ftk_da_trace_origin (da)))) /
	      ((double)(ftk_da_trace_width (da)));
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
		  FtkTrace * trace,
		  ftk_event_s * event, gboolean flush_it)
{
	FtkLegend * legend = ftk_ev_legend_da(eventviewer);
	FtkDrawingArea * da = ftk_ev_da(eventviewer);
  //  gint o_x, o_y;
  //  gint d_w, d_h;
  gboolean kill_cr;

  ftk_event_hloc (event) = -1;

  if (NULL == cr) {
    cr = gdk_cairo_create (GTK_WIDGET (ftk_ev_da (eventviewer))->window);
    kill_cr = TRUE;
  }
  else kill_cr = FALSE;

  {
    gdouble time_offset = gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
    
    ftk_marker_s * marker
      = ftk_legend_marker (legend, ftk_event_marker (event));
    double mark_time = (ftk_event_time (event) - ftk_ev_zero (eventviewer))
      - time_offset;
    if (mark_time >= 0.0) {
      double loc_d = mark_time / ftk_ev_span (eventviewer);

      if ((loc_d >= 0.0) && (loc_d <= 1.0)) {
	int h_offset = ftk_da_trace_origin (da) +
	  lrint (((double)ftk_da_trace_width (da)) * loc_d);
	int sym_idx = ftk_marker_glyph (marker);
	int v_offset = ftk_trace_vpos_d (trace) +
	  ((ftk_trace_label_dheight(trace) >> 1) -
	   ftk_symbol_v_center(sym_idx));

	gdk_cairo_set_source_color (cr, &ftk_marker_color (marker));
	//	cairo_set_source_rgb (cr,
	//			      (double)(ftk_marker_color_red (marker)) /
	//			      (double)65535,
	//			      (double)(ftk_marker_color_green (marker)) /
	//			      (double)65535,
	//			      (double)(ftk_marker_color_blue (marker)) /
	//			      (double)65535);

	cairo_move_to (cr,
		       (double)(h_offset - ftk_symbol_h_center(sym_idx)),
		       (double)(v_offset /*- ftk_symbol_v_center(sym_idx) */ ));
	
	pango_cairo_show_layout (cr, ftk_symbol_layout (sym_idx));
	cairo_stroke (cr);
	
	ftk_event_hloc (event) = h_offset;
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
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  if (!ftk_ev_symbols_initted (eventviewer)) ftk_init_cr (eventviewer);
  
  /* compute legend extents and baselines */
  
  if (ftk_legend_markers_modified (legend) ||
      ftk_ev_widget_modified(eventviewer)) {
    gint l_h_pos = LEGEND_MARGIN;
    gint dww = (int)(dwidge->allocation.width);
    gint legend_width;
    gint i;
    gint width = 0;
    gint height = 0;
    gint legend_v_pos = 0;
    
    ftk_legend_markers_modified (legend) = FALSE;
    //    ftk_ev_widget_modified (eventviewer) = FALSE;

    for ( i = 0; i < ftk_legend_markers_next (legend); i++) {
      gint gwidth, gheight;
      ftk_marker_s * marker = ftk_legend_marker (legend, i);

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
    gint i;

    cairo_t * cr = gdk_cairo_create (dwidge->window);
      
    for ( i = 0; i < ftk_legend_markers_next (legend); i++) {
      ftk_marker_s * marker = ftk_legend_marker (legend, i);

      gdk_cairo_set_source_color (cr, &ftk_marker_color (marker));
      //      cairo_set_source_rgb (cr,
      //			    (double)(ftk_marker_color_red (marker)) /
      //			    (double)65535,
      //			    (double)(ftk_marker_color_green (marker)) /
      //			    (double)65535,
      //			    (double)(ftk_marker_color_blue (marker)) /
      //			    (double)65535);
	
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
  FtkDrawingArea * da = ftk_ev_da(eventviewer);
  
  if (!ftk_ev_symbols_initted (eventviewer)) ftk_init_cr (eventviewer);

  /* compute label extents and baselines */
  if (ftk_da_trace_modified (da)) {
    gint i;
    gint max_label_width  = 0;
    gint total_label_height = 0;
    
    ftk_da_trace_modified (da) = FALSE;

    for (i = 0; i < ftk_da_trace_order_next(da); i++) {
      FtkTrace * trace = ftk_da_trace(da,
					 ftk_da_trace_order_ety (da, i));
      
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
  
  
  /* Request more space if more traces. */
  if ((ftk_ev_da_height(eventviewer) > (int)(dwidge->allocation.height)) ||
      (ftk_ev_da_height(eventviewer) < ((int)(dwidge->allocation.height) - 12))) {
    gtk_widget_set_size_request (GTK_WIDGET (ftk_ev_da (eventviewer)),
				 (int)(dwidge->allocation.width),
				 ftk_ev_da_height(eventviewer));
  }
  
  {
    cairo_t * cr = gdk_cairo_create (dwidge->window);

    gint dww = (int)(dwidge->allocation.width);
    gint dwh = (int)(dwidge->allocation.height);
  
    cairo_rectangle (cr, 0, 0, dww, dwh);
    gdk_cairo_set_source_color (cr, &ftk_ev_bg_color(eventviewer));
    //    cairo_set_source_rgb (cr,
    //			  (double)ftk_ev_bg_red (eventviewer)/65535.0,
    //			  (double)ftk_ev_bg_green (eventviewer)/65535.0,
    //			  (double)ftk_ev_bg_blue (eventviewer)/65535.0);
			  
    cairo_fill_preserve (cr);
    cairo_clip (cr);
    cairo_stroke (cr);

    {		/* draw labels and baselines */
      gint i;

#define LABEL_GAP 10
      ftk_da_trace_origin (da) =
	ftk_ev_label_box_width (eventviewer) + LABEL_GAP;
      ftk_da_trace_width (da)  =
	(dww - LABEL_GAP) - ftk_da_trace_origin (da);

      for (i = 0; i < ftk_da_trace_order_next(da); i++) {
	FtkTrace * trace = ftk_da_trace(da,
					   ftk_da_trace_order_ety (da, i));
	if (ftk_trace_valid (trace)) {
	  gdk_cairo_set_source_color (cr, &ftk_trace_color(trace));
	  //	  cairo_set_source_rgb (cr,
	  //				(double)(ftk_trace_color_red (trace))/(double)65535,
	  //				(double)(ftk_trace_color_green (trace))/(double)65535,
	  //				(double)(ftk_trace_color_blue (trace))/(double)65535);
	  cairo_move_to (cr,
			 (double)(ftk_ev_label_box_width (eventviewer) -
				  ftk_trace_label_dwidth (trace)),
			 (double)(ftk_trace_vpos_d(trace)));
	  pango_cairo_show_layout (cr, ftk_trace_label(trace));
	  cairo_stroke (cr);


	/* Draw Traces. */
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
	    
	    /* Trace is selected. */
	    if (ftk_trace_selected(trace)) {
	    	cairo_save(cr);
	    	GtkStyle *style = gtk_widget_get_style (GTK_WIDGET (eventviewer));
		    GdkColor color = style->bg[GTK_STATE_SELECTED];
	    	
	    	cairo_set_line_width (cr, cairo_get_line_width(cr) + 3.0);
	    	
	    	gdk_cairo_set_source_color (cr, &color);
	    	cairo_move_to (cr, (double) (ftk_da_trace_origin (da)),
	    	vp);
	    	cairo_line_to (cr, (double)(ftk_da_trace_origin (da) +
	    				ftk_da_trace_width (da)), vp);
	    				
	    	cairo_stroke (cr);
	    	
	    	cairo_restore (cr);
	    	 color = style->fg[GTK_STATE_SELECTED];
		    gdk_cairo_set_source_color (cr, &color);
	    } 
	    cairo_move_to (cr, (double)(ftk_da_trace_origin (da)),
			   vp);
	    cairo_line_to (cr, (double)(ftk_da_trace_origin (da) +
					ftk_da_trace_width (da)), vp);
	  
	    cairo_stroke (cr);
	  
	    if (0.0 < ftk_trace_linewidth (trace)) cairo_restore (cr);
	  }
	

	}
      }
    }

    cairo_rectangle (cr,
		     ftk_da_trace_origin (da),
		     0,
		     ftk_da_trace_width (da),
		     dwh);
    cairo_clip (cr);

    {				/* draw points */
      gint i,j;

      for (i = 0; i < ftk_da_trace_order_next(da); i++) {
	FtkTrace * trace = ftk_da_trace(da,
					   ftk_da_trace_order_ety (da, i));
	if (!ftk_trace_valid (trace)) continue;
	
	for (j = 0; j < ftk_trace_event_next(trace); j++) {
	  ftk_event_s * event = ftk_trace_event (trace, j);
	
	  draw_cairo_point (eventviewer, cr, trace, event, FALSE);
	}
      }
    }
    
    {				/* draw links */
      gint i;

      for (i = 0; i < ftk_ev_link_next(eventviewer); i++) {
	ftk_link_s * link = ftk_ev_link (eventviewer, i);
	
	draw_link (eventviewer, cr, link, FALSE);
      }
    }
    {				/* draw dlinks */
      gint i;

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

	//Do not remove, needs to be here according to Chris Moller.
    if (event) {
	  gtk_container_propagate_expose (GTK_CONTAINER 
	  								  (ftk_ev_button_legend_box(eventviewer)),
	  				  ftk_ev_hbutton_box (eventviewer),
	  				  event);
      gtk_container_propagate_expose (GTK_CONTAINER (ftk_ev_button_legend_box(eventviewer)),
				      GTK_WIDGET (ftk_ev_legend_frame (eventviewer)),
				      event);
	  gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_da_frame (eventviewer)),
				      event);
	  
	  #ifndef SCROLLBAR   
      gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_scroll (eventviewer)),
				      event);
	  #endif
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
	
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  GtkStyle * style = gtk_widget_get_style(GTK_WIDGET(da));
	
  GdkColor * backgrounds = style->bg;
  ftk_eventviewer_preset_bg_rgb_e(eventviewer, backgrounds->red, 
				  backgrounds->green, backgrounds->blue, NULL);
		
  GdkColor * foregrounds = style->fg;
	
  for (gint i = 0; i < ftk_da_trace_next(da); i++) {
    if (ftk_trace_valid (ftk_da_trace (da, i))) {
      ftk_eventviewer_preset_trace_rgb_e(eventviewer, i, 
					 foregrounds->red, foregrounds->green,
					 foregrounds->blue, NULL);
    }
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
  gint i;
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (widget);
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  g_return_if_fail (FTK_IS_EVENTVIEWER (widget));

  for (i = 0; i < ftk_da_trace_next(da); i++) {
    FtkTrace * trace = ftk_da_trace(da, i);
      
    if (ftk_trace_label(trace)) g_object_unref (ftk_trace_label (trace));
    if (ftk_trace_string(trace)) free (ftk_trace_string (trace));
    if (ftk_trace_gc(trace)) g_object_unref (ftk_trace_gc (trace));
    if (ftk_trace_events (trace))  free (ftk_trace_events(trace));
  }
  if (ftk_da_traces (da))  free (ftk_da_traces(da));

  for (i = 0; i < ftk_ev_tie_next(eventviewer); i++) {
    ftk_tie_s * tie = ftk_ev_tie(eventviewer, i);

    if (ftk_tie_gc(tie)) g_object_unref (ftk_tie_gc (tie));
  }
  if (ftk_ev_ties (eventviewer))    free (ftk_ev_ties(eventviewer));

  for (i = 0; i < ftk_legend_markers_next(legend); i++) {
    ftk_marker_s * marker = ftk_legend_marker(legend, i);
      
    if (ftk_marker_gc(marker)) g_object_unref (ftk_marker_gc (marker));
    if (ftk_marker_string (marker)) free (ftk_marker_string (marker));
  }
  if (ftk_legend_markers (legend)) free (ftk_legend_markers (legend));

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

static gboolean
ftk_eventviewer_scroll_cv(GtkRange     *range,
			  GtkScrollType scroll,
			  gdouble       value,
			  gpointer      user_data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (user_data);
  GtkAdjustment * adj = gtk_range_get_adjustment (range);
  gtk_adjustment_set_value (adj, value);

  ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL, eventviewer);
  return TRUE; //Inhibit propagation.
}

static gint
do_append (FtkEventViewer * eventviewer,
	   gint trace_index,
	   gint marker,
	   gchar * string,
	   double now_d)
{
	FtkDrawingArea *da = ftk_ev_da(eventviewer);
  ftk_event_s * event;
  gint event_nr = -1;
  FtkTrace * trace = ftk_da_trace (da, trace_index);

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
  ftk_event_hloc (event) = -1;
  
  ftk_trace_min_time (trace) = fmin (ftk_trace_min_time (trace), now_d);
  ftk_trace_max_time (trace) = fmax (ftk_trace_max_time (trace), now_d);
  ftk_trace_time_set (trace) = TRUE;
  
  ftk_trace_selected (trace) = FALSE;
  
  ftk_ev_min_time (eventviewer) = fmin (ftk_ev_min_time (eventviewer), now_d);
  ftk_ev_max_time (eventviewer) = fmax (ftk_ev_max_time (eventviewer), now_d);
  ftk_ev_time_set (eventviewer) = TRUE;
  
  {
    gdouble upper;
    gdouble nt = ftk_event_time (event) - ftk_ev_zero (eventviewer);
    GtkAdjustment * adj = ftk_ev_scroll_adj (eventviewer);
    

    g_object_get (G_OBJECT (adj), "upper", &upper, NULL);
    fprintf(stderr, "The upper value is: %g\n", upper);
    if (upper < nt) {
      g_object_set (G_OBJECT (adj),
		    "upper", nt,
		    NULL);
      gtk_adjustment_changed (adj);
      
    }
    if (!gtk_toggle_button_get_active 
    ( GTK_TOGGLE_BUTTON (ftk_ev_hold_toggle_button(eventviewer))))
    {
	gtk_adjustment_set_value (adj, nt - ftk_ev_span (eventviewer));
	if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) 
		{
      		ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)), NULL,
			eventviewer);
	  	}	
	 return event_nr;
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

GType
ftk_trace_get_type ()
{
	static GType trace_type = 0;
	
	if (!trace_type) {
		static const GTypeInfo trace_info = {
			sizeof (FtkTraceClass), 	/* class_size	*/
			NULL,				/* base_init.		*/
			NULL,				/* base_finalize	*/
			(GClassInitFunc) ftk_trace_class_init,	/* class_init	*/
			NULL,
			NULL,
			sizeof (FtkTrace),		/* instance size	*/
			0,					/* n_preallocs	*/
			(GInstanceInitFunc) ftk_trace_init,	/* instance_init	*/
		};
		
		trace_type = g_type_register_static (GTK_TYPE_WIDGET,
								"Gtk_Trace",
								&trace_info, 0);
	}
	
	return trace_type;
}

GtkWidget*
ftk_eventviewer_new ()
{
  FtkEventViewer * eventviewer
    = g_object_new (ftk_eventviewer_get_type (),
		    NULL);

  
  
  return GTK_WIDGET (eventviewer);
}

GtkWidget*
ftk_trace_new ()
{
	FtkTrace * trace
	= g_object_new (ftk_trace_get_type(),
			NULL);

	return GTK_WIDGET (trace);
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
  
  GtkStyle *style = GTK_WIDGET (eventviewer) ->style;
  ftk_ev_bg_color(eventviewer)= style->bg[GTK_STATE_NORMAL]; 
  //  ftk_ev_bg_red(eventviewer)		= red;
  //  ftk_ev_bg_green(eventviewer)		= green;
  //  ftk_ev_bg_blue(eventviewer)		= blue;	
  
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
  gint tag = -1;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }
 FtkDrawingArea *da = ftk_ev_da(eventviewer);
  {
    char * t_label;
    FtkTrace * trace = NULL;
#define FTK_EV_TRACE_INCR	8

    if (ftk_da_trace_pool (da) &&
	(0 < ftk_da_trace_pool_next (da))) {
      tag = ftk_da_trace_pool_ety(da,
				  --ftk_da_trace_pool_next (da));
    }
    if (-1 == tag) {
      if (ftk_da_trace_max(da) <= ftk_da_trace_next(da)) {
	ftk_da_trace_max(da) += FTK_EV_TRACE_INCR;
	ftk_da_traces(da)
	  = realloc (ftk_da_traces(da),
		     ftk_da_trace_max(da) * sizeof(FtkTrace));
      }
      tag = ftk_da_trace_next(da)++;
     	
    }

    if (ftk_da_trace_order_next (da) >= ftk_da_trace_order_max (da)) {
#define FTK_TRACE_ORDER_INCR 4
      ftk_da_trace_order_max (da) += FTK_TRACE_ORDER_INCR;
      ftk_da_trace_order(da)
	= realloc (ftk_da_trace_order(da),
		   ftk_da_trace_order_max (da) * sizeof(int));
    }
    ftk_da_trace_order_ety(da, ftk_da_trace_order_next(da)++) = tag;
    
    trace = ftk_da_trace (da, tag);
    ftk_trace_gc (trace)		= NULL;
    ftk_trace_vpos_d (trace)		= 0.0;
    ftk_trace_linestyle (trace)		= -1.0;
    ftk_trace_linewidth (trace)		= -1.0;
    gtk_widget_ensure_style(GTK_WIDGET (ftk_ev_da(eventviewer)));
    GtkStyle *style = gtk_widget_get_style (GTK_WIDGET (ftk_ev_da(eventviewer)));
    ftk_trace_color (trace) = style->fg[GTK_STATE_NORMAL];
    //    ftk_trace_color_red (trace)		= style->fg[0].red;
    //    ftk_trace_color_green (trace)	= style->fg[0].green;
    //    ftk_trace_color_blue (trace)	= style->fg[0].blue;
    ftk_trace_event_next(trace) 	= 0;
    ftk_trace_event_max(trace)  	= 0;
    ftk_trace_events(trace)     	= NULL;
    ftk_trace_min_time (trace)		= HUGE_VAL;
    ftk_trace_max_time (trace)		= -HUGE_VAL;
    ftk_trace_index (trace)			= tag;
    ftk_trace_selected (trace)		= FALSE;
    
    
    if (label) asprintf (&t_label, "%s :%2d", label, tag);
    else       asprintf (&t_label, ":%2d", tag);

    ftk_trace_label(trace) =
      gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
    free (t_label);
    ftk_trace_string (trace) = string ? strdup (string) : NULL;
    ftk_trace_label_modified (trace)	= TRUE;
    ftk_trace_valid (trace)    = TRUE;
    
    ftk_da_trace_modified (da) = TRUE;

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
  
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  {
    gint i;

    for (i = 0; i < ftk_da_trace_order_next (da); i++) {
      if (ftk_da_trace_order_ety (da, i) == trace_index) {
	if (i < (ftk_da_trace_order_next (da) - 1)) {
	  memmove (&ftk_da_trace_order_ety (da, i),
		   &ftk_da_trace_order_ety (da, i + 1),
		   ((ftk_da_trace_order_next(da) - i) - 1) *
		   sizeof(int));
	  ftk_da_trace_order_next (da)--;
	  i--;
	}
      }
    }
  }

  {
    FtkTrace * trace = ftk_da_trace (da, trace_index);

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

    if (ftk_da_trace_pool_next(da) >=
	ftk_da_trace_pool_max(da)) {
#define FTK_EVENt_POOL_INCR 4
      ftk_da_trace_pool_max(da) += FTK_EVENt_POOL_INCR;
      ftk_da_trace_pool(da) =
	realloc (ftk_da_trace_pool(da),
		 ftk_da_trace_pool_max(da) * sizeof(int));
    }
    ftk_da_trace_pool_ety(da, ftk_da_trace_pool_next(da)++) =
      trace_index;
  }

  /* delete ftk_ev_dlinks(eventviewer) */
  if (ftk_ev_dlinks (eventviewer)) {
    gint i;

    for (i = 0; i < ftk_ev_dlink_next (eventviewer); i++) {
      ftk_dlink_s * dlink = ftk_ev_dlink (eventviewer, i);

      if (ftk_dlink_event_pair_list(dlink)) {
	int j;

	for (j = 0; j < ftk_dlink_event_list_next(dlink); j++) {
	  gint ti = ftk_dlink_event_pair_trace(dlink, j);
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
    gint i;

    for (i = 0; i < ftk_ev_link_next (eventviewer); i++) {
      ftk_link_s * link = ftk_ev_link (eventviewer, i);

      if (ftk_link_trace_list(link)) {
	int j;

	for (j = 0; j < ftk_link_trace_list_next(link); j++) {
	  gint ti = ftk_link_trace(link, j);
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

  ftk_da_trace_modified(da) = TRUE;
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
  FtkTrace * trace;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
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

  trace = ftk_da_trace (da, trace_index);
  ftk_trace_color_red (trace)		= red;
  ftk_trace_color_green (trace)		= green;
  ftk_trace_color_blue (trace)		= blue;
  ftk_da_trace_modified (da)	= TRUE;
	
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
  FtkTrace * trace;
  char * t_label;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  trace = ftk_da_trace (da, trace_index);
  if (label) asprintf (&t_label, "%s :%2d", label, trace_index);
  else       asprintf (&t_label, ":%2d", trace_index);
  ftk_trace_label_modified (trace)	= TRUE;
  ftk_da_trace_modified (da)	= TRUE;
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
	
  FtkTrace * trace;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }
  
  trace = ftk_da_trace (da, trace_index);
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
 * Grab all the selected traces.
 */
 GList *
 ftk_eventviewer_get_selected_traces (FtkEventViewer * eventviewer)
 {
 	FtkDrawingArea *da = ftk_ev_da(eventviewer);
 	GList * traces = NULL;
 	for (long traceID = 0; traceID < ftk_da_trace_next(da); 
 	traceID++) 
 	{
 		FtkTrace *trace = ftk_da_trace(da, traceID);
 		if (ftk_trace_selected(trace)) 
 		{		
 			
 			traces = g_list_append(traces, GINT_TO_POINTER(traceID));
 		}
 	}
	
 	return traces;
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
  
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  
  if (/*(glyph != FTK_GLYPH_AUTOMATIC) && */((glyph < 0) || (glyph >= FTK_GLYPH_LAST))) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_EV_ERROR_INVALID_GLYPH,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return -1;
  }

	 
#define FTK_MARKER_INCR 8
  
  if (ftk_legend_markers_max (legend) <= ftk_legend_markers_next (legend)) {
    ftk_legend_markers_max (legend) += FTK_MARKER_INCR;
    ftk_legend_markers (legend)
      = realloc (ftk_legend_markers (legend),
		 ftk_legend_markers_max (legend) * sizeof(ftk_marker_s));
  }
  
  marker = ftk_legend_marker (legend, ftk_legend_markers_next (legend));
  ftk_marker_index(marker) = ftk_legend_markers_next(legend);
  ftk_marker_gc (marker)    = NULL;
  ftk_marker_label (marker) =
    gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), label);
  ftk_marker_label_modified(marker) = TRUE;
//  if (glyph == FTK_GLYPH_AUTOMATIC) {
//    gint ci = ftk_ev_next_color (eventviewer)++;
//    //ftk_ev_next_color (eventviewer) %= colors_count;
//
//    ftk_marker_glyph (marker) = ftk_ev_next_glyph (eventviewer)++;
//    ftk_ev_next_glyph (eventviewer) %= db_symbols_count;
//
//    ftk_marker_color (marker) = *ftk_ev_color_value(eventviewer, ci);
//  }
//  else 
  {
    ftk_marker_glyph (marker) = glyph;
    ftk_marker_color_red (marker)   = 0;
    ftk_marker_color_green (marker) = 0;
    ftk_marker_color_blue (marker)  = 0;
  }
  
  pango_layout_get_pixel_size (ftk_marker_label(marker),
			       &ftk_marker_label_width(marker),
			       &ftk_marker_label_height(marker));

  ftk_marker_string (marker) = string ? strdup (string) : NULL;
  ftk_legend_markers_modified (legend) = TRUE;

  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) {
    ftk_eventviewer_legend_da_expose(GTK_WIDGET(ftk_ev_legend_da(eventviewer)),
				     NULL, eventviewer);
    ftk_eventviewer_da_expose(GTK_WIDGET(ftk_ev_da(eventviewer)),
			      NULL, eventviewer);
  }

  return ftk_legend_markers_next (legend)++;
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
  
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  if ((marker_index < 0) ||
      (marker_index >= ftk_legend_markers_next (legend))) {
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

  marker = ftk_legend_marker (legend, marker_index);
  ftk_marker_color_red (marker)		= red;
  ftk_marker_color_green (marker)	= green;
  ftk_marker_color_blue (marker)	= blue;
  ftk_legend_markers_modified (legend)	= TRUE;
	
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
 
  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (eventviewer))) {
    ftk_eventviewer_legend_da_expose(GTK_WIDGET(ftk_ev_legend_da(eventviewer)), NULL, eventviewer);
  }
 
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
  
  FtkLegend * legend = ftk_ev_legend_da(eventviewer);
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return NULL;
  }
  
  if ((marker_index < 0) ||
      (marker_index >= ftk_legend_markers_next (legend))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return NULL;
  }
  
  marker = ftk_legend_marker (legend, marker_index);
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
  gint tag = -1;
  
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
  gint rc = -1;
  
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return -1;
  }
  
  FtkLegend * legend = ftk_ev_legend_da(eventviewer);
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return -1;
  }

  if ((marker < 0) || (marker >= ftk_legend_markers_next (legend))) {
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
  FtkLegend *legend = ftk_ev_legend_da(eventviewer);
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  if ((marker_index < 0) ||
      (marker_index >= ftk_legend_markers_next (legend))) {
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

    trace_index = va_arg (ap, gint);
    if (-1 == trace_index) break;
    
    marker_index = va_arg (ap, gint);
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
  gint i;
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
  FtkDrawingArea *da = ftk_ev_da(eventviewer);
  FtkTrace * trace;

  if ((trace_index < 0) || (trace_index >= ftk_da_trace_next (da)) ||
      !ftk_trace_valid (ftk_da_trace (da, trace_index))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }
  
  trace = ftk_da_trace (da, trace_index);
  
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
  gint i;
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

/* ================================================================== */

/* ========================== ACCESSIBILITY ========================= */

/* ================================================================== */


static gpointer accessible_parent_class;
static gpointer accessible_trace_parent_class;
static GQuark accessible_private_data_quark = 0;

static gpointer accessible_marker_parent_class;

#define FTK_TYPE_MARKER_ACCESSIBLE (ftk_marker_accessible_get_type())
#define FTK_MARKER_ACCESSIBLE(obj)      (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_TYPE_MARKER_ACCESSIBLE, FtkMarkerAccessible))
#define FTK_IS_MARKER_ACCESSIBLE(obj)   (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_TYPE_MARKER_ACCESSIBLE))

static GType ftk_marker_accessible_get_type (void);

typedef struct
{
  AtkObject parent;
	
  ftk_marker_s * marker;
	
  AtkStateSet *state_set;
	
  GtkWidget *widget;
	
  GtkTextBuffer *text_buffer;
	
} FtkMarkerAccessible;

typedef struct _FtkMarkerAccessibleClass
{
  AtkObjectClass parent_class;
} FtkMarkerAccessibleClass;

static gboolean ftk_marker_accessible_is_showing (FtkMarkerAccessible *item);


//static void
//ftk_marker_accessible_get_extents (AtkComponent *component,
//                                           gint         *x,
//                                           gint         *y,
//                                           gint         *width,
//                                           gint         *height,
//                                           AtkCoordType  coord_type)
//{
//	FtkMarkerAccessible *marker;
//  AtkObject *parent_obj;
//  gint l_x, l_y;
//
//  g_return_if_fail (FTK_IS_MARKER_ACCESSIBLE (component));
//
//  marker = FTK_MARKER_ACCESSIBLE (component);
//  if (!GTK_IS_WIDGET (marker->widget))
//    return;
//
//  if (atk_state_set_contains_state (marker->state_set, ATK_STATE_DEFUNCT))
//    return;
//
//  *width = marker->marker->label_width;
//  *height = marker->marker->label_height;
//  if (ftk_marker_accessible_is_showing (marker))
//    {
//      parent_obj = gtk_widget_get_accessible (marker->widget);
//      atk_component_get_position (ATK_COMPONENT (parent_obj), &l_x, &l_y, coord_type);
//      *x = l_x + marker->marker->glyph_hpos_d;
//      *y = l_y + marker->marker->vpos;
//    }
//  else
//    {
//      *x = G_MININT;
//      *y = G_MININT;
//    }
//}

//static gboolean
//ftk_marker_accessible_grab_focus (AtkComponent *component)
//{
//FtkMarkerAccessible *marker;
//  GtkWidget *toplevel;
//
//  g_return_val_if_fail (FTK_IS_MARKER_ACCESSIBLE (component), FALSE);
//
//  marker = FTK_MARKER_ACCESSIBLE (component);
//  if (!GTK_IS_WIDGET (marker->widget))
//    return FALSE;
//
//  gtk_widget_grab_focus (marker->widget);
//  //gtk_icon_view_set_cursor_item (FTK_DRAWING_AREA (marker->widget), marker->marker, -1);
//  toplevel = gtk_widget_get_toplevel (GTK_WIDGET (marker->widget));
//  if (GTK_WIDGET_TOPLEVEL (toplevel))
//    gtk_window_present (GTK_WINDOW (toplevel));
//
//  return TRUE;
//}
//
//static void
//atk_component_item_interface_init (AtkComponentIface *iface)
//{
//iface->get_extents = ftk_marker_accessible_get_extents;
//  iface->grab_focus = ftk_marker_accessible_grab_focus;
//}

static gboolean
ftk_marker_accessible_add_state (FtkMarkerAccessible *marker,
					     AtkStateType               state_type,
					     gboolean                   emit_signal)
{
  gboolean rc;

  rc = atk_state_set_add_state (marker->state_set, state_type);
  /*
   * The signal should only be generated if the value changed,
   * not when the item is set up.  So states that are set
   * initially should pass FALSE as the emit_signal argument.
   */

  if (emit_signal)
    {
      atk_object_notify_state_change (ATK_OBJECT (marker), state_type, TRUE);
      /* If state_type is ATK_STATE_VISIBLE, additional notification */
      if (state_type == ATK_STATE_VISIBLE)
        g_signal_emit_by_name (marker, "visible_data_changed");
    }

  return rc;
}

static gboolean
ftk_marker_accessible_remove_state (FtkMarkerAccessible *marker,
						AtkStateType               state_type,
						gboolean                   emit_signal)
{
  if (atk_state_set_contains_state (marker->state_set, state_type))
    {
      gboolean rc;

      rc = atk_state_set_remove_state (marker->state_set, state_type);
      /*
       * The signal should only be generated if the value changed,
       * not when the item is set up.  So states that are set
       * initially should pass FALSE as the emit_signal argument.
       */

      if (emit_signal)
        {
          atk_object_notify_state_change (ATK_OBJECT (marker), state_type, FALSE);
          /* If state_type is ATK_STATE_VISIBLE, additional notification */
          if (state_type == ATK_STATE_VISIBLE)
            g_signal_emit_by_name (marker, "visible_data_changed");
        }

      return rc;
    }
  else
    return FALSE;
}

static gboolean
ftk_marker_accessible_is_showing (FtkMarkerAccessible *marker)
{
  GdkRectangle visible_rect;
  gboolean is_showing;

  /*
   * An item is considered "SHOWING" if any part of the item is in the
   * visible rectangle.
   */

  if (!FTK_IS_DRAWING_AREA (marker->widget))
    return FALSE;

  if (marker->marker == NULL)
    return FALSE;

 visible_rect.x = 0;
  visible_rect.y = 0;
  visible_rect.width = marker->widget->allocation.width;
  visible_rect.height = marker->widget->allocation.height;

  if (((marker->marker->glyph_hpos_d + marker->marker->label_width) < visible_rect.x) ||
      ((marker->marker->vpos + marker->marker->label_height) < (visible_rect.y)) ||
      (marker->marker->glyph_hpos_d > (visible_rect.x + visible_rect.width)) ||
      (marker->marker->vpos > (visible_rect.y + visible_rect.height)))
    is_showing =  FALSE;
  else
    is_showing = TRUE;

  return is_showing;
}

static gboolean
ftk_marker_accessible_set_visibility (FtkMarkerAccessible *marker,
						  gboolean                   emit_signal)
{
  if (ftk_marker_accessible_is_showing (marker))
    return ftk_marker_accessible_add_state (marker, ATK_STATE_SHOWING,
							emit_signal);
  else
    return ftk_marker_accessible_remove_state (marker, ATK_STATE_SHOWING,
							   emit_signal);
}

static void
ftk_marker_accessible_object_init (FtkMarkerAccessible *marker)
{

  marker->state_set = atk_state_set_new ();

  atk_state_set_add_state (marker->state_set, ATK_STATE_ENABLED);
  atk_state_set_add_state (marker->state_set, ATK_STATE_FOCUSABLE);
  atk_state_set_add_state (marker->state_set, ATK_STATE_SENSITIVE);
  atk_state_set_add_state (marker->state_set, ATK_STATE_VISIBLE);
}

static void
ftk_marker_accessible_finalize (GObject *object)
{
  FtkMarkerAccessible *marker;
 
  g_return_if_fail (FTK_IS_MARKER_ACCESSIBLE (object));

  marker = FTK_MARKER_ACCESSIBLE (object);

  if (marker->widget)
    g_object_remove_weak_pointer (G_OBJECT (marker->widget), (gpointer) &marker->widget);

  if (marker->state_set)
    g_object_unref (marker->state_set);

  if (marker->text_buffer)
    g_object_unref (marker->text_buffer);

  G_OBJECT_CLASS (accessible_marker_parent_class)->finalize (object);
}

static G_CONST_RETURN gchar*
ftk_marker_accessible_get_name (AtkObject *obj)
{
  if (obj->name)
    return obj->name;
  else
    {
      FtkMarkerAccessible *marker;
      GtkTextIter start_iter;
      GtkTextIter end_iter;

      marker = FTK_MARKER_ACCESSIBLE (obj);
 
      gtk_text_buffer_get_start_iter (marker->text_buffer, &start_iter); 
      gtk_text_buffer_get_end_iter (marker->text_buffer, &end_iter); 

      return gtk_text_buffer_get_text (marker->text_buffer, &start_iter, &end_iter, FALSE);
    }
}

static AtkObject*
ftk_marker_accessible_get_parent (AtkObject *obj)
{
  FtkMarkerAccessible *marker;

  g_return_val_if_fail (FTK_IS_MARKER_ACCESSIBLE (obj), NULL);
  marker = FTK_MARKER_ACCESSIBLE (obj);

  if (marker->widget)
    return gtk_widget_get_accessible (marker->widget);
  else
    return NULL;
}

static gint
ftk_marker_accessible_get_index_in_parent (AtkObject *obj)
{
  FtkMarkerAccessible *marker;

  g_return_val_if_fail (FTK_IS_MARKER_ACCESSIBLE (obj), 0);
  marker = FTK_MARKER_ACCESSIBLE (obj);

  return marker->marker->index; 
}

static AtkStateSet *
ftk_marker_accessible_ref_state_set (AtkObject *obj)
{
  FtkMarkerAccessible *marker;
  FtkDrawingArea *da;

  marker = FTK_MARKER_ACCESSIBLE (obj);
  g_return_val_if_fail (marker->state_set, NULL);

  if (!marker->widget)
    return NULL;

  da = FTK_DRAWING_AREA (marker->widget);

  return g_object_ref (marker->state_set);
}

static void
ftk_marker_accessible_class_init (AtkObjectClass *klass)
{
  GObjectClass *gobject_class;

  accessible_marker_parent_class = g_type_class_peek_parent (klass);

  gobject_class = (GObjectClass *)klass;

  gobject_class->finalize = ftk_marker_accessible_finalize;

  klass->get_index_in_parent = ftk_marker_accessible_get_index_in_parent; 
  klass->get_name = ftk_marker_accessible_get_name; 
  klass->get_parent = ftk_marker_accessible_get_parent; 
  klass->ref_state_set = ftk_marker_accessible_ref_state_set; 
}

static GType
ftk_marker_accessible_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
	{
	  sizeof (FtkMarkerAccessibleClass),
	  (GBaseInitFunc) NULL, /* base init */
	  (GBaseFinalizeFunc) NULL, /* base finalize */
	  (GClassInitFunc) ftk_marker_accessible_class_init, /* class init */
	  (GClassFinalizeFunc) NULL, /* class finalize */
	  NULL, /* class data */
	  sizeof (FtkMarkerAccessible), /* instance size */
	  0, /* nb preallocs */
	  (GInstanceInitFunc) ftk_marker_accessible_object_init, /* instance init */
	  NULL /* value table */
	};

      //      static const GInterfaceInfo atk_component_info =
      //      {
      //        (GInterfaceInitFunc) atk_component_item_interface_init,
      //        (GInterfaceFinalizeFunc) NULL,
      //        NULL
      //      };
      type = g_type_register_static (ATK_TYPE_OBJECT,
                                     _("FtkMarkerAccessible"), &tinfo, 0);
      //      g_type_add_interface_static (type, ATK_TYPE_COMPONENT,
      //                                   &atk_component_info);
    }

  return type;
}

static gpointer accessible_trace_parent_class;

#define FTK_TYPE_TRACE_ACCESSIBLE (ftk_trace_accessible_get_type())
#define FTK_TRACE_ACCESSIBLE(obj)      (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_TYPE_TRACE_ACCESSIBLE, FtkTraceAccessible))
#define FTK_IS_TRACE_ACCESSIBLE(obj)   (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_TYPE_TRACE_ACCESSIBLE))

static GType ftk_trace_accessible_get_type (void);

typedef struct
{
  AtkObject parent;
	
  FtkTrace * trace;
	
  AtkStateSet *state_set;
	
  GtkWidget *widget;
	
  GtkTextBuffer *text_buffer;
	
} FtkTraceAccessible;

typedef struct _FtkTraceAccessibleClass
{
  AtkObjectClass parent_class;
} FtkTraceAccessibleClass;

static gboolean ftk_trace_accessible_is_showing (FtkTraceAccessible *item);


//static void
//ftk_trace_accessible_get_extents (AtkComponent *component,
//                                           gint         *x,
//                                           gint         *y,
//                                           gint         *width,
//                                           gint         *height,
//                                           AtkCoordType  coord_type)
//{
//	FtkTraceAccessible *trace;
//  AtkObject *parent_obj;
//  gint l_x, l_y;
//
//  g_return_if_fail (FTK_IS_TRACE_ACCESSIBLE (component));
//
//  trace = FTK_TRACE_ACCESSIBLE (component);
//  if (!GTK_IS_WIDGET (trace->widget))
//    return;
//
//  if (atk_state_set_contains_state (trace->state_set, ATK_STATE_DEFUNCT))
//    return;
//
//  *width = trace->trace->label_width;
//  *height = trace->trace->label_height;
//  if (ftk_trace_accessible_is_showing (trace))
//    {
//      parent_obj = gtk_widget_get_accessible (trace->widget);
//      atk_component_get_position (ATK_COMPONENT (parent_obj), &l_x, &l_y, coord_type);
//      *x = l_x + trace->trace->glyph_hpos_d;
//      *y = l_y + trace->trace->vpos;
//    }
//  else
//    {
//      *x = G_MININT;
//      *y = G_MININT;
//    }
//}

//static gboolean
//ftk_trace_accessible_grab_focus (AtkComponent *component)
//{
//FtkTraceAccessible *trace;
//  GtkWidget *toplevel;
//
//  g_return_val_if_fail (FTK_IS_TRACE_ACCESSIBLE (component), FALSE);
//
//  trace = FTK_TRACE_ACCESSIBLE (component);
//  if (!GTK_IS_WIDGET (trace->widget))
//    return FALSE;
//
//  gtk_widget_grab_focus (trace->widget);
//  //gtk_icon_view_set_cursor_item (FTK_DRAWING_AREA (trace->widget), trace->trace, -1);
//  toplevel = gtk_widget_get_toplevel (GTK_WIDGET (trace->widget));
//  if (GTK_WIDGET_TOPLEVEL (toplevel))
//    gtk_window_present (GTK_WINDOW (toplevel));
//
//  return TRUE;
//}
//
//static void
//atk_component_item_interface_init (AtkComponentIface *iface)
//{
//iface->get_extents = ftk_trace_accessible_get_extents;
//  iface->grab_focus = ftk_trace_accessible_grab_focus;
//}

static gboolean
ftk_trace_accessible_add_state (FtkTraceAccessible *trace,
					    AtkStateType               state_type,
					    gboolean                   emit_signal)
{
  gboolean rc;

  rc = atk_state_set_add_state (trace->state_set, state_type);
  /*
   * The signal should only be generated if the value changed,
   * not when the item is set up.  So states that are set
   * initially should pass FALSE as the emit_signal argument.
   */

  if (emit_signal)
    {
      atk_object_notify_state_change (ATK_OBJECT (trace), state_type, TRUE);
      /* If state_type is ATK_STATE_VISIBLE, additional notification */
      if (state_type == ATK_STATE_VISIBLE)
        g_signal_emit_by_name (trace, "visible_data_changed");
    }

  return rc;
}

static gboolean
ftk_trace_accessible_remove_state (FtkTraceAccessible *trace,
					       AtkStateType               state_type,
					       gboolean                   emit_signal)
{
  if (atk_state_set_contains_state (trace->state_set, state_type))
    {
      gboolean rc;

      rc = atk_state_set_remove_state (trace->state_set, state_type);
      /*
       * The signal should only be generated if the value changed,
       * not when the item is set up.  So states that are set
       * initially should pass FALSE as the emit_signal argument.
       */

      if (emit_signal)
        {
          atk_object_notify_state_change (ATK_OBJECT (trace), state_type, FALSE);
          /* If state_type is ATK_STATE_VISIBLE, additional notification */
          if (state_type == ATK_STATE_VISIBLE)
            g_signal_emit_by_name (trace, "visible_data_changed");
        }

      return rc;
    }
  else
    return FALSE;
}

static gboolean
ftk_trace_accessible_is_showing (FtkTraceAccessible *trace)
{
  FtkDrawingArea *da;
  GdkRectangle visible_rect;
  gboolean is_showing;

  /*
   * An item is considered "SHOWING" if any part of the item is in the
   * visible rectangle.
   */

  if (!FTK_IS_DRAWING_AREA (trace->widget))
    return FALSE;

  if (trace->trace == NULL)
    return FALSE;

  da = FTK_DRAWING_AREA (trace->widget);
  visible_rect.x = 0;
  visible_rect.y = 0;
//  if (gtk_scrolled_window_get_vadjustment(ftk_ev_da_scrolled_window(da)))
//    visible_rect.y += gtk_scrolled_window_get_vadjustment(ftk_ev_da_scrolled_window(da))->value;
//  	
  visible_rect.width = trace->widget->allocation.width;
  visible_rect.height = trace->widget->allocation.height;

  if (((trace->trace->vpos_d + trace->trace->label_height_d) < (visible_rect.y)) ||
      (trace->trace->vpos_d > (visible_rect.y + visible_rect.height)))
    is_showing =  FALSE;
  else
    is_showing = TRUE;

  return is_showing;
}

static gboolean
ftk_trace_accessible_set_visibility (FtkTraceAccessible *trace,
						 gboolean                   emit_signal)
{
  if (ftk_trace_accessible_is_showing (trace))
    return ftk_trace_accessible_add_state (trace, ATK_STATE_SHOWING,
						       emit_signal);
  else
    return ftk_trace_accessible_remove_state (trace, ATK_STATE_SHOWING,
							  emit_signal);
}

static void
ftk_trace_accessible_object_init (FtkTraceAccessible *trace)
{

  trace->state_set = atk_state_set_new ();

  atk_state_set_add_state (trace->state_set, ATK_STATE_ENABLED);
  atk_state_set_add_state (trace->state_set, ATK_STATE_FOCUSABLE);
  atk_state_set_add_state (trace->state_set, ATK_STATE_SENSITIVE);
  atk_state_set_add_state (trace->state_set, ATK_STATE_VISIBLE);
}

static void
ftk_trace_accessible_finalize (GObject *object)
{
  FtkTraceAccessible *trace;
 
  g_return_if_fail (FTK_IS_TRACE_ACCESSIBLE (object));

  trace = FTK_TRACE_ACCESSIBLE (object);

  if (trace->widget)
    g_object_remove_weak_pointer (G_OBJECT (trace->widget), (gpointer) &trace->widget);

  if (trace->state_set)
    g_object_unref (trace->state_set);

  if (trace->text_buffer)
    g_object_unref (trace->text_buffer);

  G_OBJECT_CLASS (accessible_trace_parent_class)->finalize (object);
}

static G_CONST_RETURN gchar*
ftk_trace_accessible_get_name (AtkObject *obj)
{
  if (obj->name)
    return obj->name;
  else
    {
      FtkTraceAccessible *trace;
      GtkTextIter start_iter;
      GtkTextIter end_iter;

      trace = FTK_TRACE_ACCESSIBLE (obj);
 
      gtk_text_buffer_get_start_iter (trace->text_buffer, &start_iter); 
      gtk_text_buffer_get_end_iter (trace->text_buffer, &end_iter); 

      return gtk_text_buffer_get_text (trace->text_buffer, &start_iter, &end_iter, FALSE);
    }
}

static AtkObject*
ftk_trace_accessible_get_parent (AtkObject *obj)
{
  FtkTraceAccessible *trace;

  g_return_val_if_fail (FTK_IS_TRACE_ACCESSIBLE (obj), NULL);
  trace = FTK_TRACE_ACCESSIBLE (obj);

  if (trace->widget)
    return gtk_widget_get_accessible (trace->widget);
  else
    return NULL;
}

static gint
ftk_trace_accessible_get_index_in_parent (AtkObject *obj)
{
  FtkTraceAccessible *trace;

  g_return_val_if_fail (FTK_IS_TRACE_ACCESSIBLE (obj), 0);
  trace = FTK_TRACE_ACCESSIBLE (obj);

  return trace->trace->index; 
}

static AtkStateSet *
ftk_trace_accessible_ref_state_set (AtkObject *obj)
{
  FtkTraceAccessible *trace;
  
  trace = FTK_TRACE_ACCESSIBLE (obj);
  g_return_val_if_fail (trace->state_set, NULL);

  if (!trace->widget)
    return NULL;

  return g_object_ref (trace->state_set);
}

static void
ftk_trace_accessible_class_init (AtkObjectClass *klass)
{
  GObjectClass *gobject_class;

  accessible_trace_parent_class = g_type_class_peek_parent (klass);

  gobject_class = (GObjectClass *)klass;

  gobject_class->finalize = ftk_trace_accessible_finalize;

  klass->get_index_in_parent = ftk_trace_accessible_get_index_in_parent; 
  klass->get_name = ftk_trace_accessible_get_name; 
  klass->get_parent = ftk_trace_accessible_get_parent; 
  klass->ref_state_set = ftk_trace_accessible_ref_state_set; 
}

static GType
ftk_trace_accessible_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
	{
	  sizeof (FtkTraceAccessibleClass),
	  (GBaseInitFunc) NULL, /* base init */
	  (GBaseFinalizeFunc) NULL, /* base finalize */
	  (GClassInitFunc) ftk_trace_accessible_class_init, /* class init */
	  (GClassFinalizeFunc) NULL, /* class finalize */
	  NULL, /* class data */
	  sizeof (FtkTraceAccessible), /* instance size */
	  0, /* nb preallocs */
	  (GInstanceInitFunc) ftk_trace_accessible_object_init, /* instance init */
	  NULL /* value table */
	};

      //      static const GInterfaceInfo atk_component_info =
      //      {
      //        (GInterfaceInitFunc) atk_component_item_interface_init,
      //        (GInterfaceFinalizeFunc) NULL,
      //        NULL
      //      };
      type = g_type_register_static (ATK_TYPE_OBJECT,
                                     _("FtkTraceAccessible"), &tinfo, 0);
      //      g_type_add_interface_static (type, ATK_TYPE_COMPONENT,
      //                                   &atk_component_info);
    }

  return type;
}

#define FTK_TYPE_DRAWING_AREA_ACCESSIBLE      (ftk_drawing_area_accessible_get_type ())
#define FTK_DRAWING_AREA_ACCESSIBLE(obj)      (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_TYPE_DRAWING_AREA_ACCESSIBLE, FtkDrawingAreaAccessible))
#define FTK_IS_DRAWING_AREA_ACCESSIBLE(obj)   (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_TYPE_DRAWING_AREA_ACCESSIBLE))

static GType ftk_drawing_area_accessible_get_type (void);

typedef struct
{
  AtkObject parent;
} FtkDrawingAreaAccessible;

typedef struct
{
  AtkObject *marker;
  gint       index;
} FtkMarkerAccessibleInfo;

typedef struct
{
  AtkObject *trace;
  gint index;
}	FtkTraceAccessibleInfo;

typedef struct
{
  GList *buttons;
  GList *markers;

  GList *traces;
  //  GList *events;
} FtkDrawingAreaAccessiblePrivate;

static FtkDrawingAreaAccessiblePrivate *
ftk_drawing_area_accessible_get_priv (AtkObject *accessible)
{
  return g_object_get_qdata (G_OBJECT (accessible),
                             accessible_private_data_quark);
}

static void
ftk_marker_accessible_info_new (AtkObject *accessible,
					    AtkObject *marker,
					    gint       index)
{
  FtkMarkerAccessibleInfo *info;
  FtkMarkerAccessibleInfo *tmp_info;
  FtkDrawingAreaAccessiblePrivate *priv;
  GList *markers;

  info = g_new (FtkMarkerAccessibleInfo, 1);
  info->marker = marker;
  info->index = index;

  priv = ftk_drawing_area_accessible_get_priv (accessible);
  markers = priv->markers;
  while (markers)
    {
      tmp_info = markers->data;
      if (tmp_info->index > index)
        break;
      markers = markers->next;
    }
  priv->markers = g_list_insert_before (priv->markers, markers, info);

}

static void
ftk_trace_accessible_info_new (AtkObject *accessible,
					   AtkObject *trace,
					   gint index)
{
  FtkTraceAccessibleInfo *info;
  FtkTraceAccessibleInfo *tmp_info;
  FtkDrawingAreaAccessiblePrivate *priv;
  GList *traces;
	
  info = g_new (FtkTraceAccessibleInfo, 1);
  info->trace = trace;
  info->index = index;
	
  priv = ftk_drawing_area_accessible_get_priv (accessible);
  traces = priv->traces;
  while (traces)
    {
      tmp_info = traces->data;
      if (tmp_info->index > index)
	break;
      traces=traces->next;	
    }
  priv->traces = g_list_insert_before (priv->traces, traces, info);
}

static gint
ftk_drawing_area_accessible_get_n_children (AtkObject *accessible)
{
  FtkDrawingArea *da;
  GtkWidget *widget;

  widget = GTK_ACCESSIBLE (accessible)->widget;
  if (!widget)
    return 0;

  da = FTK_DRAWING_AREA(widget);
  return ftk_da_trace_next(da);
}

static AtkObject *
ftk_drawing_area_accessible_find_child (AtkObject *accessible,
				       gint       index)
{
  GtkWidget *widget = GTK_ACCESSIBLE (accessible)->widget;
	
  if (!widget)
    return 0;
		
  FtkDrawingAreaAccessiblePrivate *priv;
  FtkTraceAccessibleInfo *trace_info;
  GList *traces;
  		
  
      priv = ftk_drawing_area_accessible_get_priv (accessible);      
      traces = priv->traces;
    	
      while (traces)
    	{
	  trace_info = traces->data;
	  if (trace_info->index == index)
	    return trace_info->trace;
	  traces = traces->next;
    	}
      
  return NULL;
}

static AtkObject *
ftk_drawing_area_accessible_ref_child (AtkObject *accessible,
				      gint       index)
{
  //GtkDrawingArea *legend;
  FtkDrawingArea *da;
  GtkWidget *widget;
  //GList *icons;
  AtkObject *obj;
  FtkTraceAccessible *trace_item;

  widget = GTK_ACCESSIBLE (accessible)->widget;
  if (!widget)
    return NULL;

  //legend = GTK_DRAWING_AREA (widget);
  da = FTK_DRAWING_AREA(widget);

  obj = ftk_drawing_area_accessible_find_child(accessible, index);
  
 
  if (!obj)
    {
       		
	FtkTrace *trace = ftk_da_trace(da, index);
	obj = g_object_new (ftk_trace_accessible_get_type(), NULL);
	ftk_trace_accessible_info_new (accessible, obj, index);
  		
	obj->role = ATK_ROLE_UNKNOWN;
	trace_item = FTK_TRACE_ACCESSIBLE(obj);
	trace_item->trace = trace;
	trace_item->widget = widget;
	trace_item->text_buffer = gtk_text_buffer_new (NULL);
  		
	gtk_text_buffer_set_text (trace_item->text_buffer,
				  pango_layout_get_text (trace->label), -1);
  						
	ftk_trace_accessible_set_visibility (trace_item, FALSE);
	g_object_add_weak_pointer (G_OBJECT (widget), (gpointer) & (trace_item->widget));
      
    }
   
  g_object_ref (obj);

  return obj;
}

static void
ftk_drawing_area_accessible_clear_cache (FtkDrawingAreaAccessiblePrivate *priv)
{
  FtkMarkerAccessibleInfo *info;
  GList *markers;

  markers = priv->markers;
  while (markers)
    {
      info = (FtkMarkerAccessibleInfo *) markers->data;
      g_object_unref (info->marker);
      g_free (markers->data);
      markers = markers->next;
    }
  g_list_free (priv->markers);
  priv->markers = NULL;
  
  FtkTraceAccessibleInfo *trace_info;
  GList * traces;
  
  traces = priv->traces;
  while (traces)
    {
      trace_info = (FtkTraceAccessibleInfo *) traces->data;
      g_object_unref(trace_info->trace);
      g_free (traces->data);
      traces = traces->next;
    }
  g_list_free (priv->traces);
  priv->traces = NULL;
}

//static void
//ftk_drawing_area_accessible_notify_gtk (GObject *obj,
//                                     GParamSpec *pspec)
//{
//  GtkDrawingArea *legend;
//  GtkWidget *widget;
//  AtkObject *atk_obj;
//  FtkDrawingAreaAccessiblePrivate *priv;
//
//  if (strcmp (pspec->name, "model") == 0)
//    {
//      widget = GTK_WIDGET (obj); 
//      atk_obj = gtk_widget_get_accessible (widget);
//      priv = ftk_drawing_area_accessible_get_priv (atk_obj);
//      if (priv->model)
//        {
//          g_object_remove_weak_pointer (G_OBJECT (priv->model),
//                                        (gpointer *)&priv->model);
//          gtk_icon_view_accessible_disconnect_model_signals (priv->model, widget);
//        }
//      ftk_drawing_area_accessible_clear_cache (priv);
//
//      legend = GTK_DRAWING_AREA (obj);
//      priv->model = legend->priv->model;
//      /* If there is no model the GtkIconView is probably being destroyed */
//      if (priv->model)
//        {
//          g_object_add_weak_pointer (G_OBJECT (priv->model), (gpointer *)&priv->model);
//          gtk_icon_view_accessible_connect_model_signals (icon_view);
//        }
//    }
//
//  return;
//}

static void
ftk_drawing_area_accessible_initialize (AtkObject *accessible,
				       gpointer   data)
{
  FtkDrawingAreaAccessiblePrivate *priv;
  // GtkDrawingArea *legend;
  
  if (ATK_OBJECT_CLASS (accessible_parent_class)->initialize)
    ATK_OBJECT_CLASS (accessible_parent_class)->initialize (accessible, data);

  priv = g_new0 (FtkDrawingAreaAccessiblePrivate, 1);
  g_object_set_qdata (G_OBJECT (accessible),
                      accessible_private_data_quark,
                      priv);

  //legend = GTK_DRAWING_AREA (data);
  
  //  g_signal_connect (data,
  //                    "notify",
  //                    G_CALLBACK (gtk_icon_view_accessible_notify_gtk),
  //                    NULL);

  //  priv->model = icon_view->priv->model;
  //  if (priv->model)
  //    {
  //      g_object_add_weak_pointer (G_OBJECT (priv->model), (gpointer *)&priv->model);
  //      gtk_icon_view_accessible_connect_model_signals (icon_view);
  //    }
                          
  accessible->role = ATK_ROLE_DRAWING_AREA;
}

static void
ftk_drawing_area_accessible_finalize (GObject *object)
{
  FtkDrawingAreaAccessiblePrivate *priv;

  priv = ftk_drawing_area_accessible_get_priv (ATK_OBJECT (object));
  ftk_drawing_area_accessible_clear_cache (priv);

  g_free (priv);

  G_OBJECT_CLASS (accessible_parent_class)->finalize (object);
}

static void
ftk_drawing_area_accessible_destroyed (GtkWidget *widget,
				      GtkAccessible *accessible)
{
  AtkObject *atk_obj;
  FtkDrawingAreaAccessiblePrivate *priv;

  atk_obj = ATK_OBJECT (accessible);
  priv = ftk_drawing_area_accessible_get_priv (atk_obj);
}

static void
ftk_drawing_area_accessible_connect_widget_destroyed (GtkAccessible *accessible)
{
  if (accessible->widget)
    {
      g_signal_connect_after (accessible->widget,
                              "destroy",
                              G_CALLBACK (ftk_drawing_area_accessible_destroyed),
                              accessible);
    }
  GTK_ACCESSIBLE_CLASS (accessible_parent_class)->connect_widget_destroyed (accessible);
}

static void
ftk_drawing_area_accessible_class_init (AtkObjectClass *klass)
{
  GObjectClass *gobject_class;
  GtkAccessibleClass *accessible_class;

  accessible_parent_class = g_type_class_peek_parent (klass);

  gobject_class = (GObjectClass *)klass;
  accessible_class = (GtkAccessibleClass *)klass;

  gobject_class->finalize = ftk_drawing_area_accessible_finalize;

  klass->get_n_children = ftk_drawing_area_accessible_get_n_children;
  klass->ref_child = ftk_drawing_area_accessible_ref_child;
  klass->initialize = ftk_drawing_area_accessible_initialize;

  accessible_class->connect_widget_destroyed = ftk_drawing_area_accessible_connect_widget_destroyed;

  accessible_private_data_quark = g_quark_from_static_string ("event_viewer-accessible-private-data");
}

static AtkObject*
ftk_drawing_area_accessible_ref_accessible_at_point (AtkComponent *component,
						    gint          x,
						    gint          y,
						    AtkCoordType  coord_type)
{
  GtkWidget *widget;
 gint x_pos, y_pos;

  widget = GTK_ACCESSIBLE (component)->widget;
  if (widget == NULL)
    /* State is defunct */
    return NULL;

  atk_component_get_extents (component, &x_pos, &y_pos, NULL, NULL, coord_type);
  // XXX: Must create a function to find marker at given x, y coordinates.
  //  marker = ftk_eventviewer_legend_get_item_at_coords (legend, x - x_pos, y - y_pos, TRUE, NULL);
  //  if (marker)
  //    return ftk_drawing_area_accessible_ref_child (ATK_OBJECT (component), marker->index);

  return NULL;
}

static void
atk_component_interface_init (AtkComponentIface *iface)
{
  iface->ref_accessible_at_point = ftk_drawing_area_accessible_ref_accessible_at_point;
}

static GType
ftk_drawing_area_accessible_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static GTypeInfo tinfo =
	{
	  0, /* class size */
	  (GBaseInitFunc) NULL, /* base init */
	  (GBaseFinalizeFunc) NULL, /* base finalize */
	  (GClassInitFunc) ftk_drawing_area_accessible_class_init,
	  (GClassFinalizeFunc) NULL, /* class finalize */
	  NULL, /* class data */
	  0, /* instance size */
	  0, /* nb preallocs */
	  (GInstanceInitFunc) NULL, /* instance init */
	  NULL /* value table */
	};
      static const GInterfaceInfo atk_component_info =
	{
	  (GInterfaceInitFunc) atk_component_interface_init,
	  (GInterfaceFinalizeFunc) NULL,
	  NULL
	};
     
      /*
       * Figure out the size of the class and instance
       * we are deriving from
       */
      AtkObjectFactory *factory;
      GType derived_type;
      GTypeQuery query;
      GType derived_atk_type;

      derived_type = g_type_parent (FTK_DRAWING_AREA_TYPE);
      factory = atk_registry_get_factory (atk_get_default_registry (), 
                                          derived_type);
      derived_atk_type = atk_object_factory_get_accessible_type (factory);
      g_type_query (derived_atk_type, &query);
      tinfo.class_size = query.class_size;
      tinfo.instance_size = query.instance_size;
 
      type = g_type_register_static (derived_atk_type, 
                                     "FtkDrawingAreaAccessible", 
                                     &tinfo, 0);
      g_type_add_interface_static (type, ATK_TYPE_COMPONENT,
                                   &atk_component_info);
    }
  return type;
}

static AtkObject *
ftk_drawing_area_accessible_new (GObject *obj)
{
  AtkObject *accessible;

  g_return_val_if_fail (GTK_IS_WIDGET (obj), NULL);

  accessible = g_object_new (ftk_drawing_area_accessible_get_type (), NULL);
  atk_object_initialize (accessible, obj);

  return accessible;
}

static GType
ftk_drawing_area_accessible_factory_get_accessible_type (void)
{
  return ftk_drawing_area_accessible_get_type ();
}

static AtkObject*
ftk_drawing_area_accessible_factory_create_accessible (GObject *obj)
{
  return ftk_drawing_area_accessible_new (obj);
}

static void
ftk_drawing_area_accessible_factory_class_init (AtkObjectFactoryClass *klass)
{
  klass->create_accessible = ftk_drawing_area_accessible_factory_create_accessible;
  klass->get_accessible_type = ftk_drawing_area_accessible_factory_get_accessible_type;
}

static GType
ftk_drawing_area_accessible_factory_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
	{
	  sizeof (AtkObjectFactoryClass),
	  NULL,           /* base_init */
	  NULL,           /* base_finalize */
	  (GClassInitFunc) ftk_drawing_area_accessible_factory_class_init,
	  NULL,           /* class_finalize */
	  NULL,           /* class_data */
	  sizeof (AtkObjectFactory),
	  0,             /* n_preallocs */
	  NULL, NULL
	};

      type = g_type_register_static (ATK_TYPE_OBJECT_FACTORY, 
				     "FtkDrawingAreaAccessibleFactory",
				     &tinfo, 0);
    }
    
  return type;
}


static AtkObject *
ftk_drawing_area_get_accessible (GtkWidget *widget)
{
  static gboolean first_time = TRUE;

  if (first_time)
    {
      AtkObjectFactory *factory;
      AtkRegistry *registry;
      GType derived_type; 
      GType derived_atk_type; 

      /*
       * Figure out whether accessibility is enabled by looking at the
       * type of the accessible object which would be created for
       * the parent type of GtkIconView.
       */
      derived_type = g_type_parent (FTK_DRAWING_AREA_TYPE);

      registry = atk_get_default_registry ();
      factory = atk_registry_get_factory (registry,
                                          derived_type);
      derived_atk_type = atk_object_factory_get_accessible_type (factory);
      if (g_type_is_a (derived_atk_type, GTK_TYPE_ACCESSIBLE)) 
	atk_registry_set_factory_type (registry, 
				       FTK_DRAWING_AREA_TYPE,
				       ftk_drawing_area_accessible_factory_get_type ());
      first_time = FALSE;
    } 
 
  return (* GTK_WIDGET_CLASS (parent_class)->get_accessible) (widget);
}

//static void
//ftk_trace_accessible_initialize (AtkObject *accessible,
//				       gpointer   data)
//{
//  
//  if (ATK_OBJECT_CLASS (accessible_trace_parent_class)->initialize)
//    ATK_OBJECT_CLASS (accessible_trace_parent_class)->initialize (accessible, data);
//
//                     
//  accessible->role = ATK_ROLE_UNKNOWN;
//}
//
//static void
//ftk_trace_accessible_finalize (GObject *object)
//{
//  G_OBJECT_CLASS (accessible_trace_parent_class)->finalize (object);
//}
//
//static void
//ftk_trace_accessible_destroyed (GtkWidget *widget,
//				      GtkAccessible *accessible)
//{
//}
//
//static void
//ftk_trace_accessible_connect_widget_destroyed (GtkAccessible *accessible)
//{
//  if (accessible->widget)
//    {
//      g_signal_connect_after (accessible->widget,
//                              "destroy",
//                              G_CALLBACK (ftk_trace_accessible_destroyed),
//                              accessible);
//    }
//  GTK_ACCESSIBLE_CLASS (accessible_trace_parent_class)->connect_widget_destroyed (accessible);
//}

static AtkObject *
ftk_trace_accessible_new (GObject *obj)
{
  AtkObject *accessible;

  g_return_val_if_fail (GTK_IS_WIDGET (obj), NULL);

  accessible = g_object_new (ftk_trace_accessible_get_type (), NULL);
  atk_object_initialize (accessible, obj);

  return accessible;
}

static GType
ftk_trace_accessible_factory_get_accessible_type (void)
{
  return ftk_trace_accessible_get_type ();
}

static AtkObject*
ftk_trace_accessible_factory_create_accessible (GObject *obj)
{
  return ftk_trace_accessible_new (obj);
}

static void
ftk_trace_accessible_factory_class_init (AtkObjectFactoryClass *klass)
{
  klass->create_accessible = ftk_trace_accessible_factory_create_accessible;
  klass->get_accessible_type = ftk_trace_accessible_factory_get_accessible_type;
}

static GType
ftk_trace_accessible_factory_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
	{
	  sizeof (AtkObjectFactoryClass),
	  NULL,           /* base_init */
	  NULL,           /* base_finalize */
	  (GClassInitFunc) ftk_trace_accessible_factory_class_init,
	  NULL,           /* class_finalize */
	  NULL,           /* class_data */
	  sizeof (AtkObjectFactory),
	  0,             /* n_preallocs */
	  NULL, NULL
	};

      type = g_type_register_static (ATK_TYPE_OBJECT_FACTORY, 
				     "FtkTraceAccessibleFactory",
				     &tinfo, 0);
    }
    
  return type;
}

static AtkObject *
ftk_trace_get_accessible (GtkWidget *widget)
{
	static gboolean first_time = TRUE;
	
	if (first_time)
	{
		AtkObjectFactory *factory;
		AtkRegistry *registry;
		GType derived_type;
		GType derived_atk_type;
		
	  /*
       * Figure out whether accessibility is enabled by looking at the
       * type of the accessible object which would be created for
       * the parent type of GtkIconView.
       */
      derived_type = g_type_parent (FTK_TRACE_TYPE);

      registry = atk_get_default_registry ();
      factory = atk_registry_get_factory (registry,
                                          derived_type);
      derived_atk_type = atk_object_factory_get_accessible_type (factory);
      if (g_type_is_a (derived_atk_type, GTK_TYPE_ACCESSIBLE)) 
	atk_registry_set_factory_type (registry, 
				       FTK_TRACE_TYPE,
				       ftk_trace_accessible_factory_get_type ());
      first_time = FALSE;
    } 
 
  return (* GTK_WIDGET_CLASS (trace_parent_class)->get_accessible) (widget);
}

#define FTK_TYPE_LEGEND_ACCESSIBLE      (ftk_legend_accessible_get_type ())
#define FTK_LEGEND_ACCESSIBLE(obj)      (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_TYPE_LEGEND_ACCESSIBLE, FtkLegendAccessible))
#define FTK_IS_LEGEND_ACCESSIBLE(obj)   (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_TYPE_LEGEND_ACCESSIBLE))

static GType ftk_legend_accessible_get_type (void);

typedef struct
{
  AtkObject parent;
} FtkLegendAccessible;

typedef struct
{
  AtkObject *marker;
  gint       index;
} FtkLegendMarkerAccessibleInfo;

typedef struct
{
  GList *markers;

} FtkLegendAccessiblePrivate;

static FtkLegendAccessiblePrivate *
ftk_legend_accessible_get_priv (AtkObject *accessible)
{
  return g_object_get_qdata (G_OBJECT (accessible),
                             accessible_private_data_quark);
}

static gint
ftk_legend_accessible_get_n_children (AtkObject *accessible)
{
  FtkLegend *legend;
  GtkWidget *widget;

  widget = GTK_ACCESSIBLE (accessible)->widget;
  if (!widget)
    return 0;

  legend = FTK_LEGEND(widget);
  return ftk_legend_markers_next(legend);
}

static AtkObject *
ftk_legend_accessible_find_child (AtkObject *accessible,
				       gint       index)
{
  GtkWidget *widget = GTK_ACCESSIBLE (accessible)->widget;
	
  if (!widget)
    return 0;
		  	
  FtkLegendAccessiblePrivate *priv;
  FtkLegendMarkerAccessibleInfo *info;
  GList *markers;
  		
  
      priv = ftk_legend_accessible_get_priv (accessible);
      markers = priv->markers;

      while (markers)
    	{
	  info = markers->data;
	  if (info->index == index)
	    return info->marker;
	  markers = markers->next; 
    	}
    	           
  return NULL;
}

static AtkObject *
ftk_legend_accessible_ref_child (AtkObject *accessible,
				      gint       index)
{
  //GtkDrawingArea *legend;
  FtkLegend *legend;
  GtkWidget *widget;
  AtkObject *obj;
  FtkMarkerAccessible *ally_item;
 
  widget = GTK_ACCESSIBLE (accessible)->widget;
  if (!widget)
    return NULL;

  //legend = GTK_DRAWING_AREA (widget);
  legend = FTK_LEGEND(widget);

  obj = ftk_legend_accessible_find_child(accessible, index);
  
 
  if (!obj)
    {
    ftk_marker_s *marker = ftk_legend_marker(legend, index);
  	obj = g_object_new (ftk_marker_accessible_get_type(), NULL);
  	ftk_marker_accessible_info_new (accessible, obj, index);
  	
  	obj->role = ATK_ROLE_UNKNOWN;
  	ally_item = FTK_MARKER_ACCESSIBLE(obj);
  	ally_item->marker = marker;
  	ally_item->widget = widget;
  	ally_item->text_buffer = gtk_text_buffer_new (NULL);

	gtk_text_buffer_set_text (ally_item->text_buffer, 
				  pango_layout_get_text (marker->label), -1);

	ftk_marker_accessible_set_visibility (ally_item, FALSE);
	g_object_add_weak_pointer (G_OBJECT (widget), (gpointer) &(ally_item->widget));
    
    }
   
  g_object_ref (obj);

  return obj;
}

static void
ftk_legend_accessible_clear_cache (FtkLegendAccessiblePrivate *priv)
{
  FtkLegendMarkerAccessibleInfo *info;
  GList *markers;

  markers = priv->markers;
  while (markers)
    {
      info = (FtkLegendMarkerAccessibleInfo *) markers->data;
      g_object_unref (info->marker);
      g_free (markers->data);
      markers = markers->next;
    }
  g_list_free (priv->markers);
  priv->markers = NULL;
      
}

//static void
//ftk_legend_accessible_notify_gtk (GObject *obj,
//                                     GParamSpec *pspec)
//{
//  GtkDrawingArea *legend;
//  GtkWidget *widget;
//  AtkObject *atk_obj;
//  FtkLegendAccessiblePrivate *priv;
//
//  if (strcmp (pspec->name, "model") == 0)
//    {
//      widget = GTK_WIDGET (obj); 
//      atk_obj = gtk_widget_get_accessible (widget);
//      priv = ftk_legend_accessible_get_priv (atk_obj);
//      if (priv->model)
//        {
//          g_object_remove_weak_pointer (G_OBJECT (priv->model),
//                                        (gpointer *)&priv->model);
//          gtk_icon_view_accessible_disconnect_model_signals (priv->model, widget);
//        }
//      ftk_legend_accessible_clear_cache (priv);
//
//      legend = GTK_DRAWING_AREA (obj);
//      priv->model = legend->priv->model;
//      /* If there is no model the GtkIconView is probably being destroyed */
//      if (priv->model)
//        {
//          g_object_add_weak_pointer (G_OBJECT (priv->model), (gpointer *)&priv->model);
//          gtk_icon_view_accessible_connect_model_signals (icon_view);
//        }
//    }
//
//  return;
//}

static void
ftk_legend_accessible_initialize (AtkObject *accessible,
				       gpointer   data)
{
  FtkLegendAccessiblePrivate *priv;
  // GtkDrawingArea *legend;
  FtkLegend * legend;

  if (ATK_OBJECT_CLASS (accessible_parent_class)->initialize)
    ATK_OBJECT_CLASS (accessible_parent_class)->initialize (accessible, data);

  priv = g_new0 (FtkLegendAccessiblePrivate, 1);
  g_object_set_qdata (G_OBJECT (accessible),
                      accessible_private_data_quark,
                      priv);

  //legend = GTK_DRAWING_AREA (data);
  legend = FTK_LEGEND (data);
  
  //  g_signal_connect (data,
  //                    "notify",
  //                    G_CALLBACK (gtk_icon_view_accessible_notify_gtk),
  //                    NULL);

  //  priv->model = icon_view->priv->model;
  //  if (priv->model)
  //    {
  //      g_object_add_weak_pointer (G_OBJECT (priv->model), (gpointer *)&priv->model);
  //      gtk_icon_view_accessible_connect_model_signals (icon_view);
  //    }
                          
  accessible->role = ATK_ROLE_DRAWING_AREA;
}

static void
ftk_legend_accessible_finalize (GObject *object)
{
  FtkLegendAccessiblePrivate *priv;

  priv = ftk_legend_accessible_get_priv (ATK_OBJECT (object));
  ftk_legend_accessible_clear_cache (priv);

  g_free (priv);

  G_OBJECT_CLASS (accessible_parent_class)->finalize (object);
}

static void
ftk_legend_accessible_destroyed (GtkWidget *widget,
				      GtkAccessible *accessible)
{
  AtkObject *atk_obj;
  FtkLegendAccessiblePrivate *priv;

  atk_obj = ATK_OBJECT (accessible);
  priv = ftk_legend_accessible_get_priv (atk_obj);
}

static void
ftk_legend_accessible_connect_widget_destroyed (GtkAccessible *accessible)
{
  if (accessible->widget)
    {
      g_signal_connect_after (accessible->widget,
                              "destroy",
                              G_CALLBACK (ftk_legend_accessible_destroyed),
                              accessible);
    }
  GTK_ACCESSIBLE_CLASS (accessible_parent_class)->connect_widget_destroyed (accessible);
}

static void
ftk_legend_accessible_class_init (AtkObjectClass *klass)
{
  GObjectClass *gobject_class;
  GtkAccessibleClass *accessible_class;

  accessible_parent_class = g_type_class_peek_parent (klass);

  gobject_class = (GObjectClass *)klass;
  accessible_class = (GtkAccessibleClass *)klass;

  gobject_class->finalize = ftk_legend_accessible_finalize;

  klass->get_n_children = ftk_legend_accessible_get_n_children;
  klass->ref_child = ftk_legend_accessible_ref_child;
  klass->initialize = ftk_legend_accessible_initialize;

  accessible_class->connect_widget_destroyed = ftk_legend_accessible_connect_widget_destroyed;

  accessible_private_data_quark = g_quark_from_static_string ("event_viewer-accessible-private-data");
}

static AtkObject*
ftk_legend_accessible_ref_accessible_at_point (AtkComponent *component,
						    gint          x,
						    gint          y,
						    AtkCoordType  coord_type)
{
  GtkWidget *widget;
  // GtkDrawingArea *legend;
  FtkLegend *legend;
  // ftk_marker_s *marker;
  gint x_pos, y_pos;

  widget = GTK_ACCESSIBLE (component)->widget;
  if (widget == NULL)
    /* State is defunct */
    return NULL;

  //legend = GTK_DRAWING_AREA (widget);
  legend = FTK_LEGEND (widget);
  atk_component_get_extents (component, &x_pos, &y_pos, NULL, NULL, coord_type);
  // XXX: Must create a function to find marker at given x, y coordinates.
  //  marker = ftk_legend_legend_get_item_at_coords (legend, x - x_pos, y - y_pos, TRUE, NULL);
  //  if (marker)
  //    return ftk_legend_accessible_ref_child (ATK_OBJECT (component), marker->index);

  return NULL;
}

static void
atk_legend_component_interface_init (AtkComponentIface *iface)
{
  iface->ref_accessible_at_point = ftk_legend_accessible_ref_accessible_at_point;
}

static GType
ftk_legend_accessible_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static GTypeInfo tinfo =
	{
	  0, /* class size */
	  (GBaseInitFunc) NULL, /* base init */
	  (GBaseFinalizeFunc) NULL, /* base finalize */
	  (GClassInitFunc) ftk_legend_accessible_class_init,
	  (GClassFinalizeFunc) NULL, /* class finalize */
	  NULL, /* class data */
	  0, /* instance size */
	  0, /* nb preallocs */
	  (GInstanceInitFunc) NULL, /* instance init */
	  NULL /* value table */
	};
      static const GInterfaceInfo atk_component_info =
	{
	  (GInterfaceInitFunc) atk_legend_component_interface_init,
	  (GInterfaceFinalizeFunc) NULL,
	  NULL
	};
     
      /*
       * Figure out the size of the class and instance
       * we are deriving from
       */
      AtkObjectFactory *factory;
      GType derived_type;
      GTypeQuery query;
      GType derived_atk_type;

      derived_type = g_type_parent (FTK_LEGEND_TYPE);
      factory = atk_registry_get_factory (atk_get_default_registry (), 
                                          derived_type);
      derived_atk_type = atk_object_factory_get_accessible_type (factory);
      g_type_query (derived_atk_type, &query);
      tinfo.class_size = query.class_size;
      tinfo.instance_size = query.instance_size;
 
      type = g_type_register_static (derived_atk_type, 
                                     "FtkLegendAccessible", 
                                     &tinfo, 0);
      g_type_add_interface_static (type, ATK_TYPE_COMPONENT,
                                   &atk_component_info);
    }
  return type;
}

static AtkObject *
ftk_legend_accessible_new (GObject *obj)
{
  AtkObject *accessible;

  g_return_val_if_fail (GTK_IS_WIDGET (obj), NULL);

  accessible = g_object_new (ftk_legend_accessible_get_type (), NULL);
  atk_object_initialize (accessible, obj);

  return accessible;
}

static GType
ftk_legend_accessible_factory_get_accessible_type (void)
{
  return ftk_legend_accessible_get_type ();
}

static AtkObject*
ftk_legend_accessible_factory_create_accessible (GObject *obj)
{
  return ftk_legend_accessible_new (obj);
}

static void
ftk_legend_accessible_factory_class_init (AtkObjectFactoryClass *klass)
{
  klass->create_accessible = ftk_legend_accessible_factory_create_accessible;
  klass->get_accessible_type = ftk_legend_accessible_factory_get_accessible_type;
}

static GType
ftk_legend_accessible_factory_get_type (void)
{
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
	{
	  sizeof (AtkObjectFactoryClass),
	  NULL,           /* base_init */
	  NULL,           /* base_finalize */
	  (GClassInitFunc) ftk_legend_accessible_factory_class_init,
	  NULL,           /* class_finalize */
	  NULL,           /* class_data */
	  sizeof (AtkObjectFactory),
	  0,             /* n_preallocs */
	  NULL, NULL
	};

      type = g_type_register_static (ATK_TYPE_OBJECT_FACTORY, 
				     "FtkLegendAccessibleFactory",
				     &tinfo, 0);
    }
    
  return type;
}


static AtkObject *
ftk_legend_get_accessible (GtkWidget *widget)
{
  static gboolean first_time = TRUE;

  if (first_time)
    {
      AtkObjectFactory *factory;
      AtkRegistry *registry;
      GType derived_type; 
      GType derived_atk_type; 

      /*
       * Figure out whether accessibility is enabled by looking at the
       * type of the accessible object which would be created for
       * the parent type of GtkIconView.
       */
      derived_type = g_type_parent (FTK_LEGEND_TYPE);

      registry = atk_get_default_registry ();
      factory = atk_registry_get_factory (registry,
                                          derived_type);
      derived_atk_type = atk_object_factory_get_accessible_type (factory);
      if (g_type_is_a (derived_atk_type, GTK_TYPE_ACCESSIBLE)) 
	atk_registry_set_factory_type (registry, 
				       FTK_LEGEND_TYPE,
				       ftk_legend_accessible_factory_get_type ());
      first_time = FALSE;
    } 
 
  return (* GTK_WIDGET_CLASS (parent_class)->get_accessible) (widget);
}


