/* chlm */
#include <stdlib.h>
#define  _GNU_SOURCE
#include <math.h>
#include <signal.h>
#include <time.h>
#include <sys/types.h>
#include <sys/time.h>
#include <unistd.h>
#include <gtk/gtk.h>
#include <ftkeventviewer.h>

GtkWidget *eventviewer1;
GtkWidget *eventviewer2;
gint event_markers[5];
gint thread_traces[4];

gint thread_markers[4];
gint event_traces[5];

int max_event_nr;

int events_added = 1;
int sigs_sent = 0;
#define MAX_SIGS_SENT 30

static void
catch_sigalrm (int sig)
{
  GError * err = NULL;
  int rt = lrint (drand48() * 32.0);
  int et = lrint (drand48() * 15.0);

  if (NULL == eventviewer1) return;
  
#if 1
  fprintf (stderr, "catch_sigalrm %d\n", sigs_sent);
#endif


  if (sigs_sent++ >= MAX_SIGS_SENT)  {
    if (events_added) {
      fprintf (stderr, "stopping\n");
      struct itimerval value;
      struct itimerval ovalue;


      events_added = 0;
  
    
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[1],
				event_markers[4]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[4],
				thread_markers[1]);
    
  usleep (250000);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[2],
				event_markers[4]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[4],
				thread_markers[2]);
    
  usleep (250000);
    
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[3],
				event_markers[4]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[4],
				thread_markers[3]);

  sleep (1);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[0],
				event_markers[4]);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[4],
				thread_markers[0]);



      

      signal (SIGALRM, catch_sigalrm);

      value.it_interval.tv_sec = 0;
      value.it_interval.tv_usec = 0;
      value.it_value.tv_sec = 0;
      value.it_value.tv_usec = 0;
      setitimer (ITIMER_REAL, &value, &ovalue);
      return;
    }
#if 0
    else {
      fprintf (stderr, "changing state\n");
      sigs_sent = -200;
      events_added = 1;
      ftk_eventviewer_set_bg_rgb (FTK_EVENTVIEWER (eventviewer1),
				   28000, 28000,  28000);
      ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				     traces[2],
				     65535, 28000,  0);
      ftk_eventviewer_set_trace_label (FTK_EVENTVIEWER (eventviewer1),
				       traces[1],
				       "term");

      /* may be GDK_LINE_SOLID, GDK_LINE_ON_OFF_DASH, GDK_LINE_DOUBLE_DASH */
      ftk_eventviewer_set_trace_linestyle (FTK_EVENTVIEWER (eventviewer1),
					   traces[1],
					   3, GDK_LINE_DOUBLE_DASH);
      
      max_event_nr = 5;
    }
#endif
  }

  if (et <= max_event_nr) {
    int mi = 1 + (rt & 3);
    if (mi < 4) {
      int ti = (rt >> 2) & 3;
      ftk_eventviewer_append_event_e (FTK_EVENTVIEWER (eventviewer1),
				      thread_traces[ti],
				      event_markers[mi],
				      &err);
      ftk_eventviewer_append_event_e (FTK_EVENTVIEWER (eventviewer2),
				      event_traces[mi],
				      thread_markers[ti],
				      &err);
      if (NULL != err) {
	fprintf (stderr, "Unable to append event: %s\n", err->message);
	g_error_free (err);
	exit (1);
      }
    }
  }
  signal (SIGALRM, catch_sigalrm);
}

