#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <alloca.h>
// this isn't the same as the one in the kernel
//#include <asm/ptrace.h>

#include "utracer/utracer.h"
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
    
    switch (if_resp.type) {
    case IF_RESP_SYSCALL_DATA:
      {

	syscall_resp_s syscall_resp = if_resp.syscall_resp;
	long bytes_to_get = sizeof (struct pt_regs);
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
	
	//show_syscall_regs (regs);
	show_syscall (regs);
      }
      break;
    case IF_RESP_REG_DATA:
      {
	// fixme -- handle non-int values
	// fixme -- use reg name in addition to number
	readreg_resp_s readreg_resp = if_resp.readreg_resp;
	
	if (-1 == readreg_resp.which) {
	  int i;
	  int regs_received;
	  long * regs_list = NULL;
	  
	  regs_received = (sz - sizeof(readreg_resp))/ sizeof(long);
	  regs_list = alloca (readreg_resp.reg_count * sizeof(long));
	  
	  if (0 < regs_received)
	    memcpy (regs_list, ((void *)(&if_resp)) + sizeof(readreg_resp),
		    regs_received * sizeof(long));
	  
	  if (regs_received < readreg_resp.reg_count) {
	    size_t sz_req = (readreg_resp.reg_count -
			     regs_received) * sizeof(long);
	    sz = pread (utracer_resp_file_fd, &regs_list[regs_received],
			sz_req, sz);
	  }

	  for (i = 0; i < readreg_resp.reg_count; i++)
	    fprintf (stdout, "\t[%ld] [%d][%d (%s)]: [%#08x] %ld\n",
		     readreg_resp.utraced_pid,
		     readreg_resp.regset,
		     i,
		     reg_mapping[i].key,
		     regs_list[i],
		     regs_list[i]);
	}
	else {
	  fprintf (stdout, "\t[%ld] [%d][%d (%s)]: [%#08x] %d\n",
		   readreg_resp.utraced_pid,
		   readreg_resp.regset,
		   readreg_resp.which,
		   reg_mapping[readreg_resp.which].key,
		   (int)readreg_resp.data,
		   (int)readreg_resp.data);
	}
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_PIDS_DATA:
      {
	int i;
	int pids_received;
	long * pids_list = NULL;
	pids_resp_s pids_resp = if_resp.pids_resp;
	
	pids_received = (sz - sizeof(pids_resp))/ sizeof(long);
	
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
    case IF_RESP_SWITCHPID_DATA:
      {
	switchpid_resp_s switchpid_resp = if_resp.switchpid_resp;
	if (switchpid_resp.okay) {
	  current_pid = switchpid_resp.utraced_pid;
	  set_prompt();
	}
	else fprintf (stdout, "PID %d invalid\n", switchpid_resp.utraced_pid);
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
	if (attach_resp.okay) {
	  current_pid = attach_resp.utraced_pid;
	  set_prompt();
	}
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
