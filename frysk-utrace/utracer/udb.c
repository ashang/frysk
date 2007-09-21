// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

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

#include <utracer.h>
#define DO_UDB_INIT
#include "udb.h"
#include "udb-i386.h"

pthread_t resp_listener_thread;

#define USE_UTRACER_WAIT

extern void * resp_listener (void * arg);

void
cleanup_udb()
{
  int rc;
  void * rc_ptr;

  rc = pthread_cancel (resp_listener_thread);
  if (0 != rc) perror ("pthread_cancel");

  rc = pthread_join(resp_listener_thread, &rc_ptr);
  if (0 != rc) perror ("pthread_cancel");

  
  utracer_cleanup();

  text_ui_terminate();
  arch_specific_terminate();
}

static void
sigterm_handler (int sig)
{
  cleanup_udb();		// fixme -- eventually, just unregister
#ifdef ENABLE_MODULE_OPS  
  unload_utracer();		// and have utracer unload itsef
#endif
  utracer_unregister ((long)udb_pid);
  utracer_close_ctl_file();
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

#ifdef USE_UTRACER_WAIT
static void
utracer_wait()
{
  int i;
#define CHECKS_NR	3

  for (i = 0; i < CHECKS_NR; i++) {
    if_resp_u if_resp;
    ssize_t sz;
      
    utracer_sync (SYNC_INIT);

    LOGIT ("starting utracer_wait pread pass %d fd %d\n",
	   i, utracer_resp_file_fd());
    sz = pread (utracer_resp_file_fd(), &if_resp,
		  sizeof(if_resp), 0);
    LOGIT ("got utracer_wait pread, sz = %d\n", sz);
    if (-1 == sz) {
      uerror ("Response pread");
      // fixme -- close things
      _exit (4);
    }

    if (IF_RESP_SYNC_DATA == if_resp.type) {
      fprintf (stdout, "\tsync response received.\n");
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

	rc = utracer_attach (cp, 0, runnables_to_attach[i].quiesce);
	if (0 == rc) {
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
	    perror ("Error in spawner execlp");
	}
	else uerror ("start_runnables");
      }
      break;
    default:      // parent
      current_pid = child_pid;
      set_prompt();
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

  text_ui_init();

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
  

  LOGIT ("calling utracer_open()\n");
  if (!utracer_open((long)udb_pid)) {
    fprintf (stderr, "Exiting due to errors.\n");
    exit (1);
  }
  
#ifdef USE_UTRACER_WAIT
  LOGIT ("calling utracer_wait()\n");
  utracer_wait();
#endif
      

  if (0 < nr_pids_to_attach) {
    int i;
    LOGIT ("attaching pids()\n");
    for (i = 0; i < nr_pids_to_attach; i++) {
      int rc;
      rc =  utracer_attach (pids_to_attach[i].pid,
			    pids_to_attach[i].quiesce, 0);
      if (0 == rc) {
	current_pid = pids_to_attach[i].pid;
	set_prompt();
      }
      else uerror ("attaching cl pids");
    }
    free (pids_to_attach);
    LOGIT ("done attaching pids()\n");
  }

  LOGIT ("calling start_runnables()\n");
  start_runnables ();

  
  LOGIT ("creating response thread\n");
  {
    int rc = pthread_create (&resp_listener_thread,
			     NULL,
			     resp_listener,
			     NULL);
    if (rc) {
      utracer_shutdown((long)udb_pid);
      error (1, errno, "pthread_create() failed");
    }
  }
  
  {
    ssize_t sz;
    int resp;

    LOGIT ("calling utracer_sync()\n");
    utracer_sync (SYNC_INIT);
    LOGIT ("returning from utracer_sync()\n");
  }
  

  text_ui();

  cleanup_udb();
  utracer_unregister ((long)udb_pid);
  utracer_close_ctl_file();
  

#ifdef ENABLE_MODULE_OPS  
  if (unload_module) unload_utracer();
#endif
  
  exit (0);
}
