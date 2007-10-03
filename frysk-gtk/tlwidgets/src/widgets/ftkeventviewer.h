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

#ifndef __FTK_STRIPCHARTX_H__
#define __FTK_STRIPCHARTX_H__

#include <glib.h>
#include <glib-object.h>

#define USE_SLIDER_INTERVAL

G_BEGIN_DECLS

#ifdef INCLUDE_PRIVATE
static const GdkColor default_color_blue
#ifdef DO_INITIALISE
= {0,     0,      0, 65535}
#endif
;

static const GdkColor default_color_turq
#ifdef DO_INITIALISE
= {0,     0, 40800, 65535}
#endif
;

static const GdkColor default_color_cyan
#ifdef DO_INITIALISE
= {0,     0, 65535, 65535}
#endif
;

static const GdkColor default_color_green
#ifdef DO_INITIALISE
= {0,     0, 65535,     0}
#endif
;

static const GdkColor default_color_lime
#ifdef DO_INITIALISE
= {0, 40800, 65535,     0}
#endif
;

static const GdkColor default_color_yellow
#ifdef DO_INITIALISE
= {0, 65535, 65535,     0}
#endif
;

static const GdkColor default_color_orange
#ifdef DO_INITIALISE
= {0, 65535, 40800,     0}
#endif
;

static const GdkColor default_color_red
#ifdef DO_INITIALISE
= {0, 65535,     0,     0}
#endif
;

static const GdkColor default_color_pink
#ifdef DO_INITIALISE
= {0, 65535,     0, 65535}
#endif
;

static const GdkColor default_color_violet
#ifdef DO_INITIALISE
= {0, 40800,     0, 65535}
#endif
;

const GdkColor * default_color_set[]
#ifdef DO_INITIALISE
= {
  &default_color_blue,
  &default_color_turq,
  &default_color_cyan,
  &default_color_green,
  &default_color_lime,
  &default_color_yellow,
  &default_color_orange,
  &default_color_red,
  &default_color_pink,
  &default_color_violet
}
#endif
;

#define ftk_default_colorp(i)		default_color_set[i]
#define ftk_default_color(i)		*(default_color_set[i])
#define ftk_default_color_red(i)	default_color_set[i]->red
#define ftk_default_color_green(i)	default_color_set[i]->green
#define ftk_default_color_blue(i)	default_color_set[i]->blue

static const int colors_count
#ifdef DO_INITIALISE
= sizeof(default_color_set)/sizeof(GdkColor *)
#endif
     ;


/* unicode encodings for digbat symbols */
   
static const char filled_circle[]
#ifdef DO_INITIALISE
={0xCF, 0x25}
#endif
     ;

static const char filled_square[]
#ifdef DO_INITIALISE
= {0xA0, 0x25};
#endif
     ;

static const char filled_uptri[]
#ifdef DO_INITIALISE
= {0xB2, 0x25};
#endif
     ;

static const char filled_downtri[]
#ifdef DO_INITIALISE
= {0xBC, 0x25};
#endif
     ;

static const char filled_diamond[]
#ifdef DO_INITIALISE
= {0xC6, 0x25};
#endif
     ;

static const char filled_4star[]
#ifdef DO_INITIALISE
= {0x26, 0x27};
#endif
     ;

static const char open_4star[]
#ifdef DO_INITIALISE
= {0x27, 0x27};
#endif
     ;

static const char filled_5star[]
#ifdef DO_INITIALISE
= {0x05, 0x26};
#endif
     ;

typedef struct {
  const char * unicode;
  char * utf8;
  int    strlen;
  PangoLayout *layout;
  int h_center;
  int v_center;
} db_symbols_s;

static db_symbols_s db_symbols[]
#ifdef DO_INITIALISE
= {
  {filled_circle,	NULL, 0, NULL, 0, 0},		/* filled circle	*/
  {filled_square,	NULL, 0, NULL, 0, 0},		/* filled square	*/
  {filled_uptri,	NULL, 0, NULL, 0, 0},		/* filled uptri		*/
  {filled_downtri,	NULL, 0, NULL, 0, 0},		/* filled downtri	*/
  {filled_diamond,	NULL, 0, NULL, 0, 0},		/* filled diamond	*/
  {filled_4star,	NULL, 0, NULL, 0, 0},		/* filled 4-pt star	*/
  {open_4star,		NULL, 0, NULL, 0, 0},		/* open 4-pt star	*/
  {filled_5star,	NULL, 0, NULL, 0, 0},		/* filled 5-pt star	*/
}
#endif
;

