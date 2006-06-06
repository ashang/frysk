#define _GNU_SOURCE
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <math.h>
#include <sys/types.h>
#include <sys/time.h>
#include <gtk/gtk.h>
#include <glib-object.h>
#include "ftkeventviewer.h"

//#define USE_FTK_SIGNAL

GQuark ftk_quark;

static gboolean ftk_eventviewer_expose (GtkWidget * widget,
					GdkEventExpose * event,
					gpointer data);
static gboolean ftk_eventviewer_configure (GtkWidget * widget,
					   GdkEventConfigure * event,
					   gpointer data);
static void ftk_eventviewer_realize (GtkWidget * widget,
				     gpointer data);
static void ftk_eventviewer_destroy (GtkObject * widget,
				     gpointer data);
static void ftk_eventviewer_scale_toggle (GtkToggleButton * button,
					  gpointer data);

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

static gboolean ftk_ev_leave_notify_event (GtkWidget * widget,
					   GdkEventCrossing * event,
					   gpointer data);
#if 0
static gboolean ftk_ev_popup_expose (GtkWidget * widget,
				     GdkEventExpose * event,
				     gpointer data);
#endif

#ifdef USE_SLIDER_INTERVAL
static gchar * ftk_eventviewer_slider_format (GtkScale *scale,
					      gdouble   arg1,
					      gpointer  user_data);

static gboolean ftk_eventviewer_slider_cv (GtkRange * range,
					   GtkScrollType scroll,
					   gdouble   value,
					   gpointer  user_data);
#endif
    
static inline double timeval_to_double (struct timeval * tv);
static inline void double_to_timeval (struct timeval * tv,
				      double time_d);

#define DEFAULT_BG_RED		32768
#define DEFAULT_BG_GREEN	32768
#define DEFAULT_BG_BLUE		32768

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
ftk_eventviewer_init (FtkEventViewer * eventviewer)
{
  GtkTooltips * eventviewer_tips = gtk_tooltips_new ();
  GtkVBox * vbox = ftk_ev_vbox (eventviewer);

  {
    struct timeval now;
    gettimeofday (&now, NULL);
    ftk_ev_zero (eventviewer) = timeval_to_double (&now);
  }

  ftk_ev_popup_window (eventviewer)	= NULL;
  ftk_ev_popup_type (eventviewer)	= FTK_POPUP_TYPE_NONE;
  ftk_ev_popup_trace (eventviewer)	= -1;
  ftk_ev_popup_marker (eventviewer)	= -1;

  ftk_ev_bg_red(eventviewer)	= DEFAULT_BG_RED;
  ftk_ev_bg_green(eventviewer)	= DEFAULT_BG_GREEN;
  ftk_ev_bg_blue(eventviewer)	= DEFAULT_BG_BLUE;
  ftk_ev_bg_color_modified(eventviewer) = TRUE;
  ftk_ev_bg_gc(eventviewer) = NULL;
  ftk_ev_bg_gc(eventviewer) = NULL;
  ftk_ev_label_box_width(eventviewer)  = 0;
  ftk_ev_label_box_height(eventviewer) = 0;

  ftk_ev_traces(eventviewer)     = NULL;
  ftk_ev_trace_next(eventviewer) = 0;
  ftk_ev_trace_max(eventviewer)  = 0;
  ftk_ev_trace_modified(eventviewer)  = FALSE;

  ftk_ev_ties(eventviewer)     = NULL;
  ftk_ev_tie_next(eventviewer) = 0;
  ftk_ev_tie_max(eventviewer)  = 0;
  ftk_ev_tie_modified(eventviewer)  = FALSE;
  
  ftk_ev_events(eventviewer)     = NULL;
  ftk_ev_event_next(eventviewer) = 0;
  ftk_ev_event_max(eventviewer)  = 0;
  
  ftk_ev_links(eventviewer)     = NULL;
  ftk_ev_link_next(eventviewer) = 0;
  ftk_ev_link_max(eventviewer)  = 0;

  ftk_ev_markers (eventviewer)		= NULL;
  ftk_ev_markers_next (eventviewer)	= 0;
  ftk_ev_markers_max (eventviewer)	= 0;
  ftk_ev_markers_modified (eventviewer)	= FALSE;

  ftk_ev_drawable(eventviewer)  = FALSE;

  ftk_ev_pixmap (eventviewer)	= NULL;
  
  ftk_ev_span(eventviewer) = DEFAULT_SPAN;
  
  ftk_ev_widget_modified (eventviewer) = TRUE;

#ifdef USE_FTK_SIGNAL
  g_signal_connect (GTK_OBJECT (eventviewer), "ftkeventviewer",
		    (GtkSignalFunc) ftk_eventviewer_expose, NULL);
#endif
  g_signal_connect (GTK_OBJECT (eventviewer), "expose-event",
		    (GtkSignalFunc) ftk_eventviewer_expose, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer),"realize",
		    (GtkSignalFunc) ftk_eventviewer_realize, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer), "destroy",
		    GTK_SIGNAL_FUNC(ftk_eventviewer_destroy),
		    NULL);

