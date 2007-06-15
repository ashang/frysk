#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <alloca.h>

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
utrace_testsig_if ()		// fixme -- diagnostic
{
  testsig_cmd_s testsig_cmd = {IF_CMD_TESTSIG, (long)getpid()};
  ssize_t sz = write (utracer_file_fd, &testsig_cmd, sizeof(testsig_cmd));
  if (-1 == sz) uerror ("Writing testsig command.");
  else {
    fprintf (stdout, "\ttestsig sent\n");
  }
}

void
utrace_attach_if (long pid)
{
  attach_cmd_s attach_cmd = {IF_CMD_ATTACH, (long)getpid(), pid};
  ssize_t sz = write (utracer_file_fd, &attach_cmd, sizeof(attach_cmd));
  if (-1 == sz) uerror ("Writing attach command.");
  else {
    current_pid = pid;
    set_prompt();
    fprintf (stdout, "\t%d  attached\n", pid);
  }
}

void
utrace_detach_if (long pid)
{
  attach_cmd_s attach_cmd = {IF_CMD_DETACH, (long)getpid(), pid};
  ssize_t sz = write (utracer_file_fd, &attach_cmd, sizeof(attach_cmd));
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
  run_cmd_s run_cmd = {IF_CMD_RUN, (long)getpid(), pid};
  ssize_t sz = write (utracer_file_fd, &run_cmd, sizeof(run_cmd));
  if (-1 == sz) uerror ("Writing run command.");
  else fprintf (stdout, "\t%d  running\n", pid);
}

void
utrace_quiesce_if (long pid)
{
  run_cmd_s run_cmd = {IF_CMD_QUIESCE, (long)getpid(), pid};
  ssize_t sz = write (utracer_file_fd, &run_cmd, sizeof(run_cmd));
  if (-1 == sz) uerror ("Writing run command.");
  else fprintf (stdout, "\t%d  quiesced\n", pid);
}

// fixme--do stuff about gp, fp, fpx regs, etc
void
utrace_readreg_if (long pid, int regset, int reg)
{
  readreg_cmd_s readreg_cmd = {IF_CMD_READ_REG, (long)getpid(),
			       pid, regset, reg};
  ssize_t sz = write (utracer_file_fd, &readreg_cmd, sizeof(readreg_cmd));
  if (-1 == sz) uerror ("Writing readreg command.");
  else {
    readreg_resp_s readreg_resp;
    ssize_t sz = pread (utracer_file_fd, &readreg_resp,
			sizeof(readreg_resp), 0);
    if (-1 == sz) uerror ("Reading readreg command.");
    else if (0 == sz) {
      fprintf (stderr, "\tNo register data returned.\n");
    }
    else {
      fprintf (stdout, "\t[%d][%d]: %d [%#08x]\n",
	       readreg_resp.regset,
	       readreg_resp.which,
	       (int)readreg_resp.data,
	       (int)readreg_resp.data);
#if 0
      fprintf (stderr, "readreg got %d bytes:\n", sz);
      fprintf (stderr, "\ttype         = %ld\n", readreg_resp.type);
      fprintf (stderr, "\tutracing_pid = %ld\n", readreg_resp.utracing_pid);
      fprintf (stderr, "\tutraced_pid  = %ld\n", readreg_resp.utraced_pid);
      fprintf (stderr, "\tregset       = %d\n",  readreg_resp.regset);
      fprintf (stderr, "\twhich        = %d\n",  readreg_resp.which);
      fprintf (stderr, "\tbyte_count   = %d\n",  readreg_resp.byte_count);
      fprintf (stderr, "\treg_count    = %d\n",  readreg_resp.reg_count);
      fprintf (stderr, "\tdata         = %d\n",
	       (int)readreg_resp.data);
#endif
    }
  }
}
