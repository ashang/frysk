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

GtkWidget * eventviewer1;
GtkWidget * eventviewer2;

gint clone_tie;
gint term_tie;

FtkEventViewer * thread_viewer;
FtkEventViewer * event_viewer;

GdkColor bg_color;
GdkColor bg_color1;

typedef struct {
  gint marker_id;
  gint trace_id;
  char * label;
  char * string;
  char * tstring;
  GdkColor * color;
} plot_parms_s;

static plot_parms_s thread_parms[] = {
  {0, 0, "Parent",	"The parent process.",	"Mama proc"},
  {0, 0, "Thread 0",	NULL,			"Kid 0"},
  {0, 0, "Thread 1",	NULL,			"Kid 1"},
  {0, 0, "Thread 2",	NULL,			"Kid 2"},
};

enum {
  IDX_PARENT,
  IDX_THREAD0,
  IDX_THREAD1,
  IDX_THREAD2
};

static int threads_count = sizeof(thread_parms)/sizeof(plot_parms_s);

static plot_parms_s event_parms[] = {
  {0, 0, "clone",	"A clone event.",	"Bring in the clones."},
  {0, 0, "event A",	NULL},
  {0, 0, "event B",	NULL},
  {0, 0, "event C",	NULL},
  {0, 0, "term",	"A termination event.",	"Die!"},
};

enum {
  IDX_CLONE,
  IDX_EVENTA,
  IDX_EVENTB,
  IDX_EVENTC,
  IDX_TERM
};

static int events_count = sizeof(event_parms)/sizeof(plot_parms_s);

int max_event_nr;

int events_added = 1;
int sigs_sent = 0;
#define MAX_SIGS_SENT 30

GdkColor clone_tie_color	= {0, 65535, 32768, 32769};
GdkColor term_tie_color		= {0, 0, 32760, 32769};

static void
catch_sigalrm (int sig)
{
  GError * err = NULL;
  int rt = lrint (drand48() * 32.0);
  int et = lrint (drand48() * 15.0);

  if (NULL == eventviewer1) return;
  
#if 0
  fprintf (stderr, "catch_sigalrm %d\n", sigs_sent);
#endif


  if (sigs_sent++ >= MAX_SIGS_SENT)  {
    if (events_added) {
      fprintf (stderr, "stopping\n");
      struct itimerval value;
      struct itimerval ovalue;
      int clone_term_event;
      int parent_term_event;


      events_added = 0;
  
    
      clone_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_THREAD0].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 0");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_THREAD0].marker_id,
				    "termination of thread 0");
    
      usleep (125000);
      parent_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_PARENT].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 0");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_PARENT].marker_id,
				    "termination of thread 0");

      ftk_eventviewer_tie_events (thread_viewer,
				  term_tie,
				  clone_term_event,
				  parent_term_event,
				  -1);
      usleep (250000);
  
      clone_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_THREAD1].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 1");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_THREAD1].marker_id,
				    "termination of thread 1");
    
      usleep (125000);
      parent_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_PARENT].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 1");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_PARENT].marker_id,
				    "termination of thread 1");

      ftk_eventviewer_tie_events (thread_viewer,
				  term_tie,
				  clone_term_event,
				  parent_term_event,
				  -1);
      usleep (250000);
    
      clone_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_THREAD2].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 2");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_THREAD2].marker_id,
				    "termination of thread 2");
      usleep (125000);
      parent_term_event =
	ftk_eventviewer_append_event (thread_viewer,
				      thread_parms[IDX_PARENT].trace_id,
				      event_parms[IDX_TERM].marker_id,
				      "termination of thread 2");
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_PARENT].marker_id,
				    "termination of thread 2");


      ftk_eventviewer_tie_events (thread_viewer,
				  term_tie,
				  clone_term_event,
				  parent_term_event,
				  -1);
      sleep (1);
      ftk_eventviewer_append_event (thread_viewer,
				    thread_parms[IDX_PARENT].trace_id,
				    event_parms[IDX_TERM].marker_id,
				    "termination of parent");
  
      ftk_eventviewer_append_event (event_viewer,
				    event_parms[IDX_TERM].trace_id,
				    thread_parms[IDX_PARENT].marker_id,
				    "termination of parent");



      

      signal (SIGALRM, catch_sigalrm);

      value.it_interval.tv_sec = 0;
      value.it_interval.tv_usec = 0;
      value.it_value.tv_sec = 0;
      value.it_value.tv_usec = 0;
      setitimer (ITIMER_REAL, &value, &ovalue);
      return;
    }