#if 1
  g_signal_connect (GTK_OBJECT(eventviewer), "leave-notify-event",
		    G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
#endif

  gtk_box_set_homogeneous (GTK_BOX (vbox), FALSE);
  gtk_box_set_spacing (GTK_BOX (vbox), 0);

  {
    /*****************   button box  **************/
    
    GtkWidget * hbutton_box = gtk_hbox_new(FALSE, 5);
    ftk_ev_hbutton_box (eventviewer) = hbutton_box;

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
#if 0  /* fixme -- not yet implemented */
      gtk_box_pack_start (GTK_BOX (hbutton_box),
			  scale_toggle_button,
			  FALSE, FALSE, 0);
#endif
    }
    {
      /* hold button */
      
      GtkWidget * hold_toggle_button
	= gtk_toggle_button_new_with_mnemonic ("_Hold");

      ftk_ev_hold_toggle_button (eventviewer) = hold_toggle_button;
      
      gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			    ftk_ev_hold_toggle_button (eventviewer),
			    "Enable or disable auto-updates.",
			    "private");
    
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


    
    gtk_widget_show_all (hbutton_box);

    gtk_box_pack_start (GTK_BOX (vbox), hbutton_box, FALSE, FALSE, 0);
  }

  
  {
    /*****************  drawing area **************/

    GtkWidget * frame = gtk_frame_new (NULL);
    GtkWidget * da = gtk_drawing_area_new();
      
    ftk_ev_da_frame (eventviewer) = frame;
    ftk_ev_da(eventviewer) = GTK_DRAWING_AREA (da);
    gtk_drawing_area_size (ftk_ev_da (eventviewer),
			   DEFAULT_INITIAL_WIDTH,
			   DEFAULT_INITIAL_HEIGHT);
    gtk_widget_show (da);

 
    gtk_widget_set_events(GTK_WIDGET (da),
			  GDK_POINTER_MOTION_MASK  |
#if 0
			  GDK_ENTER_NOTIFY_MASK |
#endif
			  GDK_LEAVE_NOTIFY_MASK |
                          GDK_BUTTON_PRESS_MASK    |
                          GDK_BUTTON_RELEASE_MASK);

#if 0
    gtk_tooltips_set_tip (GTK_TOOLTIPS (eventviewer_tips),
			  da,
			  "da tooltip.",
			  "private");
#endif

    g_signal_connect (GTK_OBJECT(da), "button_press_event",
		      G_CALLBACK (ftk_ev_button_press_event), eventviewer);

    g_signal_connect (GTK_OBJECT(da), "button_release_event",
		      G_CALLBACK (ftk_ev_button_press_event), eventviewer);

    g_signal_connect (GTK_OBJECT(da), "motion_notify_event",
		      G_CALLBACK (ftk_ev_motion_notify_event), eventviewer);

    g_signal_connect (GTK_OBJECT(eventviewer), "leave_notify_event",
		      G_CALLBACK (ftk_ev_leave_notify_event), eventviewer);
    
    g_signal_connect (GTK_OBJECT(da),"configure-event",
                      G_CALLBACK (ftk_eventviewer_configure), eventviewer);
    
    gtk_container_add (GTK_CONTAINER(frame), da);
    gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_IN);
    gtk_widget_show (frame);
    gtk_box_pack_start (GTK_BOX (vbox), frame, TRUE, TRUE, 0);
  }

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
    gtk_range_set_update_policy (GTK_RANGE (h_scroll),
                                 GTK_UPDATE_CONTINUOUS);
    gtk_widget_show (h_scroll);
    ftk_ev_scroll (eventviewer) = h_scroll;
    ftk_ev_scroll_adj (eventviewer) = GTK_ADJUSTMENT (scroll_adj);
    g_signal_connect (GTK_OBJECT(h_scroll),"change-value",
                      (GtkSignalFunc) ftk_eventviewer_scroll_cv, eventviewer);
    gtk_box_pack_start (GTK_BOX (vbox), h_scroll, FALSE, FALSE, 0);
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

static void
check_gc_mods (FtkEventViewer * eventviewer)
{
  int label_height = 0;
  int label_width = 0;
  gboolean modded = FALSE;
  GtkWidget * da  = GTK_WIDGET (ftk_ev_da(eventviewer));
  
  if (NULL == ftk_ev_bg_gc(eventviewer))
    ftk_ev_bg_gc (eventviewer) =  gdk_gc_new (ftk_ev_pixmap (eventviewer));

  if (ftk_ev_bg_color_modified(eventviewer)) {
    gdk_gc_set_rgb_fg_color(ftk_ev_bg_gc (eventviewer),
			    &ftk_ev_bg_color (eventviewer));
    ftk_ev_bg_color_modified(eventviewer) = FALSE;
  }

  
  if (ftk_ev_trace_modified (eventviewer) ||
      ftk_ev_widget_modified (eventviewer)) {
    int i;

    for (i = 0; i < ftk_ev_trace_next(eventviewer); i++) {
      ftk_trace_s * trace = ftk_ev_trace(eventviewer, i);
      
      if (NULL == ftk_trace_gc(trace))
	ftk_trace_gc (trace) = gdk_gc_new (ftk_ev_pixmap (eventviewer));
      
      if (ftk_trace_color_modified(trace)) {
	gdk_gc_set_rgb_fg_color (ftk_trace_gc (trace),
				&ftk_trace_color (trace));
	ftk_trace_color_modified(trace) = FALSE;
      }
      if (ftk_trace_label_modified(trace)) {
	pango_layout_get_pixel_size (ftk_trace_label(trace),
				     &ftk_trace_label_width(trace),
				     &ftk_trace_label_height(trace));
	ftk_trace_label_modified(trace) = FALSE;
      }
      if (ftk_trace_linestyle_modded(trace)) {
	gdk_gc_set_line_attributes (ftk_trace_gc (trace),
				    ftk_trace_linewidth(trace),	/* line wid */
				    ftk_trace_linestyle(trace),
				    GDK_CAP_BUTT,
				    GDK_JOIN_MITER);
	ftk_trace_linestyle_modded(trace) = FALSE;
      }
      label_height += ftk_trace_label_height (trace);
      if (label_width < ftk_trace_label_width (trace))
	label_width = ftk_trace_label_width (trace);
    }

    ftk_ev_trace_modified (eventviewer) = FALSE;
    modded = TRUE;
  }
  
  if (ftk_ev_tie_modified (eventviewer) ||
      ftk_ev_widget_modified (eventviewer)) {
    int i;

    for (i = 0; i < ftk_ev_tie_next(eventviewer); i++) {
      ftk_tie_s * tie = ftk_ev_tie(eventviewer, i);
      
      if (NULL == ftk_tie_gc(tie))
	ftk_tie_gc (tie) = gdk_gc_new (ftk_ev_pixmap (eventviewer));
      
      if (ftk_tie_color_modified(tie)) {
	gdk_gc_set_rgb_fg_color (ftk_tie_gc (tie),
				&ftk_tie_color (tie));
	ftk_tie_color_modified(tie) = FALSE;
      }
      if (ftk_tie_label_modified(tie)) {
	pango_layout_get_pixel_size (ftk_tie_label(tie),
				     &ftk_tie_label_width(tie),
				     &ftk_tie_label_height(tie));
	ftk_tie_label_modified(tie) = FALSE;
      }
      if (ftk_tie_linestyle_modded(tie)) {
	gdk_gc_set_line_attributes (ftk_tie_gc (tie),
				    ftk_tie_linewidth(tie),	/* line wid */
				    ftk_tie_linestyle(tie),
				    GDK_CAP_BUTT,
				    GDK_JOIN_MITER);
	ftk_tie_linestyle_modded(tie) = FALSE;
      }
    }

    ftk_ev_tie_modified (eventviewer) = FALSE;
  }

  if (ftk_ev_markers_modified (eventviewer) ||
      ftk_ev_widget_modified (eventviewer)) {
    int i;

    for (i = 0; i < ftk_ev_markers_next (eventviewer); i++) {
      ftk_marker_s * marker = ftk_ev_marker (eventviewer, i);
      
      if (NULL == ftk_marker_gc(marker))
	ftk_marker_gc (marker) =  gdk_gc_new (ftk_ev_pixmap (eventviewer));
      
      if (ftk_marker_color_modified(marker)) {
	gdk_gc_set_rgb_fg_color (ftk_marker_gc (marker),
				&ftk_marker_color (marker));
	ftk_marker_color_modified(marker) = FALSE;
      }
      if (ftk_marker_label_modified(marker)) {
	pango_layout_get_pixel_size (ftk_marker_label(marker),
				     &ftk_marker_label_width(marker),
				     &ftk_marker_label_height(marker));
	ftk_marker_label_modified(marker) = FALSE;
      }
    }
    
    ftk_ev_markers_modified (eventviewer) = FALSE;
  }
  
  if (modded || ftk_ev_widget_modified (eventviewer)) {

    /* resize if necessary */
    
    int tw = label_width + DEFAULT_MIN_TRACE_LENGTH;
    int th = label_height + DEFAULT_MIN_TRACE_HEIGHT;
    int draw_width  = da->allocation.width;
    int draw_height = da->allocation.height;
    ftk_ev_label_box_width(eventviewer)  = label_width;
    ftk_ev_label_box_height(eventviewer) = label_height;
    if ((draw_width < tw) ||
	(draw_height < th) ||
	ftk_ev_widget_modified (eventviewer)) {
      if (draw_width  > tw) tw = draw_width;
      if (draw_height > th) th = draw_height;
      gtk_widget_set_size_request (GTK_WIDGET (ftk_ev_da (eventviewer)),
				   tw, th);
      
      if (ftk_ev_pixmap (eventviewer))
	gdk_pixmap_unref(ftk_ev_pixmap (eventviewer));
      ftk_ev_pixmap (eventviewer)
	= gdk_pixmap_new((GTK_WIDGET (eventviewer))->parent->window,
			 tw, th,
			 -1);
    }
    ftk_ev_widget_modified (eventviewer) = FALSE;
  }

  {
    int draw_width  = da->allocation.width;
    ftk_ev_trace_origin (eventviewer) =
      ftk_ev_label_box_width (eventviewer) + DEFAULT_TRACE_OFFSET;
    ftk_ev_trace_width (eventviewer) =
      (draw_width - DEFAULT_TRACE_OFFSET) - ftk_ev_trace_origin (eventviewer);
  }
}

