/* chlm */
#define  _GNU_SOURCE
#include <stdlib.h>
#include <math.h>
#include <signal.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <gtk/gtk.h>
#include <ftkconsole.h>

GtkWidget *console;
int lcount = 0;

static void
catch_sigalrm (int sig)
{
  if (lcount < 10) {
    char * abfr;
    asprintf (&abfr,"[%3d] another line\n", lcount++);
    ftk_console_append_text (FTK_CONSOLE (console), abfr);
    free (abfr);
    signal (SIGALRM, catch_sigalrm);
  }
}


static void
ftk_console_cb (GtkWidget * widget,
		gpointer data)
{
  const char * istr;
  FILE * pfile;
  
  istr = ftk_console_read_entry (FTK_CONSOLE (console), TRUE);

  if (!strncmp (istr, "quit", strlen ("quit"))) exit (0);

  pfile = popen (istr, "r");
  if ((NULL != pfile) && ((void *)-1 != pfile)) {
#define FBUF_SIZE 4096
    char * fbuf = alloca (FBUF_SIZE);

    while (fgets (fbuf, FBUF_SIZE, pfile)) {
      ftk_console_append_text (FTK_CONSOLE (console), fbuf);
    }

    pclose (pfile);
  }
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



  
  console = ftk_console_new ();
  gtk_signal_connect (GTK_OBJECT (console), "ftkconsole",
		      (GtkSignalFunc) ftk_console_cb, NULL);
  
  ftk_console_resize (FTK_CONSOLE (console), 500, 300);

  ftk_console_append_text (FTK_CONSOLE (console), "a line");
  ftk_console_append_text (FTK_CONSOLE (console), "an lf-terminated line\n");
  ftk_console_append_text (FTK_CONSOLE (console), "another line\n");
  ftk_console_append_text (FTK_CONSOLE (console), "a very long lf-terminated line  with word breaks iuhiuguyg uyguyg uguyg uyguyguyguyg uyguyguyguy uyguyguyguy guyguyguyguy uyguyiguyiguy guyguyguyguy uyguyguyguy guyguy\n");
  ftk_console_append_text (FTK_CONSOLE (console), "a very long lf-terminated line  without word breaks iuhiuguyguyguyguguyguyguyguyguyguyguyguyguyuyguyguyguyguyguyguyguyuyguyiguyiguyguyguyguyguyuyguyguyguyjhguftvyytfyttttttttttttttttttttttttttttttttttttguyguy\n");

  
  gtk_widget_show (console);
  frame = gtk_frame_new ("A frame for Console 1");
  gtk_container_add (GTK_CONTAINER(frame), console);
  gtk_widget_show (frame);

  gtk_box_pack_start_defaults (GTK_BOX (vbox), frame);

  gtk_widget_show (window);


  {
    struct itimerval value;
    struct itimerval ovalue;

    signal (SIGALRM, catch_sigalrm);

    value.it_interval.tv_sec = 0;
    value.it_interval.tv_usec = 1000000;
    value.it_value.tv_sec = 0;
    value.it_value.tv_usec = 500000;
    setitimer (ITIMER_REAL, &value, &ovalue);
  }
  
  

  gtk_main ();
  
  return 0;
}
