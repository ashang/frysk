// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>

// When counter is zero stop.
static volatile int counter;

// Dummy counter to increment so we look busy.
static volatile int i;

// struct to set itimer.
static struct itimerval ival;

static void
signal_handler(int sig)
{
  if (sig == SIGPROF)
    {
      counter--;
      if (counter == 0)
	{
	  // Shutdown timer.
	  ival.it_value.tv_sec = 0;
	  ival.it_value.tv_usec = 0;
	  setitimer (ITIMER_PROF, &ival, NULL);
	}
    }
  else
    {
      fprintf (stderr, "Wrong signal recieved %d\n", sig);
      exit (-1);
    }
}

int
main (int argc, char *argv[])
{
  counter = 3;

  signal (SIGPROF, &signal_handler);

  // Setup a timer to fire after 0.005 seconds again and again.
  ival.it_value.tv_sec = 0;
  ival.it_value.tv_usec = 5000;
  ival.it_interval.tv_sec = 0;
  ival.it_interval.tv_usec = 5000;
  if (setitimer (ITIMER_PROF, &ival, NULL) != 0)
    {
      perror ("setitimer failed");
      exit (-1);
    }

  while (counter > 0)
    i += counter; // Do something useful...

  return 0;
}
