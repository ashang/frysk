#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <gtk/gtk.h>
#include "ftktimeline.h"

#define ALLOCATION_WIDTH 150

static gboolean ftk_timeline_expose( GtkWidget * widget, GdkEventExpose * event, gpointer data);
static gboolean ftk_timeline_configure( GtkWidget * widget, GdkEventConfigure * event, gpointer data);
static gboolean button_press_event (GtkWidget *widget, GdkEventButton *event, gpointer        data);
static void     normalise_traces (FtkTimeline * timeline, gboolean force);
static void     rebuild_trace (FtkTimeline * timeline, gint trace);
static void *   bsearch_nearest (const void *key, const void *base, size_t nmemb, size_t size,
				 int (*compar) (const void *, const void *));

GdkGC * red_gc;


/**************  object stuff ***********/

enum {
  FTK_TIMELINE_SIGNAL,
  LAST_SIGNAL
};

static void ftk_timeline_class_init          (FtkTimelineClass * klass);
static void ftk_timeline_init                (FtkTimeline      * timeline);
static guint ftk_timeline_signals[LAST_SIGNAL] = { 0 };

static void
ftk_timeline_class_init (FtkTimelineClass *klass)
{
  ftk_timeline_signals[FTK_TIMELINE_SIGNAL] = g_signal_new ("ftktimeline",
					 G_TYPE_FROM_CLASS (klass),
	                                 G_SIGNAL_RUN_FIRST | G_SIGNAL_ACTION,
	                                 G_STRUCT_OFFSET (FtkTimelineClass, ftktimeline),
                                         NULL, 
                                         NULL,                
					 g_cclosure_marshal_VOID__VOID,
                                         G_TYPE_NONE, 0);


}

static void
ftk_timeline_init (FtkTimeline * timeline)
{
  /* empty */
}

/*********************** callbacks ********************/

static int
point_search_fcn (a, b)
     double * a;
     point_s * b;
{
  double av = *a;
  double bv = b->x;
  
  return (av < bv) ? -1 : ((av > bv) ? 1 : 0);
}

static gboolean
motion_notify_event (GtkWidget      *widget,
		    GdkEventMotion *event,
		    gpointer        data)
{
  int i;
  int x, y;
  GdkModifierType state;
  char * str;
  double xv;
  int doit;
  gint trace = GPOINTER_TO_INT (data);
  FtkTimeline * timeline = FTK_TIMELINE (widget->parent);
#if 1
  double val = 0.0;		/* fixme -- timebase delay */
#else
  double val = gtk_adjustment_get_value (GTK_ADJUSTMENT (timeline_drawing_adj(timeline)));
#endif
  
#define p_x(x)    (val + (((double)(x)) - timeline_x_offset(timeline))/ \
                              timeline_x_scale(timeline))

#define c_x(x)	  lrint (timeline_x_offset(timeline) +		\
		   (timeline_x_scale(timeline) * ((x) - val)))
  
  gdk_window_get_pointer (event->window, &x, &y, &state);

  xv = p_x (x);
  
  asprintf (&str,"%g", xv);
  gtk_label_set_text (GTK_LABEL (timeline_readout(timeline)), str);
  free (str);

  for (i = 0; i < timeline_ntraces(timeline); i++) {
    point_s * found = bsearch_nearest (&xv,
				       timeline_trace_points(timeline, i),
				       timeline_trace_point_next(timeline, i),
				       sizeof(point_s),
				       point_search_fcn);
    if (found) {
      // fprintf (stderr, "[%g, %g]\n", found->x, found->y);
      switch (timeline_trace_type(timeline, trace)) {
      case FTK_TRACE_INT:
      case FTK_TRACE_DOUBLE:
	doit = 1;
	break;
      case FTK_TRACE_PID:
	{
	  int dx = x - c_x (found->x);
	  if (dx < 0) dx = -dx;
	  doit = (dx < 15) ? 1 : 0;
	}
      }
    }
    else doit = 0;

    if (doit) {
      asprintf (&str,"%g", found->x);
      gtk_label_set_text (GTK_LABEL (timeline_trace_readout_x(timeline, i)), str);
      free (str);
    
      asprintf (&str,"%g", found->y);
      gtk_label_set_text (GTK_LABEL (timeline_trace_readout_y(timeline, i)), str);
      free (str);
    }
    else {
      gtk_label_set_text (GTK_LABEL (timeline_trace_readout_x(timeline, i)), " ");
      gtk_label_set_text (GTK_LABEL (timeline_trace_readout_y(timeline, i)), " ");
    }
  }
  
  return TRUE;
}

