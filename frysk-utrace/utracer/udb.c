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
#include "udb-i386.h"

pthread_t resp_listener_thread;

extern void * resp_listener (void * arg);

static void
cleanup_udb()
{
  int rc;
  void * rc_ptr;

  rc = pthread_cancel (resp_listener_thread);
  if (0 != rc) perror ("pthread_cancel");

  rc = pthread_join(resp_listener_thread, &rc_ptr);
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
static int default_quiesce = 1;

static struct option options[] = {
  {"watch",		required_argument, NULL, (int)'w'},
  {"attach",		required_argument, NULL, (int)'a'},
  {"module",		required_argument, NULL, (int)'m'},
  {"load",		required_argument, NULL, (int)'l'},
  {"run",		required_argument, NULL, (int)'r'},
  {"cmd",		required_argument, NULL, (int)'c'},
  {"default-quiesce",	no_argument, &default_quiesce, 1},
  {"default-run",	no_argument, &default_quiesce, 0},
  {"no-unload",		no_argument, &unload_module, 0},
  {NULL,0, NULL, 0}
};

typedef struct {
  long pid;
  long quiesce;
} pta_s;

typedef struct {
  char * cmd;
  long quiesce;
} ptc_s;
static pta_s * pids_to_attach = NULL;
static int nr_pids_to_attach = 0;
static int max_pids_to_attach = 0;
static ptc_s * runnables_to_attach = NULL;
static int nr_runnables_to_attach = 0;
static int max_runnables_to_attach = 0;
#define PIDS_TO_ATTACH_INCR	4

static void
append_runnable (char * oa, long quiesce)
{
  if (max_runnables_to_attach <= nr_runnables_to_attach) {
    max_runnables_to_attach += PIDS_TO_ATTACH_INCR;
    runnables_to_attach = realloc (runnables_to_attach,
			      max_runnables_to_attach * sizeof(ptc_s));
  }
  runnables_to_attach[nr_runnables_to_attach].quiesce = quiesce;
  runnables_to_attach[nr_runnables_to_attach++].cmd = strdup (oa);
}

static void
append_cmd (char * cmd)
{
  if (cl_cmds_max <= cl_cmds_next) {
    cl_cmds_max += CMDS_INCR;
    cl_cmds = realloc (cl_cmds, cl_cmds_max * sizeof(char *));
  }
  cl_cmds[cl_cmds_next++] = strdup (cmd);
}

main (int ac, char * av[])
{

  udb_pid = getpid();

  signal (SIGHUP,  sigterm_handler);
  signal (SIGTERM, sigterm_handler);

  {
    int run = 1;
    while (1 == run) {
      int val = getopt_long (ac, av, "a:m:w:l:r:c:", options, NULL);
      switch (val) {
      case -1:
	run = 0;
	break;
      case 'c':
	append_cmd (optarg);
	break;
      case 'm':
	if (optarg) {
	  if (module_name) free (module_name);
	  module_name = strdup (optarg);
	}
	break;
      case 'a':
      case 'w':
	if (optarg) {
	  if (max_pids_to_attach <= nr_pids_to_attach) {
	    max_pids_to_attach += PIDS_TO_ATTACH_INCR;
	    pids_to_attach = realloc (pids_to_attach,
				      max_pids_to_attach * sizeof(pta_s));
	  }
	  pids_to_attach[nr_pids_to_attach].quiesce =
	    ('a' == val) ? 1 : 0;
	  pids_to_attach[nr_pids_to_attach++].pid = atol (optarg);
	}
	break;
      case 'r':		// execlp the given pgm and run
      case 'l':		// execlp the given pgm and quiesce
	if (optarg) append_runnable (optarg, ('l' == val) ? 1 : 0);
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

  for (;optind < ac; optind++) append_runnable (av[optind], default_quiesce);

  //fixme -- some kind of path?
  if (!module_name) module_name = strdup ("utracer/utracer");

  if (!utracer_loaded()) load_utracer();
  register_utracer (udb_pid);

  {
    char * cfn;
    
    asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	      udb_pid, UTRACER_CMD_FN);
    utracer_cmd_file_fd = open (cfn, O_WRONLY);
    free (cfn);
    if (-1 == utracer_cmd_file_fd) {
      unregister_utracer (udb_pid);
      close (ctl_file_fd);
      error (1, errno, "Error opening control file");
    }
    
    asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	      udb_pid, UTRACER_RESP_FN);
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
      utrace_attach_if (pids_to_attach[i].pid,
			pids_to_attach[i].quiesce, 0);
    free (pids_to_attach);
  }
  
  {
    int i;

    for (i = 0; i < nr_runnables_to_attach; i++) {
      pid_t child_pid;
    
      child_pid = fork();
      switch (child_pid) {
      case -1:
	error (1, errno, "Error forking spawner");
	break;
      case 0:       // child
	{
	  int rc;
	  long cp = (long)getpid();
	  char * cl_copy = strdup (runnables_to_attach[i].cmd);
	  char * tok = strtok (cl_copy, " \t");
	  char ** args = NULL;
	  int args_next = 0;
	  int args_max  = 0;
#define ARGS_INCR 4
	  
	  utrace_attach_if (cp, 0, runnables_to_attach[i].quiesce);

	  while (tok) {
	    if (args_max >= args_next) {
	      args_max += ARGS_INCR;
	      args = realloc (args, ARGS_INCR * sizeof(char *));
	    }
	    args[args_next++] = tok;
	    tok = strtok (NULL, " \t");
	  }
	  if (args_max >= args_next) {
	    args_max += ARGS_INCR;
	    args = realloc (args, ARGS_INCR * sizeof(char *));
	  }
	  args[args_next++] = NULL;

	  rc = execvp (args[0], args);

	  if (args) free (args);
	  if (cl_copy) free (cl_copy);
	  
	  if (-1 == rc)
	    error (1, errno, "Error in spawner execlp");
	}
	break;
      default:      // parent
	// fixme -- record pid somewhere?
	break;
      }
    }
  }
   
  utrace_sync_if (SYNC_INIT);

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
