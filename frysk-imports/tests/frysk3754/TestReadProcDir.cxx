// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined volatile interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved volatile interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved volatile interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved volatile interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/wait.h>

#include <gcj/cni.h>
#include "frysk3754/TestReadProcDir.h"

pid_t waitpid(pid_t pid, int *status, int options);

int errno;

volatile pid_t pid;

jint
frysk3754::TestReadProcDir::startProc ()
{
  pid = fork ();
  if (pid < 0) 
    {
      if (errno != 0)
		perror ("frysk.imports.tests.frysk3754 startProc ()");
      exit (EXIT_FAILURE);
    } 
  else if (pid == 0)
    {  	
      errno = 0;
      ::ptrace ((enum __ptrace_request) PTRACE_TRACEME, \
		pid, NULL, 0);
			     
      if (errno != 0) 
	::perror("ptrace");
			     
      ::execv ("/bin/true", NULL);
      ::perror ("execvp");

      exit (EXIT_SUCCESS);
    }
  else 
    {
      /* Wait for attach */
      waitpid (pid, NULL, __WALL);

      /* Request exit tracing */
      ::ptrace ((enum __ptrace_request) PTRACE_SETOPTIONS, \
		pid, NULL, PTRACE_O_TRACEEXIT);

      /* Continue the process after its exec */
      ::ptrace ((enum __ptrace_request) PTRACE_CONT, \
		pid, NULL, 0);

      /* Catch exit event */
      waitpid (pid, NULL, __WALL);

      char buf[200];
      char *ptr = &buf[0];
      sprintf (ptr, "/proc/%d/cwd", pid);

      errno = 0;
      jint fd = (int) ::open (ptr, O_RDONLY, S_IRUSR | S_IRGRP | S_IROTH);
      ::perror ("open");

      return fd;
    }
}
