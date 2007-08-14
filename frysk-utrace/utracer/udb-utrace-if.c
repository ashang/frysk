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

// fixme -- make these ioctl too

void
register_utracer(pid_t pid)
{
  register_cmd_s register_cmd = {CTL_CMD_REGISTER, (long)pid};
  ssize_t sz = write (ctl_file_fd, &register_cmd, sizeof(register_cmd));
  if (-1 == sz) uerror ("Writing register command");
  else fprintf (stdout, "\t%d  registering\n", pid);
}

void
unregister_utracer(pid_t pid)
{
  register_cmd_s register_cmd = {CTL_CMD_UNREGISTER, (long)pid};
  ssize_t sz = write (ctl_file_fd, &register_cmd, sizeof(register_cmd));
  if (-1 == sz) uerror ("Writing unregister command");
  else fprintf (stdout, "\t%d  unregistering\n", pid);
}