/********************** internal fcns ************************/

static void *
bsearch_nearest (const void *key, const void *base, size_t nmemb, size_t size,
         int (*compar) (const void *, const void *))
{
  size_t l, u, idx;
  const void *p;
  int comparison;

  l = 0;
  u = nmemb;
  while (l < u)
    {
      idx = (l + u) / 2;
      p = (void *) (((const char *) base) + (idx * size));
      comparison = (*compar) (key, p);
      if (comparison < 0)
        u = idx;
      else if (comparison > 0)
        l = idx + 1;
      else
        return (void *) p;
    }

  return (void *)p;		/* return closest match */
}


typedef struct {
  char * lbl;
  double factor;
} tb_scale_s;
  
static tb_scale_s tb_scale[] = {
  {"nsec",                       1.0    },	/* 1.00e+0  - 1.00e+3    0    - 3     */
#define TB_SCALE_NSEC 0
  {"usec",                       1.0e3  },	/* 1.00e+3  - 1.00e+6    3    - 6     */
#define TB_SCALE_USEC 1
  {"msec",                       1.0e6  },	/* 1.00e+6  - 1.00e+9    6    - 9     */
#define TB_SCALE_MSEC 2
  {"sec",                        1.0e9  },	/* 1.00e+9  - 1.00e+12   9    - 12    */
#define TB_SCALE_SEC  3
  {"min",                 60.0 * 1.0e9  },	/* 6.00e+10 - 6.00e+13  10.78 - 13.78 */
#define TB_SCALE_MIN  4
  {"hr",           60.0 * 60.0 * 1.0e9  },	/* 3.60e+12 - 3.60e+15  12.56 - 15.56 */
#define TB_SCALE_HR   5
  {"day",   24.0 * 60.0 * 60.0 * 1.0e9  },	/* 8.64e+13 - 8.64e+16  13.94 - 16.94 */
#define TB_SCALE_DAY  6
};
#define TB_SCALE_MAX 7

/*  17                *
    16              * *
    15              * *
    14            * * *
    13            * *
    12          * *
    11          * *
    10          *
     9        * *
     8        *
     7        *
     6      * *
     5      *
     4      *
     3    * *
     2    *
     1    *
     0    *
*/

static inline double
set_time_range (FtkTimeline * timeline)
{
  return timeline_range_val (timeline) *  tb_scale[timeline_range_factor (timeline)].factor;
}

static void
set_timebase(FtkTimeline * timeline)
{
  int i;
  GValue val = {0, };
  g_value_init (&val,G_TYPE_DOUBLE);
  //  fprintf (stderr, "scale = %g\n", set_time_range (timeline));

  
#if 1
  normalise_traces (timeline, FALSE);

  for (i = 0; i < timeline_ntraces (timeline); i++) {
    rebuild_trace (timeline, i);

    if (GDK_IS_PIXMAP (timeline_trace_pixmap (timeline, i)))
      gdk_draw_pixmap(timeline_trace_area(timeline, i)->window,
      timeline_trace_area(timeline, i)->style->fg_gc[GTK_WIDGET_STATE (timeline_trace_area(timeline, i))],
		      timeline_trace_pixmap (timeline, i),
		      0, 0,
		      0, 0,
		      timeline_trace_area(timeline, i)->allocation.width,
		      timeline_trace_area(timeline, i)->allocation.height);
  }
#endif
}

