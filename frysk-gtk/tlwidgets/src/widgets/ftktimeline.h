#ifndef __FTK_TIMELINE_H__
#define __FTK_TIMELINE_H__

#include <sys/types.h>
#include <glib.h>
#include <glib-object.h>
#include <gtk/gtktable.h>

G_BEGIN_DECLS

#define FTK_TIMELINE_TYPE            (ftk_timeline_get_type ())
#define FTK_TIMELINE(obj)            (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_TIMELINE_TYPE, FtkTimeline))
#define FTK_TIMELINE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST ((klass), FTK_TIMELINE_TYPE, FtkTimelineClass))
#define IS_FTK_TIMELINE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_TIMELINE_TYPE))
#define IS_FTK_TIMELINE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), FTK_TIMELINE_TYPE))

typedef enum {
  FTK_TRACE_UNKNOWN,
  FTK_TRACE_INT,
  FTK_TRACE_DOUBLE,
  FTK_TRACE_PID
} FtkTraceType;

typedef struct {
  double x;
  double y;           /* fixme -- double array for multiple y-vals */
} point_s;

typedef struct {
  int point_max;
  int point_next;
  int point_incr;
  point_s * points;
#define POINT_INCR_INITIAL 16
  gboolean changed;
  double y_min;
  double y_max;
  double y_scale;
  double y_offset;
  GtkWidget * drawing_area;
  GdkPixmap * pixmap;
  GtkWidget * label;
  GtkWidget * readout_x;
  GtkWidget * readout_y;
  FtkTraceType type;
} trace_s;

#define timeline_trace_point_max(t, i)      (t)->traces[i].point_max
#define timeline_trace_point_next(t, i)     (t)->traces[i].point_next
#define timeline_trace_point_incr(t, i)     (t)->traces[i].point_incr
#define timeline_trace_changed(t, i)        (t)->traces[i].changed
#define timeline_trace_points(t, i)         (t)->traces[i].points
#define timeline_trace_y_min(t, i)    	    (t)->traces[i].y_min
#define timeline_trace_y_max(t, i)   	    (t)->traces[i].y_max
#define timeline_trace_y_scale(t, i) 	    (t)->traces[i].y_scale
#define timeline_trace_y_offset(t, i)	    (t)->traces[i].y_offset
#define timeline_trace_pixmap(t, i)	    (t)->traces[i].pixmap
#define timeline_trace_label(t, i)	    (t)->traces[i].label
#define timeline_trace_readout_x(t, i)	    (t)->traces[i].readout_x
#define timeline_trace_readout_y(t, i)	    (t)->traces[i].readout_y
#define timeline_trace_area(t, i)           (t)->traces[i].drawing_area
#define timeline_trace_points(t, i)         (t)->traces[i].points
#define timeline_trace_point_x(t, i, p)     (t)->traces[i].points[p].x
#define timeline_trace_point_y(t, i, p)     (t)->traces[i].points[p].y
#define timeline_trace_type(t, i)           (t)->traces[i].type

typedef struct _FtkTimeline {
  GtkTable table;
  gint ntraces;
  trace_s * traces;
  gboolean changed;
  gboolean initted;
  gint     width;
  double x_min;
  double x_max;
  double x_scale;
  double x_offset;
  GtkWidget * range_spin;
  GtkWidget * range_units;
  int         range_factor;
  double      range_val;
  GtkWidget * delay_spin;
  GtkWidget * delay_units;
  int         delay_factor;
  double      delay_val;
  GtkWidget * readout;
} FtkTimeline;

#define timeline_traces_changed(t)       (t)->changed
#define timeline_ntraces(t)          	 (t)->ntraces
#define timeline_initted(t)        	 (t)->initted
#define timeline_x_min(t)  	  	 (t)->x_min
#define timeline_x_max(t)   	 	 (t)->x_max
#define timeline_x_scale(t) 	 	 (t)->x_scale
#define timeline_x_offset(t)	 	 (t)->x_offset
#define timeline_range_spin(t)		 (t)->range_spin
#define timeline_range_units(t)		 (t)->range_units
#define timeline_range_factor(t)	 (t)->range_factor
#define timeline_range_val(t)	 	 (t)->range_val
#define timeline_delay_spin(t)		 (t)->delay_spin
#define timeline_delay_units(t)		 (t)->delay_units
#define timeline_delay_factor(t)	 (t)->delay_factor
#define timeline_delay_val(t)	 	 (t)->delay_val
#define timeline_width(t)	 	 (t)->width
#define timeline_readout(t)	 	 (t)->readout


typedef struct _FtkTimelineClass {
  GtkTableClass parent_class;

  void (* ftktimeline) (FtkTimeline * timeline);
} FtkTimelineClass;

typedef union {
  int    i;
  double d;
  pid_t  p;
} data_u;


/*************** public api *****************/

GType          ftk_timeline_get_type		(void);
GtkWidget *    ftk_timeline_new			(gint rows);
gboolean       ftk_timeline_append_point_int	(FtkTimeline * timeline, gint trace, double x, int y);
gboolean       ftk_timeline_append_point_dbl	(FtkTimeline * timeline, gint trace, double x, double y);
gboolean       ftk_timeline_append_point_pid	(FtkTimeline * timeline, gint trace, double x, pid_t y);
gboolean       ftk_timeline_set_trace_label	(FtkTimeline * timeline, gint trace, const gchar * label);
gboolean       ftk_timeline_set_trace_type	(FtkTimeline * timeline, gint trace, FtkTraceType type);

G_END_DECLS

#endif /* __FTK_TIMELINE_H__ */
