#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <gtk/gtk.h>
#include "ftkstripchartx.h"
#include "ll.xpm"
#include "rr.xpm"
#define _GNU_SOURCE
#include <string.h>

#define DEFAULT_DA_WIDTH   100
#define DEFAULT_DA_HEIGHT   25

#define INITIAL_DELTA	  60.0
#define SLIDER_WIDTH	 100
#define SLIDER_HEIGHT	  20

#define DEFAULT_BG_RED   28000
#define DEFAULT_BG_GREEN 28000
#define DEFAULT_BG_BLUE  28000

#define RIGHT_MARGIN         10
#define TITLE_BASE_X_OFFSET  10
#define TITLE_ASCENT         16
#define TITLE_SPACING        10
#define BOTTOM_MARGIN        15
#define TOP_MARGIN           18
#define RIGHT_MARGIN         10
#define TIC_LENGTH            8

#define SLIDER_GEOMETRY_OFFSET	(SLIDER_HEIGHT/2)
#define SLIDER_LEFT_ADJUST	(SLIDER_GEOMETRY_OFFSET - 8)
#define SLIDER_RIGHT_ADJUST	(SLIDER_GEOMETRY_OFFSET + 4)
#define SLIDER_RANGE_ADJUST	(SLIDER_RIGHT_ADJUST - SLIDER_LEFT_ADJUST)

#define slider_position(v) (SLIDER_LEFT_ADJUST + \
      ((v) * ((double)(da->allocation.width - SLIDER_RANGE_ADJUST))))

#define get_slider_right	lrint(slider_position(stripchart_slider_adj_right (stripchart)))
#define get_slider_left		lrint(slider_position(stripchart_slider_adj_left (stripchart)))
#define get_slider_value  \
   lrint(slider_position(gtk_adjustment_get_value (GTK_ADJUSTMENT (stripchart_slider_adjustment (stripchart)))))

#define slider_value(p) (((double)((p) - SLIDER_LEFT_ADJUST)) / \
                         ((double)(da->allocation.width - SLIDER_RANGE_ADJUST)))

typedef enum {
  FTK_BUTTON_NONE,
  FTK_BUTTON_START,
  FTK_BUTTON_START_LOCK,
  FTK_BUTTON_HOLD,
  FTK_BUTTON_END
} ftk_button_e;

typedef enum {
  FTK_UPDATED_NONE,
  FTK_UPDATED_START_LIMIT,	/* */
  FTK_UPDATED_START_SPIN,	/* */
  FTK_UPDATED_RANGE_SPIN,	/* */
  FTK_UPDATED_END_SPIN,		/* */
  FTK_UPDATED_END_LIMIT,	/* */
  FTK_UPDATED_START_LOCK,	/* */
  FTK_UPDATED_HOLD,		/* */
  FTK_UPDATED_LEFT_SLIDER,	/* */
  FTK_UPDATED_CENTER_SLIDER,	/* */
  FTK_UPDATED_RIGHT_SLIDER,	/* */
  FTK_UPDATED_NOW,		/* */
} ftk_updated_e;


static gboolean ftk_stripchart_expose( GtkWidget * widget,
				       GdkEventExpose * event, gpointer data);
#if 0
static gboolean ftk_stripchart_configure( GtkWidget * widget,
					  GdkEventConfigure * event,
					  gpointer data);
#endif
static void ftk_spinbutton_vc(GtkSpinButton *spinbutton, gpointer user_data);
static void draw_chart (GtkWidget * widget, gpointer data);
static void ftk_paint_slider (FtkStripchartX * stripchart);

GQuark ftk_quark;

/**************  object stuff ***********/

enum {
  FTK_STRIPCHART_SIGNAL,
  LAST_SIGNAL
};

static void ftk_stripchart_class_init (FtkStripchartXClass * klass);
static void ftk_stripchart_init       (FtkStripchartX      * stripchart);
static guint ftk_stripchart_signals[LAST_SIGNAL] = { 0 };

static void
ftk_stripchart_class_init (FtkStripchartXClass *klass)
{
  //GtkContainerClass *container_class = (GtkContainerClass *) klass;

  ftk_stripchart_signals[FTK_STRIPCHART_SIGNAL]
    = g_signal_new ("ftkstripchartx",
		    G_TYPE_FROM_CLASS (klass),
		    G_SIGNAL_RUN_FIRST | G_SIGNAL_ACTION,
		    G_STRUCT_OFFSET (FtkStripchartXClass, ftkstripchart),
		    NULL, 
		    NULL,                
		    g_cclosure_marshal_VOID__VOID,
		    G_TYPE_NONE, 0);


}

static void
init_current_bin (FtkStripchartX * stripchart)
{
  event_s * current_event = malloc (sizeof(event_s));

  gettimeofday (&(current_event->tv), NULL);
  current_event->modified = FALSE;
  current_event->total = 0;
  current_event->count_rho = stripchart_event_spec_next(stripchart);
  current_event->count_vec = malloc (current_event->count_rho * sizeof(int));
  bzero (current_event->count_vec, current_event->count_rho * sizeof(int));
  stripchart_current(stripchart) = current_event;
}

static struct timeval
double_to_timeval (double rv)
{
  double ipart;
  struct timeval rs;

  rv = modf (rv, &ipart);
  rs.tv_sec  = (time_t)ipart;
  rs.tv_usec = (suseconds_t)lrint (1000000.0 * rv);

  return rs;
}

static inline double
timeval_to_double (struct timeval * tv)
{
  return ((double)(tv->tv_sec)) + (((double)(tv->tv_usec))/1.0e6);
}
 
static char *
format_time (FtkStripchartX * stripchart, struct timeval * tv)
{
  char * ms = NULL;
  struct tm result;

  localtime_r(&(tv->tv_sec), &result);
  switch(result.tm_mon) {
  case  0: ms = "Jan"; break;
  case  1: ms = "Feb"; break;
  case  2: ms = "Mar"; break;
  case  3: ms = "Apr"; break;
  case  4: ms = "May"; break;
  case  5: ms = "Jun"; break;
  case  6: ms = "Jul"; break;
  case  7: ms = "Aug"; break;
  case  8: ms = "Sep"; break;
  case  9: ms = "Oct"; break;
  case 10: ms = "Nov"; break;
  case 11: ms = "Dec"; break;
  }
  sprintf (stripchart_ostring (stripchart), "%s %02d %02d:%02d:%02d.%03d",
	   ms,
	   result.tm_mday,
	   result.tm_hour,
	   result.tm_min,
	   result.tm_sec,
	   (int)(tv->tv_usec)/1000);
  return stripchart_ostring (stripchart);
}

static struct timeval
get_lock_time (GtkWidget * sb)
{
  struct timeval now;
  struct timeval res;
  struct timeval rs;
  double rv, ipart;

  gettimeofday (&now, NULL);
  rv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (sb));
  rv = modf (-rv, &ipart);
  rs.tv_sec  = (time_t)ipart;
  rs.tv_usec = (suseconds_t)lrint (1000000.0 * rv);
  timersub (&now, &rs, &res);
  
  return res;
}

static double
get_spin_val (struct timeval * tv)
{
  struct timeval now;
  struct timeval res;

  gettimeofday (&now, NULL);
  timersub (tv, &now, &res);
  return timeval_to_double (&res);
}

#if 0
static double
get_offset_time (struct timeval * tvp, FtkStripchartX * stripchart, double dv)
{
  struct timeval tvr;
  double d_beginning = timeval_to_double (&stripchart_locked_start_time(stripchart));
  double d_ending = timeval_to_double (&stripchart_locked_end_time(stripchart));
  double tvd = d_beginning + (dv  * (d_ending - d_beginning));

  tvr = double_to_timeval (tvd);
  tvp->tv_sec  = tvr.tv_sec;
  tvp->tv_usec = tvr.tv_usec;
  
  return tvd;
}
#endif

