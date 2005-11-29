#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <signal.h>
#include <math.h>
#include <time.h>
#include <gtk/gtk.h>
#include "ftkconsole.h"

GQuark ftk_quark;

enum {
  FTK_CONSOLE_SIGNAL,
  LAST_SIGNAL
};

static void ftk_console_class_init (FtkConsoleClass * klass);
static void ftk_console_init       (FtkConsole      * console);
static guint ftk_console_signals[LAST_SIGNAL] = { 0 };

/************************** object stuff **********************/

static void
ftk_console_class_init (FtkConsoleClass *klass)
{
  ftk_console_signals[FTK_CONSOLE_SIGNAL]
    = g_signal_new ("ftkconsole",
		    G_TYPE_FROM_CLASS (klass),
		    G_SIGNAL_RUN_LAST,
		    /*		    G_SIGNAL_RUN_FIRST | G_SIGNAL_ACTION, */
		    G_STRUCT_OFFSET (FtkConsoleClass, ftkconsole),
		    NULL, 
		    NULL,                
		    g_cclosure_marshal_VOID__VOID,
		    G_TYPE_NONE, 0);

  ftk_quark = g_quark_from_string ("Ftk console function");
}

static void
ftk_console_expose( GtkWidget * widget,
		    GdkEventExpose * event,
		    gpointer data)
{
#if 0
  FtkConsole * console = FTK_CONSOLE (console);

  fprintf (stderr, "expose\n");
#endif
}

static void
ftk_entry_activate (GtkEntry *entry, gpointer user_data)
{
  FtkConsole * console = FTK_CONSOLE (user_data);

  g_signal_emit (console,
                 ftk_console_signals[FTK_CONSOLE_SIGNAL],
                 0);
}

#if 0 /* doc for enum */
typedef enum 
{
  GTK_MOVEMENT_LOGICAL_POSITIONS, /* move by forw/back graphemes */
  GTK_MOVEMENT_VISUAL_POSITIONS,  /* move by left/right graphemes */
  GTK_MOVEMENT_WORDS,             /* move by forward/back words */
  GTK_MOVEMENT_DISPLAY_LINES,     /* move up/down lines (wrapped lines) */
  GTK_MOVEMENT_DISPLAY_LINE_ENDS, /* move up/down lines (wrapped lines) */
  GTK_MOVEMENT_PARAGRAPHS,        /* move up/down paragraphs (newline-ended lines) */
  GTK_MOVEMENT_PARAGRAPH_ENDS,    /* move to either end of a paragraph */
  GTK_MOVEMENT_PAGES,	          /* move by pages */
  GTK_MOVEMENT_BUFFER_ENDS,       /* move to ends of the buffer */
  GTK_MOVEMENT_HORIZONTAL_PAGES   /* move horizontally by pages */
} GtkMovementStep;
#endif

#if 0
static void
ftk_entry_move_cursor (GtkEntry *entry,
		       GtkMovementStep arg1,
		       gint arg2,
		       gboolean arg3,
		       gpointer user_data)
{
  FtkConsole * console = FTK_CONSOLE (user_data);
  fprintf (stderr, "move cursor, arg1 = %d, arg2 = %d, arg3 = %s\n",
	   (int)arg1, arg2, ((TRUE == arg3) ? ".t." : ".f."));
}
#endif

static void
ftk_console_init (FtkConsole * console)
{
  GtkTextIter  end;
  GtkWidget  * vbox  = GTK_WIDGET (&(console_vbox (console)));
  GtkWidget  * sw    = gtk_scrolled_window_new (NULL, NULL);
  GtkWidget  * entry = gtk_entry_new();

  console_entry (console) = GTK_ENTRY (entry);
  console_entry_text (console) = NULL;

  // fprintf (stderr, "ftk_console_init\n");

  gtk_scrolled_window_set_policy (GTK_SCROLLED_WINDOW (sw),
                                  GTK_POLICY_AUTOMATIC,
                                  GTK_POLICY_AUTOMATIC);
  
  gtk_widget_set_size_request (GTK_WIDGET (console),
			       FTK_CONSOLE_INITIAL_WIDTH,
			       FTK_CONSOLE_INITIAL_HEIGHT);

  gtk_signal_connect (GTK_OBJECT (console), "expose_event",
		      (GtkSignalFunc) ftk_console_expose, NULL);

  gtk_signal_connect (GTK_OBJECT (entry), "activate",
		      (GtkSignalFunc) ftk_entry_activate, console);

#if 0
  gtk_signal_connect (GTK_OBJECT (entry), "move-cursor",
		      (GtkSignalFunc) ftk_entry_move_cursor, console);
#endif

  gtk_box_set_homogeneous (GTK_BOX (vbox), FALSE);
  gtk_box_set_spacing (GTK_BOX (vbox), 0);


  console_log_view (console) = gtk_text_view_new ();
  console_log_buffer (console) = 
    gtk_text_view_get_buffer (GTK_TEXT_VIEW (console_log_view (console)));
  gtk_text_buffer_set_text (console_log_buffer (console),
                            "Console opened.\n\n",
                            -1);
  gtk_text_buffer_get_end_iter (console_log_buffer (console), &end);
  console_log_eob_mark (console) =
    gtk_text_buffer_create_mark (console_log_buffer (console),
				 NULL,
				 &end,
				 FALSE);

  gtk_text_view_set_editable (GTK_TEXT_VIEW (console_log_view (console)),
			      FALSE);
  gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (console_log_view (console)),
			       GTK_WRAP_WORD_CHAR);
  gtk_text_view_set_cursor_visible (GTK_TEXT_VIEW (console_log_view (console)),
				    FALSE);

