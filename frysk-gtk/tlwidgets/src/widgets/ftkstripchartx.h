#ifndef __FTK_STRIPCHARTX_H__
#define __FTK_STRIPCHARTX_H__

#include <sys/types.h>
#include <sys/time.h>
#include <glib.h>
#include <glib-object.h>
#include <gtk/gtktable.h>

G_BEGIN_DECLS

#define FTK_STRIPCHARTX_TYPE            \
        (ftk_stripchartx_get_type ())

#define FTK_STRIPCHARTX(obj)		\
        (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_STRIPCHARTX_TYPE, FtkStripchartX))

#define FTK_STRIPCHARTX_CLASS(klass)	\
        (G_TYPE_CHECK_CLASS_CAST ((klass), FTK_STRIPCHARTX_TYPE, FtkStripchartXClass))

#define FTK_IS_STRIPCHARTX(obj) 		\
        (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_STRIPCHARTX_TYPE))

#define FTK_IS_STRIPCHARTX_CLASS(klass)	\
        (G_TYPE_CHECK_CLASS_TYPE ((klass), FTK_STRIPCHARTX_TYPE))


typedef enum {
  FTK_SPIN_NONE,
  FTK_SPIN_LEFT,
  FTK_SPIN_CENTER,
  FTK_SPIN_RIGHT,
} ftk_spin_e;

typedef struct {
  GdkColor color;
  GdkGC * gc;
  PangoLayout * title;
} event_spec_s;

typedef struct {
  struct timeval tv;
  gboolean modified;
  int total;
  int * count_vec;
  int count_rho;
} event_s;

typedef struct _FtkStripchartX {
  GtkTable    table;
  GtkWidget * left_label;
  GtkWidget * right_label;
  GtkWidget * left_limit_button;
  GtkWidget * lock_button;
  GtkWidget * left_spin_box;
  gulong      left_spin_handler_id;
  GtkWidget * center_spin_box;
  gulong      center_spin_handler_id;
  GtkWidget * right_spin_box;
  gulong      right_spin_handler_id;
  GtkWidget * hold_button;
  GtkWidget * right_limit_button;
  GtkWidget * slider_da;
  GtkWidget * slider_frame;
  GtkWidget * da_frame;
  GtkObject * slider_adjustment;
  char      * ostring;
  struct	 timeval range;
  gboolean	 range_set;
  gboolean 	 kill_timer_pending;
  gboolean 	 timer_set;
  struct timeval bin_width;
  struct timeval left_lock_time;
  struct timeval right_lock_time;
  ftk_spin_e	 spin_button_grabbed;

  GdkPixmap * pixmap;
  GtkDrawingArea * drawingarea;
  GdkColor bg_color;
  gboolean bg_color_modified;
  GdkGC * bg_gc;;
  GdkColor readout_color;
  gboolean readout_color_modified;
  GdkGC * readout_gc;
  event_spec_s * event_specs;
  int event_spec_next;
  int event_spec_max;
  event_s ** events;
  int event_next;
  int event_max;
  event_s * current_event;
} FtkStripchartX;

