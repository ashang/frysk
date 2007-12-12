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

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include <sys/syscall.h>

#ifdef __NR_gettid
static pid_t gettid (void)
{
    return syscall(__NR_gettid);
}
#else
static pid_t gettid (void)
{
    return -ENOSYS;
}
#endif

pthread_t tester_thread;

pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

static char * myname;

void
*do_it ()
{
  int t = 34543;
  while (t > 0)
    t--;

  //fprintf (stderr,"attach %s pid=%d -task tid=%d -cli\n", myname, getpid(), gettid());

  int d = 1;
  int e = 0;
  pid_t f = gettid();
  f++;

  while (1)
    {
      d++;
      e++;
      if (d == 3)
        {
          if (e == 3)
            e = 0;
          d = 0;
        }
    }
    
  return NULL;
}

void
bak ()
{
  while (1)
    {
      //fprintf (stderr,"attach %s pid=%d -task tid=%d -cli\n", myname, getpid(), gettid());
      int a = 0;
      int b = 0;
      int c = 0;
      while (1)
        {
          a++;
          b++;
          c++;
          if (a + b > 4)
            {
              a = a - c;
              b = b - c;
              c = 0;
            }
        }
    }
}

void 
baz ()
{
  int a = 1;
  int b = 0;
  a++;
  b++;
  bak ();
}

void 
bar ()
{
  close (-1);
  close (-1);
  baz ();
  /*Comment */
}

void
foo ()
{
  bar ();
}


int 
main (int argc, char **argv)
{
  myname = argv[0];
  pthread_attr_t attr;
  pthread_attr_init (&attr);
  pthread_create (&tester_thread, &attr, do_it, NULL);

  /* This is a comment */
  foo ();
  int t = 30;
  t++;
  exit (0);
}

