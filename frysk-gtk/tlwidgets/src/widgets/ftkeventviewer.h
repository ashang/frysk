#ifndef __FTK_STRIPCHARTX_H__
#define __FTK_STRIPCHARTX_H__

#include <glib.h>
#include <glib-object.h>

G_BEGIN_DECLS

#define FTK_EVENTVIEWER_TYPE            \
        (ftk_eventviewer_get_type ())

#define FTK_EVENTVIEWER(obj)		\
        (G_TYPE_CHECK_INSTANCE_CAST ((obj), \
         FTK_EVENTVIEWER_TYPE, FtkEventViewer))

#define FTK_EVENTVIEWER_CLASS(klass)	\
        (G_TYPE_CHECK_CLASS_CAST ((klass), \
         FTK_EVENTVIEWER_TYPE, FtkEventViewerClass))

#define FTK_IS_EVENTVIEWER(obj) 	\
        (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_EVENTVIEWER_TYPE))

#define FTK_IS_EVENTVIEWER_CLASS(klass)	\
        (G_TYPE_CHECK_CLASS_TYPE ((klass), FTK_EVENTVIEWER_TYPE))

typedef enum {
  FTK_GLYPH_OPEN_CIRCLE,
  FTK_GLYPH_FILLED_CIRCLE,
  FTK_GLYPH_OPEN_SQUARE,
  FTK_GLYPH_FILLED_SQUARE,
#if 0
  FTK_GLYPH_OPEN_UP_TRIANGLE,
  FTK_GLYPH_FILLED_UP_TRIANGLE,
  FTK_GLYPH_OPEN_DOWN_TRIANGLE,
  FTK_GLYPH_FILLED_DOWN_TRIANGLE,
#endif
  FTK_GLYPH_LAST
} FtkGlyph;

typedef struct {
  GdkGC       * gc;
  PangoLayout * label;
  FtkGlyph	glyph;
  GdkColor	color;
  gboolean 	color_modified;
  gint		label_height;
  gint		label_width;
  gboolean	label_modified;
} ftk_marker_s;

#define ftk_marker_gc(m)		(m)->gc
#define ftk_marker_label(m)		(m)->label
#define ftk_marker_glyph(m)		(m)->glyph
#define ftk_marker_color(m)		(m)->color
#define ftk_marker_color_red(m)		(m)->color.red
#define ftk_marker_color_green(m)	(m)->color.green
#define ftk_marker_color_blue(m)	(m)->color.blue
#define ftk_marker_color_modified(m)	(m)->color_modified
#define ftk_marker_label_height(m)	(m)->label_height
#define ftk_marker_label_width(m)	(m)->label_width
#define ftk_marker_label_modified(m)	(m)->label_modified

typedef struct _ftk_trace_s {
  GdkGC       * gc;
  gint		linewidth;
  GdkLineStyle	linestyle;
  gboolean	linestyle_modified;
  GdkColor	color;
  gboolean 	color_modified;
  gint		vpos;
  PangoLayout * label;
  gint		label_height;
  gint		label_width;
  gboolean	label_modified;
} ftk_trace_s;

#define ftk_trace_gc(t)			(t)->gc
#define ftk_trace_linestyle(t)		(t)->linestyle
#define ftk_trace_linewidth(t)		(t)->linewidth
#define ftk_trace_linestyle_modded(t)	(t)->linestyle_modified
#define ftk_trace_color(t)		(t)->color
#define ftk_trace_color_pixel(t)	(t)->color.pixel
#define ftk_trace_color_red(t)		(t)->color.red
#define ftk_trace_color_green(t)	(t)->color.green
#define ftk_trace_color_blue(t)		(t)->color.blue
#define ftk_trace_color_modified(t)	(t)->color_modified
#define ftk_trace_vpos(t)		(t)->vpos
#define ftk_trace_label(t)		(t)->label
#define ftk_trace_label_height(t)	(t)->label_height
#define ftk_trace_label_width(t)	(t)->label_width
#define ftk_trace_label_modified(t)	(t)->label_modified

typedef struct {
  gint		marker;
  int		trace;
  double	time;
} ftk_event_s;

#define ftk_event_marker(e)	(e)->marker
#define ftk_event_trace(e)	(e)->trace
#define ftk_event_time(e)	(e)->time

typedef struct _FtkEventViewer {
  GtkVBox		  vbox;
  double		  zero_d;
  double		  now_d;
  double		  span_d;
  GtkWidget		* hbutton_box;
  GtkWidget		* scale_toggle_button;
  GtkWidget		* hold_toggle_button;
  GtkWidget		* interval_spin_button;
  GtkWidget		* da_frame;
  GtkWidget		* scroll;
  GtkAdjustment		* scroll_adj;
  GtkDrawingArea	* da;
  GdkPixmap		* pixmap;
  GdkColor		  bg_color;
  GdkGC			* bg_gc;
  ftk_marker_s	        * markers;
  int	      		  markers_next;
  int	      		  markers_max;
  ftk_trace_s		* traces;
  int			  trace_next;
  int			  trace_max;
  ftk_event_s		* events;
  int			  event_next;
  int			  event_max;
  int			  label_box_width;
  int			  label_box_height;
  int			  trace_origin;
  int			  trace_width;
  gboolean		  bg_color_modified;
  gboolean		  trace_modified;
  gboolean     		  markers_modified;
  gboolean		  drawable;
} FtkEventViewer;

