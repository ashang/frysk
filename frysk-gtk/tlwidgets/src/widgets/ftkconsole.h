#ifndef __FTK_CONSOLE_H__
#define __FTK_CONSOLE_H__

#include <sys/types.h>
#include <sys/time.h>
#include <glib.h>
#include <glib-object.h>
#include <gtk/gtktable.h>

G_BEGIN_DECLS

#define FTK_CONSOLE_TYPE           \
  (ftk_console_get_type ())
#define FTK_CONSOLE(obj)           \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), FTK_CONSOLE_TYPE, FtkConsole))
#define FTK_CONSOLE_CLASS(klass)   \
  (G_TYPE_CHECK_CLASS_CAST ((klass), FTK_CONSOLE_TYPE, FtkConsoleClass))
#define FTK_IS_CONSOLE(obj)        \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), FTK_CONSOLE_TYPE))
#define FTK_IS_CONSOLE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE ((klass), FTK_CONSOLE_TYPE))

#define FTK_CONSOLE_INITIAL_WIDTH  300
#define FTK_CONSOLE_INITIAL_HEIGHT 200

typedef enum {
  FTK_ERROR_NONE,
  FTK_ERROR_INVALID_CONSOLE_WIDGET,
} ftk_error_e;

typedef struct _FtkConsole {
  GtkVBox         vbox;
  GtkTextBuffer * log_buffer;
  GtkWidget     * log_view;
  GtkTextMark   * log_eob_mark;
  GtkEntry      * entry;
  char          * entry_text;
} FtkConsole;

#define console_vbox(s)	          (s)->vbox
#define console_log_buffer(s)	  (s)->log_buffer
#define console_log_view(s)	  (s)->log_view
#define console_log_eob_mark(s)	  (s)->log_eob_mark
#define console_entry(s)	  (s)->entry
#define console_entry_text(s)	  (s)->entry_text

typedef struct _FtkConsoleClass {
  GtkVBoxClass parent_class;

  void (* ftkconsole) (FtkConsole * console);
} FtkConsoleClass;

/*************** public api *****************/

GType       ftk_console_get_type      (void);
GtkWidget * ftk_console_new           (void);

gboolean    ftk_console_resize_e      (FtkConsole * console,
				       gint width, gint height,
				       GError ** err);
gboolean    ftk_console_resize        (FtkConsole * console,
				       gint width, gint height);

gboolean    ftk_console_append_text_e (FtkConsole * console,
				       char * str,
				       GError ** err);
gboolean    ftk_console_append_text   (FtkConsole * console,
				       char * str);
const char * ftk_console_read_entry_e  (FtkConsole * console,
					gboolean clear,
					GError ** err);
const char * ftk_console_read_entry    (FtkConsole * console,
					gboolean clear);

G_END_DECLS

#endif /* __FTK_CONSOLE_H__ */
