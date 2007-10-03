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
#include <stdio.h>
#include <sys/types.h>
#include <signal.h>
#include <unistd.h>
#include <errno.h>
#include <stdlib.h>
#include <pthread.h>

volatile pid_t pid;
volatile int sig;
pthread_t thread;

void bar ();

void *signal_parent (void* args)
{
  kill (pid, sig);  
  while (1);
}

void jump ()
{
	volatile int z = 1;										// _stepOutStart_
	volatile int y = 2;										// _stepAdvanceStart_
	volatile int x = 3;
	volatile int w = (((((x + y + z) * 20) / 10) - 0) + 1);	// _instructionStep_
	w++;													// _lineStepEnd_
	return;
}

  volatile int a = 0;
  volatile int b = 0;
  volatile int c = 0;
  volatile long d = 0;

void foo ()
{
  
  while (1)
    {
      a++;
      b++;
      c++;
      d = a;
      if (a + b == 2)
		{
	 	 if (b + d == 2)
	  	  {
	 	     a = 0;
	 	     b = 0;												
		     c = 0;												
		     d = 0;
	 		 if (d == 0)
				d = 1;
	    	}
		}
		jump ();											// _stepOver_
    }														
}

int main (int argc, char ** argv)
{

  if(argc < 3)
    {
      printf ("Usage: funit-rt-stepper <pid> <signal>\n");
      exit (0);
    }

  errno = 0;
  pid_t target_pid = (pid_t) strtoul (argv[1], (char **) NULL, 10);
  if (errno)
    {
      perror ("Invalid pid");
      exit (1);
    }
  
  errno = 0;
  int signal = (int) strtoul (argv[2], (char **) NULL, 10);
  if (errno)
    {
      perror ("Invalid signal");
      exit (1);
    }
    
  pthread_attr_t attr;
  pthread_attr_init (&attr);
    
  pid = target_pid;
  sig = signal;
    
  pthread_create (&thread, &attr, signal_parent, NULL);

  foo ();
  
  return 0;
}