#if 1
    else {
      fprintf (stderr, "changing state\n");
      sigs_sent = -200;
      events_added = 1;
      ftk_eventviewer_set_bg_rgb (thread_viewer, 65535, 65535,  65535);
#if 0
      ftk_eventviewer_set_trace_rgb (FTK_EVENTVIEWER (eventviewer1),
				     traces[2],
				     65535, 28000,  0);
      ftk_eventviewer_set_trace_label (FTK_EVENTVIEWER (eventviewer1),
				       traces[1],
				       LABEL_TERM);

      /* width in pixels.  a width <= 0 implies default width. values 1 - 8 work ok */
      /* style is dash spec */
      ftk_eventviewer_set_trace_linestyle (FTK_EVENTVIEWER (eventviewer1),
					   traces[1],
					   3, 3);
#endif
      
      max_event_nr = 5;
    }
#endif
  }

  if (et <= max_event_nr) {
    int mi = 1 + (rt & 3);
    if (mi < 4) {
      
      int ti = (rt >> 2) & 3;
      ftk_eventviewer_append_event_e (FTK_EVENTVIEWER (eventviewer1),
				      thread_parms[ti].trace_id,
				      event_parms[mi].marker_id,
				      NULL,
				      &err);
      if (NULL != err) {
	fprintf (stderr, "Unable to append event: %s\n", err->message);
	g_error_free (err);
      }
      ftk_eventviewer_append_event_e (FTK_EVENTVIEWER (eventviewer2),
				      event_parms[mi].trace_id,
				      thread_parms[ti].marker_id,
				      NULL,
				      &err);
      if (NULL != err) {
	fprintf (stderr, "Unable to append event: %s\n", err->message);
	g_error_free (err);
      }
    }
  }
  signal (SIGALRM, catch_sigalrm);
}

