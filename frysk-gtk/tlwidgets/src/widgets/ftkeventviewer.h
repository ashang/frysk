#ifndef __FTK_STRIPCHARTX_H__
#define __FTK_STRIPCHARTX_H__

#include <glib.h>
#include <glib-object.h>

#define USE_SLIDER_INTERVAL

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

#define ftk_tie_s ftk_trace_s

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

#define ftk_tie_gc(t)			(t)->gc
#define ftk_tie_linestyle(t)		(t)->linestyle
#define ftk_tie_linewidth(t)		(t)->linewidth
#define ftk_tie_linestyle_modded(t)	(t)->linestyle_modified
#define ftk_tie_color(t)		(t)->color
#define ftk_tie_color_pixel(t)		(t)->color.pixel
#define ftk_tie_color_red(t)		(t)->color.red
#define ftk_tie_color_green(t)		(t)->color.green
#define ftk_tie_color_blue(t)		(t)->color.blue
#define ftk_tie_color_modified(t)	(t)->color_modified
#define ftk_tie_vpos(t)			(t)->vpos
#define ftk_tie_label(t)		(t)->label
#define ftk_tie_label_height(t)		(t)->label_height
#define ftk_tie_label_width(t)		(t)->label_width
#define ftk_tie_label_modified(t)	(t)->label_modified

typedef struct {
  gint		marker;
  int		trace;
  int		loc;
  double	time;
} ftk_event_s;

#define ftk_event_marker(e)	(e)->marker
#define ftk_event_trace(e)	(e)->trace
#define ftk_event_loc(e)	(e)->loc
#define ftk_event_time(e)	(e)->time

typedef struct {
  double when;
  int tie_index;
  int * trace_list;
  int trace_list_next;
  int trace_list_max;
} ftk_link_s;

#define ftk_link_when(l)		(l)->when
#define ftk_link_tie_index(l)		(l)->tie_index
#define ftk_link_trace_list(l)		(l)->trace_list
#define ftk_link_trace(l,i)		(l)->trace_list[i]
#define ftk_link_trace_list_next(l)	(l)->trace_list_next
#define ftk_link_trace_list_max(l)	(l)->trace_list_max

typedef enum {
  FTK_POPUP_TYPE_NONE,
  FTK_POPUP_TYPE_LABEL,
  FTK_POPUP_TYPE_MARKER
} ftk_popup_type_e;

typedef struct _FtkEventViewer {
  GtkVBox		  vbox;
  double		  zero_d;
  double		  now_d;
  double		  span_d;
  GtkWidget		* popup_window;
  GtkWidget		* popup_label;
  GtkWidget		* hbutton_box;
  GtkWidget		* scale_toggle_button;
  GtkWidget		* hold_toggle_button;
#ifdef USE_SLIDER_INTERVAL
  GtkWidget		* interval_scale;
#endif
#ifndef USE_SLIDER_INTERVAL  
  GtkWidget		* interval_spin_button;
#endif
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
  ftk_tie_s		* ties;
  int			  tie_next;
  int			  tie_max;
  ftk_event_s		* events;
  int			  event_next;
  int			  event_max;
  ftk_link_s		* links;
  int			  link_next;
  int			  link_max;
  int			  label_box_width;
  int			  label_box_height;
  int			  trace_origin;
  int			  trace_width;
  int			  popup_trace;
  int			  popup_marker;
  ftk_popup_type_e	  popup_type;
  gboolean		  bg_color_modified;
  gboolean		  trace_modified;
  gboolean		  tie_modified;
  gboolean     		  markers_modified;
  gboolean     		  widget_modified;
  gboolean		  drawable;
} FtkEventViewer;

#define ftk_ev_vbox(v)		      &((v)->vbox)
#define ftk_ev_popup_window(v)		(v)->popup_window
#define ftk_ev_popup_label(v)		(v)->popup_label
#define ftk_ev_popup_type(v)		(v)->popup_type
#define ftk_ev_popup_trace(v)		(v)->popup_trace
#define ftk_ev_popup_marker(v)		(v)->popup_marker
#define ftk_ev_hbutton_box(v)		(v)->hbutton_box
#define ftk_ev_scale_toggle_button(v)	(v)->scale_toggle_button
#define ftk_ev_hold_toggle_button(v)	(v)->hold_toggle_button
#ifndef USE_SLIDER_INTERVAL  
#define ftk_ev_interval_button(v)	(v)->interval_spin_button
#endif
#ifdef USE_SLIDER_INTERVAL
#define ftk_ev_interval_scale(v)	(v)->interval_scale
#endif
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
#define ftk_ev_ties(v)			(v)->ties
#define ftk_ev_tie(v,i)	 	      &((v)->ties[i])
#define ftk_ev_tie_next(v)		(v)->tie_next
#define ftk_ev_tie_max(v)		(v)->tie_max
#define ftk_ev_tie_modified(v)		(v)->tie_modified
#define ftk_ev_label_box_width(v)	(v)->label_box_width
#define ftk_ev_label_box_height(v)	(v)->label_box_height
#define ftk_ev_events(v)		(v)->events
#define ftk_ev_event(v,i)	      &((v)->events[i])
#define ftk_ev_event_next(v)		(v)->event_next
#define ftk_ev_event_max(v)		(v)->event_max
#define ftk_ev_links(v)			(v)->links
#define ftk_ev_link(v,i)	      &((v)->links[i])
#define ftk_ev_link_next(v)		(v)->link_next
#define ftk_ev_link_max(v)		(v)->link_max
#define ftk_ev_trace_origin(v)		(v)->trace_origin
#define ftk_ev_trace_width(v)		(v)->trace_width
#define ftk_ev_markers(v)		(v)->markers
#define ftk_ev_marker(v,i)	      &((v)->markers[i])
#define ftk_ev_markers_next(v)		(v)->markers_next
#define ftk_ev_markers_max(v)		(v)->markers_max
#define ftk_ev_markers_modified(v)	(v)->markers_modified
#define ftk_ev_widget_modified(v)	(v)->widget_modified

