#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <signal.h>
#include <math.h>
#include <time.h>
#include <gtk/gtk.h>
#include "ftkstripchart.h"

#define ALLOCATION_WIDTH 150

GQuark ftk_quark;

enum {
  FTK_STRIPCHART_SIGNAL,
  LAST_SIGNAL
};

static void ftk_stripchart_class_init (FtkStripchartClass * klass);
static void ftk_stripchart_init       (FtkStripchart      * stripchart);
static guint ftk_stripchart_signals[LAST_SIGNAL] = { 0 };

/************************** object stuff **********************/

static void
ftk_stripchart_class_init (FtkStripchartClass *klass)
{
  ftk_stripchart_signals[FTK_STRIPCHART_SIGNAL]
    = g_signal_new ("ftkstripchart",
		    G_TYPE_FROM_CLASS (klass),
		    G_SIGNAL_RUN_FIRST | G_SIGNAL_ACTION,
		    G_STRUCT_OFFSET (FtkStripchartClass, ftkstripchart),
		    NULL, 
		    NULL,                
		    g_cclosure_marshal_VOID__VOID,
		    G_TYPE_NONE, 0);

  ftk_quark = g_quark_from_string ("Ftk function");
}

static void
ftk_stripchart_configure( GtkWidget * widget,
			  GdkEventConfigure * event,
			  gpointer data)
{
  FtkStripchart * stripchart = FTK_STRIPCHART (widget);
  GtkDrawingArea * da = &(stripchart_drawingarea(stripchart));

  //  fprintf (stderr, "configure\n");

  if (stripchart_pixmap (stripchart))
    gdk_pixmap_unref(stripchart_pixmap (stripchart));
  stripchart_pixmap (stripchart) = gdk_pixmap_new(widget->parent->window,
						  widget->allocation.width,
						  widget->allocation.height,
						  -1);
  {
    int i;
    for (i = 0; i < FTK_STRIPCHART_TYPE_LAST; i++) {
      if (stripchart_event_spec_gc (stripchart, i))
	g_object_unref(stripchart_event_spec_gc (stripchart, i));
      stripchart_event_spec_gc (stripchart, i) =
	gdk_gc_new(stripchart_pixmap (stripchart));
      gdk_gc_set_rgb_fg_color(stripchart_event_spec_gc (stripchart, i),
			      &stripchart_event_spec_color (stripchart, i));
    }
  }
}

/* stripchart_event_spec_gc (stripchart, FTK_STRIPCHART_TYPE_TERMINATE), */
  
