// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <linux/unistd.h>

void
handler (int n)
{
  _exit (0);
}

int
main (int argc, char *argv[], char *envp[])
{
  if (argc != 4)
    {
      printf ("Usage: kill/loop <pid> <signal> <loop-seconds>\n");
      exit (1);
    }
  int pid = atol (argv[1]);
  int sig = atol (argv[2]);
  int sec = atol (argv[3]);

  // Use tkill, instead of tkill (pid, sig) so that an exact task is
  // signalled.  Normal kill can send to any task and other tasks may
  // not be ready.
  if (syscall (__NR_tkill, pid, sig) < 0) {
    perror ("tkill");
    exit (errno);
  }

  signal (SIGALRM, handler);
  alarm (sec);

  while (1);
}
