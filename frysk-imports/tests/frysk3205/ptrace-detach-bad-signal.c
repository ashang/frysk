// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <linux/unistd.h>
#include <linux/ptrace.h>

pid_t pid;

void
timeout (int sig)
{
  _exit (1);
}
  
int
main (int ac, char * av[])
{
  int rc = 1;		/* assume we're going to fail	*/
  
  /* dummy out SIGUSR1 so it won't default to TERM */
  signal (SIGUSR1, SIG_IGN);
  
  signal (SIGALRM, timeout);
  alarm (2);
  
  pid = fork();

  switch ((int)pid) {
  case -1:
    perror ("fork():");
    exit (1);
  case 0:			/* child */
    pause();
    break;
  default:			/* parent */
    if (0 == ptrace (PTRACE_ATTACH, pid, NULL, NULL)) {

      /* wait for the attach */
      if (-1 != waitpid (pid, NULL,  0)) {

	/* this PTRACE_DETACH should fail because of the invalid signr;	*/
	/* if it passes, then the behaviour is wrong so the test fails.	*/
	if ( -1 == ptrace (PTRACE_DETACH, pid, NULL, (void *)999)) {
    
	  /* but if the previous DETACH failed, as it should, there's	*/
	  /* no way to tell whether te failure is due to the invalid	*/
	  /* signal or to an actual failed detach except by trying to 	*/
	  /* detach again, this time with a good signal.  if this	*/
	  /* DETACH passes, that means the previous DETACH didn't	*/
	  /* detach, which it shouldn't have because of the bad signr,	*/
	  /* so the behaviour matches historical ptrace and therefore 	*/
	  /* the test passes.  if this DETACH fails, it can only, in 	*/
	  /* theory, be beacuse the previous DETACH succeeded, which it	*/
	  /*  shouldn't have, so the test fails.			*/
	  if (0 == ptrace (PTRACE_DETACH, pid, NULL, (void *)SIGUSR1)) rc = 0;
	}
      }
    }
    
    kill (pid, SIGKILL);
    if (-1 == waitpid (pid, NULL,  0)) _exit (1);

    exit (rc);
    break;
  }
  exit (1);  /* shouldn't be reached */
}