static void
sb_cb (GtkSpinButton *spinbutton,
       FtkTimeline * timeline)
{
  gdouble vv = gtk_spin_button_get_value_as_float (spinbutton);
  
  if (timeline_range_val (timeline) < vv) {		/* inc */
    if      ((vv >   1.0) && (vv <=    2.0)) vv =    2.0;
    else if ((vv >   2.0) && (vv <=    5.0)) vv =    5.0;
    else if ((vv >   5.0) && (vv <=   10.0)) vv =   10.0;
    else if ((vv >  10.0) && (vv <=   20.0)) vv =   20.0;
    else if ((vv >  20.0) && (vv <=   50.0)) vv =   50.0;
    else if ((vv >  50.0) && (vv <=  100.0)) vv =  100.0;
    else if ((vv > 100.0) && (vv <=  200.0)) vv =  200.0;
    else if ((vv > 200.0) && (vv <=  500.0)) vv =  500.0;
    else if ((vv > 500.0) && (vv <= 1000.0)) vv = 1000.0;
  } else {						/* dec */
    if      ((vv >=   1.0) && (vv <    2.0)) vv =    1.0;
    else if ((vv >=   2.0) && (vv <    5.0)) vv =    2.0;
    else if ((vv >=   5.0) && (vv <   10.0)) vv =    5.0;
    else if ((vv >=  10.0) && (vv <   20.0)) vv =   10.0;
    else if ((vv >=  20.0) && (vv <   50.0)) vv =   20.0;
    else if ((vv >=  50.0) && (vv <  100.0)) vv =   50.0;
    else if ((vv >= 100.0) && (vv <  200.0)) vv =  100.0;
    else if ((vv >= 200.0) && (vv <  500.0)) vv =  200.0;
    else if ((vv >= 500.0) && (vv < 1000.0)) vv =  500.0;
  }
  timeline_range_val (timeline) = vv;
  gtk_spin_button_set_value (spinbutton, vv);
  set_timebase(timeline);
}

#if 0  /* removed */
static void
combo_cb (GtkComboBox * combo_box,
	  FtkTimeline * timeline)
{
  timeline_range_factor(timeline) = gtk_combo_box_get_active (combo_box);
  set_timebase(timeline);
}
#endif
  