#define ftk_symbol_unicode(i)	db_symbols[i].unicode
#define ftk_symbol_utf8(i)	db_symbols[i].utf8
#define ftk_symbol_strlen(i)	db_symbols[i].strlen
#define ftk_symbol_layout(i)	db_symbols[i].layout
#define ftk_symbol_h_center(i)	db_symbols[i].h_center
#define ftk_symbol_v_center(i)	db_symbols[i].v_center

static const int db_symbols_count
#ifdef DO_INITIALISE
= sizeof(db_symbols)/sizeof(db_symbols_s)
#endif
     ;

static db_symbols_s little_dot
#ifdef DO_INITIALISE
= {filled_circle,	NULL, 0, NULL, 0, 0}
#endif
;

#endif  /* INCLUDE_PRIVATE */

typedef enum {
  FTK_GLYPH_FILLED_CIRCLE,
  FTK_GLYPH_FILLED_SQUARE,
  FTK_GLYPH_FILLED_UP_TRIANGLE,
  FTK_GLYPH_FILLED_DOWN_TRIANGLE,
  FTK_GLYPH_FILLED_DIAMOND,
  FTK_GLYPH_FILLED_FOUR_STAR,
  FTK_GLYPH_OPEN_FOUR_STAR,
  FTK_GLYPH_FILLED_FIVE_STAR,
  FTK_GLYPH_LAST,
  FTK_GLYPH_AUTOMATIC
} FtkGlyph;


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

typedef struct {
  GdkGC       * gc;
  PangoLayout * label;
  int		glyph_hpos_d;
  int		label_hpos_d;
  int		label_width;
  int		label_height;
  int		vpos;
  FtkGlyph	glyph;
  GdkColor	color;
  char	      * string;
  gboolean	label_modified;
} ftk_marker_s;

#define ftk_marker_gc(m)		(m)->gc
#define ftk_marker_label(m)		(m)->label
#define ftk_marker_glyph_hpos(m)	(m)->glyph_hpos_d
#define ftk_marker_label_hpos(m)	(m)->label_hpos_d
#define ftk_marker_label_width(m)	(m)->label_width
#define ftk_marker_label_height(m)	(m)->label_height
#define ftk_marker_vpos(m)		(m)->vpos
#define ftk_marker_glyph(m)		(m)->glyph
#define ftk_marker_string(m)		(m)->string
#define ftk_marker_color(m)		(m)->color
#define ftk_marker_color_red(m)		(m)->color.red
#define ftk_marker_color_green(m)	(m)->color.green
#define ftk_marker_color_blue(m)	(m)->color.blue
#define ftk_marker_label_modified(m)	(m)->label_modified

typedef struct {
  gint		marker;
  gchar	      * string;
  int		loc;
  double	time;
} ftk_event_s;

#define ftk_event_marker(e)	(e)->marker
#define ftk_event_string(e)	(e)->string
#define ftk_event_loc(e)	(e)->loc
#define ftk_event_time(e)	(e)->time

typedef struct _ftk_trace_s {
  double	linestyle;
  double	linewidth;
  double	min_time;
  double	max_time;
  GdkColor	color;
  GdkGC       * gc;
  PangoLayout * label;
  int		label_width_d;
  int		label_height_d;
  int		vpos_d;
  char	      * string;
  ftk_event_s * events;
  int		event_next;
  int		event_max;
  gboolean	label_modified;		/* used */
  gboolean	valid;
  gboolean	time_set;
} ftk_trace_s;

#define ftk_tie_s ftk_trace_s

