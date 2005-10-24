#include <stdlib.h>
#define  _GNU_SOURCE
#include <math.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>
#include <gtk/gtk.h>
#include "ftkstripchart.h"

GtkWidget *stripchart1;

static void
catch_sigalrm (int sig)
{
  GError * err = NULL;
  int et = lrint (drand48() * 15.0);

#if 0
  fprintf (stderr, "catch_sigalrm %d\n", et);
#endif
  if ((et >= FTK_STRIPCHART_TYPE_FORK) &&
      (et <= FTK_STRIPCHART_TYPE_TERMINATE)) {
    ftk_stripchart_append_event_e (FTK_STRIPCHART (stripchart1), et, &err);
    if (NULL != err) {
      fprintf (stderr, "Unable to append event: %s\n", err->message);
      g_error_free (err);
      exit (1);
    }
  }
  signal (SIGALRM, catch_sigalrm);
}

int main( int   argc,
          char *argv[] )
{
  GtkWidget *window;
  GtkWidget *vbox;
  GtkWidget *frame;
  gboolean rc;
  GError * err;
  
  gtk_init (&argc, &argv);

  srand48 (time (0));

  window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_signal_connect(GTK_OBJECT(window),
                     "destroy",
                     GTK_SIGNAL_FUNC(gtk_main_quit),
                     NULL);
  
  gtk_window_set_title (GTK_WINDOW (window), "Frysk Stripchart");
  
  g_signal_connect (G_OBJECT (window), "destroy",
		    G_CALLBACK (exit), NULL);
  
  gtk_container_set_border_width (GTK_CONTAINER (window), 10);

  vbox = gtk_vbox_new(FALSE, 10);
  gtk_container_add(GTK_CONTAINER(window), vbox);
  gtk_widget_show(vbox);

  stripchart1 = ftk_stripchart_new ();
  ftk_stripchart_resize (FTK_STRIPCHART (stripchart1), 500, 100);

  /* defaults are provided if you don't want to set this stuff */
  ftk_stripchart_set_event_rgb (FTK_STRIPCHART (stripchart1),
				FTK_STRIPCHART_TYPE_TERMINATE,
				65535, 65535, 0); /* red + green = yellow */
  ftk_stripchart_set_event_title (FTK_STRIPCHART (stripchart1),
				  FTK_STRIPCHART_TYPE_FORK, "Spoon");

  /* _e suffixed versions of the fcns tell you if something screwed up */
  err = NULL;
  ftk_stripchart_set_update_e (FTK_STRIPCHART (stripchart1),
			       1111, &err); /* ms bin width */
  if (NULL != err) {
    fprintf (stderr, "Unable to set update: %s\n", err->message);
    g_error_free (err);
    exit (1);
  }
    
  /* in addition, if you don't care about the details, all */
  /* fcns return gboolean TRUE on success and FALSE on failure */
  rc = ftk_stripchart_set_range     (FTK_STRIPCHART (stripchart1),
				     60000); /* ms display width */
  if (FALSE == rc) exit (2);

  gtk_widget_show (stripchart1);
  frame = gtk_frame_new ("A frame for SC 1");
  gtk_container_add (GTK_CONTAINER(frame), stripchart1);
  gtk_widget_show (frame);

  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);

  gtk_widget_show (window);

  {
    struct itimerval value;
    struct itimerval ovalue;

    signal (SIGALRM, catch_sigalrm);

    value.it_interval.tv_sec = 0;
    value.it_interval.tv_usec = 50000;
    value.it_value.tv_sec = 0;
    value.it_value.tv_usec = 50000;
    setitimer (ITIMER_REAL, &value, &ovalue);
  }

  gtk_main ();
  
  return 0;
}