static void
ftk_timeline_init_2 (FtkTimeline * timeline)
{
  GtkWidget * radio1;
  GtkWidget * radio2;
  gint row;

  /************** drawing area ************/

  timeline_range_val (timeline) = 1.0;
  timeline_delay_val (timeline) = 0.0;

  /* /from mnt/mattsbox/home/moller/tinkering/oprofile/oprofile-with-op_visualise/op_visualise/main.c */
  for (row = 0; row < timeline_ntraces(timeline); row++) {
    GtkWidget * da;
    
    timeline_trace_area (timeline, row) = da = gtk_drawing_area_new ();
    
    gtk_widget_set_size_request (da, timeline_width(timeline), 50); /* fixme -- parms, resize */
    gtk_widget_show (da);
    gtk_table_attach( GTK_TABLE(timeline), da, 1, 2, row, row+1,
		      GTK_EXPAND|GTK_FILL, GTK_FILL, 0, 0 );

    gtk_widget_add_events (da,
			   GDK_POINTER_MOTION_MASK |
			   GDK_POINTER_MOTION_HINT_MASK |
			   GDK_BUTTON_PRESS_MASK);
    
    gtk_signal_connect (GTK_OBJECT (da), "expose_event",
			(GtkSignalFunc) ftk_timeline_expose, GINT_TO_POINTER (row));
    gtk_signal_connect (GTK_OBJECT(da),"configure_event",
			(GtkSignalFunc) ftk_timeline_configure, GINT_TO_POINTER (row));

    gtk_signal_connect (GTK_OBJECT(da), "motion_notify_event",
		      (GtkSignalFunc) motion_notify_event, GINT_TO_POINTER (row));

    timeline_trace_label(timeline, row) = gtk_label_new ("hi there");
    gtk_widget_show (timeline_trace_label(timeline, row));
    gtk_table_attach( GTK_TABLE(timeline), timeline_trace_label(timeline, row), 0, 1, row, row+1,
		      0, GTK_FILL, 0, 0 );

    {
      GtkWidget * frame = gtk_frame_new ("");
      GtkWidget * vbox  = gtk_vbox_new (TRUE, 0);  /* homogeneous, space 0 */
      
      timeline_trace_readout_x(timeline, row) = gtk_label_new ("          ");
      gtk_label_set_width_chars (GTK_LABEL (timeline_trace_readout_x(timeline, row)), 10);
      gtk_widget_show (timeline_trace_readout_x(timeline, row));
      gtk_box_pack_start_defaults (GTK_BOX (vbox), timeline_trace_readout_x(timeline, row));
				   
      timeline_trace_readout_y(timeline, row) = gtk_label_new ("          ");
      gtk_label_set_width_chars (GTK_LABEL (timeline_trace_readout_y(timeline, row)), 10);
      gtk_widget_show (timeline_trace_readout_y(timeline, row));
      gtk_box_pack_start_defaults (GTK_BOX (vbox), timeline_trace_readout_y(timeline, row));
      
      gtk_widget_show (vbox);
      gtk_widget_show (frame);

      gtk_container_add (GTK_CONTAINER(frame), vbox);
      
      gtk_table_attach( GTK_TABLE(timeline), frame, 2, 3, row, row+1,
			0, GTK_FILL, 0, 0 );
    }
  }

  /*********************** timebase delay *****************/
  {
    GtkAdjustment * spinner_adj;
    GtkWidget * spinner;
    GtkWidget * combo;
    GtkWidget * frame = gtk_frame_new ("Delay");
    GtkWidget * hbox  = gtk_hbox_new (TRUE, 0);  /* homogeneous, space 0 */

    radio1 = gtk_radio_button_new_with_label (NULL, "Lock");
    
    spinner_adj  = (GtkAdjustment *)gtk_adjustment_new (timeline_delay_val (timeline), /* gdouble value */ 
							0.0,	/* gdouble lower */         
							1000000,	/* gdouble upper */         
							1.0,	/* gdouble step_increment */
							5.0,	/* gdouble page_increment */
							5.0);	/* gdouble page_size      */
    timeline_delay_spin (timeline) = spinner = gtk_spin_button_new (spinner_adj, 1.0, 0);

    gtk_spin_button_set_numeric      ( GTK_SPIN_BUTTON (spinner), TRUE);
    gtk_spin_button_set_snap_to_ticks( GTK_SPIN_BUTTON (spinner), TRUE);
    gtk_spin_button_set_update_policy( GTK_SPIN_BUTTON (spinner), GTK_UPDATE_IF_VALID);
    
    timeline_delay_units(timeline) = combo = gtk_combo_box_new_text ();
    {
      int i;
      for (i = 0; i < TB_SCALE_MAX; i++)
	gtk_combo_box_append_text (GTK_COMBO_BOX (combo), tb_scale[i].lbl);
    }
    timeline_delay_factor (timeline) = TB_SCALE_SEC;
    gtk_combo_box_set_active (GTK_COMBO_BOX (combo), TB_SCALE_NSEC);
    
    gtk_widget_show (hbox);
    gtk_widget_show (frame);
    gtk_widget_show (spinner);
    gtk_widget_show (combo);
    gtk_widget_show (radio1);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), radio1);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), spinner);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), combo);

    gtk_container_add (GTK_CONTAINER(frame), hbox);
    gtk_table_attach (GTK_TABLE (timeline), frame, 0, 1, row, row+1,
		      0, GTK_FILL, 0, 0); 
  }

  /*********************** timebase range *****************/
  {
    GtkAdjustment * spinner_adj;
    GtkWidget * spinner;
    GtkWidget * combo;
    GtkWidget * frame = gtk_frame_new ("Range");
    GtkWidget * hbox  = gtk_hbox_new (TRUE, 0);  /* homogeneous, space 0 */
    
    radio2 = gtk_radio_button_new_with_label_from_widget (GTK_RADIO_BUTTON (radio1), "Lock");
    
    spinner_adj  = (GtkAdjustment *)gtk_adjustment_new (timeline_range_val (timeline), /* gdouble value */ 
							1.0,	/* gdouble lower */         
							1000.0,	/* gdouble upper */         
							1.0,	/* gdouble step_increment */
							5.0,	/* gdouble page_increment */
							5.0);	/* gdouble page_size      */
    timeline_range_spin (timeline) = spinner = gtk_spin_button_new (spinner_adj, 1.0, 0);

    gtk_spin_button_set_numeric      ( GTK_SPIN_BUTTON (spinner), TRUE);
    gtk_spin_button_set_snap_to_ticks( GTK_SPIN_BUTTON (spinner), TRUE);
    gtk_spin_button_set_update_policy( GTK_SPIN_BUTTON (spinner), GTK_UPDATE_IF_VALID);
    
    gtk_signal_connect (GTK_OBJECT (spinner), "value-changed",
			G_CALLBACK (sb_cb), timeline);

    timeline_range_units(timeline) = combo = gtk_combo_box_new_text ();
    {
      int i;
      for (i = 0; i < TB_SCALE_MAX; i++)
	gtk_combo_box_append_text (GTK_COMBO_BOX (combo), tb_scale[i].lbl);
    }
    timeline_range_factor (timeline) = TB_SCALE_SEC;
    gtk_combo_box_set_active (GTK_COMBO_BOX (combo), TB_SCALE_NSEC);

#if 0 /* removed */
    gtk_signal_connect (GTK_OBJECT (combo), "changed",
			G_CALLBACK (combo_cb), timeline);
#endif
    
      
    gtk_widget_show (hbox);
    gtk_widget_show (frame);
    gtk_widget_show (spinner);
    gtk_widget_show (combo);
    gtk_widget_show (radio2);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), radio2);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), spinner);
    gtk_box_pack_start_defaults (GTK_BOX (hbox), combo);

    gtk_container_add (GTK_CONTAINER(frame), hbox);
    gtk_table_attach (GTK_TABLE (timeline), frame, 1, 2, row, row+1,
		      0, GTK_FILL, 0, 0); 
  }

  /*********************** time readout *****************/

  {
    timeline_readout(timeline) = gtk_label_new ("          ");
    gtk_widget_show (timeline_readout(timeline));
    gtk_table_attach( GTK_TABLE(timeline), timeline_readout(timeline), 2, 3, row, row+1,
		      0, GTK_FILL, 0, 0 );
    
  }
}

