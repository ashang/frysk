#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <error.h>
#include <errno.h>
#include <alloca.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>
#include <getopt.h>
#include <malloc.h>
#include <signal.h>
#include <sys/types.h>
#include <fcntl.h>
#include <pthread.h>

#include "utracer/utracer.h"
#define DO_UDB_INIT
#include "udb.h"

pthread_t resp_listener_thread;

static void
cleanup_udb()
{
  int rc = pthread_cancel (resp_listener_thread);
  if (0 != rc) perror ("pthread_cancel");
  
  if (-1 != utracer_cmd_file_fd) {
    close (utracer_cmd_file_fd);
    utracer_cmd_file_fd = -1;
  }
  if (-1 != utracer_resp_file_fd) {
    close (utracer_resp_file_fd);
    utracer_resp_file_fd = -1;
  }
}

static void
sigterm_handler (int sig)
{
  cleanup_udb();		// fixme -- eventually, just unregister
  unload_utracer();		// and have utracer unload itsef 
  exit (0);			// when nothing else is registered
}

static int unload_module = 1;

static struct option options[] = {
  {"attach",    required_argument, NULL, (int)'a'},
  {"module",    required_argument, NULL, (int)'m'},
  {"no-unload", no_argument, &unload_module, 0},
  {NULL,0, NULL, 0}
};

static void *
resp_listener (void * arg)
{
  if_resp_u if_resp;
  ssize_t sz;

  while (1) {
    sz = pread (utracer_resp_file_fd, &if_resp,
		sizeof(if_resp), 0);

    switch (if_resp.type) {
    case IF_RESP_SIGNAL_DATA:
      {
	signal_resp_s signal_resp = if_resp.signal_resp;
	fprintf (stdout, "\tsignal %ld\n",
		 signal_resp.signal);
      }
      break;
    case IF_RESP_REG_DATA:
      {
	readreg_resp_s readreg_resp = if_resp.readreg_resp;
	fprintf (stdout, "\t[%d][%d]: %d [%#08x]\n",
		 readreg_resp.regset,
		 readreg_resp.which,
		 (int)readreg_resp.data,
		 (int)readreg_resp.data);
      }
      break;
    case IF_RESP_CLONE_DATA:
      {
	clone_resp_s clone_resp = if_resp.clone_resp;
	fprintf (stdout, "\t%ld cloned to %ld\n",
		 clone_resp.utracing_pid,
		 clone_resp.new_utraced_pid);
      }
      break;
    default:
      break;
    }
  }
}

main (int ac, char * av[])
{
  int * pids_to_attach = NULL;
  int nr_pids_to_attach = 0;
  int max_pids_to_attach = 0;
#define PIDS_TO_ATTACH_INCR	4

  udb_pid = getpid();

  signal (SIGHUP,  sigterm_handler);
  signal (SIGTERM, sigterm_handler);
  
  {
    int run = 1;
    while (1 == run) {
      int val = getopt_long (ac, av, "a:m:", options, NULL);
      switch (val) {
      case -1:
	run = 0;
	break;
      case 'm':
	if (optarg) {
	  if (module_name) free (module_name);
	  module_name = strdup (optarg);
	}
	break;
      case 'a':
	if (optarg) {
	  if (max_pids_to_attach <= nr_pids_to_attach) {
	    max_pids_to_attach += PIDS_TO_ATTACH_INCR;
	    pids_to_attach = realloc (pids_to_attach,
				      max_pids_to_attach * sizeof(int));
	  }
	  pids_to_attach[nr_pids_to_attach++] = atoi (optarg);
	}
	break;
      case 0:		// flags already handled--do nothing
	break;
      case (int)'?':
      default:
	fprintf (stderr, "Unknown option.\n");
	break;
      }
    }
  }
  
  if (!utracer_loaded()) load_utracer();
  register_utracer (udb_pid);

  {
    char * cfn;
    
    asprintf (&cfn, "/proc/%s/cmd_%ld", BASE_DIR, udb_pid);
    utracer_cmd_file_fd = open (cfn, O_WRONLY);
    free (cfn);
    if (-1 == utracer_cmd_file_fd) {
      unregister_utracer (udb_pid);
      close (ctl_file_fd);
      error (1, errno, "Error opening control file");
    }
    
    asprintf (&cfn, "/proc/%s/resp_%ld", BASE_DIR, udb_pid);
    utracer_resp_file_fd = open (cfn, O_RDONLY);
    free (cfn);
    if (-1 == utracer_resp_file_fd) {
      unregister_utracer (udb_pid);
      close (ctl_file_fd);
      close (utracer_cmd_file_fd);
      error (1, errno, "Error opening response file");
    }
  }

  {
    int rc = pthread_create (&resp_listener_thread,
			     NULL,
			     resp_listener,
			     NULL);
    if (rc) {
      close (ctl_file_fd);
      close (utracer_cmd_file_fd);
      close (utracer_resp_file_fd);
      unregister_utracer (udb_pid);
      error (1, errno, "pthread_create() failed");
    }
  }

  if (0 < nr_pids_to_attach) {
    int i;
    for (i = 0; i < nr_pids_to_attach; i++)
      utrace_attach_if (pids_to_attach[i]);
    free (pids_to_attach);
  }

  text_ui_init();
  text_ui();

  cleanup_udb();
  unregister_utracer (udb_pid);
  if (-1 != ctl_file_fd) {
    close (ctl_file_fd);
    ctl_file_fd = -1;
  }
  

  if (unload_module) unload_utracer();
  exit (0);
}
