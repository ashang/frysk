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
#include <unistd.h>
#include <string.h>
#include <alloca.h>
#include <sys/types.h>
#include <signal.h>
// this isn't the same as the one in the kernel
//#include <asm/ptrace.h>

#include <utracer.h>
#include "udb.h"
#include "udb-i386.h"

void *
resp_listener (void * arg)
{
  if_resp_u if_resp;
  ssize_t sz;
  int run = 1;
  void * extra = NULL;

  LOGIT ("in response thread\n");

  while (run) {
    LOGIT ("starting response thread pread\n");
    sz = utracer_read (&if_resp, &extra);
    if (-1 == sz) {
      utracer_uerror ("Response pread");
      _exit (4);
    }
    
    LOGIT ("returned from response thread pread, type = %d\n",
	   (int)(if_resp.type));
    switch (if_resp.type) {
    case IF_RESP_EXEC_DATA:
      {
	exec_resp_s exec_resp = if_resp.exec_resp;
	if (extra) {
	  fprintf (stdout, "\tProcess %ld execing %s (%s)\n",
		   exec_resp.utraced_pid,
		   (char *)extra,
		   ((char *)extra + 1 + strlen ((char *)extra)));
	}
	else
	  fprintf (stdout, "\tProcess %ld execing <unknown> (<unknown>)\n",
		   exec_resp.utraced_pid);
      }
      break;
    case IF_RESP_SYSCALL_ENTRY_DATA:
    case IF_RESP_SYSCALL_EXIT_DATA:
      {
	// fixme -- handle /proc/<pid>/mem to access ptr args
	syscall_resp_s syscall_resp = if_resp.syscall_resp;
	show_syscall (if_resp.type, syscall_resp.utraced_pid, extra);
        if (extra) free (extra);
      }
      break;
    case IF_RESP_DEATH_DATA:
      {
	death_resp_s death_resp = if_resp.death_resp;
	fprintf (stdout, "\t[%ld] died\n",
		 death_resp.utraced_pid);
      }
      break;
    case IF_RESP_SYNC_DATA:
      {
	sync_resp_s sync_resp = if_resp.sync_resp;
	LOGIT ("\tsync %ld received\n", sync_resp.sync_type);

	switch(sync_resp.sync_type) {
	case SYNC_INIT:
	  if (cl_cmds && (0 < cl_cmds_next)) {
	    int i;

	    for (i = 0; i < cl_cmds_next; i++) {
	      int rc;
	    
	      fprintf (stderr, "cmd \"%s\"\n", cl_cmds[i]);
	      rc = exec_cmd (cl_cmds[i]);

	      if (0 == rc) {
		kill (udb_pid, SIGTERM);
		break;
	      }
	    }
	  }
	  break;
	case SYNC_HALT:
	  LOGIT ("stopping listener\n");
	  run = 0;
	  break;
	}
      }
      break;
    case IF_RESP_QUIESCE_DATA:
      {
	quiesce_resp_s quiesce_resp = if_resp.quiesce_resp;
	fprintf (stdout, "\t[%ld] quiesced\n",
		 quiesce_resp.utraced_pid);
      }
      break;
    case IF_RESP_EXIT_DATA:
      {
	exit_resp_s exit_resp = if_resp.exit_resp;
	fprintf (stdout, "\t[%ld] exit with code %ld\n",
		 exit_resp.utraced_pid,
		 exit_resp.code);
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
      }
      break;
    case IF_RESP_CLONE_DATA:
      {
	clone_resp_s clone_resp = if_resp.clone_resp;
	fprintf (stdout, "\t[%ld] cloned to %ld, attach rc = %ld\n",
		 clone_resp.utracing_pid,
		 clone_resp.new_utraced_pid,
		 clone_resp.attach_rc);
      }
      break;
    default:
      break;
    }
    fprintf (stdout, "%s", prompt);
    fflush (stdout);
  }
}