static int
point_sort_fcn (a, b)
     point_s * a;
     point_s * b;
{
  double av = a->x;
  double bv = b->x;
  
  return (av < bv) ? -1 : ((av > bv) ? 1 : 0);
}

static void
normalise_traces (FtkTimeline * timeline, gboolean force)
{
  int i;
  GtkWidget * widget;

  //  fprintf (stderr, "normalise_traces\n");
  
  timeline_x_min (timeline) = HUGE_VAL;
  timeline_x_max (timeline) = -HUGE_VAL;
  for (i = 0; i < timeline_ntraces (timeline); i++) {
    if (0 < timeline_trace_point_next(timeline, i)) {
      if (force || timeline_trace_changed(timeline, i)) {
	widget = timeline_trace_area (timeline, i);
	qsort (timeline_trace_points(timeline, i),
	       timeline_trace_point_next(timeline, i),
	       sizeof(point_s),
	       point_sort_fcn);
	timeline_trace_changed(timeline, i) = FALSE;
	timeline_trace_y_scale (timeline, i) = ((double)(widget->allocation.height)) /
	  (timeline_trace_y_max (timeline, i) - timeline_trace_y_min (timeline, i));
	timeline_trace_y_offset  (timeline, i)=
	  -timeline_trace_y_min (timeline, i) * timeline_trace_y_scale (timeline, i);
      }
      if (timeline_x_min (timeline) > timeline_trace_point_x(timeline, i, 0))
	timeline_x_min (timeline) = timeline_trace_point_x(timeline, i, 0);
      if (timeline_x_max (timeline) <
	  timeline_trace_point_x(timeline, i, timeline_trace_point_next(timeline, i) - 1))
	timeline_x_max (timeline) =
	  timeline_trace_point_x(timeline, i, timeline_trace_point_next(timeline, i) - 1);
    }
  }

  if (HUGE_VAL !=  timeline_x_min (timeline)) {
    double u_range;
    double t_range = timeline_x_max (timeline) - timeline_x_min (timeline);
    {
      int i, j;
      double mm[] = {  1.0,   2.0,   5.0,
		      10.0,  20.0,  50.0,
		       100.0, 200.0, 500.0, 1000.0};
      int mml = sizeof(mm)/sizeof(double);
      for (i = 0; i < TB_SCALE_MAX; i++) {
	if ((1000.0 * tb_scale[i].factor) >= t_range) break;
      }

      /* fixme -- make part of autorange */
#if 1
      timeline_range_factor(timeline) = i;
#else
      gtk_combo_box_set_active (GTK_COMBO_BOX (timeline_range_units(timeline)), i);
#endif
      for (j = 0; j < mml; j++) {
	if ((mm[j] * tb_scale[i].factor) >= t_range) break;
      }
      u_range = mm[j] * tb_scale[i].factor;
#if 1
      timeline_range_val (timeline) == mm[j];
#else
      gtk_spin_button_set_value (GTK_SPIN_BUTTON (timeline_range_spin (timeline)), mm[j]);
#endif
    }
    timeline_x_scale (timeline)= ((double)(timeline_width(timeline))) / u_range;
    timeline_x_offset (timeline) = -timeline_x_min (timeline) * timeline_x_scale (timeline);

  }
}