/*******************************************************/
/*                                                     */
/*                   drawing operations                */
/*                                                     */
/*******************************************************/

static int
int_sort (a, b)
     int * a;
     int * b;
{
  return ((*a) == (*b)) ? 0 : (((*a) < (*b)) ? -1 : 1);
}

static void
draw_link (FtkEventViewer * eventviewer,
	   ftk_link_s * link,
	   gboolean flush_it)
{
  gdouble time_offset = gtk_adjustment_get_value (ftk_ev_scroll_adj (eventviewer));
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
      
	for (i = 0; i < ftk_link_trace_list_next(link); i++) {
	  ftk_trace_s * trace = ftk_ev_trace (eventviewer, ftk_link_trace(link, i));
	  vps[i] = ftk_trace_vpos (trace);
	  
	  gdk_draw_arc (ftk_ev_pixmap (eventviewer),	/* GdkDrawable * */
			ftk_tie_gc(tie),		/* GdKGC *	*/
			TRUE,				/* filled	*/
			h_offset-2,			/* left bound	*/
			vps[i]-2,			/* top bound	*/
			4,				/* width	*/
			4,				/* height	*/
			0,				/* ang/64 start	*/
			23040);				/* ang/64 end	*/
	}

	if (2 < ftk_link_trace_list_next(link)) {
	  qsort(vps, ftk_link_trace_list_next(link), sizeof(int), int_sort);
	}
	
	gdk_draw_line (ftk_ev_pixmap (eventviewer),
		       ftk_tie_gc(tie),
		       h_offset, vps[0],
		       h_offset, vps[ftk_link_trace_list_next(link) - 1]);
      
	if (flush_it) {
	  GtkWidget * da  = GTK_WIDGET (ftk_ev_da(eventviewer));
	  int o_x = h_offset - 3;
	  int o_y = ((vps[0] <= vps[ftk_link_trace_list_next(link) - 1]) ?
		     vps[0] : vps[ftk_link_trace_list_next(link) - 1]) - 3;
	  int d_w = 6;
	  int d_h = 6 + abs (vps[0] <= vps[ftk_link_trace_list_next(link) - 1]);
	  gdk_draw_drawable(da->window,
			    da->style->bg_gc[GTK_WIDGET_STATE (eventviewer)],
			    ftk_ev_pixmap (eventviewer),
			    o_x, o_y,
			    o_x, o_y,
			    d_w, d_h);

	  gdk_display_flush (gtk_widget_get_display (GTK_WIDGET (eventviewer)));
	}
      }
    }
  }
}