static void
ftk_stripchart_expose( GtkWidget * widget,
		       GdkEventExpose * event,
		       gpointer data)
{
  int i;
  struct timeval now;
  struct timeval beginning;
  double d_beginning;
  double d_range;
  double d_bin_width;
  int bin_width;
  double d_this;
  int max_count;
  int draw_width;
  int draw_height;
  int base_y;
  int title_offset;
  
#define TITLE_BASE_X_OFFSET  10
#define TITLE_ASCENT         16
#define TITLE_SPACING         10
#define BASE_OFFSET    15
#define TIC_LENGTH      8

#define float_tv(s,u) (((double)(s)) + (((double)(u))/1.0e6))
  
  FtkStripchart * stripchart = FTK_STRIPCHART (widget);
  GtkDrawingArea * da = &(stripchart_drawingarea(stripchart));

  draw_width  = da->widget.allocation.width;
  draw_height = da->widget.allocation.height;

  gdk_draw_rectangle (stripchart_pixmap (stripchart),
		      widget->style->black_gc,
		      TRUE,
		      0, 0,
		      draw_width, draw_height);


  gettimeofday (&now, NULL);
  timersub (&now, &stripchart_range(stripchart), &beginning);
  d_beginning = float_tv (beginning.tv_sec, beginning.tv_usec);
  d_range = float_tv (stripchart_range_secs(stripchart),
		      stripchart_range_secs(stripchart));
  d_bin_width = float_tv (stripchart_bin_width_secs(stripchart),
			  stripchart_bin_width_secs(stripchart));
  bin_width =
    lrint (((double)(draw_width)) * d_bin_width/d_range);

  max_count = 0;
  for (i = stripchart_event_next (stripchart) - 1;  i >= 0;  i--) {
    if (timercmp (&stripchart_event_tv(stripchart, i), &beginning, <)) break;
    if (max_count <
	stripchart_event_count(stripchart, i, FTK_STRIPCHART_TYPE_TOTAL))
      max_count =
	stripchart_event_count(stripchart, i, FTK_STRIPCHART_TYPE_TOTAL);
  }

  title_offset = TITLE_BASE_X_OFFSET;
  for (i = FTK_STRIPCHART_TYPE_FORK; i < FTK_STRIPCHART_TYPE_LAST; i++) {
    int width;
    gdk_draw_layout (stripchart_pixmap (stripchart),
		     stripchart_event_spec_gc(stripchart, i),
		     title_offset,
		     draw_height - TITLE_ASCENT,
		     stripchart_event_spec_title(stripchart, i));
    pango_layout_get_pixel_size (stripchart_event_spec_title(stripchart, i),
				 &width, NULL);
    title_offset += width + TITLE_SPACING;
  }
  
		   
  base_y = draw_height - BASE_OFFSET;
  for (i = 0; i <= max_count; i++) {
    int dy = lrint (((double)((draw_height - BASE_OFFSET) * i))
		    / ((double)max_count));
    gdk_draw_line (stripchart_pixmap (stripchart),
		   widget->style->white_gc,
		   draw_width - TIC_LENGTH,
		   base_y - dy,
		   draw_width,
		   base_y - dy);
  }
  
  for (i = stripchart_event_next (stripchart) - 1;  i >= 0;  i--) {
    if (timercmp (&stripchart_event_tv(stripchart, i), &beginning, <)) break;

    if (0 < stripchart_event_count(stripchart, i, FTK_STRIPCHART_TYPE_TOTAL)) {
      int j;
      double d_this, offset;
      gint dx, dy;
      
      d_this = float_tv (stripchart_event_tv_sec(stripchart, i),
			 stripchart_event_tv_usec(stripchart, i));
      offset = (d_this - (d_beginning + d_bin_width))/d_range;
      dx = lrint (offset * (double)(draw_width));
      
      base_y = draw_height - BASE_OFFSET;
      for (j = FTK_STRIPCHART_TYPE_FORK; j < FTK_STRIPCHART_TYPE_LAST; j++) {
	if (0 < stripchart_event_count(stripchart, i, j)) {
	  dy = lrint (((double)((draw_height - BASE_OFFSET) *
				stripchart_event_count(stripchart, i, j)))
		      / ((double)max_count));
	
	  gdk_draw_rectangle (stripchart_pixmap (stripchart),
			      stripchart_event_spec_gc(stripchart, j),
			      TRUE,
			      dx,		/* x	 */
			      base_y - dy,	/* y	 */
			      bin_width,	/* width */
			      dy);		/* height */
	  base_y -= dy;
	}
      }
    }
  }
  
  if (GDK_IS_PIXMAP (stripchart_pixmap (stripchart)))
      gdk_draw_drawable(widget->window,
			widget->style->bg_gc[GTK_WIDGET_STATE (widget)],
			stripchart_pixmap (stripchart),
			0, 0,				/* src offsets */
			0, 0,				/* dst offsets */
			draw_width,
			draw_height);
		      
}
  
static void
ftk_stripchart_stripchart( GtkWidget * widget,
			   GdkEventExpose * event,
			   gpointer data)
{
  fprintf (stderr, "ftkstripchart signal\n");
}

static void
init_current_bin (FtkStripchart * stripchart)
{
  event_s * current_event = malloc (sizeof(event_s));

  gettimeofday (&(current_event->tv), NULL);
  current_event->modified = FALSE;
  bzero (&(current_event->count_vec), FTK_STRIPCHART_TYPE_LAST * sizeof(int));
  stripchart_current(stripchart) = current_event;
}