static void
rebuild_trace (FtkTimeline * timeline, gint trace)
{
  int i, p;
  GtkWidget * widget;
  GdkPoint * points;
#if 1
  double val = 0.0;	/* fixme -- timebase delay */
#else
  double val = gtk_adjustment_get_value (GTK_ADJUSTMENT (timeline_drawing_adj(timeline)));
#endif
  
#define x_p(x)    ((gint)rint(timeline_x_offset(timeline) + \
                      (timeline_x_scale(timeline) * ((x) - val))))
#define y_p(y, c) ((gint)rint(timeline_trace_y_offset(timeline, (c)) \
                    + (timeline_trace_y_scale(timeline, (c)) * (y))))

  if (0 < timeline_trace_point_next(timeline, trace)) {
    points = malloc (timeline_trace_point_next(timeline, trace) * sizeof (GdkPoint));
    widget = timeline_trace_area (timeline, trace);
    for (p = 0; p < timeline_trace_point_next(timeline, trace); p++) {
      points[p].x  = x_p (timeline_trace_point_x(timeline, trace, p));
      switch (timeline_trace_type(timeline, trace)) {
      case  FTK_TRACE_INT:
      case  FTK_TRACE_DOUBLE:
	points[p].y  = widget->allocation.height - y_p (timeline_trace_point_y(timeline, trace, p), trace);
	break;
      case  FTK_TRACE_PID:
	points[p].y  = widget->allocation.height/2;
	break;
      }
    }

    if (timeline_trace_pixmap (timeline, trace)) gdk_pixmap_unref(timeline_trace_pixmap (timeline, trace));
    timeline_trace_pixmap (timeline, trace) = gdk_pixmap_new(widget->parent->window,
							     widget->allocation.width,
							     widget->allocation.height,
							     -1);
    {
      static GdkColor red    = {0, 65535,     0, 0};
      red_gc = gdk_gc_new(timeline_trace_pixmap (timeline, trace));
      gdk_gc_set_rgb_fg_color(red_gc, &red);
    }	

    gdk_draw_rectangle (timeline_trace_pixmap (timeline, trace),
			widget->style->white_gc,
			TRUE,
			0, 0,
			widget->allocation.width,
			widget->allocation.height);

    switch (timeline_trace_type(timeline, trace)) {
    case  FTK_TRACE_INT:
    case  FTK_TRACE_DOUBLE:
      gdk_draw_lines (timeline_trace_pixmap (timeline, trace),
		      widget->style->fg_gc[widget->state],
		      points,
		      timeline_trace_point_next(timeline, trace));
      break;
    case  FTK_TRACE_PID:
#if 1
      for (p = 0; p < timeline_trace_point_next(timeline, trace); p++) {
	gdk_draw_rectangle (timeline_trace_pixmap (timeline, trace),
			    red_gc,
			    TRUE,			/* filled */
			    points[p].x - 1,  
			    points[p].y - 1,
			    3,				/* width */
			    3);				/* height */
      }
#else
      gdk_draw_points (timeline_trace_pixmap (timeline, trace),
		       red_gc,
		       points,
		       timeline_trace_point_next(timeline, trace));
#endif
      break;
    }


    free (points);
  }
}
     
