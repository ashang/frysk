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
// #include <sys/ioctl.h>
#include <fcntl.h>
#include <pthread.h>

#include "utracer/utracer.h"
#define DO_UDB_INIT
#include "udb.h"
#include "udb-i386.h"

pthread_t resp_listener_thread;

extern void * resp_listener (void * arg);

void
close_ctl_file()
{
  if (-1 != ctl_file_fd) {
    close (ctl_file_fd);
    ctl_file_fd = -1;
  }
}

void
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
#ifdef ENABLE_MODULE_OPS  
  unload_utracer();		// and have utracer unload itsef
#endif
  unregister_utracer (udb_pid);
  close_ctl_file();
  exit (0);			// when nothing else is registered
}

#ifdef ENABLE_MODULE_OPS  
static int unload_module = 1;
#endif
static int default_quiesce = 1;

static struct option options[] = {
  {"watch",		required_argument, NULL, (int)'w'},
  {"attach",		required_argument, NULL, (int)'a'},
  {"load",		required_argument, NULL, (int)'l'},
  {"run",		required_argument, NULL, (int)'r'},
  {"cmd",		required_argument, NULL, (int)'c'},
  {"default-quiesce",	no_argument, &default_quiesce, 1},
  {"default-run",	no_argument, &default_quiesce, 0},
#ifdef ENABLE_MODULE_OPS  
  {"module",		required_argument, NULL, (int)'m'},
  {"no-unload",		no_argument, &unload_module, 0},
#endif
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

#ifdef USE_UTRACER_SYNC
static void
utracer_sync()
{
  int i;
#define CHECKS_NR	3

  for (i = 0; i < CHECKS_NR; i++) {
    if_resp_u if_resp;
    ssize_t sz;
      
    utrace_sync_if (SYNC_INIT);

    sz = pread (utracer_resp_file_fd, &if_resp,
		  sizeof(if_resp), 0);
    if (-1 == sz) {
      uerror ("Response pread.");
      // fixme -- close things
      _exit (4);
    }

    if (IF_RESP_SYNC_DATA == if_resp.type) {
      fprintf (stdout, "\tsync respose received.\n");
      break;
    }
  }

  if (CHECKS_NR == i) {
      // fixme -- close things
    fprintf (stderr, "Synchronisation with the utracer module failed.\n");
    _exit (1);
  }
}
#endif

static void
start_runnables ()
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

main (int ac, char * av[])
{

#ifndef ENABLE_MODULE_OPS
  if (!utracer_loaded()) {
    fprintf (stderr, "utracer module not loaded, exiting.\n");
    _exit (1);
  }
#endif

  udb_pid = getpid();

  signal (SIGHUP,  sigterm_handler);
  signal (SIGTERM, sigterm_handler);
  //  signal (SIGQUIT, sigterm_handler);

  {
#ifdef ENABLE_MODULE_OP
#define OPTS_STRING "a:w:l:r:c:m:"
#else
#define OPTS_STRING "a:w:l:r:c:"
#endif
    int run = 1;
    while (1 == run) {
      int val = getopt_long (ac, av, OPTS_STRING, options, NULL);
      switch (val) {
      case -1:
	run = 0;
	break;
      case 'c':
	append_cmd (optarg);
	break;
#ifdef ENABLE_MODULE_OPS  
      case 'm':
	if (optarg) {
	  if (module_name) free (module_name);
	  module_name = strdup (optarg);
	}
	break;
#endif
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

#ifdef ENABLE_MODULE_OPS
  //fixme -- some kind of path?
  if (!module_name) module_name = strdup ("utracer/utracer");

  if (!utracer_loaded()) load_utracer();
#endif
  
  
  {
    char * cfn;
    asprintf (&cfn, "/proc/%s/%s", UTRACER_BASE_DIR, UTRACER_CONTROL_FN);
    ctl_file_fd = open (cfn, O_RDWR);

#if 0
    if (0) {
#define UTB_LEN 64
      char utb[UTB_LEN];
      utracer_ioctl_s utracer_ioctl = {56, UTB_LEN, utb};
      
      int irc;
      fprintf (stderr, "starting ioctl\n");

      irc = ioctl (ctl_file_fd, sizeof(utracer_ioctl_s), &utracer_ioctl);
      fprintf (stderr, "bffr ret = %s\n", &utb);
      
      fprintf (stderr, "ioctl rc = %d\n", irc);
      if (-1 == irc) perror ("ioctl");
    }
#endif
    free (cfn);
    if (-1 == ctl_file_fd)
      error (1, errno, "Error opening control file");
  }
  
  register_utracer (udb_pid);

  {
    char * cfn;
    
    asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	      udb_pid, UTRACER_CMD_FN);
    utracer_cmd_file_fd = open (cfn, O_RDWR);
    free (cfn);
    if (-1 == utracer_cmd_file_fd) {
      unregister_utracer (udb_pid);
      close_ctl_file();
      error (1, errno, "Error opening command file");
    }
    
    asprintf (&cfn, "/proc/%s/%ld/%s", UTRACER_BASE_DIR,
	      udb_pid, UTRACER_RESP_FN);
    utracer_resp_file_fd = open (cfn, O_RDONLY);
    free (cfn);
    if (-1 == utracer_resp_file_fd) {
      unregister_utracer (udb_pid);
      close_ctl_file();
      close (utracer_cmd_file_fd);
      error (1, errno, "Error opening response file");
    }
  }

  
#ifdef USE_UTRACER_SYNC
  utracer_sync();
#endif
      

  if (0 < nr_pids_to_attach) {
    int i;
    for (i = 0; i < nr_pids_to_attach; i++)
      utrace_attach_if (pids_to_attach[i].pid,
			pids_to_attach[i].quiesce, 0);
    free (pids_to_attach);
  }

  start_runnables ();

  {
    int rc = pthread_create (&resp_listener_thread,
			     NULL,
			     resp_listener,
			     NULL);
    if (rc) {
      close (utracer_cmd_file_fd);
      close (utracer_resp_file_fd);
      unregister_utracer (udb_pid);
      close_ctl_file();
      error (1, errno, "pthread_create() failed");
    }
  }
  
  {
    ssize_t sz;
    int resp;

    sz = pread (utracer_cmd_file_fd, &resp, sizeof(int), 0);
    if (-1 == sz) error (1, errno, "pread listener sync");

    // fixme -- all this syncing should make the sleep unnecessary, but it
    // still isn't working right.  try again some other time.
    usleep (5000);
    
    utrace_sync_if (SYNC_INIT);
  }

  text_ui_init();
  text_ui();

  cleanup_udb();
  unregister_utracer (udb_pid);
  close_ctl_file();
  

#ifdef ENABLE_MODULE_OPS  
  if (unload_module) unload_utracer();
#endif
  
  exit (0);
}