static void
timer_catcher (sigval_t sigval)
{
  FtkStripchart * stripchart = GINT_TO_POINTER(sigval.sival_int);
  ftk_stripchart_expose(GTK_WIDGET (stripchart), NULL, NULL);
  
  if (TRUE == stripchart_current_tv_modified(stripchart)) {

#define STRIPCHART_EVENTS_MAX_INCR 4096
    
    if (stripchart_event_next (stripchart) <=
	stripchart_event_max (stripchart)) {
      int om = stripchart_event_max (stripchart);
    
      if (stripchart_event_max (stripchart) < STRIPCHART_EVENTS_MAX_INCR)
	stripchart_event_max (stripchart) *= 2;
      else 
	stripchart_event_max (stripchart) += STRIPCHART_EVENTS_MAX_INCR;
      stripchart_events (stripchart) =
	realloc (stripchart_events (stripchart),
		 stripchart_event_max (stripchart) * sizeof(event_s *));
      bzero (&stripchart_event(stripchart, om),
	     (stripchart_event_max (stripchart) - om) * sizeof(event_s *));
    }

    stripchart_event(stripchart, stripchart_event_next(stripchart)++) =
      stripchart_current(stripchart);
    init_current_bin (stripchart);
  }

  
#if 0
  g_signal_emit_by_name (GTK_OBJECT (refresh_button),
                           "clicked");
#endif
}

static gboolean
timer_init(FtkStripchart * stripchart, gint ms, GError ** err)
{
  gboolean rc;
  /* least valid update interval (to keep from beating the system to death) */
#define MINIMUM_UPDATE 1000


  /* if a timer is running and either a 0ms or a valid ms is specified,
     kill the timer */
  /* (0ms implies stop the auto-updates.) */
  
  if ((TRUE == stripchart_timer_set(stripchart)) &&
      ((ms == 0) || (ms >= MINIMUM_UPDATE))) {
    timer_delete(stripchart_timer_id(stripchart));
    stripchart_timer_set(stripchart) = FALSE;
  }

  if (ms >= MINIMUM_UPDATE) {
    timer_t timer_id;
    sigval_t sigval;
    sigevent_t sigevent;

    sigval.sival_int = GPOINTER_TO_INT(stripchart);

    sigevent.sigev_value = sigval;
    sigevent.sigev_signo = 0;
    sigevent.sigev_notify = SIGEV_THREAD;
    sigevent.sigev_notify_function = timer_catcher;
    sigevent.sigev_notify_attributes = 0;
  
    rc = (0 ==  timer_create(CLOCK_REALTIME, &sigevent, &timer_id))
      ? TRUE : FALSE;

    if (TRUE == rc) {
      struct itimerspec value;
      struct itimerspec ovalue;
      int secs;
      int nsecs;
      
      stripchart_timer_set(stripchart) = TRUE;
      stripchart_bin_width_secs(stripchart)  = ms/1000;
      stripchart_bin_width_usecs(stripchart) = (ms%1000) * 1000;

      secs = ms/1000;
      nsecs = (ms%1000) * 1000000;
      value.it_interval.tv_sec = secs;
      value.it_interval.tv_nsec = nsecs;
      value.it_value.tv_sec = secs;
      value.it_value.tv_nsec = nsecs;

      init_current_bin (stripchart);

      rc = (0 == timer_settime (timer_id, 0, &value, &ovalue)) ? TRUE : FALSE;
    }
  }
  else {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_UPDATE_INTERVAL,	/* error code */
		 "Invalid update interval: %d ns.",
		 ms);
    rc = FALSE;	/* oob update specified */
  }

  return rc;
}

typedef struct {
  GdkColor color;
  char * title;
} event_info_s;

static event_info_s event_info[FTK_STRIPCHART_TYPE_LAST] = {
  {				/* FTK_STRIPCHART_TYPE_TOTAL*/
    { 0, 65535, 65535, 65535 },	
    "Total"
  },
  {				/* FTK_STRIPCHART_TYPE_FORK */
    { 0, 65535,     0,     0 },	
    "Fork"
  },
  {				/* FTK_STRIPCHART_TYPE_EXEC */
    { 0, 0,     65535,     0 },
    "Exec"
  },
  {				/* FTK_STRIPCHART_TYPE_TERM */
    { 0, 0,         0, 65535 },
    "Term"
  }
};