static void
render_marker (FtkEventViewer * eventviewer,
	       ftk_marker_s * marker,
	       int h_offset,
	       int v_offset,
	       int * o_x_p,
	       int * o_y_p,
	       int * d_w_p,
	       int * d_h_p)
{
  switch(ftk_marker_glyph (marker)) {
  case FTK_GLYPH_OPEN_CIRCLE:
    if (o_x_p) {
      *o_x_p = h_offset-5;
      *o_y_p = v_offset-5;
      *d_w_p = 10;
      *d_h_p = 10;
    }
    gdk_draw_arc (ftk_ev_pixmap (eventviewer),	/* GdkDrawable * */
		  ftk_marker_gc(marker),		/* GdKGC *	*/
		  FALSE,				/* filled	*/
		  h_offset-4,			/* left bound	*/
		  v_offset-4,			/* top bound	*/
		  8,				/* width	*/
		  8,				/* height	*/
		  0,				/* ang/64 start	*/
		  23040);				/* ang/64 end	*/
    break;	
  case FTK_GLYPH_FILLED_CIRCLE:
    if (o_x_p) {
      *o_x_p = h_offset-5;
      *o_y_p = v_offset-5;
      *d_w_p = 10;
      *d_h_p = 10;
    }
    gdk_draw_arc (ftk_ev_pixmap (eventviewer),	/* GdkDrawable * */
		  ftk_marker_gc(marker),		/* GdKGC *	*/
		  TRUE,				/* filled	*/
		  h_offset-4,			/* left bound	*/
		  v_offset-4,			/* top bound	*/
		  8,				/* width	*/
		  8,				/* height	*/
		  0,				/* ang/64 start	*/
		  23040);				/* ang/64 end	*/
    break;
  case FTK_GLYPH_OPEN_SQUARE:
    if (o_x_p) {
      *o_x_p = h_offset-5;
      *o_y_p = v_offset-5;
      *d_w_p = 10;
      *d_h_p = 10;
    }
    gdk_draw_rectangle (ftk_ev_pixmap (eventviewer),	/* GdkDrawable * */
			ftk_marker_gc(marker),		/* GdKGC *	*/
			FALSE,				/* filled	*/
			h_offset-4,			/* left bound	*/
			v_offset-4,			/* top bound	*/
			8,				/* width	*/
			8);				/* height	*/
    break;	
  case FTK_GLYPH_FILLED_SQUARE:
    if (o_x_p) {
      *o_x_p = h_offset-5;
      *o_y_p = v_offset-5;
      *d_w_p = 10;
      *d_h_p = 10;
    }
    gdk_draw_rectangle (ftk_ev_pixmap (eventviewer),	/* GdkDrawable * */
			ftk_marker_gc(marker),		/* GdKGC *	*/
			TRUE,				/* filled	*/
			h_offset-4,			/* left bound	*/
			v_offset-4,			/* top bound	*/
			8,				/* width	*/
			8);				/* height	*/
    break;
  case FTK_GLYPH_LAST:	/* never reached -- just here to satisfy the	*/
    break;		/* warning == error thing			*/
  }
}

static gboolean
draw_point (FtkEventViewer * eventviewer,
	    ftk_event_s * event,
	    gboolean flush_it)
{
  int o_x, o_y;
  int d_w, d_h;
  
  ftk_event_loc (event) = -1;
  if (!ftk_ev_drawable(eventviewer)) return FALSE;

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
	ftk_trace_s * trace =
	  ftk_ev_trace (eventviewer, ftk_event_trace (event));
	int v_offset = ftk_trace_vpos (trace);

	render_marker (eventviewer, marker, h_offset, v_offset,
		       &o_x, &o_y, &d_w, &d_h);
	ftk_event_loc (event) = h_offset;
      }
    }
  }

  if (flush_it) {
    GtkWidget * da  = GTK_WIDGET (ftk_ev_da(eventviewer));
    gdk_draw_drawable(da->window,
		      da->style->bg_gc[GTK_WIDGET_STATE (eventviewer)],
		      ftk_ev_pixmap (eventviewer),
		      o_x, o_y,
		      o_x, o_y,
		      d_w, d_h);

    gdk_display_flush (gtk_widget_get_display (GTK_WIDGET (eventviewer)));
  }

  return TRUE;
}

static void
draw_plot (FtkEventViewer * eventviewer)
{
  if ((GDK_IS_PIXMAP (ftk_ev_pixmap (eventviewer))) &&
      GTK_WIDGET_MAPPED(eventviewer)) {
    GtkWidget * da  = GTK_WIDGET (ftk_ev_da(eventviewer));
    int draw_width  = da->allocation.width;
    int draw_height = da->allocation.height;
    int base_line = 0;

    check_gc_mods (eventviewer);

    /*  fill bg */
    gdk_draw_rectangle (ftk_ev_pixmap (eventviewer),
			ftk_ev_bg_gc (eventviewer),
			TRUE,
			0, 0,
			draw_width, draw_height);

    {
      /* draw labels and baselines */
      
      int i;

      for (i = 0; i < ftk_ev_trace_next(eventviewer); i++) {
	ftk_trace_s * trace = ftk_ev_trace(eventviewer, i);
	gdk_draw_layout (ftk_ev_pixmap (eventviewer),
			 ftk_trace_gc(trace),
			 ftk_ev_label_box_width (eventviewer) -
			   ftk_trace_label_width(trace),
			 base_line,
			 ftk_trace_label (trace));
	ftk_trace_vpos (trace) = base_line + ftk_trace_label_height(trace)/2;
	gdk_draw_line (ftk_ev_pixmap (eventviewer),
		       ftk_trace_gc(trace),
		       ftk_ev_trace_origin (eventviewer),
		       ftk_trace_vpos (trace),
		       draw_width - DEFAULT_TRACE_OFFSET,
		       ftk_trace_vpos (trace));
	base_line += ftk_trace_label_height(trace);
      };
    }

    {
      /* draw legend */

      int i;
      int h_offset = DEFAULT_TRACE_OFFSET;
      int v_offset = base_line + DEFAULT_TRACE_OFFSET;
      
      for ( i = 0; i < ftk_ev_markers_next (eventviewer); i++) {
	ftk_marker_s * marker = ftk_ev_marker (eventviewer, i);

	render_marker (eventviewer, marker, h_offset, v_offset,
		       NULL, NULL, NULL, NULL);
	h_offset += 10;

	gdk_draw_layout (ftk_ev_pixmap (eventviewer),
			 ftk_marker_gc(marker),
			 h_offset,
			 v_offset,
			 ftk_marker_label (marker));
	h_offset += ftk_marker_label_width(marker) + DEFAULT_TRACE_OFFSET;
      }
    }

    if (0 < ftk_ev_event_next(eventviewer)) {

      /* draw points */

      int i;

      for (i = 0; i < ftk_ev_event_next(eventviewer); i++) {
	ftk_event_s * event = ftk_ev_event (eventviewer, i);

	draw_point (eventviewer, event, FALSE);
      }
    }

    if (0 < ftk_ev_link_next(eventviewer)) {

      /* draw links */

      int i;

      for (i = 0; i < ftk_ev_link_next(eventviewer); i++) {
	ftk_link_s * link = ftk_ev_link (eventviewer, i);

	draw_link (eventviewer, link, FALSE);
      }
    }

    gdk_draw_drawable(da->window,
		      da->style->bg_gc[GTK_WIDGET_STATE (eventviewer)],
		      ftk_ev_pixmap (eventviewer),
		      0, 0,				/* src offsets */
		      0, 0,				/* dst offsets */
		      draw_width,
		      draw_height);

    gdk_display_flush (gtk_widget_get_display (GTK_WIDGET (eventviewer)));

    ftk_ev_drawable(eventviewer)  = TRUE;
  }
}