#if 1
  gtk_container_add (GTK_CONTAINER (sw), console_log_view (console));
#else
  gtk_scrolled_window_add_with_viewport (GTK_SCROLLED_WINDOW (sw),
					 console_log_view (console));
#endif
  gtk_box_pack_start(GTK_BOX(vbox),
                     sw,
                     TRUE,              /* expand */
                     TRUE,              /* fill */
                     0);                /* padding */

  gtk_box_pack_start(GTK_BOX(vbox),
                     entry,
                     FALSE,             /* expand */
                     FALSE,             /* fill */
                     0);                /* padding */

  gtk_widget_show_all (vbox);
}

/********************** public api ***************/

GType
ftk_console_get_type ()
{
  static GType console_type = 0;

  if (!console_type)
    {
      static const GTypeInfo console_info =
      {
	sizeof (FtkConsoleClass),		/* class_size		*/
	NULL, 					/* base_init		*/
        NULL,					/* base_finalize	*/
	(GClassInitFunc) ftk_console_class_init,	/* class_init	*/
        NULL, 					/* class_finalize	*/
	NULL, 					/* class_data		*/
        sizeof (FtkConsole),			/* instance size	*/
	0,					/* n_preallocs		*/
	(GInstanceInitFunc) ftk_console_init,	/* instance_init*/
      };

      console_type = g_type_register_static (GTK_TYPE_VBOX,
					     "Gtk_Console",
					     &console_info, 0);
    }

  return console_type;
}

GtkWidget*
ftk_console_new (void)
{
  gint i;
  
  FtkConsole * stripchart = g_object_new (ftk_console_get_type (), NULL);
  
  return GTK_WIDGET (stripchart);
}

/*
 *
 *	resize widget
 *
 */

gboolean
ftk_console_resize_e (FtkConsole * console,
		      gint width, gint height,
		      GError ** err)
{
  if (!FTK_IS_CONSOLE (console)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_CONSOLE_WIDGET,	/* error code */
		 "Invalid FtkConsole widget.");
    return FALSE;
  }
  
  gtk_widget_set_size_request (GTK_WIDGET (console), width, height);
  return TRUE;
}

gboolean
ftk_console_resize (FtkConsole * console,
		    gint width, gint height)
{
  return ftk_console_resize_e (console,width, height, NULL);
}

gboolean
ftk_console_append_text_e(FtkConsole * console, char * str, GError ** err)
{
  if (!FTK_IS_CONSOLE (console)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_CONSOLE_WIDGET,	/* error code */
		 "Invalid FtkConsole widget.");
    return FALSE;
  }

  gtk_text_buffer_insert_at_cursor (console_log_buffer (console), str, -1);

  if (console_log_view (console)) {
    gtk_text_view_scroll_to_mark (GTK_TEXT_VIEW (console_log_view (console)),
                                  console_log_eob_mark (console),
                                  0.0,
                                  TRUE,
                                  1.0,
                                  1.0);
  }
}

gboolean
ftk_console_append_text(FtkConsole * console, char * str)
{
  return ftk_console_append_text_e(console, str, NULL);
}

const char *
ftk_console_read_entry_e(FtkConsole * console, gboolean clear, GError ** err)
{
  if (!FTK_IS_CONSOLE (console)) {
    g_set_error (err,
		 ftk_quark,				/* error domain */
		 FTK_ERROR_INVALID_CONSOLE_WIDGET,	/* error code */
		 "Invalid FtkConsole widget.");
    return NULL;
  }

  if (console_entry_text (console)) free(console_entry_text (console));
  console_entry_text (console) = strdup(gtk_entry_get_text (console_entry (console)));

  if (TRUE == clear)  gtk_entry_set_text (console_entry (console), "");
  
  return console_entry_text (console);
}

const char *
ftk_console_read_entry(FtkConsole * console, gboolean clear)
{
  return ftk_console_read_entry_e(console, clear, NULL);
}