#define ftk_trace_gc(t)			(t)->gc
#define ftk_trace_min_time(t)		(t)->min_time
#define ftk_trace_max_time(t)		(t)->max_time
#define ftk_trace_time_set(t)		(t)->time_set
#define ftk_trace_string(t)		(t)->string
#define ftk_trace_label(t)		(t)->label
#define ftk_trace_label_dwidth(t)	(t)->label_width_d
#define ftk_trace_label_dheight(t)	(t)->label_height_d
#define ftk_trace_vpos_d(t)		(t)->vpos_d
#define ftk_trace_linestyle(t)		(t)->linestyle
#define ftk_trace_linewidth(t)		(t)->linewidth
#define ftk_trace_color(t)		(t)->color
#define ftk_trace_color_pixel(t)	(t)->color.pixel
#define ftk_trace_color_red(t)		(t)->color.red
#define ftk_trace_color_green(t)	(t)->color.green
#define ftk_trace_color_blue(t)		(t)->color.blue
#define ftk_trace_label_modified(t)	(t)->label_modified
#define ftk_trace_events(t)		(t)->events
#define ftk_trace_event(t,i)	      &((t)->events[i])
#define ftk_trace_event_next(t)		(t)->event_next
#define ftk_trace_event_max(t)		(t)->event_max
#define ftk_trace_valid(t)		(t)->valid

#define ftk_tie_gc(t)			(t)->gc
#define ftk_tie_label(t)		(t)->label
#define ftk_tie_label_dheight(t)	(t)->label_height_d
#define ftk_tie_label_dwidth(t)		(t)->label_width_d
#define ftk_tie_vpos_d(t)		(t)->vpos_d
#define ftk_tie_linestyle(t)		(t)->linestyle
#define ftk_tie_linewidth(t)		(t)->linewidth
#define ftk_tie_color(t)		(t)->color
#define ftk_tie_color_pixel(t)		(t)->color.pixel
#define ftk_tie_color_red(t)		(t)->color.red
#define ftk_tie_color_green(t)		(t)->color.green
#define ftk_tie_color_blue(t)		(t)->color.blue
#define ftk_tie_label_modified(t)	(t)->label_modified

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

typedef struct {
  gint trace_idx;
  gint event_idx;
} ftk_event_pair_s;

typedef struct {
  int tie_index;
  ftk_event_pair_s * event_pair_list;
  int event_list_next;
  int event_list_max;
} ftk_dlink_s;

#define ftk_dlink_tie_index(l)		(l)->tie_index
#define ftk_dlink_event_pair_list(l)	(l)->event_pair_list
#define ftk_dlink_event_pair(l,i)	(l)->event_pair_list[i]
#define ftk_dlink_event_pair_trace(l,i)	(l)->event_pair_list[i].trace_idx
#define ftk_dlink_event_pair_event(l,i)	(l)->event_pair_list[i].event_idx
#define ftk_dlink_event_list_next(l)	(l)->event_list_next
#define ftk_dlink_event_list_max(l)	(l)->event_list_max

typedef enum {
  FTK_POPUP_TYPE_NONE,
  FTK_POPUP_TYPE_LABEL,
  FTK_POPUP_TYPE_MARKER,
  FTK_POPUP_TYPE_LEGEND,
  FTK_POPUP_TYPE_TIME
} ftk_popup_type_e;

typedef struct _FtkEventViewer {
  GtkVBox		  vbox;
  double		  zero_d;
  double		  now_d;
  double		  span_d;
  double		  min_time;
  double		  max_time;
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
  GtkWidget		* legend_frame;
  GtkWidget		* readout;
  GtkWidget		* scroll;
  GtkAdjustment		* scroll_adj;
  GtkDrawingArea	* da;
  GtkDrawingArea	* legend_da;
  GdkColor		  bg_color;
  const GdkColor       ** color_set;
  ftk_marker_s	        * markers;
  int	      		  markers_next;
  int	      		  markers_max;
  ftk_trace_s		* traces;
  int			  trace_next;
  int			  trace_max;
  int			* trace_pool;
  int			  trace_pool_next;
  int			  trace_pool_max;
  int			* trace_order;
  int			  trace_order_next;
  int			  trace_order_max;
  ftk_tie_s		* ties;
  int			  tie_next;
  int			  tie_max;
  ftk_link_s		* links;
  int			  link_next;
  int			  link_max;
  ftk_dlink_s		* dlinks;
  int			  dlink_next;
  int			  dlink_max;
  int			  label_box_width;
  int			  label_box_height;
  int			  da_height;
  int			  legend_height;
  int			  trace_origin;
  int			  trace_width;
  int			  popup_trace;
  int			  popup_marker;
  ftk_popup_type_e	  popup_type;
  FtkGlyph		  next_glyph;
  int			  next_color;
  gboolean		  trace_modified;		/* used */
  gboolean		  tie_modified;
  gboolean     		  markers_modified;		/* used */
  gboolean     		  widget_modified;		/* used */
  gboolean     		  symbols_initted;
  gboolean		  drawable;
  gboolean		  time_set;
} FtkEventViewer;