static gboolean
ftk_timeline_expose( GtkWidget * widget, GdkEventExpose * event, gpointer data)
{
  int trace = GPOINTER_TO_INT (data);
  FtkTimeline * timeline = FTK_TIMELINE (widget->parent);
  //  fprintf (stderr, "ftk_timeline_expose\n");

  if (0 < timeline_ntraces(timeline)) {
    normalise_traces (timeline, FALSE);
    rebuild_trace (timeline, GPOINTER_TO_INT (data));
  
    if (GDK_IS_PIXMAP (timeline_trace_pixmap (timeline, trace)))
      gdk_draw_pixmap(widget->window,
		      widget->style->fg_gc[GTK_WIDGET_STATE (widget)],
		      timeline_trace_pixmap (timeline, trace),
		      event->area.x, event->area.y,
		      event->area.x, event->area.y,
		      event->area.width, event->area.height);
  }
}
     
static gboolean
ftk_timeline_configure( GtkWidget * widget, GdkEventConfigure * event, gpointer data)
{
  int trace = GPOINTER_TO_INT (data);
  FtkTimeline * timeline = FTK_TIMELINE (widget->parent);
  //  fprintf (stderr, "ftk_timeline_configure\n");

  if (0 < timeline_ntraces(timeline)) {
    timeline_width(timeline) = widget->allocation.width;

    normalise_traces (timeline, TRUE);
    rebuild_trace (timeline, trace);

    if (GDK_IS_PIXMAP (timeline_trace_pixmap (timeline, trace)))
      gdk_draw_pixmap(widget->window,
		      widget->style->fg_gc[GTK_WIDGET_STATE (widget)],
		      timeline_trace_pixmap (timeline, trace),
		      event->x, event->y,
		      event->x, event->y,
		      event->width, event->height);
  }
}

/********************** public api ***************/

GType
ftk_timeline_get_type ()
{
  static GType timeline_type = 0;

  if (!timeline_type)
    {
      static const GTypeInfo timeline_info =
      {
	sizeof (FtkTimelineClass),			/* class_size		*/
	NULL, 					/* base_init		*/
        NULL,					/* base_finalize	*/
	(GClassInitFunc) ftk_timeline_class_init,	/* class_init		*/
        NULL, 					/* class_finalize	*/
	NULL, 					/* class_data		*/
        sizeof (FtkTimeline),			/* instance size	*/
	0,					/* n_preallocs		*/
	(GInstanceInitFunc) ftk_timeline_init,	/* instance_init	*/
      };

      timeline_type = g_type_register_static (GTK_TYPE_TABLE, "Gtk_Timeline", &timeline_info, 0);
    }

  return timeline_type;
}

GtkWidget*
ftk_timeline_new (gint rows)
{
  gint i;
  
  FtkTimeline * timeline = g_object_new (ftk_timeline_get_type (), NULL);
  
  GtkTable * table = &(timeline->table);
  table->homogeneous = FALSE;
  timeline->ntraces = rows;
  gtk_table_resize (GTK_TABLE (timeline), rows + 1, 3);
  gtk_table_set_col_spacings (GTK_TABLE (timeline), 15);
  
  timeline->traces = malloc (timeline->ntraces * sizeof(trace_s));
  timeline_width(timeline) = ALLOCATION_WIDTH;

  for (i = 0; i < timeline->ntraces; i++) {
    timeline_trace_point_max(timeline, i)  = 0;
    timeline_trace_point_next(timeline, i) = 0;
    timeline_trace_point_incr(timeline, i) = POINT_INCR_INITIAL;
    timeline_trace_points(timeline, i)     = NULL;
    timeline_trace_pixmap(timeline, i)     = NULL;
    timeline_trace_points(timeline, i)     = NULL;
    timeline_trace_changed(timeline, i)    = FALSE;
    timeline_trace_y_min(timeline, i)      = HUGE_VAL;
    timeline_trace_y_max(timeline, i)      = -HUGE_VAL;
    timeline_trace_type(timeline, i)	   = FTK_TRACE_UNKNOWN;
  }
  timeline_traces_changed(timeline) = FALSE;
  timeline_initted(timeline)        = FALSE;
  
  return GTK_WIDGET (timeline);
}


gboolean
ftk_timeline_set_trace_label (FtkTimeline *timeline, gint trace, const gchar * label)
{
  if (!timeline || (trace >= timeline_ntraces(timeline))) return FALSE;
  
  if (!timeline_initted(timeline)) {
    timeline_initted(timeline) = TRUE;
    ftk_timeline_init_2(timeline);
  }

  gtk_label_set_text (GTK_LABEL (timeline_trace_label(timeline, trace)), label);
}