static void
update_end_spin (FtkStripchartX * stripchart)
{
  double cv;
  double lv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)));
  double rv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));
  
  if (lv > rv) {
    rv = lv;
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)), rv);
  }
  cv = rv - lv;
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)), cv);
  stripchart_locked_end_time (stripchart) =
    get_lock_time (stripchart_right_spin_box (stripchart));
  gtk_label_set_text (GTK_LABEL (stripchart_right_label (stripchart)),	
		      format_time (stripchart, &stripchart_locked_end_time(stripchart)));
}

static void
update_start_spin (FtkStripchartX * stripchart)
{
  double cv;
  double lv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)));
  double rv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));

  gboolean is_locked =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_lock_button(stripchart)));
    
  gboolean is_held =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_hold_button(stripchart)));
  
  if (lv > rv) {
    lv = rv;
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)), lv);
  }
  cv = rv - lv;
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)), cv);
  
  if (is_locked || is_held) {
    stripchart_locked_start_time (stripchart) =
      get_lock_time (stripchart_left_spin_box (stripchart));
    gtk_label_set_text (GTK_LABEL (stripchart_left_label (stripchart)),	
			format_time (stripchart, &stripchart_locked_start_time(stripchart)));
  }	
}

static void
update_range_spin (FtkStripchartX * stripchart)
{
  double cv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)));
  double rv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));
  double lv = rv - cv;

  gboolean is_locked =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_lock_button(stripchart)));
    
  gboolean is_held =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_hold_button(stripchart)));
  
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)), lv);
  if (is_locked || is_held)
    stripchart_locked_start_time (stripchart) =
      get_lock_time (stripchart_left_spin_box (stripchart));
  gtk_label_set_text (GTK_LABEL (stripchart_left_label (stripchart)),	
		      format_time (stripchart, &stripchart_locked_start_time(stripchart)));
}

static void
update_now (FtkStripchartX * stripchart)
{
/*******************************

HOLD on
    start time constant				1
    start delay set to now - start time		1
    stop  time constant				 2
    stop delay set to now - stop time		 2
    interval constant

HOLD off LOCK on
    start time constant				1
    start delay set to now - start time		1
    stop  time set to now - stop delay		  3
    stop delay constant				  3
    interval set to start time - stop time

HOLD off LOCK off
    start time set to now - start delay		   4
    start delay constant			   4
    stop  time set to now - stop delay		  3
    stop delay constant				  3
    interval constant


 ******************************/
  struct timeval now;
  struct timeval tv;
  struct timeval uv;
  double dv, ev;

  gboolean is_locked =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_lock_button(stripchart)));
    
  gboolean is_held =
    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_hold_button(stripchart)));
      
  gettimeofday (&now, NULL);

  if (is_locked || is_held) {
    timersub (&now, &stripchart_locked_start_time(stripchart), &tv);
    dv = timeval_to_double (&tv);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)), -dv);
  }
    
  if (is_held) {
    timersub (&now, &stripchart_locked_end_time(stripchart), &tv);
    dv = timeval_to_double (&tv);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)), -dv);
  }
  else {
    dv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));
    uv = double_to_timeval (-dv);
    timersub (&now, &uv, &tv);
    stripchart_locked_end_time(stripchart) = tv;
#if 0
    fprintf (stderr, "setting right from catcher to %s\n", 
	     format_time (stripchart, &stripchart_locked_end_time(stripchart)));
#endif
    gtk_label_set_text (GTK_LABEL (stripchart_right_label (stripchart)),	
			format_time (stripchart, &stripchart_locked_end_time(stripchart)));
    if (is_locked) {
      dv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));
      ev = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)));
      gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)),
				 dv - ev);
    }	
    else {
      dv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)));
      uv = double_to_timeval (-dv);
      timersub (&now, &uv, &tv);
      stripchart_locked_start_time(stripchart) = tv;
#if 0
      fprintf (stderr, "setting left from catcher to %s\n", 
	       format_time (stripchart, &stripchart_locked_start_time(stripchart)));
#endif
      gtk_label_set_text (GTK_LABEL (stripchart_left_label (stripchart)),	
			  format_time (stripchart, &stripchart_locked_start_time(stripchart)));
    }
  }
}

static void
ftk_update_button_start (FtkStripchartX * stripchart)
{
  stripchart_locked_start_time (stripchart) = stripchart_event_tv(stripchart, 0);
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)),
			     get_spin_val(&stripchart_locked_start_time (stripchart)));
  gtk_label_set_text (GTK_LABEL (stripchart_left_label (stripchart)),	
		      format_time (stripchart, &stripchart_locked_start_time(stripchart)));
}

static void
update_button_end (FtkStripchartX * stripchart)
{
  stripchart_locked_end_time (stripchart)
    = stripchart_event_tv(stripchart, stripchart_event_next (stripchart) - 1);
  gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)),
			     get_spin_val(&stripchart_locked_end_time (stripchart)));
  gtk_label_set_text (GTK_LABEL (stripchart_right_label (stripchart)),	
		      format_time (stripchart, &stripchart_locked_end_time(stripchart)));
}

static void
update_start_lock (FtkStripchartX * stripchart)
{
  if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_lock_button (stripchart)))) {
    stripchart_locked_start_time (stripchart) =
      get_lock_time (stripchart_left_spin_box (stripchart));
  }
}

static void
update_button_hold (FtkStripchartX * stripchart)
{
  if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (stripchart_hold_button (stripchart)))) {
    stripchart_locked_start_time (stripchart) =
      get_lock_time (stripchart_left_spin_box (stripchart));
    stripchart_locked_end_time (stripchart) =
      get_lock_time (stripchart_right_spin_box (stripchart));
  }
}

static void
right_slider_updates (FtkStripchartX * stripchart, double dp)
{

  /* data range */
  double d_ending_d;
  double d_range_d;
  struct timeval d_ending;
  struct timeval d_beginning	= stripchart_event_tv(stripchart, 0);
  double         d_beginning_d	= timeval_to_double (&d_beginning);
  
  gettimeofday (&d_ending, NULL);
  d_ending_d	= timeval_to_double (&d_ending);
  d_range_d	= d_ending_d - d_beginning_d;

  if (d_range_d > 0) {
    double d_right_d;
    struct timeval d_right;

    d_right_d = d_beginning_d + (dp * d_range_d);
    d_right   = double_to_timeval (d_right_d);

    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)),
			       d_right_d - d_ending_d);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)),
			       (d_right_d - d_ending_d) -
	gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart))) );
    stripchart_locked_end_time (stripchart) = d_right;
    gtk_label_set_text (GTK_LABEL (stripchart_right_label (stripchart)),	
			format_time (stripchart, &stripchart_locked_end_time(stripchart)));
  }
}

static void
left_slider_updates (FtkStripchartX * stripchart, double dp)
{

  /* data range */
  double d_ending_d;
  double d_range_d;
  struct timeval d_ending;
  struct timeval d_beginning	= stripchart_event_tv(stripchart, 0);
  double         d_beginning_d	= timeval_to_double (&d_beginning);
  
  gettimeofday (&d_ending, NULL);
  d_ending_d	= timeval_to_double (&d_ending);
  d_range_d	= d_ending_d - d_beginning_d;
  
  if (d_range_d > 0) {
    double csb;
    double d_left_d;
    struct timeval d_left;

    d_left_d = d_beginning_d + (dp * d_range_d);
    d_left   = double_to_timeval (d_left_d);

    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)),
			       d_left_d - d_ending_d);
    csb = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)))
      - (d_left_d - d_ending_d);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)), csb);
    stripchart_locked_start_time (stripchart) = d_left;
    gtk_label_set_text (GTK_LABEL (stripchart_left_label (stripchart)),	
			format_time (stripchart, &stripchart_locked_start_time(stripchart)));
  }
}