#define ftk_ev_vbox(v)		      &((v)->vbox)
#define ftk_ev_hbutton_box(v)		(v)->hbutton_box
#define ftk_ev_scale_toggle_button(v)	(v)->scale_toggle_button
#define ftk_ev_hold_toggle_button(v)	(v)->hold_toggle_button
#define ftk_ev_interval_button(v)	(v)->interval_spin_button
#define ftk_ev_da_frame(v)		(v)->da_frame
#define ftk_ev_da(v)			(v)->da
#define ftk_ev_pixmap(v)		(v)->pixmap
#define ftk_ev_scroll(v)		(v)->scroll
#define ftk_ev_scroll_adj(v)		(v)->scroll_adj
#define ftk_ev_zero(v)			(v)->zero_d
#define ftk_ev_now(v)			(v)->now_d
#define ftk_ev_span(v)			(v)->span_d
#define ftk_ev_drawable(v)		(v)->drawable
#define ftk_ev_bg_gc(v)			(v)->bg_gc
#define ftk_ev_bg_color(v)		(v)->bg_color
#define ftk_ev_bg_color_modified(v)	(v)->bg_color_modified
#define ftk_ev_bg_pixel(v)		(v)->bg_color.pixel
#define ftk_ev_bg_red(v)		(v)->bg_color.red
#define ftk_ev_bg_green(v)		(v)->bg_color.green
#define ftk_ev_bg_blue(v)		(v)->bg_color.blue
#define ftk_ev_traces(v)		(v)->traces
#define ftk_ev_trace(v,i)	      &((v)->traces[i])
#define ftk_ev_trace_next(v)		(v)->trace_next
#define ftk_ev_trace_max(v)		(v)->trace_max
#define ftk_ev_trace_modified(v)	(v)->trace_modified
#define ftk_ev_label_box_width(v)	(v)->label_box_width
#define ftk_ev_label_box_height(v)	(v)->label_box_height
#define ftk_ev_events(v)		(v)->events
#define ftk_ev_event(v,i)	      &((v)->events[i])
#define ftk_ev_event_next(v)		(v)->event_next
#define ftk_ev_event_max(v)		(v)->event_max
#define ftk_ev_trace_origin(v)		(v)->trace_origin
#define ftk_ev_trace_width(v)		(v)->trace_width
#define ftk_ev_markers(v)		(v)->markers
#define ftk_ev_marker(v,i)	      &((v)->markers[i])
#define ftk_ev_markers_next(v)		(v)->markers_next
#define ftk_ev_markers_max(v)		(v)->markers_max
#define ftk_ev_markers_modified(v)	(v)->markers_modified

typedef struct _FtkEventViewerClass {
  GtkVBoxClass parent_class;
  void (* ftkeventviewer) (FtkEventViewer * eventviewer);
} FtkEventViewerClass;

typedef enum {
  FTK_EV_ERROR_NONE,
  FTK_EV_ERROR_UNDRAWABLE,
  FTK_EV_ERROR_INVALID_WIDGET,
  FTK_EV_ERROR_INVALID_TRACE,
  FTK_EV_ERROR_INVALID_EVENT_TYPE,
  FTK_EV_ERROR_INVALID_COLOR,
  FTK_EV_ERROR_INVALID_LINESTYLE,
  FTK_EV_ERROR_INVALID_GLYPH,
} ftk_ev_error_e;

/*************** public api *****************/

GType
ftk_eventviewer_get_type		(void);

GtkWidget *
ftk_eventviewer_new		();

gboolean
ftk_eventviewer_set_bg_rgb_e	(FtkEventViewer * eventviewer,
				 gint red, gint green, gint blue,
				 GError ** err);
gboolean
ftk_eventviewer_set_bg_rgb	(FtkEventViewer * eventviewer,
				 gint red, gint green, gint blue);

gboolean
ftk_eventviewer_set_timebase_e	(FtkEventViewer * eventviewer,
				 double span,
				 GError ** err);
gboolean
ftk_eventviewer_set_timebase	(FtkEventViewer * eventviewer,
				 double span);

gint
ftk_eventviewer_add_trace_e	(FtkEventViewer * eventviewer,
				 char * label,
				 GError ** err);
gint
ftk_eventviewer_add_trace	(FtkEventViewer * eventviewer,
				 char * label);

gboolean
ftk_eventviewer_set_trace_rgb_e	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint red, gint green, gint blue,
				 GError ** err);
gboolean
ftk_eventviewer_set_trace_rgb	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint red, gint green, gint blue);

gboolean
ftk_eventviewer_set_trace_label_e	(FtkEventViewer * eventviewer,
					 gint trace,
					 char * label,
					 GError ** err);
gboolean
ftk_eventviewer_set_trace_label		(FtkEventViewer * eventviewer,
					 gint trace,
					 char * label);

gboolean
ftk_eventviewer_set_trace_linestyle_e	(FtkEventViewer * eventviewer,
					 gint trace,
					 gint lw,
					 GdkLineStyle ls,
					 GError ** err);
gboolean
ftk_eventviewer_set_trace_linestyle	(FtkEventViewer * eventviewer,
					 gint trace,
					 gint lw,
					 GdkLineStyle ls);

gboolean
ftk_eventviewer_append_event_e	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint marker,
				 GError ** err);
gboolean
ftk_eventviewer_append_event	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint marker);

gint
ftk_marker_new_e		(FtkEventViewer * eventviewer,
				 FtkGlyph glyph,
				 char * label,
				 GError ** err);
gint
ftk_marker_new 			(FtkEventViewer * eventviewer,
				 FtkGlyph glyph,
				 char * label);

gboolean
ftk_eventviewer_set_marker_rgb_e	(FtkEventViewer * eventviewer,
					 gint marker,
					 gint red, gint green, gint blue,
					 GError ** err);
gboolean
ftk_eventviewer_set_marker_rgb		(FtkEventViewer * eventviewer,
					 gint marker,
					 gint red, gint green, gint blue);
G_END_DECLS

#endif /* __FTK_EVENTVIEWER_H__ */