gboolean
ftk_timeline_set_trace_type (FtkTimeline *timeline, gint trace, FtkTraceType type)
{
  if (!timeline || (trace >= timeline_ntraces(timeline))) return FALSE;
  
  if (!timeline_initted(timeline)) {
    timeline_initted(timeline) = TRUE;
    ftk_timeline_init_2(timeline);
  }
  
  timeline_trace_type(timeline, trace) = type;
}

static gboolean
ftk_timeline_append_point  (FtkTimeline *timeline, gint trace, double x, FtkTraceType type, data_u * y)
{
  if (!timeline || (trace >= timeline_ntraces(timeline))) return FALSE;
  
  if (!timeline_initted(timeline)) {
    timeline_initted(timeline) = TRUE;
    ftk_timeline_init_2(timeline);
  }
  if (FTK_TRACE_UNKNOWN == timeline_trace_type(timeline, trace))
    timeline_trace_type(timeline, trace) = type;
  else if (timeline_trace_type(timeline, trace) != type) return FALSE;
  

  if (timeline_trace_point_max(timeline, trace) <= timeline_trace_point_next(timeline, trace)) {
    timeline_trace_point_max(timeline, trace) += timeline_trace_point_incr(timeline, trace);
    if (4096 > sizeof(point_s) * timeline_trace_point_incr(timeline, trace))
      timeline_trace_point_incr(timeline, trace) *= 2;
    timeline_trace_points(timeline, trace) = realloc (timeline_trace_points(timeline, trace),
                                         timeline_trace_point_max(timeline, trace) * sizeof(point_s));
  }
  /* fixme -- sort as an option? */
  timeline_trace_point_x(timeline, trace, timeline_trace_point_next(timeline, trace)) = x;

  /* fixme -- make data allocation match type */
  switch (type) {
  case FTK_TRACE_INT:
    {
      double ty = (double)((*y).i);
      timeline_trace_point_y(timeline, trace, timeline_trace_point_next(timeline, trace)) = ty;
      if (timeline_trace_y_max(timeline, trace) < ty) timeline_trace_y_max(timeline, trace) = ty;
      if (timeline_trace_y_min(timeline, trace) > ty) timeline_trace_y_min(timeline, trace) = ty;
    }
    break;
  case FTK_TRACE_DOUBLE:
    {
      double ty = (*y).d;
      timeline_trace_point_y(timeline, trace, timeline_trace_point_next(timeline, trace)) = ty;
      if (timeline_trace_y_max(timeline, trace) < ty) timeline_trace_y_max(timeline, trace) = ty;
      if (timeline_trace_y_min(timeline, trace) > ty) timeline_trace_y_min(timeline, trace) = ty;
    }
    break;
  case FTK_TRACE_PID:
    {
      double ty = (double)((*y).p);
      timeline_trace_point_y(timeline, trace, timeline_trace_point_next(timeline, trace)) = ty;
      if (timeline_trace_y_max(timeline, trace) < ty) timeline_trace_y_max(timeline, trace) = ty;
      if (timeline_trace_y_min(timeline, trace) > ty) timeline_trace_y_min(timeline, trace) = ty;
    }
    break;
  }
  
  timeline_trace_point_next(timeline, trace)++;
  timeline_trace_changed(timeline, trace) = TRUE;
  timeline_traces_changed(timeline) = TRUE;

  return TRUE;
}

gboolean
ftk_timeline_append_point_int (FtkTimeline *timeline, gint trace, double x, int y)
{
  data_u u = (data_u)y;
  return ftk_timeline_append_point  (timeline, trace, x, FTK_TRACE_INT, &u);
}

gboolean
ftk_timeline_append_point_dbl (FtkTimeline *timeline, gint trace, double x, double y)
{
  data_u u = (data_u)y;
  return ftk_timeline_append_point  (timeline, trace, x, FTK_TRACE_DOUBLE, &u);
}

gboolean
ftk_timeline_append_point_pid (FtkTimeline *timeline, gint trace, double x, pid_t y)
{
  data_u u = (data_u)y;
  return ftk_timeline_append_point  (timeline, trace, x, FTK_TRACE_PID, &u);
}


