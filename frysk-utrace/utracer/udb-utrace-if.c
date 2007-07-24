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

static int
do_get_printmmap (long pid,
		  printmmap_resp_s ** pr,
		  vm_struct_subset_s ** vss,
		  char ** vs,
		  long vssl_req,
		  long vsl_req,
		  long * vssl_actual,
		  long * vsl_actual)
{
  int irc;
  long vm_struct_subset_length;
  long vm_strings_length;

  if (!pr || ! vss || !vs) return UTRACER_EMAX;

  *pr  = malloc (sizeof(printmmap_resp_s));
  *vss = malloc (vssl_req);
  *vs  = malloc (vsl_req);
  
  printmmap_cmd_s printmmap_cmd = {IF_CMD_PRINTMMAP,
				   (long)udb_pid,
				   pid,
				   vssl_req,
				   vsl_req,
				   &vm_struct_subset_length,
				   &vm_strings_length,
				   *pr,
				   *vss,
				   *vs};
  irc = ioctl (utracer_cmd_file_fd,
	       sizeof(printmmap_cmd),
	       &printmmap_cmd);

  if (vssl_actual) *vssl_actual = vm_struct_subset_length;
  if (vsl_actual)  *vsl_actual = vm_strings_length;

  return irc;
}

int
utracer_get_printmmap (long pid,
		       printmmap_resp_s ** printmmap_resp_p,
		       vm_struct_subset_s ** vm_struct_subset_p,
		       char ** vm_strings_p)
{
  int irc;
  printmmap_resp_s * pr = NULL;
  vm_struct_subset_s * vss = NULL;
  char * vs = NULL;
  long vssl;
  long vsl;
  
  irc = do_get_printmmap (pid, &pr, &vss, &vs,
			  PAGE_SIZE, PAGE_SIZE, &vssl, &vsl);

  if (0 == irc) {
    if ((vssl > PAGE_SIZE) ||
	(vsl > PAGE_SIZE)) {
      if (pr)  free (pr);
      if (vss) free (vss);
      if (vs)  free (vs);
      irc = do_get_printmmap (pid, &pr, &vss, &vs,
			      vssl, vsl, NULL, NULL);
    }
  }
  switch (irc) {
  case UTRACER_EMAX:
    fprintf (stderr, "do_get_printmmap failed.\n");
    break;
  case -1:
    uerror ("printmmap ioctl.");
    break;
  default:
    if (printmmap_resp_p)	*printmmap_resp_p	= pr;
    if (vm_struct_subset_p)	*vm_struct_subset_p	= vss;
    if (vm_strings_p)		*vm_strings_p		= vs;
    break;
  }

  return irc;
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
