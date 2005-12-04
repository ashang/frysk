#ifndef __FTK_STRIPCHART_H__
#define __FTK_STRIPCHART_H__

#include <sys/types.h>
#include <sys/time.h>
#include <glib.h>
#include <glib-object.h>
#include <gtk/gtktable.h>

G_BEGIN_DECLS

#define FTK_STRIPCHART_TYPE           \
  (ftk_stripchart_get_type ())
#define FTK_STRIPCHART(obj)           \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_STRIPCHART_TYPE, FtkStripchart))
#define FTK_STRIPCHART_CLASS(klass)   \
  (G_TYPE_CHECK_CLASS_CAST ((klass), FTK_STRIPCHART_TYPE, FtkStripchartClass))
#define FTK_IS_STRIPCHART(obj)        \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_STRIPCHART_TYPE))
#define FTK_IS_STRIPCHART_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE ((klass), FTK_STRIPCHART_TYPE))

#define FTK_STRIPCHART_INITIAL_WIDTH  300
#define FTK_STRIPCHART_INITIAL_HEIGHT  60

typedef enum {
  FTK_ERROR_NONE,
  FTK_ERROR_INVALID_UPDATE_INTERVAL,
  FTK_ERROR_INVALID_STRIPCHART_WIDGET,
  FTK_ERROR_INVALID_RANGE,
  FTK_ERROR_INVALID_TYPE,
  FTK_ERROR_TIMER_NOT_ENABLED,
  FTK_ERROR_INVALID_DRAWING_AREA
} ftk_error_e;

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

typedef struct _FtkStripchart {
  GtkDrawingArea drawingarea;
  GdkColor bg_color;
  gboolean bg_color_modified;
  GdkGC * bg_gc;
  GdkPixmap * pixmap;
  event_spec_s * event_specs;
  int event_spec_next;
  int event_spec_max;
  event_s ** events;
  int event_next;
  int event_max;
  event_s * current_event;
  timer_t timer_id;
  gboolean timer_set;
  struct timeval range;
  struct timeval bin_width;
  PangoLayout * base_readout;
  PangoLayout * motion_readout;
  double readout;
} FtkStripchart;

#define stripchart_drawingarea(s)	  (s)->drawingarea
#define stripchart_bg_gc(s)		  (s)->bg_gc
#define stripchart_bg_color(s)		  (s)->bg_color
#define stripchart_bg_color_modified(s)	  (s)->bg_color_modified
#define stripchart_bg_pixel(s)		  (s)->bg_color.pixel
#define stripchart_bg_red(s)		  (s)->bg_color.red
#define stripchart_bg_green(s)		  (s)->bg_color.green
#define stripchart_bg_blue(s)		  (s)->bg_color.blue
#define stripchart_pixmap(s)		  (s)->pixmap
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
#define stripchart_timer_id(s)            (s)->timer_id
#define stripchart_timer_set(s)           (s)->timer_set
#define stripchart_range(s)               (s)->range
#define stripchart_range_secs(s)          (s)->range.tv_sec
#define stripchart_range_usecs(s)         (s)->range.tv_usec
#define stripchart_bin_width(s)           (s)->bin_width
#define stripchart_bin_width_secs(s)      (s)->bin_width.tv_sec
#define stripchart_bin_width_usecs(s)     (s)->bin_width.tv_usec
#define stripchart_base_readout(s)        (s)->base_readout
#define stripchart_motion_readout(s)      (s)->motion_readout

#define float_tv(s,u) (((double)(s)) + (((double)(u))/1.0e6))

typedef struct _FtkStripchartClass {
  GtkDrawingAreaClass parent_class;

  void (* ftkstripchart) (FtkStripchart * stripchart);
} FtkStripchartClass;

/*************** public api *****************/

GType       ftk_stripchart_get_type      (void);
GtkWidget * ftk_stripchart_new           (void);

gboolean    ftk_stripchart_resize_e      (FtkStripchart * stripchart,
					  gint width, gint height,
					  GError ** err);
gboolean    ftk_stripchart_resize        (FtkStripchart * stripchart,
					  gint width, gint height);

gboolean    ftk_stripchart_set_bg_rgb_e (FtkStripchart * stripchart,
					 gint red, gint green, gint blue,
					 GError ** err);
gboolean    ftk_stripchart_set_bg_rgb (FtkStripchart * stripchart,
				       gint red, gint green, gint blue);

#ifdef OLD_API
gboolean    ftk_stripchart_set_event_rgb_e (FtkStripchart * stripchart,
					    FtkStripchartTypeEnum type,
					    gint red, gint green, gint blue,
					    GError ** err);
gboolean    ftk_stripchart_set_event_rgb (FtkStripchart * stripchart,
					  FtkStripchartTypeEnum type,
					  gint red, gint green, gint blue);

gboolean    ftk_stripchart_set_event_title_e (FtkStripchart * stripchart,
					      FtkStripchartTypeEnum type,
					      const char * title,
					      GError ** err);
gboolean    ftk_stripchart_set_event_title (FtkStripchart * stripchart,
					    FtkStripchartTypeEnum type,
					    const char * title);
#endif /* OLD_API */

gint        ftk_stripchart_new_event_e (FtkStripchart * stripchart,
					const char * title,
					gint red, gint green, gint blue,
					GError ** err);
gint        ftk_stripchart_new_event    (FtkStripchart * stripchart,
					 const char * title,
					 gint red, gint green, gint blue);

gboolean    ftk_stripchart_set_update_e  (FtkStripchart * stripchart,
					  gint milliseconds,
					  GError ** err);
gboolean    ftk_stripchart_set_update    (FtkStripchart * stripchart,
					  gint milliseconds);

gboolean    ftk_stripchart_set_range_e   (FtkStripchart * stripchart,
					  gint milliseconds,
					  GError ** err);
gboolean    ftk_stripchart_set_range     (FtkStripchart * stripchart,
					  gint milliseconds);

gboolean    ftk_stripchart_append_event_e (FtkStripchart * stripchart,
					   gint type,
					   GError ** err);
gboolean    ftk_stripchart_append_event  (FtkStripchart * stripchart,
					   gint type);

G_END_DECLS

#endif /* __FTK_STRIPCHART_H__ */
