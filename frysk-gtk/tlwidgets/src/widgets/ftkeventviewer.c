#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/types.h>
#include <sys/time.h>
#include <gtk/gtk.h>
#include "ftkeventviewer.h"

GQuark ftk_quark;

static gboolean ftk_eventviewer_expose(GtkWidget * widget,
				       GdkEventExpose * event,
				       gpointer data);
static gboolean ftk_eventviewer_configure(GtkWidget * widget,
					  GdkEventConfigure * event,
					  gpointer data);
static void ftk_eventviewer_realize(GtkWidget * widget,
				    gpointer data);
static void ftk_eventviewer_destroy(GtkObject * widget,
				    gpointer data);
static void ftk_eventviewer_scale_toggle(GtkToggleButton * button,
					 gpointer data);
static void ftk_eventviewer_spin_vc (GtkSpinButton *spinbutton,
				     gpointer       user_data);
#if 0
static void ftk_eventviewer_scroll_ab(GtkRange *range,
				      gdouble   arg1,
				      gpointer  user_data);
static void ftk_eventviewer_scroll_ms(GtkRange     *range,
				      GtkScrollType arg1,
				      gpointer      user_data);
static void ftk_eventviewer_scroll_vc(GtkRange *range,
				      gpointer  user_data);

#endif
static void ftk_eventviewer_scroll_cv(GtkRange     *range,
				      GtkScrollType scroll,
				      gdouble       value,
				      gpointer      user_data);

static inline double timeval_to_double (struct timeval * tv);

#define DEFAULT_BG_RED		32768
#define DEFAULT_BG_GREEN	32768
#define DEFAULT_BG_BLUE		32768

#define DEFAULT_TRACE_RED	    0
#define DEFAULT_TRACE_GREEN	    0
#define DEFAULT_TRACE_BLUE	    0

#define DEFAULT_MIN_TRACE_LENGTH	300
#define DEFAULT_MIN_TRACE_HEIGHT	 30
#define DEFAULT_TRACE_OFFSET		 10
#define DEFAULT_INITIAL_WIDTH		500
#define DEFAULT_INITIAL_HEIGHT		 30

#define DEFAULT_SPAN			 60.0

/*******************************************************/
/*                                                     */
/*                    object stuff                     */
/*                                                     */
/*******************************************************/

enum {
  FTK_EVENTVIEWER_SIGNAL,
  LAST_SIGNAL
};

static void  ftk_eventviewer_class_init (FtkEventViewerClass * klass);
static void  ftk_eventviewer_init       (FtkEventViewer      * eventviewer);
static guint ftk_eventviewer_signals[LAST_SIGNAL] = { 0 };