#define stripchart_table(s)			&((s)->table)
#define stripchart_initted(s)			(s)->initted
#define stripchart_left_label(s)		(s)->left_label
#define stripchart_right_label(s)		(s)->right_label
#define stripchart_left_limit_button(s)		(s)->left_limit_button
#define stripchart_lock_button(s)		(s)->lock_button
#define stripchart_left_spin_box(s)		(s)->left_spin_box
#define stripchart_left_spin_id(s)		(s)->left_spin_handler_id
#define stripchart_center_spin_box(s)		(s)->center_spin_box
#define stripchart_center_spin_id(s)		(s)->center_spin_handler_id
#define stripchart_right_spin_box(s)		(s)->right_spin_box
#define stripchart_right_spin_id(s)		(s)->right_spin_handler_id
#define stripchart_right_limit_button(s)	(s)->right_limit_button
#define stripchart_hold_button(s)		(s)->hold_button
#define stripchart_slider_da(s)			(s)->slider_da
#define stripchart_slider_frame(s)		(s)->slider_frame
#define stripchart_da_frame(s)			(s)->da_frame
#define stripchart_ostring(s)			(s)->ostring
#define stripchart_slider_adjustment(s)		(s)->slider_adjustment
#define stripchart_slider_adj_left(s)		GTK_ADJUSTMENT ((s)->slider_adjustment)->lower
#define stripchart_slider_adj_right(s)		GTK_ADJUSTMENT ((s)->slider_adjustment)->upper
#define stripchart_range(s)			(s)->range
#define stripchart_range_secs(s)		(s)->range.tv_sec
#define stripchart_range_usecs(s)		(s)->range.tv_usec
#define stripchart_range_set(s)			(s)->range_set
#define stripchart_kill_timer_pending(s)	(s)->kill_timer_pending
#define stripchart_timer_set(s)			(s)->timer_set
#define stripchart_vc_disabled(s)		(s)->vc_disabled
#define stripchart_vc_in_progress(s)		(s)->vc_in_progress
#define stripchart_bin_width(s)			(s)->bin_width
#define stripchart_bin_width_secs(s)		(s)->bin_width.tv_sec
#define stripchart_bin_width_usecs(s)		(s)->bin_width.tv_usec
#define stripchart_locked_start_time(s)		(s)->left_lock_time
#define stripchart_locked_start_time_secs(s)	(s)->left_lock_time.tv_sec
#define stripchart_locked_start_time_usecs(s)	(s)->left_lock_time.tv_usec
#define stripchart_locked_end_time(s)		(s)->right_lock_time
#define stripchart_locked_end_time_secs(s)	(s)->right_lock_time.tv_sec
#define stripchart_locked_end_time_usecs(s)	(s)->right_lock_time.tv_usec
#define stripchart_spin_button_grabbed(s)	(s)->spin_button_grabbed

#define stripchart_pixmap(s)		  (s)->pixmap
#define stripchart_drawingarea(s)	  (s)->drawingarea
#define stripchart_bg_gc(s)		  (s)->bg_gc
#define stripchart_bg_color(s)		  (s)->bg_color
#define stripchart_bg_color_modified(s)	  (s)->bg_color_modified
#define stripchart_bg_pixel(s)		  (s)->bg_color.pixel
#define stripchart_bg_red(s)		  (s)->bg_color.red
#define stripchart_bg_green(s)		  (s)->bg_color.green
#define stripchart_bg_blue(s)		  (s)->bg_color.blue
#define stripchart_readout_gc(s)		  (s)->readout_gc
#define stripchart_readout_color(s)		  (s)->readout_color
#define stripchart_readout_color_modified(s)	  (s)->readout_color_modified
#define stripchart_readout_pixel(s)		  (s)->readout_color.pixel
#define stripchart_readout_red(s)		  (s)->readout_color.red
#define stripchart_readout_green(s)		  (s)->readout_color.green
#define stripchart_readout_blue(s)		  (s)->readout_color.blue
#define stripchart_event_specs(s)	  (s)->event_specs
#define stripchart_event_spec(s, i)	  (s)->event_specs[i]
#define stripchart_event_spec_gc(s, i)	  (s)->event_specs[i].gc
#define stripchart_event_spec_color(s, i) (s)->event_specs[i].color
#define stripchart_event_spec_pixel(s, i) (s)->event_specs[i].color.pixel
#define stripchart_event_spec_red(s, i)   (s)->event_specs[i].color.red
#define stripchart_event_spec_green(s, i) (s)->event_specs[i].color.green
#define stripchart_event_spec_blue(s, i)  (s)->event_specs[i].color.blue
#define stripchart_event_spec_title(s, i) (s)->event_specs[i].title
#define stripchart_event_spec_next(s)	  (s)->event_spec_next
#define stripchart_event_spec_max(s)	  (s)->event_spec_max
#define stripchart_events(s)              (s)->events
#define stripchart_event(s,i)             (s)->events[i]
#define stripchart_event_tv(s,i)          (s)->events[i]->tv
#define stripchart_event_tv_sec(s,i)      (s)->events[i]->tv.tv_sec
#define stripchart_event_tv_usec(s,i)     (s)->events[i]->tv.tv_usec
#define stripchart_event_modified(s,i)    (s)->events[i]->modified
#define stripchart_event_count_vec(s,i)   (s)->events[i]->count_vec
#define stripchart_event_count(s,i,j)     (s)->events[i]->count_vec[j]
#define stripchart_event_count_rho(s,i)   (s)->events[i]->count_rho
#define stripchart_event_total(s,i)       (s)->events[i]->total
#define stripchart_event_next(s)          (s)->event_next
#define stripchart_event_max(s)           (s)->event_max
#define stripchart_current(s)             (s)->current_event
#define stripchart_current_tv(s)          (s)->current_event->tv
#define stripchart_current_tv_sec(s)      (s)->current_event->tv.tv_sec
#define stripchart_current_tv_usec(s)     (s)->current_event->tv.tv_usec
#define stripchart_current_modified(s)    (s)->current_event->modified
#define stripchart_current_count_vec(s)   (s)->current_event->count_vec
#define stripchart_current_count(s,j)     (s)->current_event->count_vec[j]
#define stripchart_current_count_rho(s)   (s)->current_event->count_rho
#define stripchart_current_total(s)       (s)->current_event->total