/*******************************************************/
/*                                                     */
/*                     callbacks                       */
/*                                                     */
/*******************************************************/

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
    
    return FALSE;
  }
  else return TRUE;	/* do no more */
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

#if 0
static gboolean
ftk_ev_popup_expose(GtkWidget * window, GdkEventExpose * event,
		    gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);

  fprintf (stderr, "popup expose\n");

#if 0
  {
    GtkRequisition req;

    gtk_widget_size_request (window, &req);
    gtk_paint_flat_box (
			window->style,
			window->window,
#if 0
			ftk_ev_popup_window (eventviewer)->style,
			ftk_ev_popup_window (eventviewer)->window,
#endif
#if 0
			GTK_WIDGET (ftk_ev_da (eventviewer))->style,
			GTK_WIDGET (ftk_ev_da (eventviewer))->window,
#endif
			GTK_STATE_NORMAL,
			GTK_SHADOW_ETCHED_OUT,
			NULL,
			ftk_ev_popup_window (eventviewer), "tooltip",
			0, 0,
			req.width, req.height);
  }
#endif
  
  return FALSE;	/* last handler */
}
#endif

static void
ftk_create_popup_window (FtkEventViewer * eventviewer, char * lbl)
{
  GtkWidget * window = gtk_window_new (GTK_WINDOW_POPUP);
  GtkWidget * label = gtk_label_new (lbl);

  ftk_ev_popup_window (eventviewer) = window;
  ftk_ev_popup_label (eventviewer) = label;
  
#if 0
  g_signal_connect (GTK_OBJECT (window), "expose-event",
		    (GtkSignalFunc) ftk_ev_popup_expose, eventviewer);
#endif
  g_signal_connect (GTK_OBJECT (window), 
		    "destroy",
		    G_CALLBACK (gtk_widget_destroyed),
		    &window);
    
  gtk_window_set_decorated (GTK_WINDOW (window), FALSE);
  gtk_window_set_resizable (GTK_WINDOW (window), FALSE);
  gtk_window_set_modal (GTK_WINDOW (window), FALSE);
  //  gtk_window_set_keep_above (GTK_WINDOW (window), TRUE);
  gtk_widget_set_app_paintable (window, TRUE);
  gtk_container_set_border_width (GTK_CONTAINER (window), 4);
  gtk_widget_ensure_style (window);
  gtk_window_set_transient_for (GTK_WINDOW (window), NULL);

  
  gtk_label_set_line_wrap (GTK_LABEL (label), TRUE);
  gtk_misc_set_alignment (GTK_MISC (label), 0.5, 0.5);
  gtk_widget_show (label);

  gtk_container_add (GTK_CONTAINER (window), label);
}

static void
ftk_create_popup (FtkEventViewer * eventviewer, char * lbl)
{
  gint px, py;
  GtkWidget * widget = GTK_WIDGET (eventviewer);

if (ftk_ev_popup_window (eventviewer)) {
    gtk_label_set_text (GTK_LABEL (ftk_ev_popup_label (eventviewer)), lbl);
  }
  else {
    ftk_create_popup_window (eventviewer, lbl);
  }

#if 1
  gdk_display_get_pointer (gdk_screen_get_display (gtk_widget_get_screen (widget)),
                           NULL, &px, &py, NULL);

  gtk_window_move (GTK_WINDOW (ftk_ev_popup_window (eventviewer)), px + 10, py);
  gtk_widget_show (ftk_ev_popup_window (eventviewer));
  
#if 1
  {
    GtkRequisition req;

    gtk_widget_size_request (ftk_ev_popup_window (eventviewer), &req);
    gtk_paint_flat_box (
#if 1
			ftk_ev_popup_window (eventviewer)->style,
			ftk_ev_popup_window (eventviewer)->window,
#else
			GTK_WIDGET (ftk_ev_da (eventviewer))->style,
			GTK_WIDGET (ftk_ev_da (eventviewer))->window,
#endif
			GTK_STATE_NORMAL,
			GTK_SHADOW_ETCHED_OUT,
			NULL,
			ftk_ev_popup_window (eventviewer), "tooltip",
			0, 0,
			req.width, req.height);
  }
#endif
#endif

}

#define POPUP_TOLERANCE 4