static void
update_left_slider (FtkStripchartX * stripchart, int pos)
{
  double dp;
  GtkWidget * da = stripchart_slider_da (stripchart);
  
  dp = slider_value(pos);
  if ((dp >= 0.0) && (dp <= 1.0)) {
    if (dp >= stripchart_slider_adj_right (stripchart))
      dp = stripchart_slider_adj_right (stripchart);
    left_slider_updates (stripchart, dp);

    stripchart_slider_adj_left (stripchart) = dp;
    gtk_adjustment_set_value (GTK_ADJUSTMENT(stripchart_slider_adjustment(stripchart)),
			      (stripchart_slider_adj_right(stripchart) +
			       stripchart_slider_adj_left (stripchart))/2.0);
    ftk_paint_slider (stripchart);			       
  }
}

static void
update_center_slider (FtkStripchartX * stripchart, int pos)
{
  double dp;
  double hp;
  GtkWidget * da = stripchart_slider_da (stripchart);

  dp = slider_value(pos);
  hp = (stripchart_slider_adj_right (stripchart) - stripchart_slider_adj_left (stripchart))/2.0;
  if ((dp - hp) < 0.0) dp = hp;
  if ((dp + hp) > 1.0) dp = 1.0 - hp;
  if (((dp - hp) >= 0.0) && ((dp + hp) <= 1.0)) {
    stripchart_slider_adj_right (stripchart) = dp + hp;
    stripchart_slider_adj_left (stripchart) = dp - hp;
    right_slider_updates (stripchart, dp + hp);
    left_slider_updates (stripchart, dp - hp);
    gtk_adjustment_set_value (GTK_ADJUSTMENT(stripchart_slider_adjustment(stripchart)), dp);
    ftk_paint_slider (stripchart);
  }
}

static void
update_right_slider (FtkStripchartX * stripchart, int pos)
{
  double dp;
  GtkWidget * da = stripchart_slider_da (stripchart);
  
  dp = slider_value(pos);
  if ((dp >= 0.0) && (dp <= 1.0)) {
    if (dp <= stripchart_slider_adj_left (stripchart))
      dp = stripchart_slider_adj_left (stripchart);
    right_slider_updates (stripchart, dp);

    stripchart_slider_adj_right (stripchart) = dp;
    gtk_adjustment_set_value (GTK_ADJUSTMENT(stripchart_slider_adjustment(stripchart)),
			      (stripchart_slider_adj_right(stripchart) +
			       stripchart_slider_adj_left (stripchart))/2.0);
    ftk_paint_slider (stripchart);
  }
}

static void
update_slider (FtkStripchartX * stripchart)
{
  /* beginning and ending visible times */

  /* fixme -- elsewhere */

  if (stripchart_event_next (stripchart) > 0) {
    struct timeval d_ending;
    double d_ending_d;

    /* visible range */
    struct timeval v_beginning	= stripchart_locked_start_time(stripchart);
    double         v_beginning_d	= timeval_to_double (&v_beginning);
    struct timeval v_ending	= stripchart_locked_end_time(stripchart);
    double         v_ending_d	= timeval_to_double (&v_ending);
  
    /* data range */
    struct timeval d_beginning	= stripchart_event_tv(stripchart, 0);
    double         d_beginning_d	= timeval_to_double (&d_beginning);

    gettimeofday (&d_ending, NULL);
    d_ending_d	= timeval_to_double (&d_ending);
    
    double d_range_d		= d_ending_d - d_beginning_d;

    if (d_range_d > 0.) {
      double f_beginning_d	= (v_beginning_d - d_beginning_d)/d_range_d;
      double f_ending_d		= (v_ending_d    - d_beginning_d)/d_range_d;

      if (f_beginning_d < 0.0)		f_beginning_d = 0.0;
      else if (f_beginning_d > 1.0)	f_beginning_d = 1.0;
      if (f_ending_d < 0.0)		f_ending_d = 0.0;
      else if (f_ending_d > 1.0)	f_ending_d = 1.0;

#if 0
      fprintf (stderr, "fb = %g, fe = %g\n", f_beginning_d, f_ending_d);
#endif
      stripchart_slider_adj_left (stripchart)  = f_beginning_d;
      stripchart_slider_adj_right (stripchart) = f_ending_d;
      ftk_paint_slider (stripchart);
    }
  }
}

static void
block_spin_signals(FtkStripchartX * stripchart)
{
  g_signal_handler_block (stripchart_left_spin_box(stripchart),
			  stripchart_left_spin_id(stripchart));
  g_signal_handler_block (stripchart_center_spin_box(stripchart),
			  stripchart_center_spin_id(stripchart));
  g_signal_handler_block (stripchart_right_spin_box(stripchart),
			  stripchart_right_spin_id(stripchart));
}

static void
unblock_spin_signals(FtkStripchartX * stripchart)
{
  g_signal_handler_unblock (stripchart_left_spin_box(stripchart),
			  stripchart_left_spin_id(stripchart));
  g_signal_handler_unblock (stripchart_center_spin_box(stripchart),
			  stripchart_center_spin_id(stripchart));
  g_signal_handler_unblock (stripchart_right_spin_box(stripchart),
			  stripchart_right_spin_id(stripchart));
}

static void
ftk_update_controls (FtkStripchartX * stripchart, ftk_updated_e cause, int userdata)
{
  /***********
    things to update:
          left slider pos
          right slider pos
          start spin val
          stop spin val
          range spin val
          start disp and start hold time
          stop disp and start hold time
  **********************************/

  gboolean update_slider_pos = FALSE;
  
  block_spin_signals(stripchart);

  switch(cause) {
  case FTK_UPDATED_START_LIMIT:
    ftk_update_button_start (stripchart);
    break;
  case FTK_UPDATED_START_SPIN:
    update_start_spin (stripchart);
    break;
  case FTK_UPDATED_RANGE_SPIN:
    update_range_spin (stripchart);
    break;
  case FTK_UPDATED_END_SPIN:
    update_end_spin (stripchart);
    break;
  case FTK_UPDATED_END_LIMIT:
    update_button_end (stripchart);
    break;
  case FTK_UPDATED_START_LOCK:
    update_start_lock (stripchart);
    break;
  case FTK_UPDATED_HOLD:
    update_button_hold (stripchart);
    break;
  case FTK_UPDATED_LEFT_SLIDER:
    update_left_slider (stripchart, userdata);
    update_slider_pos = TRUE;
    break;
  case FTK_UPDATED_CENTER_SLIDER:
    update_center_slider (stripchart, userdata);
    update_slider_pos = TRUE;
    break;
  case FTK_UPDATED_RIGHT_SLIDER:
    update_right_slider (stripchart, userdata);
    update_slider_pos = TRUE;
    break;
  case FTK_UPDATED_NOW:
    update_now (stripchart);
    break;
  case FTK_UPDATED_NONE:
    break;
  }

  if (!update_slider_pos) update_slider (stripchart);

  unblock_spin_signals(stripchart);
}