static void
ftk_stripchart_init (FtkStripchart * stripchart)
{
  GtkDrawingArea * da = &(stripchart_drawingarea(stripchart));

  gtk_widget_set_size_request (GTK_WIDGET (da),
			       FTK_STRIPCHART_INITIAL_WIDTH,
			       FTK_STRIPCHART_INITIAL_HEIGHT);
  stripchart_pixmap (stripchart) = NULL;
  
  gtk_signal_connect (GTK_OBJECT (da), "expose_event",
		      (GtkSignalFunc) ftk_stripchart_expose, NULL);
  gtk_signal_connect (GTK_OBJECT(da),"configure_event",
		      (GtkSignalFunc) ftk_stripchart_configure, NULL);
  gtk_signal_connect (GTK_OBJECT(stripchart),"ftkstripchart",
		      (GtkSignalFunc) ftk_stripchart_stripchart, NULL);

  {
    int i;
    stripchart_event_specs (stripchart) =
      malloc (FTK_STRIPCHART_TYPE_LAST * sizeof(event_spec_s));
    
    for (i = 0; i < FTK_STRIPCHART_TYPE_LAST; i++) {
      memcpy (&stripchart_event_spec_color (stripchart, i),
	      &event_info[i].color, sizeof(GdkColor));
      stripchart_event_spec_gc (stripchart, i) = NULL;
      stripchart_event_spec_title(stripchart, i) =
	gtk_widget_create_pango_layout (GTK_WIDGET (stripchart),
					event_info[i].title);
    }
  }

  stripchart_timer_set(stripchart) = FALSE;

#define STRIPCHART_EVENTS_INITIAL_INCR 64
  stripchart_event_max (stripchart)  = STRIPCHART_EVENTS_INITIAL_INCR;
  stripchart_event_next (stripchart) = 0;
  stripchart_events (stripchart)     =
    malloc (stripchart_event_max (stripchart) * sizeof(event_s));
}

/********************** public api ***************/

GType
ftk_stripchart_get_type ()
{
  static GType stripchart_type = 0;

  if (!stripchart_type)
    {
      static const GTypeInfo stripchart_info =
      {
	sizeof (FtkStripchartClass),		/* class_size		*/
	NULL, 					/* base_init		*/
        NULL,					/* base_finalize	*/
	(GClassInitFunc) ftk_stripchart_class_init,	/* class_init	*/
        NULL, 					/* class_finalize	*/
	NULL, 					/* class_data		*/
        sizeof (FtkStripchart),			/* instance size	*/
	0,					/* n_preallocs		*/
	(GInstanceInitFunc) ftk_stripchart_init,	/* instance_init*/
      };

      stripchart_type = g_type_register_static (GTK_TYPE_DRAWING_AREA,
						"Gtk_Stripchart",
						&stripchart_info, 0);
    }

  return stripchart_type;
}

GtkWidget*
ftk_stripchart_new (void)
{
  gint i;
  
  FtkStripchart * stripchart = g_object_new (ftk_stripchart_get_type (), NULL);
  
  GtkDrawingArea * drawingarea = &(stripchart->drawingarea);
  
  return GTK_WIDGET (stripchart);
}

/*
 *
 *	resize widget
 *
 */

gboolean
ftk_stripchart_resize_e (FtkStripchart * stripchart,
			 gint width, gint height,
			 GError ** err)
{
  GtkDrawingArea * da;

  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }
  
  da = &(stripchart_drawingarea(stripchart));
  if (!GTK_IS_DRAWING_AREA (da)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_DRAWING_AREA,	/* error code */
		 "Invalid drawing area.");
    return FALSE;
    return FALSE;
  }

  gtk_widget_set_size_request (GTK_WIDGET (da), width, height);
  return TRUE;
}

gboolean
ftk_stripchart_resize (FtkStripchart * stripchart,
		       gint width, gint height)
{
  return ftk_stripchart_resize_e (stripchart,width, height, NULL);
}

/*
 *
 *	setting event rgb
 *
 */