static gboolean
ftk_ev_motion_notify_event (GtkWidget * widget,
			    GdkEventMotion * event,
			    gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  int pvpos = lrint (event->y);
  int phpos = lrint (event->x);
  ftk_trace_s * trace = NULL;;
  ftk_marker_s * marker = NULL;
  int trace_idx = -1;
  int marker_idx = -1;
  char * lbl = NULL;
  ftk_popup_type_e pt = FTK_POPUP_TYPE_NONE;
  double time_d = 0.0;
  gboolean details_have_changed;
  
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (eventviewer), FALSE);
  
  {
    int i;

    trace = NULL;
    for (i = 0; i < ftk_ev_trace_next (eventviewer); i++) {
      ftk_trace_s * ltrace = ftk_ev_trace (eventviewer, i);
      if (abs (ftk_trace_vpos (ltrace) - pvpos) < POPUP_TOLERANCE) {
	trace = ltrace;
	trace_idx = i;
	break;
      }
    }
  }

  pt = (phpos <= ftk_ev_label_box_width (eventviewer))
    ? FTK_POPUP_TYPE_LABEL : FTK_POPUP_TYPE_MARKER;

  details_have_changed = FALSE;
  if (pt == FTK_POPUP_TYPE_MARKER) {
    int i;

    marker = NULL;
    marker_idx = -1;
    for (i = 0; i <  ftk_ev_event_next(eventviewer); i++) {
      ftk_event_s * pevent = ftk_ev_event (eventviewer, i);
      if (abs (ftk_event_loc (pevent) - phpos) < POPUP_TOLERANCE) {
	marker_idx = ftk_event_marker (pevent);
	marker = ftk_ev_marker (eventviewer, marker_idx);
	time_d = ftk_event_time (event) - ftk_ev_zero (eventviewer);
	break;
      }
    }
  }
  
  if (ftk_ev_popup_window (eventviewer) &&	/* if a popup is up and either */
      (!trace	||				/* we're [no longer | not] on a trace */
       (pt != ftk_ev_popup_type (eventviewer)) ||	/* the type has changed */
       (trace_idx != ftk_ev_popup_trace (eventviewer)) || /* the trace has changed */
       ((pt == FTK_POPUP_TYPE_MARKER) &&		  /* it's a marker and */
	marker_idx != ftk_ev_popup_marker (eventviewer)))) {  /* the marker has changed */
    gtk_widget_destroy ( ftk_ev_popup_window (eventviewer));
    gdk_display_flush(gtk_widget_get_display (GTK_WIDGET (eventviewer)));
    if (GTK_WIDGET_DRAWABLE (eventviewer)) draw_plot(eventviewer);
    ftk_ev_popup_window (eventviewer) = NULL;
  }

  //  fprintf (stderr, "move %d %d\n", pvpos, phpos);
  if (trace) {
    switch (pt) {
    case FTK_POPUP_TYPE_NONE:	/* for some halfwitted warning from gcc */
      break;
    case FTK_POPUP_TYPE_LABEL:

      /* fixme -- popup more label info */
      
      asprintf (&lbl, "trace %s", pango_layout_get_text (ftk_trace_label (trace)));
      ftk_ev_popup_type (eventviewer)  = FTK_POPUP_TYPE_LABEL;
      ftk_ev_popup_trace (eventviewer) = trace_idx;
      break;
    case  FTK_POPUP_TYPE_MARKER:
      if (marker) {
	struct timeval tv;
	  
	double_to_timeval (&tv, time_d);
	time_d = timeval_to_double (&tv);
	  
	asprintf (&lbl, "%g sec\nevent <<%s>>\non trace %s",
		  time_d,
		  pango_layout_get_text (ftk_marker_label (marker)),
		  pango_layout_get_text (ftk_trace_label (trace)));
	  
	ftk_ev_popup_type (eventviewer)  = FTK_POPUP_TYPE_MARKER;
	ftk_ev_popup_trace (eventviewer) = trace_idx;
	ftk_ev_popup_marker (eventviewer) = marker_idx;
      }
      break;
    }
  }

  if (lbl) {
    ftk_create_popup (eventviewer, lbl);
    free (lbl);
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

  if (ftk_ev_popup_window (eventviewer)) {
    gtk_widget_destroy ( ftk_ev_popup_window (eventviewer));
    ftk_ev_popup_window (eventviewer) = NULL;
  }

  return FALSE;
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
				      GTK_WIDGET (ftk_ev_da_frame (eventviewer)),
				      event);
      gtk_container_propagate_expose (GTK_CONTAINER (widget),
				      GTK_WIDGET (ftk_ev_scroll (eventviewer)),
				      event);
    }

    draw_plot (eventviewer);
  }

  return TRUE;	/* last handler */
}

static gboolean
ftk_eventviewer_configure(GtkWidget * widget, GdkEventConfigure * event,
			     gpointer data)
{
  FtkEventViewer * eventviewer = FTK_EVENTVIEWER (data);
  g_return_val_if_fail (event != NULL, FALSE);
  
  if (ftk_ev_pixmap (eventviewer))
    gdk_pixmap_unref(ftk_ev_pixmap (eventviewer));
  ftk_ev_pixmap (eventviewer)
    = gdk_pixmap_new((GTK_WIDGET (eventviewer))->parent->window,
		     event->width, event->height,
		     -1);

  return TRUE;	/* last handler */
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
      
    if (ftk_trace_gc(trace)) g_object_unref (ftk_trace_gc (trace));
  }

  for (i = 0; i < ftk_ev_tie_next(eventviewer); i++) {
    ftk_tie_s * tie = ftk_ev_tie(eventviewer, i);
      
    if (ftk_tie_gc(tie)) g_object_unref (ftk_tie_gc (tie));
  }

  for (i = 0; i < ftk_ev_markers_next(eventviewer); i++) {
    ftk_marker_s * marker = ftk_ev_marker(eventviewer, i);
      
    if (ftk_marker_gc(marker)) g_object_unref (ftk_marker_gc (marker));
  }

  for (i = 0; i < ftk_ev_link_next(eventviewer); i++) {
    ftk_link_s * link = ftk_ev_link(eventviewer, i);

    if (ftk_link_trace_list (link)) free (ftk_link_trace_list (link));
  }
  
  if ( ftk_ev_bg_gc(eventviewer))
    g_object_unref (ftk_ev_bg_gc (eventviewer));
      
  if (ftk_ev_pixmap (eventviewer))
    gdk_pixmap_unref(ftk_ev_pixmap (eventviewer));

  if (ftk_ev_ties (eventviewer))    free (ftk_ev_ties(eventviewer));
  if (ftk_ev_traces (eventviewer))  free (ftk_ev_traces(eventviewer));
  if (ftk_ev_events (eventviewer))  free (ftk_ev_events(eventviewer));
  if (ftk_ev_links (eventviewer))   free (ftk_ev_links(eventviewer));
  if (ftk_ev_markers (eventviewer)) free (ftk_ev_markers (eventviewer));
}

static void
ftk_eventviewer_realize(GtkWidget * widget,
			gpointer data)
{
  g_return_if_fail (FTK_IS_EVENTVIEWER (widget));
  
  {
    FtkEventViewer * eventviewer = FTK_EVENTVIEWER (widget);
    GtkDrawingArea * da = ftk_ev_da(eventviewer);

    ftk_ev_pixmap (eventviewer)
      = gdk_pixmap_new(widget->parent->window,
		       GTK_WIDGET (da)->allocation.width,
		       GTK_WIDGET (da)->allocation.height,
		       -1);
  }
}

static void
ftk_eventviewer_scale_toggle(GtkToggleButton * button,
			     gpointer data)
{
  g_return_if_fail (GTK_IS_TOGGLE_BUTTON (button));

  /* fixme -- not yet implemented */
  //  gboolean active = gtk_toggle_button_get_active (button);
}

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
  GtkAdjustment * adj = gtk_range_get_adjustment (range);
  gtk_adjustment_set_value (adj, value);
}