static gboolean
timer_catcher (gpointer sc)
{
  FtkStripchartX * stripchart;

  stripchart = FTK_STRIPCHARTX (sc);

  if (TRUE == stripchart_kill_timer_pending(stripchart)) {
    stripchart_timer_set(stripchart) = FALSE;
    stripchart_kill_timer_pending(stripchart) = FALSE;
    return FALSE;       /* kills the timer */
  }

  if (TRUE == stripchart_current_modified(stripchart)) {

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

  g_signal_emit (stripchart,
                 ftk_stripchart_signals[FTK_STRIPCHART_SIGNAL],
                 0);

  ftk_update_controls (stripchart, FTK_UPDATED_NOW, 0);

  return TRUE;
}

static gboolean
timer_init(FtkStripchartX * stripchart, gint ms, GError ** err)
{
  gboolean rc;
  /* least valid update interval (to keep from beating the system to death) */
#define MINIMUM_UPDATE 1000


  /* if a timer is running and either a 0ms or a valid ms is specified,
     kill the timer */
  /* (0ms implies stop the auto-updates.) */

  if ((TRUE == stripchart_timer_set(stripchart)) &&
      ((ms == 0) || (ms >= MINIMUM_UPDATE))) {
    stripchart_kill_timer_pending(stripchart) = TRUE;
  }

  if (ms >= MINIMUM_UPDATE) {
    stripchart_timer_set(stripchart) = TRUE;
    stripchart_bin_width_secs(stripchart)  = ms/1000;
    stripchart_bin_width_usecs(stripchart) = (ms%1000) * 1000;

    g_timeout_add ((guint)ms, timer_catcher, stripchart);

    init_current_bin (stripchart);

    rc = TRUE;
  }
  else {
    g_set_error (err,
                 ftk_quark,                             /* error domain */
                 FTK_ERROR_INVALID_UPDATE_INTERVAL,     /* error code */
                 "Invalid update interval: %d ns.",
                 ms);
    rc = FALSE; /* oob update specified */
  }

  return rc;
}

static void
ftk_paint_slider (FtkStripchartX * stripchart)
{
  if (GTK_WIDGET_DRAWABLE (GTK_WIDGET (stripchart))) {
    if (stripchart_range_set (stripchart)) {
      GtkWidget * da = stripchart_slider_da (stripchart);
      gint off_x_left = get_slider_left;
      gint off_x_right = get_slider_right;
      
      gtk_paint_flat_box (da->style,	/* GtkStyle 	 * style	*/
			da->window,	/* GdkWindow 	 * window	*/
			da->state,	/* GtkStateType    state_type	*/
			GTK_SHADOW_NONE,/* GtkShadowType   shadow_type	*/
			NULL,		/* GdkRectangle	 * area		*/
			da,		/* GtkWidget 	 * widget	*/
			NULL,		/* const gchar   * detail	*/
			0,				/* gint x	*/
			0,				/* gint y	*/
			da->allocation.width,		/* gint width	*/
			SLIDER_HEIGHT);			/* gint height	*/
      
      
      gtk_paint_slider (da->style,	/* GtkStyle 	 * style	*/
			da->window,	/* GdkWindow 	 * window	*/
			da->state,	/* GtkStateType    state_type	*/
			GTK_SHADOW_IN,	/* GtkShadowType   shadow_type	*/
			NULL,		/* GdkRectangle	 * area		*/
			da,		/* GtkWidget 	 * widget	*/
			"hscale",	/* const gchar   * detail	*/
			off_x_left+SLIDER_HEIGHT/4,	/* gint x	*/
			1,				/* gint y	*/
			(off_x_right-off_x_left)+2,	/* gint width	*/
			SLIDER_HEIGHT-2,		/* gint height	*/
			GTK_ORIENTATION_HORIZONTAL);	/* GtkOrientation orientation */
      
      gtk_paint_arrow (da->style,	/* GtkStyle 	* style		*/
		       da->window,	/* GdkWindow    * window	*/
		       da->state,	/* GtkStateType   state_type	*/
		       GTK_SHADOW_IN,	/* GtkShadowType  shadow_type	*/
		       NULL,		/* GdkRectangle * area		*/
		       da,		/* GtkWidget 	* widget	*/
		       NULL,		/* const gchar 	* detail	*/
		       GTK_ARROW_UP,	/* GtkArrowType	  arrow_type	*/
		       TRUE,		/* gboolean	  fill		*/
		       off_x_left,		/* gint x		*/
		       -2,			/* gint y		*/
		       3*SLIDER_HEIGHT/4 - 2,	/* gint w		*/
		       4*SLIDER_HEIGHT/3);	/* gint h		*/
      
      gtk_paint_arrow (da->style,	/* GtkStyle 	* style		*/
		       da->window,	/* GdkWindow    * window	*/
		       da->state,	/* GtkStateType   state_type	*/
		       GTK_SHADOW_IN,	/* GtkShadowType  shadow_type	*/
		       NULL,		/* GdkRectangle * area		*/
		       da,		/* GtkWidget 	* widget	*/
		       NULL,		/* const gchar 	* detail	*/
		       GTK_ARROW_UP,	/* GtkArrowType	  arrow_type	*/
		       TRUE,		/* gboolean	  fill		*/
		       off_x_right,		/* gint x		*/
		       -2,			/* gint y		*/
		       3*SLIDER_HEIGHT/4 - 2,	/* gint w		*/
		       4*SLIDER_HEIGHT/3 );	/* gint h		*/
    }
  }
}

static void
decorate_button (GtkWidget * widget, gchar ** data)
{
  GdkBitmap * mask;
  GtkButton * button = GTK_BUTTON (widget);
  GtkStyle  * style  = gtk_widget_get_default_style();
  GdkPixmap *  pixmap =
    gdk_pixmap_create_from_xpm_d (widget->parent->window,
				  &mask,	/* GdkBitmap **mask */
				  &style->bg[GTK_STATE_NORMAL],
				  data);	/* gchar **data */
  GtkWidget * image = gtk_image_new_from_pixmap (pixmap, NULL);
  
  gtk_button_set_image (button, image);
}

static void
draw_chart (GtkWidget * widget, gpointer data)
{
  int draw_width;
  int draw_height;
  double d_range;
  double d_bin_width;
  int bin_width;
  int max_count;
  int base_y;
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (widget);
  double d_beginning = timeval_to_double (&stripchart_locked_start_time(stripchart));
  struct timeval beginning = double_to_timeval (d_beginning);
  double d_ending = timeval_to_double (&stripchart_locked_end_time(stripchart));
  
  GtkWidget * da = GTK_WIDGET (stripchart_drawingarea(stripchart));

  draw_width  = da->allocation.width;
  draw_height = da->allocation.height;

  if (TRUE == stripchart_bg_color_modified(stripchart)) {
    gdk_gc_set_rgb_fg_color(stripchart_bg_gc (stripchart),
			    &stripchart_bg_color(stripchart));
    stripchart_bg_color_modified(stripchart) = FALSE;
  }

  if (TRUE == stripchart_readout_color_modified(stripchart)) {
    gdk_gc_set_rgb_fg_color(stripchart_readout_gc (stripchart),
			    &stripchart_readout_color(stripchart));
    stripchart_readout_color_modified(stripchart) = FALSE;
  }


  d_range = timeval_to_double (&stripchart_range(stripchart));
  d_bin_width = timeval_to_double (&stripchart_bin_width(stripchart));
  bin_width = lrint (((double)(draw_width - RIGHT_MARGIN)) * d_bin_width/d_range);

  /* fill bg */
  gdk_draw_rectangle (stripchart_pixmap (stripchart),
		      stripchart_bg_gc (stripchart),
		      TRUE,
		      0, 0,
		      draw_width, draw_height);




  /* print legend in associated colors */
  {
    int title_offset = TITLE_BASE_X_OFFSET;
    int i;
    
    for (i = 0; i < stripchart_event_spec_next(stripchart); i++) {
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
  }

  /* get max nr events for scaling */
  {
    int i;
    max_count = 0;
    for (i = stripchart_event_next (stripchart) - 1;  i >= 0;  i--) {
      if (timercmp (&stripchart_event_tv(stripchart, i), &beginning, <)) break;
      if (max_count < stripchart_event_total(stripchart, i))
	max_count = stripchart_event_total(stripchart, i);
    }
  }

  /* draw histogram */
  {
    int i;

    for (i = stripchart_event_next (stripchart) - 1;  i >= 0;  i--) {
      if (timercmp (&stripchart_event_tv(stripchart, i), &beginning, <)) break;

      if (0 < stripchart_event_total(stripchart, i)) {
	int j;
	double d_this, offset;
	gint dx, dy;
      
	d_this = timeval_to_double (&stripchart_event_tv (stripchart, i));

	offset = ((d_this - d_bin_width/2.0) - d_beginning)/(d_ending - d_beginning);
	dx = lrint (offset * (double)(draw_width - RIGHT_MARGIN));
	
	base_y = draw_height - BOTTOM_MARGIN;
	for (j = 0; j < stripchart_event_count_rho(stripchart,i); j++) {
	  if (0 < stripchart_event_count(stripchart, i, j)) {
	    dy = lrint (((double)((draw_height - (BOTTOM_MARGIN + TOP_MARGIN)) *
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
  }

  
  /* draw ticmarks */
  {
    int i;
    int base_y = draw_height - BOTTOM_MARGIN;

    
    for (i = 0; i <= max_count; i++) {
      int dy = lrint (((double)((draw_height -
				 (BOTTOM_MARGIN + TOP_MARGIN)) * i))
		      / ((double)max_count));
      gdk_draw_line (stripchart_pixmap (stripchart),
		     stripchart_readout_gc(stripchart),
		     draw_width - TIC_LENGTH,
		     base_y - dy,
		     draw_width,
		     base_y - dy);
    }
  }
  


  


  if ((GDK_IS_PIXMAP (stripchart_pixmap (stripchart))) &&
      GTK_WIDGET_MAPPED(widget)) 
    gdk_draw_drawable(da->window,
		      da->style->bg_gc[GTK_WIDGET_STATE (widget)],
		      stripchart_pixmap (stripchart),
		      0, 0,				/* src offsets */
		      0, 0,				/* dst offsets */
		      draw_width,
		      draw_height);

   gdk_display_flush (gtk_widget_get_display (widget));
}

static gboolean
ftk_stripchart_expose( GtkWidget * widget, GdkEventExpose * event,
		       gpointer data)
{

  g_return_val_if_fail (FTK_IS_STRIPCHARTX (widget), FALSE);
  g_return_val_if_fail (event != NULL, FALSE);
  

  if (GTK_WIDGET_DRAWABLE (widget)) {
    FtkStripchartX * stripchart = FTK_STRIPCHARTX (widget);

    draw_chart (widget, NULL);
    
    {
      struct timeval now;
      struct timeval delta;
      struct timeval left_edge;
      struct timeval right_edge;
      double lv, rv;
#if 0
      char * ns;
      char * ls;
      char * rs;
#endif
      
      gettimeofday (&now, NULL);
      
      lv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)));
      delta = double_to_timeval (-lv);
      timersub (&now, &delta, &left_edge);
      
      rv = gtk_spin_button_get_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)));
      delta = double_to_timeval (-rv);
      timersub (&now, &delta, &right_edge);

#if 0
      ns = strdupa (format_time (stripchart, &now));
      ls = strdupa (format_time (stripchart, &left_edge));
      rs = strdupa (format_time (stripchart, &right_edge));
	
      fprintf (stderr, "[%g, %g]\n\t%s\n\t%s\n\t%s\n",
	       lv, rv, ns, ls, rs);
#endif
    }

    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_left_label (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_right_label (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_left_limit_button (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_lock_button (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_hold_button (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_right_limit_button (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_slider_frame (stripchart)),
				    event);
    gtk_container_propagate_expose (GTK_CONTAINER (stripchart),
				    GTK_WIDGET (stripchart_da_frame (stripchart)),
				    event);

    ftk_paint_slider (stripchart);
  }

  return TRUE;
}

#if 0
static gboolean
ftk_stripchart_configure( GtkWidget * widget, GdkEventConfigure * event,
			  gpointer data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (widget);
  GtkDrawingArea * da = stripchart_drawingarea(stripchart);
}
#endif

static gboolean
ftk_da_configure( GtkWidget * widget, GdkEventConfigure * event,
			  gpointer data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (data);
  if (stripchart_pixmap (stripchart))
    gdk_pixmap_unref(stripchart_pixmap (stripchart));
  stripchart_pixmap (stripchart) = gdk_pixmap_new(widget->parent->window,
						  (gint)(event->width),
						  (gint)(event->height),
						  -1);

  return TRUE;
}
     
static gboolean
ftk_stripchart_realize( GtkWidget * widget, gpointer data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (widget);
  GtkDrawingArea * da = stripchart_drawingarea(stripchart);
  
  gtk_drawing_area_size (GTK_DRAWING_AREA (da),
			 stripchart_da_frame (stripchart)->allocation.width - 4, 	/* gint width */
			 stripchart_da_frame (stripchart)->allocation.height - 4);	/* gint height */
  
  decorate_button (stripchart_left_limit_button(stripchart), ll_xpm);
  decorate_button (stripchart_right_limit_button(stripchart), rr_xpm);

  stripchart_pixmap (stripchart) = gdk_pixmap_new(widget->parent->window,
						  GTK_WIDGET (da)->allocation.width,
						  GTK_WIDGET (da)->allocation.height,
						  -1);
  {
    int i;
    for (i = 0; i <stripchart_event_spec_next(stripchart) ; i++) {
      if (!stripchart_event_spec_gc (stripchart, i)) {
	stripchart_event_spec_gc (stripchart, i) =
	  gdk_gc_new(stripchart_pixmap (stripchart));
      }
      gdk_gc_set_rgb_fg_color(stripchart_event_spec_gc (stripchart, i),
			      &stripchart_event_spec_color (stripchart, i));
    }
  }
  stripchart_bg_gc (stripchart) =
    gdk_gc_new(stripchart_pixmap (stripchart));
  gdk_gc_set_rgb_fg_color(stripchart_bg_gc (stripchart),
			  &stripchart_bg_color (stripchart));

  stripchart_readout_gc (stripchart) =
    gdk_gc_new(stripchart_pixmap (stripchart));
  gdk_gc_set_rgb_fg_color(stripchart_readout_gc (stripchart),
			  &stripchart_readout_color (stripchart));

  return TRUE;
}

/******************************************************

                  button callbacks

********************************************************/

static void
ftk_stripchart_button_clicked (GtkButton *widget, gpointer user_data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (GTK_WIDGET (widget)->parent);
  ftk_button_e button = (ftk_button_e)GPOINTER_TO_INT (user_data);
  //  fprintf (stderr, "button %d clicked\n", (int)button);

  switch((int)button) {
  case FTK_BUTTON_START:
    ftk_update_controls (stripchart, FTK_UPDATED_START_LIMIT, 0);
    break;
  case FTK_BUTTON_START_LOCK:
    ftk_update_controls (stripchart, FTK_UPDATED_START_LOCK, 0);
    break;
  case FTK_BUTTON_HOLD:
    ftk_update_controls (stripchart, FTK_UPDATED_HOLD, 0);
    break;
  case FTK_BUTTON_END:
    ftk_update_controls (stripchart, FTK_UPDATED_END_LIMIT, 0);
    break;
  }
}


static GtkWidget *
create_limit_button (GtkTable * table, gint col, ftk_button_e fb)
{
#if 1
  /* fxme -- this is a crummy way to get my icons drawn, but nothing else works */
  GtkWidget * button = gtk_button_new_with_label ("");
  //  GtkWidget * button = gtk_button_new_with_label ((FTK_BUTTON_START == fb) ? "|<" : ">|");
  //  GtkWidget * button = gtk_button_new();
  //  GtkRequisition requisition = {20, 20};
  //  gtk_widget_size_request (button, &requisition);
#else
  GtkWidget * button
    = gtk_button_new_with_label ((FTK_BUTTON_START == fb) ? "|<" : ">|");
#endif

  g_signal_connect (GTK_OBJECT(button),"clicked",
		      (GtkSignalFunc) ftk_stripchart_button_clicked,
		      GINT_TO_POINTER (fb));
  gtk_widget_show (button);
  
  gtk_table_attach (table, button,
		    col - 1, col,	/* left, right */	/* col  */
		    1, 2,		/* top,  botom */	/* top row */
		    0,			/* x options */
		    0,			/* y options */
		    0, 0);
  return button;
}

static GtkWidget *
create_lock_button (GtkTable * table, gint col, ftk_button_e fb)
{
  GtkWidget * button = gtk_toggle_button_new_with_label((FTK_BUTTON_START_LOCK == fb)
							? "Lock" : "Hold");
  gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (button), FALSE);
  gtk_widget_show (button);
  g_signal_connect (GTK_OBJECT(button),"toggled",
		      (GtkSignalFunc) ftk_stripchart_button_clicked,
		      GINT_TO_POINTER (fb));
  
  gtk_table_attach (table, button,
		    col - 1, col,	/* left, right */	/* col  */
		    1, 2,		/* top,  botom */	/* top row */
		    0,			/* x options */
		    0,			/* y options */
		    0, 0);
  return button;
}

static void
ftk_spinbutton_vc(GtkSpinButton *spinbutton,
		  gpointer       user_data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (GTK_WIDGET (spinbutton)->parent);

  switch (GPOINTER_TO_INT (user_data)) {
  case FTK_SPIN_LEFT:	ftk_update_controls (stripchart, FTK_UPDATED_START_SPIN, 0);	break;
  case FTK_SPIN_CENTER:	ftk_update_controls (stripchart, FTK_UPDATED_RANGE_SPIN, 0);	break;
  case FTK_SPIN_RIGHT:	ftk_update_controls (stripchart, FTK_UPDATED_END_SPIN, 0);	break;
  }
}



static GtkWidget *
create_spinner (gulong * handler_id_p, GtkTable * table, gint col, double val, ftk_spin_e sb)
{
  GtkObject * adjustment =
    gtk_adjustment_new (val,					/* gdouble value */
			(FTK_SPIN_CENTER == sb) ? 0.0 :-1e50,	/* gdouble lower */
			(FTK_SPIN_CENTER == sb) ? 1e50 : 0.0,	/* gdouble upper */
			1.0,					/* gdouble step_increment */
			1.0,					/* gdouble page_increment */
			5.0);					/* gdouble page_size      */

  GtkWidget * spinbox = gtk_spin_button_new (GTK_ADJUSTMENT (adjustment),
					     1.0,  /* gdouble climb_rate */
					     0);   /* guint digits */
  
  gtk_spin_button_set_numeric ( GTK_SPIN_BUTTON (spinbox), TRUE);
  gtk_spin_button_set_digits  ( GTK_SPIN_BUTTON (spinbox), 3);
  gtk_widget_show (spinbox);

  *handler_id_p =
    g_signal_connect (GTK_OBJECT(spinbox),"value-changed",
		      (GtkSignalFunc) ftk_spinbutton_vc,
		      GINT_TO_POINTER (sb));

  gtk_table_attach (table, spinbox,
		    col - 1, col,		/* left, right */
		    1, 2,			/* top,  botom */ /* top row */
		    0,				/* x options */
		    0,				/* y options */
		    0, 0);
  return spinbox;
}

static gint
slider_button_press_event( GtkWidget * widget, GdkEventButton * event, gpointer data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (data);
  GtkWidget * da = stripchart_slider_da (stripchart);
  int pos = (int)(event->x);
  
  if (1 == event->button) {
    switch((int)(event->type)) {
    case GDK_BUTTON_PRESS:
      if (abs (pos - get_slider_left) < SLIDER_HEIGHT/2)
	stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_LEFT;
      else if (abs (pos - get_slider_right) < SLIDER_HEIGHT/2)
	stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_RIGHT;
      else if (abs (pos - get_slider_value) < SLIDER_HEIGHT/2)
	stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_CENTER;
      else
	stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_NONE;
      break;
    case GDK_BUTTON_RELEASE:
      stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_NONE;
      break;
    }
    return TRUE;
  }
  else return FALSE;
}

static gint
slider_motion_notify_event( GtkWidget * widget, GdkEventMotion * event, gpointer data)
{
  FtkStripchartX * stripchart = FTK_STRIPCHARTX (data);
  int pos = (int)(event->x);

#if 0
  fprintf (stderr, "mne %d %d %d\n",
	   (int)(event->x), (int)get_slider_left, (int)get_slider_right);
#endif

  switch (stripchart_spin_button_grabbed (stripchart)) {
  case FTK_SPIN_LEFT:
    ftk_update_controls (stripchart, FTK_UPDATED_LEFT_SLIDER, pos);
    break;
  case FTK_SPIN_RIGHT:
    ftk_update_controls (stripchart, FTK_UPDATED_RIGHT_SLIDER, pos);
    break;
  case FTK_SPIN_CENTER:
    ftk_update_controls (stripchart, FTK_UPDATED_CENTER_SLIDER, pos);
    break;
  case FTK_SPIN_NONE:
    break;
  }
  
  return TRUE;
}


static void
ftk_stripchart_init (FtkStripchartX * stripchart)
{
  GtkTooltips * stripchart_tips = gtk_tooltips_new ();
  GtkTable * table = stripchart_table (stripchart);
  
  //  fprintf (stderr, "_init\n");
  
  gtk_table_set_homogeneous (table, FALSE);
  gtk_table_resize (table, 4, 9);	/* 4 rows, 9 columns */
  gtk_container_set_resize_mode (GTK_CONTAINER (table), GTK_RESIZE_QUEUE);
  
  g_signal_connect (GTK_OBJECT (stripchart), "expose_event",
                      (GtkSignalFunc) ftk_stripchart_expose, NULL);
#if 0
  g_signal_connect (GTK_OBJECT(stripchart),"configure_event",
                      (GtkSignalFunc) ftk_stripchart_configure, NULL);
#endif
  g_signal_connect (GTK_OBJECT(stripchart),"realize",
                      (GtkSignalFunc) ftk_stripchart_realize, NULL);
  g_signal_connect (GTK_OBJECT (stripchart), "ftkstripchartx",
                      (GtkSignalFunc) draw_chart, NULL);

  stripchart_timer_set(stripchart)    = FALSE;
  stripchart_range_set(stripchart)    = FALSE;	/* fixme -- parameterise */
  stripchart_range_secs(stripchart)   = 60;	/* fixme -- parameterise */
  stripchart_range_usecs(stripchart)  =  0;	/* fixme -- parameterise */
  stripchart_ostring (stripchart)     = malloc (64);	/* static string space for format_time */
  stripchart_spin_button_grabbed (stripchart) = FTK_SPIN_NONE;

  stripchart_pixmap (stripchart) = NULL;
  {
    int i;

    for (i = 0; i < stripchart_event_next(stripchart); i++) {
      stripchart_event_spec_gc (stripchart, i) =
	gdk_gc_new(stripchart_pixmap (stripchart));
      gdk_gc_set_rgb_fg_color(stripchart_event_spec_gc (stripchart, i),
			      &stripchart_event_spec_color (stripchart, i));
    }
  }

  stripchart_event_spec_next(stripchart)  = 0;
  stripchart_event_spec_max(stripchart)   = 0;
  stripchart_event_specs(stripchart) = NULL;

  stripchart_bg_red(stripchart)		= DEFAULT_BG_RED;
  stripchart_bg_green(stripchart)	= DEFAULT_BG_GREEN;
  stripchart_bg_blue(stripchart)	= DEFAULT_BG_BLUE;
  stripchart_bg_color_modified(stripchart) = TRUE;

#define STRIPCHART_EVENTS_INITIAL_INCR 64
  stripchart_event_max (stripchart)  = STRIPCHART_EVENTS_INITIAL_INCR;
  stripchart_event_next (stripchart) = 0;
  stripchart_events (stripchart)     =
    malloc (stripchart_event_max (stripchart) * sizeof(event_s));

  /* time readouts */
  
  {
    GtkWidget * label;
    struct timeval now;
    struct timeval start;
    
    gettimeofday (&now, NULL);
    timersub (&now, &stripchart_range(stripchart), &start);

    stripchart_left_label (stripchart) = label = gtk_label_new (format_time (stripchart, &start));
    gtk_label_set_justify (GTK_LABEL (label), GTK_JUSTIFY_LEFT);
    gtk_widget_show (label);
    gtk_table_attach (table, label,
		      0, 4,		/* left, right */ 
		      0, 1,		/* top,  botom */  /* top row */
		      GTK_EXPAND | GTK_FILL,	/* x options */
		      0,		/* y options */
		      0, 0);

    stripchart_right_label (stripchart) = label = gtk_label_new (format_time (stripchart, &now));
    gtk_label_set_justify (GTK_LABEL (label), GTK_JUSTIFY_RIGHT);
    gtk_widget_show (label);
    gtk_table_attach (table, label,
		      5, 9,		/* left, right */ 
		      0, 1,		/* top,  botom */  /* top row */
		      GTK_EXPAND | GTK_FILL,	/* x options */
		      0,		/* y options */
		      0, 0);
  }

  /**********************************************************************

   0      1    2      3    4       5    6      7    8      9
   | Lock | |< | nnnn |    | nnnn  |    | nnnn | >| | Hold |

   **********************************************************************/

  stripchart_lock_button (stripchart)
    = create_lock_button (table, 1, FTK_BUTTON_START_LOCK);
  stripchart_left_limit_button (stripchart)
    = create_limit_button (table, 2, FTK_BUTTON_START);
  stripchart_left_spin_box (stripchart)
    = create_spinner (&stripchart_left_spin_id(stripchart),
		      table, 3, INITIAL_DELTA, FTK_SPIN_LEFT);
  stripchart_center_spin_box (stripchart)
    = create_spinner (&stripchart_center_spin_id(stripchart),
		      table, 5, INITIAL_DELTA, FTK_SPIN_CENTER);
  stripchart_right_spin_box (stripchart)
    = create_spinner (&stripchart_right_spin_id(stripchart),
		      table, 7, 0.0, FTK_SPIN_RIGHT);
  stripchart_right_limit_button (stripchart)
    = create_limit_button (table, 8, FTK_BUTTON_END);
  stripchart_hold_button (stripchart)
    = create_lock_button (table, 9, FTK_BUTTON_HOLD);

  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_left_limit_button (stripchart),
			"Set trace start to earliest available value.",
			"private");
  
  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_lock_button (stripchart),
			"Lock the trace start at the present setting.",
			"private");

  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_left_spin_box (stripchart),
			"Delta before \"now\" of trace start (secs).",
			"private");

  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_center_spin_box (stripchart),
			"Range of trace (secs).",
			"private");

  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_right_spin_box (stripchart),
			"Delta before \"now\" of trace end (secs).",
			"private");
  
  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_hold_button (stripchart),
			"Lock the trace start and end at the present settings.",
			"private");

  gtk_tooltips_set_tip (GTK_TOOLTIPS (stripchart_tips),
			stripchart_right_limit_button (stripchart),
			"Set trace end to most recent available value.",
			"private");

  /* drawing area */

  {
    GtkWidget * frame = gtk_frame_new (NULL);
    GtkWidget * da = gtk_drawing_area_new();
    
    stripchart_drawingarea(stripchart) = GTK_DRAWING_AREA (da);
    
    stripchart_da_frame (stripchart) = frame;
    gtk_drawing_area_size (GTK_DRAWING_AREA (da),
			   DEFAULT_DA_WIDTH, 		/* gint width */
			   DEFAULT_DA_HEIGHT);		/* gint height */
    gtk_widget_show (da);
    
    gtk_container_add (GTK_CONTAINER(frame), da);
    gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_IN);
    gtk_widget_show (frame);

    gtk_table_attach (table, frame,
		      0, 9,		/* left, right */  /* span table */
		      2, 3,		/* top,  botom */  /* top row */
		      GTK_EXPAND | GTK_FILL,	/* x options */
		      GTK_EXPAND | GTK_FILL,	/* y options */
		      0, 0);
#if 1
  g_signal_connect (GTK_OBJECT(da),"configure_event",
                      (GtkSignalFunc) ftk_da_configure, stripchart);
#endif
  }


  /* slider */

  {
    GtkWidget * frame = gtk_frame_new (NULL);
    GtkWidget * da = gtk_drawing_area_new();
    
    stripchart_slider_frame (stripchart) = frame;
    stripchart_slider_da (stripchart) = da;
    gtk_drawing_area_size (GTK_DRAWING_AREA (da),
			   SLIDER_WIDTH, 		/* gint width */
			   SLIDER_HEIGHT);		/* gint height */
    gtk_widget_show (da);

    gtk_widget_set_events(GTK_WIDGET (da),
			  GDK_BUTTON1_MOTION_MASK  |
			  GDK_BUTTON_PRESS_MASK    |
			  GDK_BUTTON_RELEASE_MASK);
    
    g_signal_connect (GTK_OBJECT(da), "button_press_event",
			G_CALLBACK (slider_button_press_event), stripchart);
    
    g_signal_connect (GTK_OBJECT(da), "button_release_event",
			G_CALLBACK (slider_button_press_event), stripchart);

    g_signal_connect (GTK_OBJECT(da), "motion_notify_event",
			G_CALLBACK (slider_motion_notify_event), stripchart);
    
    gtk_container_add (GTK_CONTAINER(frame), da);
    gtk_frame_set_shadow_type (GTK_FRAME (frame), GTK_SHADOW_IN);
    gtk_widget_show (frame);

    gtk_table_attach (table, frame,
		      0, 9,	/* left, right */	/* center col */
		      3, 4,	/* top,  botom */	/* bottom row */
		      GTK_EXPAND | GTK_FILL,	/* x options */
		      0,	/* y options */
		      0, 0);
    
    stripchart_slider_adjustment(stripchart) =
      gtk_adjustment_new (0.5,		/* gdouble value 	  */
			  0.0,		/* min value	 	  */
			  1.0,		/* max value		  */
			  0.01,		/* gdouble step_increment */
			  0.1,		/* gdouble page_increment */
			  1.0);		/* gdouble page_size      */
  }
}