static void
ftk_eventviewer_class_init (FtkEventViewerClass *klass)
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
  GtkVBox * vbox = ftk_ev_vbox (eventviewer);
  
  {
    struct timeval now;
    gettimeofday (&now, NULL);
    ftk_ev_zero (eventviewer) = timeval_to_double (&now);
  }
  

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
  
  ftk_ev_events(eventviewer)     = NULL;
  ftk_ev_event_next(eventviewer) = 0;
  ftk_ev_event_max(eventviewer)  = 0;

  ftk_ev_markers (eventviewer)		= NULL;
  ftk_ev_markers_next (eventviewer)	= 0;
  ftk_ev_markers_max (eventviewer)	= 0;
  ftk_ev_markers_modified (eventviewer)	= FALSE;

  ftk_ev_drawable(eventviewer)  = FALSE;

  ftk_ev_pixmap (eventviewer)	= NULL;
  
  ftk_ev_span(eventviewer) = DEFAULT_SPAN;

  g_signal_connect (GTK_OBJECT (eventviewer), "expose-event",
                      (GtkSignalFunc) ftk_eventviewer_expose, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer),"realize",
                      (GtkSignalFunc) ftk_eventviewer_realize, NULL);
  g_signal_connect (GTK_OBJECT(eventviewer), "destroy",
                     GTK_SIGNAL_FUNC(ftk_eventviewer_destroy),
                     NULL);

  gtk_box_set_homogeneous (GTK_BOX (vbox), FALSE);
  gtk_box_set_spacing (GTK_BOX (vbox), 0);

  {
    /*****************   button box  **************/
    
    //    GtkWidget * hbutton_box = gtk_hbutton_box_new();
    GtkWidget * hbutton_box = gtk_hbox_new(FALSE, 5);
    ftk_ev_hbutton_box (eventviewer) = hbutton_box;

    {
      /* scale button */
      
      GtkWidget * scale_toggle_button
	= gtk_toggle_button_new_with_mnemonic ("_Scaled");

      ftk_ev_scale_toggle_button (eventviewer) = scale_toggle_button;
    
      g_signal_connect (GTK_OBJECT(scale_toggle_button),"toggled",
                      (GtkSignalFunc) ftk_eventviewer_scale_toggle, NULL);
      /* fixme -- make initial state configurable */
      gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (scale_toggle_button),
				  TRUE);
      gtk_box_pack_start (GTK_BOX (hbutton_box),
			  scale_toggle_button,
			  FALSE, FALSE, 0);
    }
    {
      /* hold button */
      
      GtkWidget * hold_toggle_button
	= gtk_toggle_button_new_with_mnemonic ("_Hold");

      ftk_ev_hold_toggle_button (eventviewer) = hold_toggle_button;
    
#if 0
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
      /* interval spin button */

      GtkWidget * frame = gtk_frame_new (NULL);
      GtkWidget * hbox  = gtk_hbox_new (FALSE, 0);
      GtkWidget * label = gtk_label_new ("interval");
      GtkObject * ival_adj
	= gtk_adjustment_new (ftk_ev_span (eventviewer),
			      0.0,
			      1e8,
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

      gtk_container_add (GTK_CONTAINER(frame), hbox);
      gtk_box_pack_start (GTK_BOX (hbox), label, FALSE, FALSE, 10);
      gtk_box_pack_start (GTK_BOX (hbox), ival_button, FALSE, FALSE, 0);
      gtk_box_pack_end (GTK_BOX (hbutton_box),
			  frame,
			  FALSE, FALSE, 0);
    }


    
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
    
    g_signal_connect (GTK_OBJECT(da),"configure-event",
                      (GtkSignalFunc) ftk_eventviewer_configure, eventviewer);
    
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
#if 0
    g_signal_connect (GTK_OBJECT(h_scroll),"adjust-bounds",
                      (GtkSignalFunc) ftk_eventviewer_scroll_ab, eventviewer);
    g_signal_connect (GTK_OBJECT(h_scroll),"move-slider",
                      (GtkSignalFunc) ftk_eventviewer_scroll_ms, eventviewer);
    g_signal_connect (GTK_OBJECT(h_scroll),"value-changed",
                      (GtkSignalFunc) ftk_eventviewer_scroll_vc, eventviewer);
#endif
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

  
  if (ftk_ev_trace_modified (eventviewer)) {
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

  if (ftk_ev_markers_modified (eventviewer)) {
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
  
  if (modded) {

    /* resize if necessary */
    
    int tw = label_width + DEFAULT_MIN_TRACE_LENGTH;
    int th = label_height + DEFAULT_MIN_TRACE_HEIGHT;
    int draw_width  = da->allocation.width;
    int draw_height = da->allocation.height;
    ftk_ev_label_box_width(eventviewer)  = label_width;
    ftk_ev_label_box_height(eventviewer) = label_height;
    if ((draw_width < tw) || (draw_height < th)) {
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

static gboolean
ftk_eventviewer_expose(GtkWidget * widget, GdkEventExpose * event,
		       gpointer data)
{
  g_return_val_if_fail (FTK_IS_EVENTVIEWER (widget), FALSE);
  g_return_val_if_fail (event != NULL, FALSE);
  
  if (GTK_WIDGET_DRAWABLE (widget)) {
    FtkEventViewer * eventviewer = FTK_EVENTVIEWER (widget);
    
    gtk_container_propagate_expose (GTK_CONTAINER (eventviewer),
				    ftk_ev_hbutton_box (eventviewer),	
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (widget),
				    GTK_WIDGET (ftk_ev_da_frame (eventviewer)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (widget),
				    GTK_WIDGET (ftk_ev_scroll (eventviewer)),
				    event);

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

  for (i = 0; i < ftk_ev_markers_next(eventviewer); i++) {
    ftk_marker_s * marker = ftk_ev_marker(eventviewer, i);
      
    if (ftk_marker_gc(marker)) g_object_unref (ftk_marker_gc (marker));
  }
  
  if ( ftk_ev_bg_gc(eventviewer))
    g_object_unref (ftk_ev_bg_gc (eventviewer));
      
  if (ftk_ev_pixmap (eventviewer))
    gdk_pixmap_unref(ftk_ev_pixmap (eventviewer));

  if (ftk_ev_traces (eventviewer))  free (ftk_ev_traces(eventviewer));
  if (ftk_ev_events (eventviewer))  free (ftk_ev_events(eventviewer));
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

#if 0
static void
ftk_eventviewer_scroll_ab(GtkRange *range,
			  gdouble   arg1,
			  gpointer  user_data)
{
  fprintf (stderr, "scroll ab\n");
}

static void
ftk_eventviewer_scroll_ms(GtkRange     *range,
			  GtkScrollType arg1,
			  gpointer      user_data)
{
  fprintf (stderr, "scroll ms\n");
}

static void
ftk_eventviewer_scroll_vc(GtkRange *range,
			  gpointer  user_data)
{
  //  fprintf (stderr, "scroll vc %#08x\n", range);
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
    = g_object_new (ftk_eventviewer_get_type (), NULL);
  
  return GTK_WIDGET (eventviewer);
}

/*
 *
 *	setting bg rgb
 *
 */

gboolean
ftk_eventviewer_set_bg_rgb_e (FtkEventViewer * eventviewer,
			      gint red, gint green, gint blue,
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
			    gint red, gint green, gint blue)
{
  return ftk_eventviewer_set_bg_rgb_e (eventviewer,
				       red, green, blue,
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
  
  ftk_ev_span(eventviewer) = span;
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (ftk_ev_interval_button (eventviewer)),
			     span);

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
				 gint red, gint green, gint blue,
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
			       gint red, gint green, gint blue)
{
  return ftk_eventviewer_set_trace_rgb_e (eventviewer, trace,
					  red, green, blue,
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
    ftk_event_s * event;
    int event_nr;
   
    gettimeofday (&now, NULL);
    
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
    ftk_event_time (event)
      = ftk_ev_now(eventviewer) = timeval_to_double (&now);

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

  return TRUE;
}

gboolean
ftk_eventviewer_append_event   (FtkEventViewer * eventviewer,
				gint trace,
				gint marker)
{
  return ftk_eventviewer_append_event_e (eventviewer, trace, marker, NULL);
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
				  gint red, gint green, gint blue,
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
				gint red, gint green, gint blue)
{
  return ftk_eventviewer_set_marker_rgb_e (eventviewer, marker,
					   red, green, blue,
					   NULL);
}
