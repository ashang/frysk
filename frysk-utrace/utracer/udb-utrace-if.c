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
#include <sys/user.h>

#include <utracer.h>
#include <utracer-errmsgs.h>
#include "udb.h"

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