/********************** public api ***************/

GType
ftk_stripchartx_get_type ()
{
  static GType stripchart_type = 0;

  if (!stripchart_type) {
    static const GTypeInfo stripchart_info = {
      sizeof (FtkStripchartXClass),		/* class_size		*/
      NULL, 					/* base_init		*/
      NULL,					/* base_finalize	*/
      (GClassInitFunc) ftk_stripchart_class_init,	/* class_init	*/
      NULL, 					/* class_finalize	*/
      NULL, 					/* class_data		*/
      sizeof (FtkStripchartX),			/* instance size	*/
      0,					/* n_preallocs		*/
      (GInstanceInitFunc) ftk_stripchart_init,	/* instance_init	*/
    };

    stripchart_type = g_type_register_static (GTK_TYPE_TABLE,
					      "Gtk_Stripchart",
					      &stripchart_info, 0);
  }

  return stripchart_type;
}



GtkWidget*
ftk_stripchartx_new ()
{

  FtkStripchartX * stripchart = g_object_new (ftk_stripchartx_get_type (), NULL);
  
  //  fprintf (stderr, "_new\n");

  return GTK_WIDGET (stripchart);
}

/*
 *
 *	resize widget
 *
 */

gboolean
ftk_stripchartx_resize_e (FtkStripchartX * stripchart,
			 gint width, gint height,
			 GError ** err)
{
  GtkDrawingArea * da;

  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  da = stripchart_drawingarea(stripchart);
  
  if (!GTK_IS_DRAWING_AREA (da)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_DRAWING_AREA,	/* error code */
		 "Invalid drawing area.");
    return FALSE;
  }
  

  gtk_widget_set_size_request (GTK_WIDGET (da), width, height);
  gtk_widget_queue_resize (GTK_WIDGET (stripchart));
  
  return TRUE;
}

