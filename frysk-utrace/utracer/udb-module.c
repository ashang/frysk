#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <error.h>
#include <errno.h>
#include <unistd.h>
#include <wait.h>
#include <fcntl.h>

#include "utracer/utracer.h"
#include "udb.h"

/* using execlp of insmod and rmmod isn't my first choice of efficient ways to
 * load and unload modules, but using init_module() is a pain and rmmod does a
 * lot of useful stuff
 *
 */

int
utracer_loaded()
{
  struct stat buf;
  int src;
  char * proc_utrace_fn;
  int rc = 1;
  
  asprintf (&proc_utrace_fn, "/proc/%s", UTRACER_BASE_DIR);
  src = stat(proc_utrace_fn, &buf);
  free(proc_utrace_fn);
  if (-1 == src) {
    if (errno != ENOENT)
      error (1, errno, "Error statting /proc/%s", UTRACER_BASE_DIR);
    rc = 0;
  }
  return rc;
}

void
load_utracer()
{
  pid_t child_pid = fork();
  switch (child_pid) {
  case -1:
    error (1, errno, "Error forking loader");
    break;
  case 0:	// child
    {
      char * mod_name;
      if (module_name) {
	asprintf (&mod_name, "./%s.ko", module_name);
	int rc = execlp ("insmod", "insmod", mod_name, NULL);
	free (mod_name);
	if (-1 == rc)
	  error (1, errno, "Error in loader execlp");
      }
      else {
	fprintf (stderr, "ERROR: No module loaded!\n");
	_exit (0);
      }
    }
    break;
  default:	// parent
    {
      int status;
      waitpid (child_pid, &status, 0);
      if (!utracer_loaded()) {	/* /proc/utrace still doesn't exist */
	if (WIFEXITED(status)) {	/* even though the child exited ok  */
	  fprintf (stderr, "\tLoader failed.\n");
	  _exit (1);
	}
	else				/* child came to sad, untimely, end */
	  error (1, errno, "Error in loader");
      }

      {
	char * cfn;
	asprintf (&cfn, "/proc/%s/%s", UTRACER_BASE_DIR, UTRACER_CONTROL_FN);
	ctl_file_fd = open (cfn, O_RDWR);
	free (cfn);
	if (-1 == ctl_file_fd)
	  error (1, errno, "Error opening control file");
      }
    }
    break;
  }
}


void
unload_utracer()
{
  pid_t child_pid = fork();
  switch (child_pid) {
  case -1:
    error (1, errno, "Error forking unloader");
    break;
  case 0:	// child
    {
      int rc;

      rc = execlp ("rmmod", "rmmod", "--wait", module_name, NULL);
      if (-1 == rc)
	error (1, errno, "Error in unloader execlp");
    }	
    break;
  default:	// parent
    {
      int status;
      waitpid (child_pid, &status, 0);
      if (utracer_loaded()) {	/* /proc/utrace still exists */
	if (WIFEXITED(status))	/* even though the child exited ok  */
	  fprintf (stderr, "\tUnloader failed.\n");
	else			/* child came to sad, untimely, end */
	  perror ("Error in unloader");
      }
    }
    break;
  }
}
