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
#include <pthread.h>
#include <stdlib.h>
#include <signal.h>

static
void infLoop (int sig)
{
  for (;;)
    ;
}

static
void *threadFunc (void *args)
{
  int *i = (int *)0;
  // We create a SIGSEGV to ensure accudog knows that the thread
  // has indeed started the infinite loop
  signal (SIGSEGV, &infLoop);
  *i = 0;
}

int
main (int argc, char **argv)
{
  pthread_t p[2]; 
  int i;

  for (i = 0; i < 2; ++i) {
    pthread_create (&p[i], NULL, &threadFunc, NULL);
  }

  for (i = 0; i < 2; ++i)
    pthread_join (p[i], NULL);

  exit (0);
}


