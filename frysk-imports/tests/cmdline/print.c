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

// Passes various combinations of argv values to a sub process, that
// sub process prints them out in a java-code like form.

#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/wait.h>

void
spawn (char *filename, char *envp[], ...)
{
  int argc;
  va_list ap;
  char **argv = NULL;
  
  argc = 0;
  va_start (ap, envp);
  while (1) {
    argv = realloc (argv, sizeof (char **) * (argc + 1));
    argv[argc] = va_arg (ap, char *);
    if (argv[argc] == NULL) break;
    argc++;
  }
  va_end (ap);

  printf ("public void test");
  for (argc = 0; argv[argc] != NULL; argc++) {
    printf ("_%s0", argv[argc]);
  }
  printf (" ()\n{\n");

  printf ("  check (new String[] {");
  for (argc = 0; argv[argc] != NULL; argc++) {
    printf ("%s\"%s\"", argc ? ", " : " ", argv[argc]);
  }
  printf (" },\n");
  printf ("         new byte[] {");
  fflush (stdout);

  int pid = fork ();
  switch (pid) {
  case 0:
    execve (filename, argv, envp);
    perror ("execve");
    abort ();
  case -1:
    perror ("fork");
    abort ();
  default:
    {
      int status;
      if (waitpid (pid, &status, 0) < 0) {
	perror ("waitpid");
	abort ();
      }
      if (!WIFEXITED (status) || WEXITSTATUS (status) != 0) {
	fprintf (stderr, "bad status %d\n", status);
	abort ();
      }
    }
  }

  printf ("});\n}\n");
}

int
main (int argc, char **argv, char **envp)
{
  char dump[] = "./cmdline/dump";
  spawn (dump, envp, NULL);
  spawn (dump, envp, "", NULL);
  spawn (dump, envp, "a", NULL);
  spawn (dump, envp, "", "", NULL);
  spawn (dump, envp, "a", "", NULL);
  spawn (dump, envp, "", "b", NULL);
  spawn (dump, envp, "a", "b", NULL);
  spawn (dump, envp, "a", "b", "c", NULL);
  return (0);
}
