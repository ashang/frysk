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

static const char * i386_signals[] = {
  "unused",			//  0
  "SIGHUP",			//  1
  "SIGINT",			//  2
  "SIGQUIT",			//  3
  "SIGILL",			//  4
  "SIGTRAP",			//  5
  "SIGABRT",			//  6
  "SIGBUS",			//  7
  "SIGFPE",			//  8
  "SIGKILL",			//  9
  "SIGUSR1",			// 10
  "SIGSEGV",			// 11
  "SIGUSR2",			// 12
  "SIGPIPE",			// 13
  "SIGALRM",			// 14
  "SIGTERM",			// 15
  "SIGSTKFLT",			// 16
  "SIGCHLD",			// 17
  "SIGCONT",			// 18
  "SIGSTOP",			// 19
  "SIGTSTP",			// 20
  "SIGTTIN",			// 21
  "SIGTTOU",			// 22
  "SIGURG",			// 23
  "SIGXCPU",			// 24
  "SIGXFSZ",			// 25
  "SIGVTALRM",			// 26
  "SIGPROF",			// 27
  "SIGWINCH",			// 28
  "SIGIO",			// 29
  "SIGPWR",			// 30
  "SIGUNUSED"			// 31
};

static int nr_signals	= sizeof(i386_signals)/sizeof(char *);

static void *
resp_listener (void * arg)
{
  if_resp_u if_resp;
  ssize_t sz;

  while (1) {
    sz = pread (utracer_resp_file_fd, &if_resp,
		sizeof(if_resp), 0);
    
    switch (if_resp.type) {
    case IF_RESP_PIDS_DATA:
      {
	int i;
	int pids_received;
	long * pids_list = NULL;
	pids_resp_s pids_resp = if_resp.pids_resp;
	
	pids_received = (sz - sizeof(pids_resp))/ sizeof(long);
	
	fprintf (stdout, "\tsz_req = %d sz = %d\n\
nr pids = %ld,pids_received = %d\n",
		 sizeof(if_resp), sz,	 
		 pids_resp.nr_pids, pids_received);

	pids_list = alloca (pids_resp.nr_pids * sizeof(long));
	if (0 < pids_received)
	  memcpy (pids_list, ((void *)(&if_resp)) + sizeof(pids_resp),
		  pids_received * sizeof(long));
	
	if (pids_received < pids_resp.nr_pids) {
	  size_t sz_req = (pids_resp.nr_pids - pids_received) * sizeof(long);
	  sz = pread (utracer_resp_file_fd, &pids_list[pids_received],
		      sz_req, sz);
	}

	for (i = 0; i < pids_resp.nr_pids; i++)
	  fprintf (stdout, "\t[%d] %ld\n", i, pids_list[i]);
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_DEATH_DATA:
      {
	death_resp_s death_resp = if_resp.death_resp;
	fprintf (stdout, "\t[%ld] died\n",
		 death_resp.utraced_pid);
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_EXIT_DATA:
      {
	exit_resp_s exit_resp = if_resp.exit_resp;
	fprintf (stdout, "\t[%ld] exit with code %ld\n",
		 exit_resp.utraced_pid,
		 exit_resp.code);
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_SIGNAL_DATA:
      {
	signal_resp_s signal_resp = if_resp.signal_resp;
	fprintf (stdout, "\t[%ld] signal %ld (%s)\n",
		 signal_resp.utraced_pid,
		 signal_resp.signal,
		 ((0 <= signal_resp.signal) &&
		  (signal_resp.signal < nr_signals)) ?
		 i386_signals[signal_resp.signal] : "unused");
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_ATTACH_DATA:
      {
	attach_resp_s attach_resp = if_resp.attach_resp;
	fprintf (stdout, "\tprocess %ld attach %s\n",
		 attach_resp.utraced_pid,
		 attach_resp.okay ? "succeeded" : "failed");
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_REG_DATA:
      {
	readreg_resp_s readreg_resp = if_resp.readreg_resp;
	fprintf (stdout, "\t[%ld] [%d][%d]: %d [%#08x]\n",
		 readreg_resp.utraced_pid,
		 readreg_resp.regset,
		 readreg_resp.which,
		 (int)readreg_resp.data,
		 (int)readreg_resp.data);
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_CLONE_DATA:
      {
	clone_resp_s clone_resp = if_resp.clone_resp;
	fprintf (stdout, "\t[%ld] cloned to %ld\n",
		 clone_resp.utracing_pid,
		 clone_resp.new_utraced_pid);
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    default:
      break;
    }
  }
}

static struct option options[] = {
  {"watch",     required_argument, NULL, (int)'w'},
  {"attach",    required_argument, NULL, (int)'a'},
  {"module",    required_argument, NULL, (int)'m'},
  {"no-unload", no_argument, &unload_module, 0},
  {NULL,0, NULL, 0}
};

typedef struct {
  long pid;
  long quiesce;
} pta_s;

main (int ac, char * av[])
{
  pta_s * pids_to_attach = NULL;
  int nr_pids_to_attach = 0;
  int max_pids_to_attach = 0;
#define PIDS_TO_ATTACH_INCR	4

  udb_pid = getpid();

  signal (SIGHUP,  sigterm_handler);
  signal (SIGTERM, sigterm_handler);
  
  {
    int run = 1;
    while (1 == run) {
      int val = getopt_long (ac, av, "a:m:w:", options, NULL);
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
      utrace_attach_if (pids_to_attach[i].pid, pids_to_attach[i].quiesce);
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