static void
do_append (FtkEventViewer * eventviewer,
	   gint trace_index,
	   gint marker,
	   double now_d)
{
    ftk_event_s * event;
    int event_nr;
    
#define FTK_EV_EVENT_INCR	64
    
  if (ftk_ev_event_max(eventviewer) <= ftk_ev_event_next(eventviewer)) {
    ftk_ev_event_max(eventviewer) += FTK_EV_EVENT_INCR;
    ftk_ev_events(eventviewer)
      = realloc (ftk_ev_events(eventviewer),
		 ftk_ev_event_max(eventviewer) * sizeof(ftk_event_s));
    }
  event
    = ftk_ev_event (eventviewer,
		    event_nr = ftk_ev_event_next(eventviewer)++);
   
  ftk_event_marker (event) = marker;
  ftk_event_trace (event) = trace_index;
  ftk_event_time (event) = ftk_ev_now(eventviewer) = now_d;
  ftk_event_loc (event) = -1;
  
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

  if (ftk_ev_drawable(eventviewer)) {
    check_gc_mods (eventviewer);
    draw_point (eventviewer, event, TRUE);
  }
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

  draw_plot (eventviewer);

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
ftk_eventviewer_set_bg_rgb_e (FtkEventViewer * eventviewer,
			      guint red, guint green, guint blue,
			      GError ** err)
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

  ftk_ev_bg_red(eventviewer)		= red;
  ftk_ev_bg_green(eventviewer)		= green;
  ftk_ev_bg_blue(eventviewer)		= blue;
  ftk_ev_bg_color_modified(eventviewer) = TRUE;

  draw_plot (eventviewer);
  
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
				GdkColor color, GError ** err)
{
  return ftk_eventviewer_set_bg_rgb_e (eventviewer,
				       color.red,
				       color.green,
				       color.blue,
				       err);
}

gboolean
ftk_eventviewer_set_bg_color (FtkEventViewer * eventviewer,
			      GdkColor color)
{
  return ftk_eventviewer_set_bg_rgb_e (eventviewer,
				       color.red,
				       color.green,
				       color.blue,
				       NULL);
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

  draw_plot (eventviewer);
  
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
    ftk_trace_s * trace;
#define FTK_EV_TRACE_INCR	8
    
    if (ftk_ev_trace_max(eventviewer) <= ftk_ev_trace_next(eventviewer)) {
      ftk_ev_trace_max(eventviewer) += FTK_EV_TRACE_INCR;
      ftk_ev_traces(eventviewer)
	= realloc (ftk_ev_traces(eventviewer),
		   ftk_ev_trace_max(eventviewer) * sizeof(ftk_trace_s));
    }
    trace = ftk_ev_trace (eventviewer, tag = ftk_ev_trace_next(eventviewer)++);
    ftk_trace_gc (trace)		= NULL;
    ftk_trace_linewidth (trace)		= 0;
    ftk_trace_linestyle (trace)		= GDK_LINE_SOLID;
    ftk_trace_linestyle_modded (trace)	= FALSE;
    ftk_trace_color_red (trace)		= DEFAULT_TRACE_RED;
    ftk_trace_color_green (trace)	= DEFAULT_TRACE_GREEN;
    ftk_trace_color_blue (trace)	= DEFAULT_TRACE_BLUE;
    ftk_trace_color_modified (trace)	= TRUE;
    ftk_trace_vpos (trace)		= 0;
    
    if (label) asprintf (&t_label, "%s :%2d", label, tag);
    else       asprintf (&t_label, ":%2d", tag);
      
    ftk_trace_label(trace) =
      gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
    ftk_trace_label_modified (trace)	= TRUE;
    free (t_label);
    
    ftk_ev_trace_modified (eventviewer) = TRUE;
  }

  return tag;
}

gint
ftk_eventviewer_add_trace (FtkEventViewer * eventviewer,
			   char * label)
{
  return ftk_eventviewer_add_trace_e (eventviewer, label, NULL);
}

/*
 *
 *	setting trace rgb
 *
 */

gboolean
ftk_eventviewer_set_trace_rgb_e (FtkEventViewer * eventviewer,
				 gint trace_index,
				 guint red, guint green, guint blue,
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
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer))) {
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
  ftk_trace_color_modified (trace)	= TRUE;
  ftk_ev_trace_modified (eventviewer)	= TRUE;

  draw_plot (eventviewer);
  
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
				   GdkColor color, GError ** err)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer,
					  trace,
					  color.red,
					  color.green,
					  color.blue,
					  err);
}

gboolean
ftk_eventviewer_set_trace_color (FtkEventViewer * eventviewer,
				 gint trace,
				 GdkColor color)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer,
					  trace,
					  color.red,
					  color.green,
					  color.blue,
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
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer))) {
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

  draw_plot (eventviewer);
  
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
				       GdkLineStyle ls,
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
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  if ((ls != GDK_LINE_SOLID) &&
      (ls != GDK_LINE_ON_OFF_DASH) &&
      (ls != GDK_LINE_DOUBLE_DASH)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_LINESTYLE,	/* error code */
		 "Invalid FtkEventViewer linestyle.");
    return FALSE;
  }

  trace = ftk_ev_trace (eventviewer, trace_index);
  ftk_trace_linestyle (trace)		= ls;
  ftk_trace_linewidth (trace)		= lw;
  ftk_trace_linestyle_modded (trace)	= TRUE;
  ftk_ev_trace_modified (eventviewer)	= TRUE;

  draw_plot (eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_trace_linestyle (FtkEventViewer * eventviewer,
				     gint trace,
				     gint lw,
				     GdkLineStyle ls)
{
  return ftk_eventviewer_set_trace_linestyle_e (eventviewer, trace,
					  lw, ls,
					  NULL);
}

gboolean
ftk_eventviewer_append_event_e (FtkEventViewer * eventviewer,
				gint trace_index,
				gint marker,
				GError ** err)
{
  if (!FTK_IS_EVENTVIEWER (eventviewer)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_WIDGET,	/* error code */
		 "Invalid FtkEventViewer widget.");
    return FALSE;
  }
  
  if ((trace_index < 0) || (trace_index >= ftk_ev_trace_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		 "Invalid FtkEventViewer trace.");
    return FALSE;
  }

  if ((marker < 0) || (marker >= ftk_ev_markers_next (eventviewer))) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		 "Invalid FtkEventViewer event type.");
    return FALSE;
  }

  {
    struct timeval now;
   
    gettimeofday (&now, NULL);

    do_append (eventviewer, trace_index, marker, timeval_to_double (&now));
  }

  return TRUE;
}

