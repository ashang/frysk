#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <alloca.h>
#include <malloc.h>
#include <string.h>
#include <limits.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/user.h>

#include "udb.h"
#include "utracer/utracer.h"
#include "utracer/utracer-errmsgs.h"

void
uerror(const char * s)
{
  if ((UTRACER_EBASE <= errno) && (errno < UTRACER_EMAX)) {
    fprintf (stderr, "%s: %s\n", s, utrace_emsg[errno - UTRACER_EBASE]);
  }
  else perror (s);
}

void
register_utracer(pid_t pid)
{
  register_cmd_s register_cmd = {CTL_CMD_REGISTER, (long)pid};
  ssize_t sz = write (ctl_file_fd, &register_cmd, sizeof(register_cmd));
  if (-1 == sz) uerror ("Writing register command.");
  else fprintf (stdout, "\t%d  registering\n", pid);
}

void
unregister_utracer(pid_t pid)
{
  register_cmd_s register_cmd = {CTL_CMD_UNREGISTER, (long)pid};
  ssize_t sz = write (ctl_file_fd, &register_cmd, sizeof(register_cmd));
  if (-1 == sz) uerror ("Writing unregister command.");
  else fprintf (stdout, "\t%d  unregistering\n", pid);
}

void
utrace_syscall_if (short which, short cmd, long cp, long syscall)
{
  syscall_cmd_s syscall_cmd;
  ssize_t sz;
  
  syscall_cmd.cmd			= IF_CMD_SYSCALL;
  syscall_cmd.utracing_pid		= (long)udb_pid;
  syscall_cmd.utraced_pid		= cp;
  syscall_cmd_which (&syscall_cmd)	= which;
  syscall_cmd_cmd (&syscall_cmd)	= cmd;
  syscall_cmd.syscall_nr		= syscall;
  sz = write (utracer_cmd_file_fd, &syscall_cmd, sizeof(syscall_cmd));
  if (-1 == sz) uerror ("Writing syscall command.");
}

void
utrace_switchpid_if (long pid)
{
  switchpid_cmd_s switchpid_cmd = {IF_CMD_SWITCHPID,
				   (long)udb_pid, pid};
  ssize_t sz = write (utracer_cmd_file_fd, &switchpid_cmd,
		      sizeof(switchpid_cmd));
  if (-1 == sz) uerror ("Writing switchpid command.");
}

void
handle_printmmap (printmmap_resp_s * prm,
		  vm_struct_subset_s * vss,
		  char * estrings)
{
  fprintf (stdout, "\n\t[%ld] mmap\n",prm->utraced_pid);
  fprintf (stdout, "\t\t%08lx mmap base\n", prm->mmap_base);
  fprintf (stdout, "\t\t%08lx task size\n", prm->task_size);
  fprintf (stdout, "\t\t%08lx def flags\n\t\t%8ld nr ptes\n",
	   prm->def_flags, prm->nr_ptes);
	
  fprintf (stdout,
	   "\n\t\tVM:  total    locked   shared   exec     stack\
    reserved\n\t\t     ");
  fprintf (stdout, "%-8ld ",prm->total_vm);
  fprintf (stdout, "%-8ld ",prm->locked_vm);
  fprintf (stdout, "%-8ld ",prm->shared_vm);
  fprintf (stdout, "%-8ld ",prm->exec_vm);
  fprintf (stdout, "%-8ld ",prm->stack_vm);
  fprintf (stdout, "%-8ld ",prm->reserved_vm);
  fprintf (stdout, "\n");

  fprintf (stdout, "\n\t\tCode ranges:\n");
  fprintf (stdout, "\t\t  %08lx - %08lx code (length = %ld) \n",
	   prm->start_code, prm->end_code, prm->end_code - prm->start_code);
  fprintf (stdout, "\t\t  %08lx - %08lx data (length = %ld)\n",
	   prm->start_data, prm->end_data, prm->end_data - prm->start_data);
  fprintf (stdout, "\t\t  %08lx - %08lx brk  (length = %ld) \n",
	   prm->start_brk, prm->brk, prm->brk - prm->start_brk);
  fprintf (stdout, "\t\t  %08lx  stack\n",
	   prm->start_stack);

  if (0 < prm->nr_mmaps) {
    int i;

    fprintf (stdout, "\n\t\tMemory maps:\n\t\t  start\
      end      flags    pathname\n");

    for (i = 0; i < prm->nr_mmaps; i++) {
      char * fn1;

      if (-1 != vss[i].name_offset)
	fn1 = &estrings[vss[i].name_offset];
      else {
	if ((vss[i].vm_start <= prm->start_brk) &&
	    (vss[i].vm_end   >= prm->brk)) fn1 = "[heap]";
	else if ((vss[i].vm_start <= prm->start_stack) &&
		 (vss[i].vm_end   >= prm->start_stack)) fn1 = "[stack]";
	else fn1 = "[vdso]";
      }
	    
      fprintf (stdout, "\t\t  %08x - %08x %08x %s\n",
	       vss[i].vm_start,
	       vss[i].vm_end,
	       vss[i].vm_flags,
	       fn1);
    }
  }
}

