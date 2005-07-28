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
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>

struct args {
  char **argv;
  char **envp;
};

static void *
exec_args (void *arg)
{
  struct args *args = arg;
  execve (args->argv[1], args->argv + 1, args->envp);
  /* Reaching here implies an error.  */
  perror ("execve");
  exit (1);
  
}

int
main (int argc, char **argv, char **envp)
{
  if (argc < 2) {
    printf ("Usage: %s command args ...\n", argv[0]);
    return 1;
  }

  struct args args;
  args.argv = argv;
  args.envp = envp;
  pthread_t thread;
  if (pthread_create (&thread, NULL, exec_args, &args)) {
    perror ("pthread_create");
    exit (1);
  }

  void *retval;
  if (pthread_join (thread, &retval)) {
    perror ("pthread_join");
    exit (1);
  }

  /* Should this ever be reached?  */
  exit (1);
}