#define ftk_ev_vbox(v)		      &((v)->vbox)
#ifdef USE_READOUT
#define ftk_ev_readout(t)		(t)->readout
#endif
#define ftk_ev_min_time(v)		(v)->min_time
#define ftk_ev_max_time(v)		(v)->max_time
#define ftk_ev_time_set(v)		(v)->time_set
#define ftk_ev_color_values(v)		(v)->color_set
#define ftk_ev_color_value(v,i)		(v)->color_set[i]
#define ftk_ev_color_value_red(v,i)	(v)->color_set[i].red
#define ftk_ev_color_value_green(v,i)	(v)->color_set[i].green
#define ftk_ev_color_value_blue(v,i)	(v)->color_set[i].blue
#define ftk_ev_next_glyph(v)		(v)->next_glyph
#define ftk_ev_next_color(v)		(v)->next_color
#define ftk_ev_symbols_initted(v)	(v)->symbols_initted
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
#define ftk_ev_legend_frame(v)		(v)->legend_frame
#define ftk_ev_da(v)			(v)->da
#define ftk_ev_legend_da(v)		(v)->legend_da
#define ftk_ev_scroll(v)		(v)->scroll
#define ftk_ev_scroll_adj(v)		(v)->scroll_adj
#define ftk_ev_zero(v)			(v)->zero_d
#define ftk_ev_now(v)			(v)->now_d
#define ftk_ev_span(v)			(v)->span_d
#define ftk_ev_drawable(v)		(v)->drawable
#define ftk_ev_bg_color(v)		(v)->bg_color
#define ftk_ev_bg_pixel(v)		(v)->bg_color.pixel
#define ftk_ev_bg_red(v)		(v)->bg_color.red
#define ftk_ev_bg_green(v)		(v)->bg_color.green
#define ftk_ev_bg_blue(v)		(v)->bg_color.blue
#define ftk_ev_trace_pool(v)		(v)->trace_pool
#define ftk_ev_trace_pool_ety(v,i)	(v)->trace_pool[i]
#define ftk_ev_trace_pool_next(v)	(v)->trace_pool_next
#define ftk_ev_trace_pool_max(v)	(v)->trace_pool_max
#define ftk_ev_trace_order(v)		(v)->trace_order
#define ftk_ev_trace_order_ety(v,i)	(v)->trace_order[i]
#define ftk_ev_trace_order_next(v)	(v)->trace_order_next
#define ftk_ev_trace_order_max(v)	(v)->trace_order_max
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
#define ftk_ev_da_height(v)		(v)->da_height
#define ftk_ev_legend_height(v)		(v)->legend_height
#define ftk_ev_links(v)			(v)->links
#define ftk_ev_link(v,i)	      &((v)->links[i])
#define ftk_ev_link_next(v)		(v)->link_next
#define ftk_ev_link_max(v)		(v)->link_max
#define ftk_ev_dlinks(v)		(v)->dlinks
#define ftk_ev_dlink(v,i)	      &((v)->dlinks[i])
#define ftk_ev_dlink_next(v)		(v)->dlink_next
#define ftk_ev_dlink_max(v)		(v)->dlink_max
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
  FTK_EV_ERROR_INVALID_GLYPH,
  FTK_EV_ERROR_INVALID_SPAN,
  FTK_EV_ERROR_INVALID_EVENT,
} ftk_ev_error_e;

/*************** public api *****************/

typedef struct {
  gint trace;
  gint marker;
  gchar * string;
} ftk_simultaneous_events_s;


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
				 GdkColor * color, GError ** err);
gboolean
ftk_eventviewer_set_bg_color	(FtkEventViewer * eventviewer,
				 GdkColor * color);
				 
gboolean
ftk_eventviewer_set_bg_default	(FtkEventViewer * eventviewer);

GdkColor *
ftk_eventviewer_get_bg_default (FtkEventViewer * eventviewer);

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
				 char * string,
				 GError ** err);

gint
ftk_eventviewer_add_trace	(FtkEventViewer * eventviewer,
				 char * label,
				 char * string);

gboolean
ftk_eventviewer_delete_trace_e	(FtkEventViewer * eventviewer,
				 gint trace_idx,
				 GError ** err);

