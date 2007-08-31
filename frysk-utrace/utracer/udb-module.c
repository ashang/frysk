// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

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

#include <utracer.h>
#include "udb.h"

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

#ifdef ENABLE_MODULE_OPS

/* using execlp of insmod and rmmod isn't my first choice of efficient ways to
 * load and unload modules, but using init_module() is a pain and rmmod does a
 * lot of useful stuff
 *
 */

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
#endif