typedef struct _FtkStripchartXClass {
  GtkTableClass parent_class;

  void (* ftkstripchart) (FtkStripchartX * stripchart);
} FtkStripchartXClass;

typedef enum {
  FTK_ERROR_NONE,
  FTK_ERROR_INVALID_UPDATE_INTERVAL,
  FTK_ERROR_INVALID_STRIPCHART_WIDGET,
  FTK_ERROR_INVALID_RANGE,
  FTK_ERROR_INVALID_TYPE,
  FTK_ERROR_TIMER_NOT_ENABLED,
  FTK_ERROR_INVALID_DRAWING_AREA,
  FTK_ERROR_INVALID_COLOR
} ftk_error_e;

/*************** public api *****************/

GType          ftk_stripchartx_get_type		(void);
GtkWidget *    ftk_stripchartx_new		();

gboolean    ftk_stripchartx_resize_e		(FtkStripchartX * stripchart,
						 gint width, gint height,
						 GError ** err);
gboolean    ftk_stripchartx_resize		(FtkStripchartX * stripchart,
						 gint width, gint height);

gboolean    ftk_stripchartx_set_bg_rgb_e		(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue,
						 GError ** err);
gboolean    ftk_stripchartx_set_bg_rgb		(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue);

gboolean    ftk_stripchartx_set_readout_rgb_e	(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue,
						 GError ** err);
gboolean    ftk_stripchartx_set_readout_rgb	(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue);

gboolean    ftk_stripchartx_set_chart_rgb_e	(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue,
						 GError ** err);
gboolean    ftk_stripchartx_set_chart_rgb	(FtkStripchartX * stripchart,
						 gint red, gint green, gint blue);

gint        ftk_stripchartx_new_event_e		(FtkStripchartX * stripchart,
						 const char * title,
						 gint red, gint green, gint blue,
						 GError ** err);
gint        ftk_stripchartx_new_event		(FtkStripchartX * stripchart,
						 const char * title,
						 gint red, gint green, gint blue);

gboolean    ftk_stripchartx_set_update_e		(FtkStripchartX * stripchart,
						 gint milliseconds,
						 GError ** err);
gboolean    ftk_stripchartx_set_update		(FtkStripchartX * stripchart,
						 gint milliseconds);

gboolean    ftk_stripchartx_set_range_e		(FtkStripchartX * stripchart,
						 gint milliseconds,
						 GError ** err);
gboolean    ftk_stripchartx_set_range		(FtkStripchartX * stripchart,
						 gint milliseconds);

gboolean    ftk_stripchartx_append_event_e	(FtkStripchartX * stripchart,
						 gint type,
						 GError ** err);
gboolean    ftk_stripchartx_append_event		(FtkStripchartX * stripchart,
						 gint type);

G_END_DECLS

#endif /* __FTK_STRIPCHARTX_H__ */