gboolean
ftk_stripchartx_resize (FtkStripchartX * stripchart,
		       gint width, gint height)
{
  return ftk_stripchartx_resize_e (stripchart,width, height, NULL);
}

/*
 *
 *	setting bg rgb
 *
 */

gboolean
ftk_stripchartx_set_bg_rgb_e (FtkStripchartX * stripchart,
			     gint red, gint green, gint blue,
			     GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkStripchartX color.");
    return FALSE;
  }

  // fprintf (stderr, "bg = %d %d %d\n", red, green, blue);
  stripchart_bg_red(stripchart)		= red;
  stripchart_bg_green(stripchart)	= green;
  stripchart_bg_blue(stripchart)	= blue;
  stripchart_bg_color_modified(stripchart) = TRUE;
  
  return TRUE;
}

gboolean
ftk_stripchartx_set_bg_rgb (FtkStripchartX * stripchart,
			   gint red, gint green, gint blue)
{
  return ftk_stripchartx_set_bg_rgb_e (stripchart,
				      red, green, blue,
				      NULL);
}


#if 1		/* not used in x version */
gboolean
ftk_stripchartx_set_readout_rgb_e (FtkStripchartX * stripchart,
				  gint red, gint green, gint blue,
				  GError ** err)
{
  return TRUE;		/* but don't do anything */
}

