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

  while (1) {
    sz = pread (utracer_resp_file_fd, &if_resp,
		sizeof(if_resp), 0);
    if (-1 == sz) {
      uerror ("Response pread");
      _exit (4);
    }
    
    switch (if_resp.type) {
    case IF_RESP_EXEC_DATA:
      {
	exec_resp_s exec_resp = if_resp.exec_resp;
	long bytes_to_get = exec_resp.data_length;
	long bytes_gotten = 0;
	char * cstr = alloca (bytes_to_get + 2);

	if (sz > sizeof(exec_resp)) {
	  long bytes_avail =  sz - sizeof(exec_resp);
	  memcpy ((void *)cstr, (void *)(&if_resp) + sizeof (exec_resp),
		  bytes_avail);
	  bytes_to_get -= bytes_avail;
	  bytes_gotten = bytes_avail;
	}

	if (0 < bytes_to_get) {
	  sz = pread (utracer_resp_file_fd,
		      (void *)cstr + bytes_gotten,
		      bytes_to_get, sz);
	  bytes_gotten += sz;
	}
	
	fprintf (stdout, "\tProcess %ld execing %s (%s)\n",
		 exec_resp.utraced_pid, cstr, cstr + 1 + strlen (cstr));
      }
      break;
    case IF_RESP_SYSCALL_ENTRY_DATA:
    case IF_RESP_SYSCALL_EXIT_DATA:
      {
	// fixme -- handle /proc/<pid>/mem to access ptr args
	syscall_resp_s syscall_resp = if_resp.syscall_resp;
	long bytes_to_get = syscall_resp.data_length;
	long bytes_gotten = 0;
	struct pt_regs * regs = alloca (bytes_to_get);

	if (sz > sizeof(syscall_resp)) {
	  long bytes_avail =  sz - sizeof(syscall_resp);
	  memcpy ((void *)regs, (void *)(&if_resp) + sizeof (syscall_resp),
		  bytes_avail);
	  bytes_to_get -= bytes_avail;
	  bytes_gotten = bytes_avail;
	}

	if (0 < bytes_to_get) {
	  sz = pread (utracer_resp_file_fd,
		      (void *)regs + bytes_gotten,
		      bytes_to_get, sz);
	}
	
	show_syscall (if_resp.type, syscall_resp.utraced_pid, regs);
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
	fprintf (stdout, "\tsync %ld received\n",
		 sync_resp.sync_type);

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