static void
quit_cb (GtkButton * button, gpointer user_data)
{
  fprintf (stderr, "clicked\n");
  gtk_widget_destroy (GTK_WIDGET (eventviewer1));
  eventviewer1 = NULL;
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
  
  gtk_window_set_title (GTK_WINDOW (window), "Frysk Eventviewer");
  
  g_signal_connect (G_OBJECT (window), "destroy",
		    G_CALLBACK (exit), NULL);
  
  gtk_container_set_border_width (GTK_CONTAINER (window), 10);

  vbox = gtk_vbox_new(FALSE, 10);
  gtk_container_add(GTK_CONTAINER(window), vbox);
  gtk_widget_show(vbox);


  eventviewer1 = ftk_eventviewer_new ();
  gtk_widget_show (eventviewer1);

  eventviewer2 = ftk_eventviewer_new ();
  gtk_widget_show (eventviewer2);

  /* if not set, default is provided */
  //  ftk_eventviewer_set_bg_rgb (FTK_EVENTVIEWER (eventviewer1),
  //  			      28000, 28000,  65535);
  
  ftk_eventviewer_set_timebase (FTK_EVENTVIEWER (eventviewer1), 20.0);
  ftk_eventviewer_set_timebase (FTK_EVENTVIEWER (eventviewer2), 20.0);
			      
  thread_traces[0] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer1), "parent");
      /* may be GDK_LINE_SOLID, GDK_LINE_ON_OFF_DASH, GDK_LINE_DOUBLE_DASH */
  ftk_eventviewer_set_trace_linestyle (FTK_EVENTVIEWER (eventviewer1),
				       thread_traces[0],
				       2, GDK_LINE_DOUBLE_DASH);
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				 thread_traces[0],
				 65535, 28000,  65535);
  thread_traces[1] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer1), "thread 0");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				 thread_traces[1],
				 65535, 0,  0);
  thread_traces[2] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer1), "thread 1");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				 thread_traces[2],
				 0, 65535,  0);
  thread_traces[3] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer1), "thread 2");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				 thread_traces[3],
				 0, 0,  65535);
  
  event_traces[0] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer2), "clone");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer2),
				  event_traces[0],
				  0, 65535,  0);
  event_traces[1] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer2), "event a");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer2),
				  event_traces[1],
				  0, 0,  65535);
  event_traces[2] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer2), "event b");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer2),
				  event_traces[2],
				  65535, 65535,  65535);
  event_traces[3] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer2), "event c");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer2),
				  event_traces[3],
				  0, 65535,  65535);
  event_traces[4] =
    ftk_eventviewer_add_trace (FTK_EVENTVIEWER (eventviewer2), "term");
  ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer2),
				  event_traces[4],
				  65535, 0,  0);

  event_markers[0] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer1),
				     FTK_GLYPH_FILLED_CIRCLE,
				     "clone");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer1),
				  event_markers[0],
				  0, 65535,  0);
  event_markers[1] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer1),
				     FTK_GLYPH_OPEN_CIRCLE,
				     "event a");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer1),
				  event_markers[1],
				  0, 0,  65535);

  event_markers[2] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer1),
				     FTK_GLYPH_FILLED_SQUARE,
				     "event b");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer1),
				  event_markers[2],
				  65535, 65535,  65535);
  event_markers[3] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer1),
				     FTK_GLYPH_OPEN_SQUARE,
				     "event c");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer1),
				  event_markers[3],
				  0, 65535,  65535);
  event_markers[4] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer1),
				     FTK_GLYPH_OPEN_SQUARE,
				     "term");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer1),
				  event_markers[4],
				  65535, 0,  0);

  thread_markers[0] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer2),
				      FTK_GLYPH_FILLED_CIRCLE,
				      "parent");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer2),
				  thread_markers[0],
				  65535, 28000,  65535);

  thread_markers[1] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer2),
				      FTK_GLYPH_OPEN_CIRCLE,
				      "thread 0");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer2),
				  thread_markers[1],
				  65535, 0,  0);

  thread_markers[2] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer2),
				      FTK_GLYPH_OPEN_CIRCLE,
				      "thread 1");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer2),
				  thread_markers[2],
				  0, 65535,  0);

  thread_markers[3] = ftk_marker_new (FTK_EVENTVIEWER (eventviewer2),
				      FTK_GLYPH_OPEN_CIRCLE,
				      "thread 2");
  ftk_eventviewer_set_marker_rgb (FTK_EVENTVIEWER (eventviewer2),
				  thread_markers[3],
				  0, 0,  65535);



  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[0],
				event_markers[0]);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[0]);
    
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[1],
				event_markers[0]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[1]);
    
  usleep (250000);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[0],
				event_markers[0]);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[0]);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[2],
				event_markers[0]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[2]);
    
  usleep (250000);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[0],
				event_markers[0]);
  
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[0]);
    
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer1),
				thread_traces[3],
				event_markers[0]);
  ftk_eventviewer_append_event (FTK_EVENTVIEWER (eventviewer2),
				event_traces[0],
				thread_markers[3]);
    
  usleep (250000);


    
#if 0
  ftk_eventviewer_resize (FTK_EVENTVIEWER (eventviewer1), 600, 100);
#endif

  max_event_nr = 4;

  /* _e suffixed versions of the fcns tell you if something screwed up */
  err = NULL;
#if 0
  ftk_eventviewerx_set_update_e (FTK_EVENTVIEWERX (eventviewer1),
			       1111, &err); /* ms bin width */
#endif
  if (NULL != err) {
    fprintf (stderr, "Unable to set update: %s\n", err->message);
    g_error_free (err);
    exit (1);
  }
    
  /* in addition, if you don't care about the details, all */
  /* fcns return gboolean TRUE on success and FALSE on failure */
#if 0
  rc = ftk_eventviewerx_set_range     (FTK_EVENTVIEWERX (eventviewer1),
				     60000); /* ms display width */
#endif
  if (FALSE == rc) exit (2);

  frame = gtk_frame_new ("Events by thread");
  gtk_container_add (GTK_CONTAINER(frame), eventviewer1);
  gtk_widget_show (frame);
  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);

  frame = gtk_frame_new ("Threads by event");
  gtk_container_add (GTK_CONTAINER(frame), eventviewer2);
  gtk_widget_show (frame);
  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);
  
  {
    GtkWidget * hbutton_box = gtk_hbutton_box_new();
    GtkWidget * quit_button
      = gtk_button_new_with_mnemonic ("_Quit");

    g_signal_connect (GTK_OBJECT(quit_button),"clicked",
                      (GtkSignalFunc) quit_cb, NULL);
    gtk_container_add (GTK_CONTAINER (hbutton_box), quit_button);
    gtk_widget_show_all (hbutton_box);

    gtk_box_pack_start (GTK_BOX (vbox), hbutton_box, FALSE, FALSE, 0);
  }

  gtk_widget_show (window);

  {
    struct itimerval value;
    struct itimerval ovalue;

    signal (SIGALRM, catch_sigalrm);

    value.it_interval.tv_sec = 0;
    value.it_interval.tv_usec = 500000;
    value.it_value.tv_sec = 0;
    value.it_value.tv_usec = 500000;
    setitimer (ITIMER_REAL, &value, &ovalue);
  }

  gtk_main ();
  
  return 0;
}