typedef struct _FtkEventViewerClass {
  GtkVBoxClass parent_class;
  void (* ftkeventviewer) (FtkEventViewer * eventviewer);
} FtkEventViewerClass;

typedef enum {
  FTK_EV_ERROR_NONE,
  FTK_EV_ERROR_UNDRAWABLE,
  FTK_EV_ERROR_INVALID_WIDGET,
  FTK_EV_ERROR_INVALID_TRACE,
  FTK_EV_ERROR_INVALID_TIE,
  FTK_EV_ERROR_INVALID_EVENT_TYPE,
  FTK_EV_ERROR_INVALID_COLOR,
  FTK_EV_ERROR_INVALID_LINESTYLE,
  FTK_EV_ERROR_INVALID_GLYPH,
  FTK_EV_ERROR_INVALID_SPAN,
} ftk_ev_error_e;

/*************** public api *****************/

GType
ftk_eventviewer_get_type		(void);

GtkWidget *
ftk_eventviewer_new		();

gboolean
ftk_eventviewer_resize_e	(FtkEventViewer * eventviewer,
				 gint width, gint height,
				 GError ** err);

gboolean
ftk_eventviewer_resize		(FtkEventViewer * eventviewer,
				 gint width, gint height);

gboolean
ftk_eventviewer_set_bg_rgb_e	(FtkEventViewer * eventviewer,
				 guint red, guint green, guint blue,
				 GError ** err);
gboolean
ftk_eventviewer_set_bg_rgb	(FtkEventViewer * eventviewer,
				 guint red, guint green, guint blue);
gboolean
ftk_eventviewer_set_bg_color_e	(FtkEventViewer * eventviewer,
				 GdkColor color, GError ** err);
gboolean
ftk_eventviewer_set_bg_color	(FtkEventViewer * eventviewer,
				 GdkColor color);

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
				 guint red, guint green, guint blue,
				 GError ** err);
gboolean
ftk_eventviewer_set_trace_rgb	(FtkEventViewer * eventviewer,
				 gint trace,
				 guint red, guint green, guint blue);
gboolean
ftk_eventviewer_set_trace_color_e	(FtkEventViewer * eventviewer,
					 gint trace,
					 GdkColor color,
					 GError ** err);
gboolean
ftk_eventviewer_set_trace_color		(FtkEventViewer * eventviewer,
					 gint trace,
					 GdkColor color);

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
gboolean
ftk_eventviewer_append_simultaneous_events_e (FtkEventViewer * eventviewer,
					      gint tie_index,
					      GError ** err, ...);
gboolean
ftk_eventviewer_append_simultaneous_events (FtkEventViewer * eventviewer,
					    gint tie_index, ...); 

gint
ftk_eventviewer_marker_new_e		(FtkEventViewer * eventviewer,
					 FtkGlyph glyph,
					 char * label,
					 GError ** err);
gint
ftk_eventviewer_marker_new 		(FtkEventViewer * eventviewer,
					 FtkGlyph glyph,
					 char * label);

gboolean
ftk_eventviewer_set_marker_rgb_e	(FtkEventViewer * eventviewer,
					 gint marker,
					 guint red, guint green, guint blue,
					 GError ** err);
gboolean
ftk_eventviewer_set_marker_rgb		(FtkEventViewer * eventviewer,
					 gint marker,
					 guint red, guint green, guint blue);
gboolean
ftk_eventviewer_set_marker_color_e	(FtkEventViewer * eventviewer,
					 gint marker,
					 GdkColor color,
					 GError ** err);
gboolean
ftk_eventviewer_set_marker_color	(FtkEventViewer * eventviewer,
					 gint marker,
					 GdkColor color);




gint
ftk_eventviewer_tie_new_e	(FtkEventViewer * eventviewer,
				 char * label,
				 GError ** err);
gint
ftk_eventviewer_tie_new		(FtkEventViewer * eventviewer,
				 char * label);

gboolean
ftk_eventviewer_set_tie_rgb_e	(FtkEventViewer * eventviewer,
				 gint tie,
				 guint red, guint green, guint blue,
				 GError ** err);
gboolean
ftk_eventviewer_set_tie_rgb	(FtkEventViewer * eventviewer,
				 gint tie,
				 guint red, guint green, guint blue);
gboolean
ftk_eventviewer_set_tie_color_e	(FtkEventViewer * eventviewer,
					 gint tie,
					 GdkColor color,
					 GError ** err);
gboolean
ftk_eventviewer_set_tie_color		(FtkEventViewer * eventviewer,
					 gint tie,
					 GdkColor color);

gboolean
ftk_eventviewer_set_tie_label_e	(FtkEventViewer * eventviewer,
					 gint tie,
					 char * label,
					 GError ** err);
gboolean
ftk_eventviewer_set_tie_label		(FtkEventViewer * eventviewer,
					 gint tie,
					 char * label);

gboolean
ftk_eventviewer_set_tie_linestyle_e	(FtkEventViewer * eventviewer,
					 gint tie,
					 gint lw,
					 GdkLineStyle ls,
					 GError ** err);
gboolean
ftk_eventviewer_set_tie_linestyle	(FtkEventViewer * eventviewer,
					 gint tie,
					 gint lw,
					 GdkLineStyle ls);




G_END_DECLS

#endif /* __FTK_EVENTVIEWER_H__ */