gboolean
ftk_stripchartx_set_readout_rgb (FtkStripchartX * stripchart,
				gint red, gint green, gint blue)
{
  return ftk_stripchartx_set_readout_rgb_e (stripchart,
					   red, green, blue,
					   NULL);
}
#else
/*
 *
 *	setting readout rgb
 *
 */

gboolean
ftk_stripchartx_set_readout_rgb_e (FtkStripchartX * stripchart,
				  gint red, gint green, gint blue,
				  GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkStripchartX color.");
    return FALSE;
  }

  // fprintf (stderr, "ro = %d %d %d\n", red, green, blue);
  stripchart_readout_red(stripchart)		= red;
  stripchart_readout_green(stripchart)		= green;
  stripchart_readout_blue(stripchart)		= blue;
  stripchart_readout_color_modified(stripchart) = TRUE;
  
  return TRUE;
}

gboolean
ftk_stripchartx_set_readout_rgb (FtkStripchartX * stripchart,
				gint red, gint green, gint blue)
{
  return ftk_stripchartx_set_readout_rgb_e (stripchart,
					   red, green, blue,
					   NULL);
}
#endif

/*
 *
 *	setting chart rgb
 *
 */

gboolean
ftk_stripchartx_set_chart_rgb_e (FtkStripchartX * stripchart,
				  gint red, gint green, gint blue,
				  GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  if ((red   < 0) || (red   > 65535) ||
      (green < 0) || (green > 65535) ||
      (blue  < 0) || (blue  > 65535)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_COLOR,		/* error code */
		 "Invalid FtkStripchartX color.");
    return FALSE;
  }

  // fprintf (stderr, "bg = %d %d %d\n", red, green, blue);
  stripchart_bg_red(stripchart)		= red;
  stripchart_bg_green(stripchart)	= green;
  stripchart_bg_blue(stripchart)	= blue;
  stripchart_bg_color_modified(stripchart) = TRUE;

  // fprintf (stderr, "ro = %d %d %d\n",
  //	   65535 - red, 65535 - green, 65535 - blue);
  stripchart_readout_red(stripchart)		= 65535 - red;
  stripchart_readout_green(stripchart)		= 65535 - green;
  stripchart_readout_blue(stripchart)		= 65535 - blue;
  stripchart_readout_color_modified(stripchart) = TRUE;
  
  return TRUE;
}