gboolean
ftk_stripchart_set_event_rgb_e (FtkStripchart * stripchart,
				FtkStripchartTypeEnum type,
				gint red, gint green, gint blue,
				GError ** err)
{
  GdkColor color;
  
  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }
  if ((type < FTK_STRIPCHART_TYPE_TOTAL) ||
      (type >= FTK_STRIPCHART_TYPE_LAST)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_INVALID_TYPE,	/* error code */
		 "Invalid FtkStripchartTypeEnum %d.",
		 type);
    return FALSE;
  }

  stripchart_event_spec_pixel(stripchart, type) = 0;
  stripchart_event_spec_red(stripchart, type)   = red;
  stripchart_event_spec_green(stripchart, type) = green;
  stripchart_event_spec_blue(stripchart, type)  = blue;
  
  return TRUE;
}

gboolean
ftk_stripchart_set_event_rgb (FtkStripchart * stripchart,
			      FtkStripchartTypeEnum type,
			      gint red, gint green, gint blue)
{
  return ftk_stripchart_set_event_rgb_e (stripchart,
					 type,
					 red, green, blue,
					 NULL);
}

/*
 *
 *	setting event title
 *
 */

gboolean
ftk_stripchart_set_event_title_e (FtkStripchart * stripchart,
				FtkStripchartTypeEnum type,
				char * title,
				GError ** err)
{
  GdkColor color;
  
  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }
  if ((type < FTK_STRIPCHART_TYPE_TOTAL) ||
      (type >= FTK_STRIPCHART_TYPE_LAST)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_INVALID_TYPE,	/* error code */
		 "Invalid FtkStripchartTypeEnum %d.",
		 type);
    return FALSE;
  }

  pango_layout_set_text (stripchart_event_spec_title(stripchart, type),
			 title, strlen (title));

  return TRUE;
}

gboolean
ftk_stripchart_set_event_title (FtkStripchart * stripchart,
				FtkStripchartTypeEnum type,
				char * title)
{
  return ftk_stripchart_set_event_title_e (stripchart,
					   type,
					   title,
					   NULL);
}

/*
 *
 *	setting update
 *
 */

gboolean
ftk_stripchart_set_update_e (FtkStripchart * stripchart,
			     gint milliseconds,
			     GError ** err)
{
  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }

  return timer_init(stripchart, milliseconds, err);
}

gboolean
ftk_stripchart_set_update (FtkStripchart * stripchart, gint milliseconds)
{
  return ftk_stripchart_set_update_e (stripchart, milliseconds, NULL);
}

/*
 *
 *	setting range
 *
 */

gboolean
ftk_stripchart_set_range_e (FtkStripchart * stripchart,
			    gint milliseconds,
			    GError ** err)
{
  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }

  if (0 < milliseconds) {
    stripchart_range_secs(stripchart)  = milliseconds/1000;
    stripchart_range_usecs(stripchart) = (milliseconds%1000) * 1000;
    return TRUE;
  }
  else {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_INVALID_RANGE,	/* error code */
		 "Invalid range %d.",
		 milliseconds);
    return FALSE;
  }
}

gboolean
ftk_stripchart_set_range (FtkStripchart * stripchart, gint milliseconds)
{
  return ftk_stripchart_set_range_e (stripchart, milliseconds, NULL);
}

/*
 *
 *	appending events
 *
 */

gboolean
ftk_stripchart_append_event_e (FtkStripchart * stripchart,
			       FtkStripchartTypeEnum type,
			       GError ** err)
{
  if (!FTK_IS_STRIPCHART (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchart widget.");
    return FALSE;
  }
  if ((type <= FTK_STRIPCHART_TYPE_TOTAL) ||
      (type >= FTK_STRIPCHART_TYPE_LAST)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_INVALID_TYPE,	/* error code */
		 "Invalid FtkStripchartTypeEnum %d.",
		 type);
    return FALSE;
  }
  if (FALSE == stripchart_timer_set(stripchart)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_TIMER_NOT_ENABLED,	/* error code */
		 "Stripchart timer not enabled.");
    return FALSE;
  }

  stripchart_current_count(stripchart, FTK_STRIPCHART_TYPE_TOTAL)++;
  stripchart_current_count(stripchart, type)++;
  stripchart_current_tv_modified(stripchart) = TRUE;

  return TRUE;
}

gboolean
ftk_stripchart_append_event (FtkStripchart * stripchart,
			     FtkStripchartTypeEnum type)
{
  return ftk_stripchart_append_event_e (stripchart,type, NULL);
}
