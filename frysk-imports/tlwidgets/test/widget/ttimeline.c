#include <stdlib.h>
#define  _GNU_SOURCE
#include <math.h>
#include <gtk/gtk.h>
#include "ftktimeline.h"


int main( int   argc,
          char *argv[] )
{
  GtkWidget *window;
  GtkWidget *vbox;
  GtkWidget *frame;
  GtkWidget *timeline1;
  GtkWidget *timeline2;
  gboolean rc;
  
  gtk_init (&argc, &argv);

  window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
  gtk_signal_connect(GTK_OBJECT(window),
                     "destroy",
                     GTK_SIGNAL_FUNC(gtk_main_quit),
                     NULL);
  
  gtk_window_set_title (GTK_WINDOW (window), "Frysk Timeline");
  
  g_signal_connect (G_OBJECT (window), "destroy",
		    G_CALLBACK (exit), NULL);
  
  gtk_container_set_border_width (GTK_CONTAINER (window), 10);

  vbox = gtk_vbox_new(FALSE, 10);
  gtk_container_add(GTK_CONTAINER(window), vbox);
  gtk_widget_show(vbox);


  /*********************** timeline 1 *********************/
  
  /* ftk_timeline_new (ntraces);						*/
  /* returns an empty timeline widget						*/
  
  timeline1 = ftk_timeline_new (4);
#if 0 /* removed */
  ftk_timeline_set_label (FTK_TIMELINE (timeline1), "curve 1");
#endif
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline1), 0, "sin(theta)");
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline1), 1, "sin(theta-pi/2)");
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline1), 2, "sum");

  /* ftk_timeline_set_trace_label() and ftk_timeline_append_point_*() all return FALSE */
  /* if they fail...								*/
  
  rc = ftk_timeline_set_trace_label (FTK_TIMELINE (timeline1), 3, "product");
  if (FALSE == rc) fprintf (stderr,"Warning: failed tl op\n");

  /* ... like if you try to set a trace bigger than you've allocated in ftk_timeline_new */
  
  rc = ftk_timeline_set_trace_label (FTK_TIMELINE (timeline1), 4, "dummy");
  if (FALSE == rc) fprintf (stderr,"Warning: failed tl op\n");

  gtk_widget_show (timeline1);
  frame = gtk_frame_new ("A frame for TL 1");
  gtk_container_add (GTK_CONTAINER(frame), timeline1);
  gtk_widget_show (frame);

  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);



  /*********************** timeline 2 *********************/
  
  timeline2 = ftk_timeline_new (4);
#if 0 /* removed */
  ftk_timeline_set_label (FTK_TIMELINE (timeline2), "curve 2");
#endif
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline2), 0, "cos(theta)");
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline2), 1, "cos(theta-pi/2)");
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline2), 2, "pid");
  ftk_timeline_set_trace_label (FTK_TIMELINE (timeline2), 3, "int");
  gtk_widget_show (timeline2);
  frame = gtk_frame_new ("A frame for TL 2");
  gtk_container_add (GTK_CONTAINER(frame), timeline2);
  gtk_widget_show (frame);

  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);

  {
#define NR_POINTS 32
   double theta;

   for (theta = 0.0; theta < 5.0 * M_PI; theta += (2.0 * M_PI)/NR_POINTS) {
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline1), 0, theta+.05, sin (theta+.05));
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline1), 1, theta+.1, sin ((theta+.1)-(M_PI/2.0)));
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline1), 2, theta+.15, sin (theta) + sin ((theta+.15)
								 		       -(M_PI/2.0)));
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline1), 3, theta, sin (theta+.2) * sin ((theta+.2)
								 		      -(M_PI/2.0)));
     
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline2), 0, theta+.05, cos (3*theta+.05));
     ftk_timeline_append_point_dbl (FTK_TIMELINE (timeline2), 1, theta+.1, cos ((3*theta+.1)-(M_PI/2.0)));
#if 0
     ftk_timeline_append_point_pid (FTK_TIMELINE (timeline2), 2, theta+.15,
				    8000 + lrint (20 * cos (3*theta) + sin ((theta+.15) -(M_PI/2.0))));
#endif
     ftk_timeline_append_point_int (FTK_TIMELINE (timeline2), 3, theta,
				    8 + lrint (2.0 + (cos (3*theta+.25) * cos ((theta+.2) -(M_PI/2.0)))));
   }
   ftk_timeline_append_point_pid (FTK_TIMELINE (timeline2), 2,  0.0, 8000);
   ftk_timeline_append_point_pid (FTK_TIMELINE (timeline2), 2,  3.0, 8006);
   ftk_timeline_append_point_pid (FTK_TIMELINE (timeline2), 2,  5.0, 8009);
   ftk_timeline_append_point_pid (FTK_TIMELINE (timeline2), 2, 11.0, 8045);
				  
 }

  gtk_widget_show (window);
  
  gtk_main ();
  
  return 0;
}
