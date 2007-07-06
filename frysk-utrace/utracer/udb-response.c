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
    case IF_RESP_PRINTMMAP_DATA:
      {
	vm_struct_subset_s * vss;
	char * estrings;
	printmmap_resp_s prm = if_resp.printmmap_resp;
	long bytes_to_get = prm.string_count +
	  (prm.nr_mmaps * sizeof(vm_struct_subset_s));
	long bytes_gotten = 0;
	void * extra = alloca (bytes_to_get + 2);

	if (sz > sizeof(prm)) {
	  long bytes_avail =  sz - sizeof(prm);
	  memcpy (extra, (void *)(&if_resp) + sizeof (prm),
		  bytes_avail);
	  bytes_to_get -= bytes_avail;
	  bytes_gotten = bytes_avail;
	}
	
	if (0 < bytes_to_get) {
	  sz = pread (utracer_resp_file_fd,
		      extra + bytes_gotten,
		      bytes_to_get, sz);
	  bytes_gotten += sz;
	}

	vss = extra;
	estrings = extra + (prm.nr_mmaps * sizeof(vm_struct_subset_s));
	
	fprintf (stdout, "\n\t[%ld] mmap\n",prm.utraced_pid);
	fprintf (stdout, "\t\t%08lx mmap base\n", prm.mmap_base);
	fprintf (stdout, "\t\t%08lx task size\n", prm.task_size);
	
	fprintf (stdout,
		 "\t\tVM:  total    locked   shared   exec     stack    reserved\n\t\t     ");
	fprintf (stdout, "%-8ld ",prm.total_vm);
	fprintf (stdout, "%-8ld ",prm.locked_vm);
	fprintf (stdout, "%-8ld ",prm.shared_vm);
	fprintf (stdout, "%-8ld ",prm.exec_vm);
	fprintf (stdout, "%-8ld ",prm.stack_vm);
	fprintf (stdout, "%-8ld ",prm.reserved_vm);
	fprintf (stdout, "\n");
	
	fprintf (stdout, "\t\t%08lx def flags\n\t\t%8ld nr ptes\n",
		 prm.def_flags, prm.nr_ptes);
	
	fprintf (stdout, "\t\t%08lx - %08lx code (length = %ld) \n",
		 prm.start_code, prm.end_code, prm.end_code - prm.start_code);
	fprintf (stdout, "\t\t%08lx - %08lx data (length = %ld)\n",
		 prm.start_data, prm.end_data, prm.end_data - prm.start_data);
	fprintf (stdout, "\t\t%08lx - %08lx brk  (length = %ld) \n",
		 prm.start_brk, prm.brk, prm.brk - prm.start_brk);
	fprintf (stdout, "\t\t%08lx  stack\n",
		 prm.start_stack);

	if (0 < prm.nr_mmaps) {
	  int i;

	  fprintf (stdout, "\t\tMemory maps:\n");

	  for (i = 0; i < prm.nr_mmaps; i++) {
	    char * fn1;
#if 0
	    char * fn2;
	    char * fn3;
#endif

	    if (-1 != vss[i].dentry_offset)
	      fn1 = &estrings[vss[i].dentry_offset];
	    else fn1 = "";
	    
#if 0
	    if (-1 != vss[i].mnt_root_offset)
	      fn2 = &estrings[vss[i].mnt_root_offset];
	    else fn2 = "mnt_root";
	    
	    if (-1 != vss[i].mnt_mountpoint_offset)
	      fn3 = &estrings[vss[i].mnt_mountpoint_offset];
	    else fn3 = "mnt_mountpoint";
#endif

#if 0
	    fprintf (stdout, "\t\t  %08x - %08x %08x %s %s %s\n",
		     vss[i].vm_start,
		     vss[i].vm_end,
		     vss[i].vm_flags,
		     fn1, fn2, fn3);
#else
	    fprintf (stdout, "\t\t  %08x - %08x %08x %s\n",
		     vss[i].vm_start,
		     vss[i].vm_end,
		     vss[i].vm_flags,
		     fn1);
#endif
	  }
	}
	
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
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
	fprintf (stdout, "%s", prompt);
	fflush (stdout);
      }
      break;
    case IF_RESP_SYSCALL_ENTRY_DATA:
    case IF_RESP_SYSCALL_EXIT_DATA:
      {
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
	
	//show_syscall_regs (regs);
	show_syscall (if_resp.type, syscall_resp.utraced_pid, regs);
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
    case IF_RESP_QUIESCE_DATA:
      {
	quiesce_resp_s quiesce_resp = if_resp.quiesce_resp;
	fprintf (stdout, "\t[%ld] quiesced\n",
		 quiesce_resp.utraced_pid);
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
