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
#include <linux/ptrace.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/wait.h>

pid_t waitpid(pid_t pid, int *status, int options);

int errno;

volatile pid_t pid;

void
exit_0_handler (int sig)
{
  printf ("exit 0: %d\n", sig);
  _exit (0);
}

void
exit_1_handler (int sig)
{
  printf ("exit 1: %d\n", sig);
  _exit (1);
}

int
main ()
{
  int status;
  int ret;
  int tochild[2];
  int toparent[2];
  int p;

  pipe (tochild);
  pipe (toparent);

  sigset_t mask;
  sigset_t umask;
  sigemptyset (&mask);
  sigaddset (&mask, SIGUSR1);
  sigaddset (&mask, SIGALRM);
  sigaddset (&mask, SIGCHLD); // totally ignore
  if (sigprocmask (SIG_BLOCK, &mask, &umask) < 0) {
    perror ("sigprocmask");
    _exit (1);
  }

  // Set up a SIGUSR1 handler, that, when the signal is received, just
  // exits.
  signal (SIGUSR1, exit_0_handler);


  p = fork ();
  if (p < 0) 
    {
      if (errno != 0)
		perror ("frysk.imports.tests.frysk3754 main ()");
      exit (EXIT_FAILURE);
    } 
  else if (p == 0)
    {  	
      fprintf(stderr, "child\n");
      int toinnerchild[2];
      int toinnerparent[2];
      
      pipe (toinnerchild);
      pipe (toinnerparent);

      errno = 0;
      char b;

      char ret_pid[5] = { '\0' };
      char *ptr = &ret_pid[0];

      int p2 = fork ();
      if (p2 < 0)
	{
	  if (errno != 0)
	    perror ("frysk.imports.tests.frysk3754 main ()");
	  exit (EXIT_FAILURE);
	}
      else if (p2 == 0)
	{
	  fprintf(stderr, "innerchild\n");
	  errno = 0;
	  char a;
	  write (toinnerparent[1], &a, 1);
	  read (toinnerchild[0], &a, 1);

	  sleep (1);

	  raise (SIGSEGV);

	  exit (EXIT_SUCCESS);
	}
      else
	{
	  sprintf(ptr, "%d", p2);

	  read (toinnerparent[0], &b, 1); 
	  fprintf(stderr, "read from innerchild\n");
	  write (toparent[1], &ret_pid, 5);
	  read (tochild[0], &b, 1);
	  write (toinnerchild[1], &b, 1);
	  fprintf(stderr, "inner parent exiting\n");
	  _exit(0);
	}
    }
  else 
    {

      fprintf (stderr, "vforked %d\n", p);
      char innerpid[5];
      char a;
      read (toparent[0], innerpid, 5);
      fprintf(stderr, "read from child\n");
      char *iptr = &innerpid[0];
      fprintf(stderr, "received PID %s\n", iptr);
      pid = atoi (iptr);

      ptrace (PTRACE_ATTACH, pid, NULL, 0);

      ret = waitpid (-1, &status, __WALL);
      if (ret < 0)
	exit (1);
     fprintf (stderr, "First wait: 0x%x %d\n", status, ret);

      /* Request exit tracing */
     if (ptrace (PTRACE_SETOPTIONS, pid, NULL,
		 (void *) (PTRACE_O_TRACEEXIT |PTRACE_O_TRACECLONE| PTRACE_O_TRACEFORK|PTRACE_O_TRACEEXEC )) < 0)
       {
	 perror ("ptrace");
	 exit (1);
       }

      /* Continue the process after its exec */
      ptrace (PTRACE_CONT, pid, NULL, NULL);

     write (tochild[1], &a, 1);

      /* Catch exit event */
      ret = waitpid (-1, &status, __WALL);
      if (ret < 0)
	exit(1);
      fprintf (stderr, "received signal: 0x%x %d\n", status, ret);

      ptrace (PTRACE_CONT, pid, (void *) NULL, (void *) SIGUSR1);

      ret = waitpid (-1, &status, __WALL);
      if (ret < 0)
	exit(1);
      fprintf (stderr, "signal delivered: 0x%x %d\n", status, ret);


      fprintf (stderr, "WIFSTOPED: %d\n", WIFSTOPPED(status));
      if (!WIFSTOPPED(status))
	exit (1);

      fprintf (stderr, "WSTOPEVENT: %d\n", (status & 0xff0000) >> 16);
      if (((status & 0xff0000) >> 16) != PTRACE_EVENT_EXIT)
	exit (1);

      char buf[200];
      char *ptr = &buf[0];
      sprintf (ptr, "/proc/%d/cwd", pid);

      errno = 0;
      int fd = (int) open (ptr, O_RDONLY, S_IRUSR | S_IRGRP | S_IROTH);
      perror ("open");

      return fd;
    }
}