gboolean
ftk_eventviewer_append_event   (FtkEventViewer * eventviewer,
				gint trace,
				gint marker)
{
  return ftk_eventviewer_append_event_e (eventviewer, trace, marker, NULL);
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

  if (-1 != tie_index) {
    if (ftk_ev_link_next(eventviewer) >= ftk_ev_link_max(eventviewer)) {
#define FTK_EV_LINK_INCR	8
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
  }
  else link = NULL;

  while(1) {
    int trace_index;
    int marker_index;

    trace_index = va_arg (ap, int);
    if (-1 == trace_index) break;
    
    marker_index = va_arg (ap, int);
  
    if ((trace_index < 0) ||
	(trace_index >= ftk_ev_trace_next (eventviewer))) {
      g_set_error (err,
		   ftk_quark,				/* error domain */
		   FTK_EV_ERROR_INVALID_TRACE,	/* error code */
		   "Invalid FtkEventViewer trace.");
      rc = FALSE;
    }

    if ((marker_index < 0) ||
	(marker_index >= ftk_ev_markers_next (eventviewer))) {
      g_set_error (err,
		   ftk_quark,				/* error domain */
		   FTK_EV_ERROR_INVALID_EVENT_TYPE,	/* error code */
		   "Invalid FtkEventViewer event type.");
      rc = FALSE;
    }

    if (rc) {
      do_append (eventviewer, trace_index, marker_index, now_d);
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
    }
  }

  if (link && ftk_ev_drawable(eventviewer)) {
    check_gc_mods (eventviewer);
    draw_link (eventviewer, link, TRUE);
  }

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

gint
ftk_eventviewer_marker_new_e (FtkEventViewer * eventviewer,
			      FtkGlyph glyph,
			      char * label,
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
  
  if ((glyph < 0) || (glyph >= FTK_GLYPH_LAST)) {
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
  ftk_marker_glyph (marker) = glyph;
  ftk_marker_color_red (marker)   = 0;
  ftk_marker_color_green (marker) = 0;
  ftk_marker_color_blue (marker)  = 0;
  ftk_marker_color_modified (marker)  = TRUE;
  ftk_ev_markers_modified (eventviewer) = TRUE;

  return ftk_ev_markers_next (eventviewer)++;
}

gint
ftk_eventviewer_marker_new (FtkEventViewer * eventviewer,
			    FtkGlyph glyph,
			    char * label)
{
  return ftk_eventviewer_marker_new_e (eventviewer, glyph, label, NULL);
}

/*
 *
 *	setting marker rgb
 *
 */

gboolean
ftk_eventviewer_set_marker_rgb_e (FtkEventViewer * eventviewer,
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
  ftk_marker_color_modified (marker)	= TRUE;
  ftk_ev_markers_modified (eventviewer)	= TRUE;

  draw_plot (eventviewer);
  
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
				    GdkColor color, GError ** err)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer,
					   marker,
					   color.red,
					   color.green,
					   color.blue,
					   err);
}

gboolean
ftk_eventviewer_set_marker_color (FtkEventViewer * eventviewer,
				  gint marker,
				  GdkColor color)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer,
					   marker,
					   color.red,
					   color.green,
					   color.blue,
					   NULL);
}







gint
ftk_eventviewer_tie_new_e (FtkEventViewer * eventviewer,
			   char * label,
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
    ftk_tie_linewidth (tie)		= 0;
    ftk_tie_linestyle (tie)		= GDK_LINE_SOLID;
    ftk_tie_linestyle_modded (tie)	= FALSE;
    ftk_tie_color_red (tie)		= DEFAULT_TIE_RED;
    ftk_tie_color_green (tie)	= DEFAULT_TIE_GREEN;
    ftk_tie_color_blue (tie)	= DEFAULT_TIE_BLUE;
    ftk_tie_color_modified (tie)	= TRUE;
    ftk_tie_vpos (tie)		= 0;
    
    if (label) asprintf (&t_label, "%s :%2d", label, tag);
    else       asprintf (&t_label, ":%2d", tag);
      
    ftk_tie_label(tie) =
      gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
    ftk_tie_label_modified (tie)	= TRUE;
    free (t_label);
    
    ftk_ev_tie_modified (eventviewer) = TRUE;
  }

  return tag;
}

gint
ftk_eventviewer_tie_new (FtkEventViewer * eventviewer,
			 char * label)
{
  return ftk_eventviewer_tie_new_e (eventviewer, label, NULL);
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
  ftk_tie_color_modified (tie)	= TRUE;
  ftk_ev_tie_modified (eventviewer)	= TRUE;

  draw_plot (eventviewer);
  
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
				   GdkColor color, GError ** err)
{
  return ftk_eventviewer_set_tie_rgb_e (eventviewer,
					  tie,
					  color.red,
					  color.green,
					  color.blue,
					  err);
}

gboolean
ftk_eventviewer_set_tie_color (FtkEventViewer * eventviewer,
				 gint tie,
				 GdkColor color)
{
  return ftk_eventviewer_set_tie_rgb_e (eventviewer,
					  tie,
					  color.red,
					  color.green,
					  color.blue,
					  NULL);
}

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
  ftk_tie_label_modified (tie)	= TRUE;
  ftk_ev_tie_modified (eventviewer)	= TRUE;
  if (ftk_tie_label(tie)) g_object_unref (ftk_tie_label(tie));
  ftk_tie_label(tie) =
    gtk_widget_create_pango_layout (GTK_WIDGET (eventviewer), t_label);
  free (t_label);

  draw_plot (eventviewer);
  
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


/*
 *
 *	setting tie linestyle
 *
 */

gboolean
ftk_eventviewer_set_tie_linestyle_e (FtkEventViewer * eventviewer,
				       gint tie_index,
				       gint lw,
				       GdkLineStyle ls,
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

  if ((ls != GDK_LINE_SOLID) &&
      (ls != GDK_LINE_ON_OFF_DASH) &&
      (ls != GDK_LINE_DOUBLE_DASH)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_EV_ERROR_INVALID_LINESTYLE,	/* error code */
		 "Invalid FtkEventViewer linestyle.");
    return FALSE;
  }

  tie = ftk_ev_tie (eventviewer, tie_index);
  ftk_tie_linestyle (tie)		= ls;
  ftk_tie_linewidth (tie)		= lw;
  ftk_tie_linestyle_modded (tie)	= TRUE;
  ftk_ev_tie_modified (eventviewer)	= TRUE;

  draw_plot (eventviewer);
  
  return TRUE;
}

gboolean
ftk_eventviewer_set_tie_linestyle (FtkEventViewer * eventviewer,
				     gint tie,
				     gint lw,
				     GdkLineStyle ls)
{
  return ftk_eventviewer_set_tie_linestyle_e (eventviewer, tie,
					  lw, ls,
					  NULL);
}