void
utrace_printmmap_if (long pid)
{
  int irc;
  printmmap_resp_s * printmmap_resp = alloca (sizeof(printmmap_resp_s));
  vm_struct_subset_s * vm_struct_subset = alloca (PAGE_SIZE);
  char * vm_strings = alloca(PAGE_SIZE);
  long vm_struct_subset_length;
  long vm_strings_length;
  
  printmmap_cmd_s printmmap_cmd = {IF_CMD_PRINTMMAP,
				   (long)udb_pid,
				   pid,
				   PAGE_SIZE,		// vm_subset_alloc
				   PAGE_SIZE,		// vm_strings_alloc
				   &vm_struct_subset_length,
				   &vm_strings_length,
				   printmmap_resp,
				   vm_struct_subset,
				   vm_strings};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(printmmap_cmd),
	       &printmmap_cmd);
  if (-1 == irc) uerror ("printmmap ioctl.");
  else {
    // fixme -- retry
    if ((vm_struct_subset_length > PAGE_SIZE) ||
	(vm_strings_length > PAGE_SIZE)) {
      fprintf (stderr, "WARNING: printmmap buffer truncated.\n");
    }
    else handle_printmmap (printmmap_resp, vm_struct_subset, vm_strings);
  }
}

void
utrace_printexe_if (long pid)
{
  int irc;
  char * filename = alloca (PATH_MAX);
  char * interp   = alloca (PATH_MAX);
  printexe_cmd_s printexe_cmd = {IF_CMD_PRINTEXE,
				 (long)udb_pid,
				 pid,
				 filename,
				 PATH_MAX,
				 interp,
				 PATH_MAX};
  irc = ioctl (utracer_cmd_file_fd, sizeof(printexe_cmd_s), &printexe_cmd);
  if (0 > irc) uerror ("printexe ioctl");
  else {
    fprintf (stdout, "\t filename: \"%s\"\n", filename);
    fprintf (stdout, "\t   interp: \"%s\"\n", interp);
  }
}

void
utrace_printenv_if (long pid)
{
  int irc;
  long length_returned;
#define PRINTENV_BUFFER_SIZE 4096
  char * buffer = alloca (PRINTENV_BUFFER_SIZE);
  printenv_cmd_s printenv_cmd = {IF_CMD_PRINTENV,
				 (long)udb_pid,
				 pid,
				 PRINTENV_BUFFER_SIZE,
				 &length_returned,
				 buffer};
  irc = ioctl (utracer_cmd_file_fd, sizeof(printenv_cmd_s), &printenv_cmd);
  if (0 > irc) uerror ("printenv ioctl");
  else {
    long i;

    // fixme -- do a retry
    if (length_returned > PRINTENV_BUFFER_SIZE) {
      fprintf (stderr, "WARNING: Environment buffer truncated.\n");
      length_returned = PRINTENV_BUFFER_SIZE;
    }
    
    for (i = 0; i < length_returned; i++) {
      if (0 == buffer[i]) buffer[i] = '\n';
    }
    fprintf (stdout, "%s\n", buffer);
  }
}

void
utrace_attach_if (long pid, long quiesce, long exec_quiesce)
{
  attach_cmd_s attach_cmd =
    {IF_CMD_ATTACH, (long)udb_pid, pid, quiesce, exec_quiesce};
  ssize_t sz = write (utracer_cmd_file_fd, &attach_cmd, sizeof(attach_cmd));
  if (-1 == sz) uerror ("Writing attach command.");
}

void
utrace_detach_if (long pid)
{
  attach_cmd_s attach_cmd = {IF_CMD_DETACH, (long)udb_pid, pid, 0};
  ssize_t sz = write (utracer_cmd_file_fd, &attach_cmd, sizeof(attach_cmd));
  if (-1 == sz) uerror ("Writing detach command.");
  else {
    current_pid = -1;	//fixme -- look for another attached pid
    set_prompt();
    fprintf (stdout, "\t%d  detached\n", pid);
  }
}

void
utrace_run_if (long pid)
{
  run_cmd_s run_cmd = {IF_CMD_RUN, (long)udb_pid, pid};
  ssize_t sz = write (utracer_cmd_file_fd, &run_cmd, sizeof(run_cmd));
  if (-1 == sz) uerror ("Writing run command.");
}

void
utrace_listpids_if ()
{
  listpids_cmd_s listpids_cmd = {IF_CMD_LIST_PIDS, (long)udb_pid};
  ssize_t sz = write (utracer_cmd_file_fd, &listpids_cmd,
		      sizeof(listpids_cmd));
  if (-1 == sz) uerror ("Writing listpids command.");
}

void
utrace_sync_if (long type)
{
  sync_cmd_s sync_cmd = {IF_CMD_SYNC, (long)udb_pid, type};
  ssize_t sz = write (utracer_cmd_file_fd, &sync_cmd,
		      sizeof(sync_cmd));
  if (-1 == sz) uerror ("Writing sync command.");
}

void
utrace_quiesce_if (long pid)
{
  run_cmd_s run_cmd = {IF_CMD_QUIESCE, (long)udb_pid, pid};
  ssize_t sz = write (utracer_cmd_file_fd, &run_cmd, sizeof(run_cmd));
  if (-1 == sz) uerror ("Writing run command.");
}

// fixme--do stuff about gp, fp, fpx regs, etc
void
utrace_readreg_if (long pid, int regset, int reg)
{
  readreg_cmd_s readreg_cmd = {IF_CMD_READ_REG, (long)udb_pid,
			       pid, regset, reg};
  ssize_t sz = write (utracer_cmd_file_fd, &readreg_cmd, sizeof(readreg_cmd));
  if (-1 == sz) uerror ("Writing readreg command.");
}
