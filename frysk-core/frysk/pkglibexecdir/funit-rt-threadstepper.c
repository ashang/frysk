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

#define FIRST 0
#define SECOND 1

pthread_t tester_thread_one;
pthread_t tester_thread_two;

volatile int lock_one;
volatile int lock_two;
volatile pid_t pid;
volatile int sig;

void bak_two ();

int kill_bool = 1;

void 
*signal_parent ()
{
  while (lock_one || lock_two);

  if (kill_bool == 1)
    {
      kill (pid, sig);
      kill_bool = 0;
    }

  signal_parent ();
  return NULL;
}

int bak_two_count = 0;

void
bak_three ()
{
  bak_two_count++;

  if (bak_two_count == 5)
    while (1);

  bak_two ();
}

/* tester_thread_two */
void
bak_two ()
{
  lock_two = 0;
  bak_three ();
}

int bak_recursive_count = 0;
int ret_count = 0;

void
bak_recursive ()
{
  if (bak_recursive_count == 20 || ret_count == 1)
    {
      ret_count = 1;
      bak_recursive_count--;
      return;
    }
  else
    {
      ret_count = 0;
      bak_recursive_count++;
      bak_recursive ();
    }
}

/* tester_thread_one: b = 1 
* tester_thread_two: b = 0 */
void
bak (int b)
{
  if (b == SECOND)
    bak_two ();

  lock_one = 0;
  //while (1);
  bak_recursive ();
}

/* tester_thread_two */
void
baz_two (int b)
{
  bak (b);
} 

/* tester_thread_one */
void
baz ()
{
  bak (FIRST);
}

/* tester_thread_two */
void
bar_two ()
{
  baz_two (SECOND);
}

/* tester_thread_one */
void
bar ()
{
  int i = 12345;
  while (i < 0)
    i--;

  baz ();
}

/* tester_thread_two: a = 0 */
void
foo_two ()
{
  bar_two ();
}

/* tester_thread_one: a = 1 */
void
*foo ()
{
  bar ();
  return NULL;
}

int
main (int argc, char ** argv)
{

  if (argc < 3)
    {
      printf("Usage: funit-rt-threader <pid> <signal>\n");
      exit(0);
    }

  errno = 0;
  pid_t target_pid = (pid_t) strtoul(argv[1], (char **) NULL, 10);
  if (errno)
    {
      perror ("Invalid pid");
      exit (EXIT_FAILURE);
    }
  
  errno = 0;
  int signal = (int) strtoul (argv[2], (char **) NULL, 10);
  if (errno)
    {
      perror ("Invalid signal");
      exit (EXIT_FAILURE);
    }
  
  pid = target_pid;
  sig = signal;

  lock_one = 1;
  lock_two = 1;

  pthread_attr_t attr;
  pthread_attr_init (&attr);

  pthread_create (&tester_thread_one, &attr, signal_parent, NULL);
  pthread_create (&tester_thread_two, &attr, foo, NULL);

  foo_two ();

  return 0;
}
