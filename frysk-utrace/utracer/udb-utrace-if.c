#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <alloca.h>
#include <sys/time.h>
#include <sys/types.h>

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
  ssize_t sz;
  syscall_cmd_s syscall_cmd;
  
  syscall_cmd.cmd			= IF_CMD_SYSCALL;
  syscall_cmd.utracing_pid		= (long)udb_pid;
  syscall_cmd.utraced_pid		= cp;
  syscall_cmd_which (syscall_cmd)	= which;
  syscall_cmd_cmd (syscall_cmd)		= cmd;
  syscall_cmd.syscall_nr		= syscall;
  sz = write (utracer_cmd_file_fd, &syscall_cmd, sizeof(syscall_cmd));
  if (-1 == sz) uerror ("Writing syscall command.");
}

void
utrace_switchpid_if (long pid)
{
  switchpid_cmd_s switchpid_cmd = {IF_CMD_SWITCHPID, (long)udb_pid, pid};
  ssize_t sz = write (utracer_cmd_file_fd, &switchpid_cmd,
		      sizeof(switchpid_cmd));
  if (-1 == sz) uerror ("Writing switchpid command.");
}

void
utrace_printmmap_if (long pid)
{
  printmmap_cmd_s printmmap_cmd = {IF_CMD_PRINTMMAP, (long)udb_pid, pid};
  ssize_t sz = write (utracer_cmd_file_fd, &printmmap_cmd,
		      sizeof(printmmap_cmd));
  if (-1 == sz) uerror ("Writing printmmap command.");
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
  else fprintf (stdout, "\t%d  running\n", pid);
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