static void
quit_cb (GtkButton * button, gpointer user_data)
{
  gtk_widget_destroy (eventviewer2);
  eventviewer2 = NULL;
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

#if 0
  {
    GdkColor color;

    if (gdk_color_parse ("purple", &color)) {
      fprintf (stderr, "purple = %#08x [%d %d %d]\n",
	       (int)color.pixel,
	       (int)color.red,
	       (int)color.green,
	       (int)color.blue);
    }
    else fprintf (stderr, "purple not found\n");
  }
#endif
  
  gdk_color_parse ("black", &bg_color);
  gdk_color_parse ("lightblue", &bg_color1);
  
  g_signal_connect (G_OBJECT (window), "destroy",
		    G_CALLBACK (exit), NULL);
  
  gtk_container_set_border_width (GTK_CONTAINER (window), 10);

  vbox = gtk_vbox_new(FALSE, 10);
  gtk_container_add(GTK_CONTAINER(window), vbox);
  gtk_widget_show(vbox);


  eventviewer1 = ftk_eventviewer_new ();
  //  ftk_eventviewer_resize (FTK_EVENTVIEWER (eventviewer1), 500, 150);
  gtk_widget_show (eventviewer1);

  eventviewer2 = ftk_eventviewer_new ();
  //  ftk_eventviewer_resize (FTK_EVENTVIEWER (eventviewer2), 500, 175);
  gtk_widget_show (eventviewer2);
  
  thread_viewer = FTK_EVENTVIEWER (eventviewer1);
  event_viewer  = FTK_EVENTVIEWER (eventviewer2);

  /* if not set, default is provided */
  ftk_eventviewer_set_bg_color (thread_viewer, &bg_color1);
  ftk_eventviewer_set_bg_color (event_viewer, &bg_color);
  
  ftk_eventviewer_set_timebase (thread_viewer, 20.0);
  ftk_eventviewer_set_timebase (event_viewer, 20.0);

  

  {
    int i;

    for (i = 0; i < events_count; i++) {		/* for every event type... */
      event_parms[i].marker_id =			/* create a marker in the thread view */
	ftk_eventviewer_marker_new (thread_viewer,	/* using auto glyph and color assgn   */
				    FTK_GLYPH_AUTOMATIC,
				    event_parms[i].label,
				    event_parms[i].string);
      event_parms[i].color =				/* retrieve the color      */
	ftk_eventviewer_get_marker_color (thread_viewer,
					  event_parms[i].marker_id);
      event_parms[i].trace_id =				/* create a trace in the event view   */
	ftk_eventviewer_add_trace (event_viewer, event_parms[i].label, event_parms[i].tstring);
      ftk_eventviewer_set_trace_color (event_viewer,
				       event_parms[i].trace_id,
				       event_parms[i].color);
    }
    
    for (i = 0; i < threads_count; i++) {		/* for every thread type... */
      thread_parms[i].marker_id =			/* create a marker in the event view */
	ftk_eventviewer_marker_new (event_viewer,
				    FTK_GLYPH_AUTOMATIC,
				    thread_parms[i].label,
				    thread_parms[i].string);
      thread_parms[i].color =				/* retrieve the color      */
	ftk_eventviewer_get_marker_color (event_viewer,
					  thread_parms[i].marker_id);
      thread_parms[i].trace_id =			/* create a trace in the thread view  */
	ftk_eventviewer_add_trace (thread_viewer, thread_parms[i].label, thread_parms[i].tstring);
      ftk_eventviewer_set_trace_color (thread_viewer,
				       thread_parms[i].trace_id,
				       thread_parms[i].color);
    }
  }

  /* just for the fun of it, change the linewidth and linestyle of some trace */
  /* width in pixels.  a width <= 0 implies default width. values 1 - 8 work ok */
  /* style is dash spec */
  ftk_eventviewer_set_trace_linestyle (thread_viewer,
				       thread_parms[IDX_PARENT].trace_id,
				       5,			/* width */
				       5);	/* style */
  
#ifdef USE_TIE_LABEL
  clone_tie = ftk_eventviewer_tie_new (thread_viewer, "clone");
#else
  clone_tie = ftk_eventviewer_tie_new (thread_viewer);
  term_tie = ftk_eventviewer_tie_new (thread_viewer);
#endif
  ftk_eventviewer_set_tie_color (thread_viewer, clone_tie, &clone_tie_color);
  ftk_eventviewer_set_tie_color (thread_viewer, term_tie,  &term_tie_color);
  ftk_eventviewer_set_tie_linestyle (thread_viewer, term_tie,  2, 2);

  ftk_eventviewer_append_simultaneous_events (thread_viewer,
					      clone_tie,
					      thread_parms[IDX_PARENT].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "a simultaneous w/ thread 0 clone",
					      thread_parms[IDX_THREAD0].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "b simultaneous w/ parent clone",
					      -1);

  ftk_eventviewer_append_simultaneous_events (event_viewer,
					      -1,
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_PARENT].marker_id,
					      "c simultaneous w/ thread 0 clone",
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_THREAD0].marker_id,
					      "d simultaneous w/ parent clone",
					      -1);
    
  usleep (250000);
  
  ftk_eventviewer_append_simultaneous_events (thread_viewer,
					      clone_tie,
					      thread_parms[IDX_PARENT].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "e simultaneous w/ thread 1\nand thread 2 clones",
					      thread_parms[IDX_THREAD2].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "f simultaneous w/ parent\nand thread 1 clones",
					      thread_parms[IDX_THREAD1].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "g simultaneous w/ parent\nand thread 2 clones",
					      -1);
  
  ftk_eventviewer_append_simultaneous_events (event_viewer,
					      -1,
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_PARENT].marker_id,
					      "h simultaneous w/ thread 1 clone",
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_THREAD1].marker_id,
					      "i simultaneous w/ parent clone",
					      -1);
    
  usleep (250000);
  
  ftk_eventviewer_append_simultaneous_events (thread_viewer,
					      clone_tie,
					      thread_parms[IDX_PARENT].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "j simultaneous w/ thread 2 clone",
					      thread_parms[IDX_THREAD2].trace_id,
					      event_parms[IDX_CLONE].marker_id,
					      "k simultaneous w/ parent clone",
					      -1);
  
  ftk_eventviewer_append_simultaneous_events (event_viewer,
					      -1,
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_PARENT].marker_id,
					      "l simultaneous w/ thread 2 clone",
					      event_parms[IDX_CLONE].trace_id,
					      thread_parms[IDX_THREAD2].marker_id,
					      "m simultaneous w/ parent clone",
					      -1);
    
  usleep (250000);


    
#if 0
  ftk_eventviewer_resize (thread_viewer, 600, 100);
#endif

  max_event_nr = 4;

  /* _e suffixed versions of the fcns tell you if something screwed up */
  err = NULL;
  if (NULL != err) {
    fprintf (stderr, "Unable to set update: %s\n", err->message);
    g_error_free (err);
    exit (1);
  }
    
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