gboolean
ftk_stripchartx_set_chart_rgb (FtkStripchartX * stripchart,
			      gint red, gint green, gint blue)
{
  return ftk_stripchartx_set_chart_rgb_e (stripchart,
					 red, green, blue,
					 NULL);
}


gint
ftk_stripchartx_new_event_e (FtkStripchartX * stripchart,
					const char * title,
					gint red, gint green, gint blue,
					GError ** err)
{
  int active_idx = -1;
   
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

   /* fixme -- initialise these */
#define EVENT_INCR 8
  if (stripchart_event_spec_next(stripchart) >=
      stripchart_event_spec_max(stripchart)) {
    stripchart_event_spec_max(stripchart) += EVENT_INCR;
    stripchart_event_specs(stripchart) =
      realloc (stripchart_event_specs(stripchart),
	       stripchart_event_spec_max(stripchart)
	       * sizeof(event_spec_s));
  }
  if (stripchart_current (stripchart)) {
    int old_val = stripchart_current_count_rho(stripchart);
    stripchart_current_count_rho(stripchart) =
      stripchart_event_spec_max(stripchart);
    stripchart_current_count_vec(stripchart) =
      realloc (stripchart_current_count_vec(stripchart),
	       stripchart_current_count_rho(stripchart) * sizeof(int));
    bzero (&stripchart_current_count(stripchart, old_val),
	   (stripchart_current_count_rho(stripchart) - old_val)
	   * sizeof(int));
  }
  active_idx = stripchart_event_spec_next(stripchart)++;
  stripchart_event_spec_pixel(stripchart, active_idx) = 0;
  stripchart_event_spec_red(stripchart, active_idx)   = red;
  stripchart_event_spec_green(stripchart, active_idx) = green;
  stripchart_event_spec_blue(stripchart, active_idx)  = blue;

  if (stripchart_pixmap (stripchart)) {
    stripchart_event_spec_gc (stripchart, active_idx) =
      gdk_gc_new(stripchart_pixmap (stripchart));
    gdk_gc_set_rgb_fg_color(stripchart_event_spec_gc (stripchart, active_idx),
			    &stripchart_event_spec_color (stripchart, active_idx));
  }
  else stripchart_event_spec_gc (stripchart, active_idx) = NULL;
   
  stripchart_event_spec_title(stripchart, active_idx) =
    gtk_widget_create_pango_layout (GTK_WIDGET (stripchart), title);

  return active_idx;
}
 
gint
ftk_stripchartx_new_event    (FtkStripchartX * stripchart,
			     const char * title,
					 gint red, gint green, gint blue)
{
  return ftk_stripchartx_new_event_e (stripchart, title,
				     red, green, blue, NULL);
}


/*
 *
 *	setting update
 *
 */

gboolean
ftk_stripchartx_set_update_e (FtkStripchartX * stripchart,
			     gint milliseconds,
			     GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  return timer_init(stripchart, milliseconds, err);
}

gboolean
ftk_stripchartx_set_update (FtkStripchartX * stripchart, gint milliseconds)
{
  return ftk_stripchartx_set_update_e (stripchart, milliseconds, NULL);
}

/*
 *
 *	setting range
 *
 */

gboolean
ftk_stripchartx_set_range_e (FtkStripchartX * stripchart,
			    gint milliseconds,
			    GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  if (0 < milliseconds) {
    double itd = ((double)milliseconds)/1000.0;
    
    stripchart_range_secs(stripchart)  = milliseconds/1000;
    stripchart_range_usecs(stripchart) = (milliseconds%1000) * 1000;
    stripchart_range_set(stripchart)   = TRUE;

    block_spin_signals(stripchart);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_center_spin_box (stripchart)),
			       itd);

    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_right_spin_box (stripchart)),
			       0.0);
    gtk_spin_button_set_value (GTK_SPIN_BUTTON (stripchart_left_spin_box (stripchart)),
			       -itd);
    unblock_spin_signals(stripchart);
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
ftk_stripchartx_set_range (FtkStripchartX * stripchart, gint milliseconds)
{
  return ftk_stripchartx_set_range_e (stripchart, milliseconds, NULL);
}

/*
 *
 *	appending events
 *
 */

gboolean
ftk_stripchartx_append_event_e (FtkStripchartX * stripchart,
			       gint type,
			       GError ** err)
{
  if (!FTK_IS_STRIPCHARTX (stripchart)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_STRIPCHART_WIDGET,	/* error code */
		 "Invalid FtkStripchartX widget.");
    return FALSE;
  }

  if (type >= stripchart_current_count_rho(stripchart)) {
    g_set_error (err,
		 ftk_quark,			/* error domain */
		 FTK_ERROR_INVALID_TYPE,	/* error code */
		 "Invalid FtkStripchartXTypeEnum %d.",
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

  stripchart_current_total(stripchart)++;
  stripchart_current_count(stripchart, type)++;
  stripchart_current_modified(stripchart) = TRUE;

  return TRUE;
}

gboolean
ftk_stripchartx_append_event (FtkStripchartX * stripchart,
			     gint type)
{
  return ftk_stripchartx_append_event_e (stripchart,type, NULL);
}
