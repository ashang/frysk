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

#include <sys/types.h>
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>
#include <signal.h>
#include <time.h>

#include <gcj/cni.h>

#include "frysk/sys/XXX.h"

jint
frysk::sys::XXX::infLoop ()
{
  pid_t pid = ::fork ();
  if (pid == 0) {
    // want child to loop forever
    for (;;)
      ;
  }
  return (jint)pid;
}

static void *
infLoopThreadFunc (void *args)
{
  for (;;)
    ;
  return NULL;
}

jint
frysk::sys::XXX::infThreadLoop (jint n)
{
  jint i = 0;
  pid_t pid = ::fork ();
  if (pid == 0) {
    // want child to create n threads performing an infinite loop
    pthread_t p[1000];  // up to 1000 threads
    for (i = 0; i < n; ++i) {
      ::pthread_create (&p[i], NULL, &infLoopThreadFunc, NULL);
    }
    
    for (i = 0; i < n; ++i)
      ::pthread_join (p[i], NULL);
    
    ::exit (0);
  }
  ::sleep (1);
  return (jint)pid;
}

jint
frysk::sys::XXX::suspendedProg ()
{
  pid_t pid = ::fork ();
  if (pid == 0) {
    // want child to suspend itself
    sigset_t k;
    ::sigemptyset (&k);
    ::sigsuspend (&k);
  }
  ::sleep (1);
  return (jint)pid;
}

static
void *threadFunc (void *args)
{
  time_t t = time (&t);
  return NULL;
}
  
static
void *cloneThreadFunc (void *args)
{
  jint i;
  pthread_t p[10];  
  for (i = 0; i < 10; ++i)
    ::pthread_create (&p[i], NULL, &threadFunc, NULL);
  
  for (i = 0; i < 10; ++i)
    ::pthread_join (p[i], NULL);

  return NULL;
}

jint
frysk::sys::XXX::infCloneLoop ()
{
  jint i;
  pid_t pid = ::fork ();
  if (pid == 0) {
    // want child to create 1000 threads creating/joining threads
    pthread_t p[1000];
    for (;;) {
      for (i = 0; i < 10; ++i) {
        ::pthread_create (&p[i], NULL, &cloneThreadFunc, NULL);
      }

      for (i = 0; i < 10; ++i)
        ::pthread_join (p[i], NULL);
    }

    ::exit (0);
  }
  ::sleep (1);
  return (jint)pid;
}
