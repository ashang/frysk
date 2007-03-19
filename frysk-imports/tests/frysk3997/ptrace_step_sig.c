// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/user.h>
#include <sys/wait.h>
#include <errno.h>
#include <pthread.h>

// The process id of the child.
static pid_t pid;

// Whether to stop the child.
static volatile int stop = 0;

// When the child gets a sig hup we are wrap up and are done.
static void
sig_handler(int sig)
{
  stop = 1;
}

// Fork and let the child install signal handlers and do some work
// while the parent ptraces it.
int
main (int argc, char **argv) 
{
  pid = fork ();
  switch (pid)
    {
    case -1:
      {
	perror ("Error: could not fork child process");
	exit (1);
      }
      
    case 0:
      {
	// Child

	// Setup sigtrap handler.
	struct sigaction action;
	action.sa_handler = sig_handler;
	sigemptyset (&action.sa_mask);
	// Here we need to make the SIGTRAP handler reentrant.  This
	// is clearly a design issue. The only reason we need to do
	// this is because stepping a thread with ptrace uses SIGTRAP
	// to signal the attached ptrace. See bug #3997 for more
	// discussion about this.
	action.sa_flags = SA_NODEFER;
	sigaction (SIGTRAP, &action, NULL);

	while (! stop)
	  /* do something useful */ ;

	// Check that handler is still setup correctly.
	sigaction (SIGTRAP, NULL, &action);
	if (action.sa_handler == SIG_DFL)
	  {
	    printf("SIGTRAP handler reset!");
	    exit (-1);
	  }

	// Just for fun, and so our parent can step through the handler.
	kill (getpid (), SIGTRAP);

	// Check that handler is still setup correctly.
	sigaction (SIGTRAP, NULL, &action);
	if (action.sa_handler == SIG_DFL)
	  {
	    printf("SIGTRAP handler reset (2)!\n");
	    exit (-1);
	  }

	exit (0);
      }

    default:
      {
	// Parent
	sleep(1);

	long r = ptrace (PTRACE_ATTACH, pid, NULL, NULL);
	if (r == -1)
	  {
	    perror ("Error: could not start trace");
	    exit (1);
	  }
	
	int status;
	pid_t waitpid = wait (&status);
	if (waitpid != pid)
	  {
	    perror ("wait didn't return the child pid");
	    exit (1);
	  }
	if (! WIFSTOPPED(status) || WSTOPSIG (status) != SIGSTOP)
	  {
	    printf ("child not stopped by SIGSTOP");
	    exit (1);
	  }

	// Let the child continue into (and past) the signal handler.
	r = ptrace (PTRACE_CONT, pid, 0, SIGTRAP);
	if (r != 0)
	  {
	    perror ("ptrace continue into signal handler failed");
	    exit (1);
	  }

	// The child will send itself a SIGTRAP now.
	waitpid = wait (&status);
	if (waitpid != pid)
	  {
	    perror ("wait for signal didn't return the child pid");
	    exit (1);
	  }
	if (! WIFSTOPPED (status) || WSTOPSIG (status) != SIGTRAP)
	  {
	    printf ("Child didn't stop with SIGTRAP\n");
	    exit (1);
	  }

	// Step into child signal handler.
	r = ptrace (PTRACE_SINGLESTEP, pid, 0, SIGTRAP);
	if (r != 0)
	  {
	    perror ("ptrace singlestep into signal handler failed");
	    exit (1);
	  }
	waitpid = wait (&status);
	if (! WIFSTOPPED (status) || WSTOPSIG (status) != SIGTRAP)
	  {
	    printf ("child not stop in signal handler with SIGTRAP\n");
	    exit (1);
	  }

	// Now single step child to the end
	do
	  {
	    long r = ptrace (PTRACE_SINGLESTEP, pid, 0, 0);
	    if (r != 0)
	      {
		perror( "ptrace singlestepping failed");
		exit (1);
	      }
	     waitpid = wait (&status);
           }
	while (WIFSTOPPED (status) && WSTOPSIG (status) == SIGTRAP);

	if (waitpid != pid)
	  {
	    perror ("wait after step didn't return the child pid");
	    exit (1);
	  }
	
	if ( ! WIFEXITED (status) || WEXITSTATUS (status) != 0)
	  {
	    printf ("child not properly exited\n");
	    exit (1);
	  }

	exit (0); 
      }
    }
}