gboolean
ftk_eventviewer_delete_trace	(FtkEventViewer * eventviewer,
				 gint trace_idx);

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
					 GdkColor * color,
					 GError ** err);

gboolean
ftk_eventviewer_set_trace_color		(FtkEventViewer * eventviewer,
					 gint trace,
					 GdkColor * color);

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
					 gint ls,
					 GError ** err);
gboolean
ftk_eventviewer_set_trace_linestyle	(FtkEventViewer * eventviewer,
					 gint trace,
					 gint lw,
					 gint ls);

gint
ftk_eventviewer_marker_new_e		(FtkEventViewer * eventviewer,
					 FtkGlyph glyph,
					 char * label,
					 char * string,
					 GError ** err);

gint
ftk_eventviewer_marker_new 		(FtkEventViewer * eventviewer,
					 FtkGlyph glyph,
					 char * label,
					 char * string);

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
					 GdkColor * color,
					 GError ** err);

gboolean
ftk_eventviewer_set_marker_color	(FtkEventViewer * eventviewer,
					 gint marker,
					 GdkColor * color);

GdkColor *
ftk_eventviewer_get_marker_color_e	(FtkEventViewer * eventviewer,
					 gint marker,
					 GError ** err);

GdkColor *
ftk_eventviewer_get_marker_color	(FtkEventViewer * eventviewer,
					 gint marker);

gint
ftk_eventviewer_tie_new_e	(FtkEventViewer * eventviewer,
#ifdef USE_TIE_LABEL
				 char * label,
#endif
				 GError ** err);
gint
ftk_eventviewer_tie_new		(FtkEventViewer * eventviewer
#ifdef USE_TIE_LABEL
				 , char * label
#endif
				 );

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
					 GdkColor * color,
					 GError ** err);

gboolean
ftk_eventviewer_set_tie_color		(FtkEventViewer * eventviewer,
					 gint tie,
					 GdkColor * color);

#ifdef USE_TIE_LABEL
gboolean
ftk_eventviewer_set_tie_label_e	(FtkEventViewer * eventviewer,
					 gint tie,
					 char * label,
					 GError ** err);
gboolean
ftk_eventviewer_set_tie_label		(FtkEventViewer * eventviewer,
					 gint tie,
					 char * label);
#endif

gboolean
ftk_eventviewer_set_tie_linestyle_e	(FtkEventViewer * eventviewer,
					 gint tie,
					 gint lw,
					 gint ls,
					 GError ** err);

gboolean
ftk_eventviewer_set_tie_linestyle	(FtkEventViewer * eventviewer,
					 gint tie,
					 gint lw,
					 gint ls);

gint
ftk_eventviewer_append_event_e	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint marker,
				 gchar * string,
				 GError ** err);

gint
ftk_eventviewer_append_event	(FtkEventViewer * eventviewer,
				 gint trace,
				 gint marker,
				 gchar * string);

gboolean
ftk_eventviewer_append_simultaneous_events_e (FtkEventViewer * eventviewer,
					      gint tie_index,
					      GError ** err, ...);

gboolean
ftk_eventviewer_append_simultaneous_events (FtkEventViewer * eventviewer,
					    gint tie_index, ...);

gboolean
ftk_eventviewer_append_simultaneous_event_array_e (FtkEventViewer * eventviewer,
						   gint tie_index,
						   gint array_count,
						   ftk_simultaneous_events_s * events_array,
						   GError ** err);

gboolean
ftk_eventviewer_append_simultaneous_event_array (FtkEventViewer * eventviewer,
						 gint tie_index,
						 gint array_count,
						 ftk_simultaneous_events_s * events_array);

gboolean
ftk_eventviewer_tie_events_e (FtkEventViewer * eventviewer,
			      gint tie_index,
			      GError ** err, ...);

gboolean
ftk_eventviewer_tie_events (FtkEventViewer * eventviewer,
			    gint tie_index, ...); 

gboolean
ftk_eventviewer_tie_event_array_e (FtkEventViewer * eventviewer,
				   gint tie_index,
				   gint count,
				   ftk_event_pair_s * events,
				   GError ** err);

gboolean
ftk_eventviewer_tie_event_array (FtkEventViewer * eventviewer,
				 gint tie_index, gint count,
				 ftk_event_pair_s * events);


G_END_DECLS

#endif /* __FTK_EVENTVIEWER_H__ */
