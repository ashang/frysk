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

#define _GNU_SOURCE

#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/unistd.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <linux.syscall.h>

_syscall0(pid_t,gettid);

static pthread_barrier_t thread_running_barrier;
static pthread_barrier_t running_thread_can_exit;
volatile pthread_t thread_id = 0;

void *
op_thread (void *np)
{
  thread_id = gettid ();
  printf ("%d.%d thread creation: TTID=%d\n",
	  getpid (), gettid(),
	  (int)thread_id);
  // Make sure scan_thread gets the thread_id
  pthread_barrier_wait (&thread_running_barrier);
  // And don't exit until the main thread is good and ready.
  pthread_barrier_wait (&running_thread_can_exit);
  return NULL;
}

#define OK(FUNC,ARGS) { if (FUNC ARGS) { perror (#FUNC); exit (1); } }

int
main (int argc, char *argv[])
{
  pthread_t p;

  alarm (5); // Always bail

  OK (pthread_barrier_init, (&thread_running_barrier, NULL, 2));
  OK (pthread_barrier_init, (&running_thread_can_exit, NULL, 2));

  // Create a thread that (almost) immediatly exits, synconronize with
  // it to ensure that the global THREAD_ID was set.
  pthread_attr_t pthread_attr;
  pthread_attr_init (&pthread_attr);
  OK (pthread_create, (&p, &pthread_attr, op_thread, NULL));

  // Wait for the thread to start running, and then open that thread's
  // /proc/stat file.
  pthread_barrier_wait (&thread_running_barrier);
  int fd = -1;
  {
    char *path = NULL;
    asprintf (&path, "/proc/%d/task/%d/stat", (int) getpid(), (int) thread_id);
    if (path == NULL) {
      perror ("asprintf");
      exit (1);
    }
    fd = open (path, O_RDONLY);
    if (fd <= 0) {
      perror ("open");
      exit (1);
    }
    printf ("%d.%d opened %s fd %d\n",
	    getpid (), gettid(),
	    path, fd);
  }

  // Let the thread exit, watch /proc/stat until it either dissapears
  // (utrace kernel), or switches to the 'X' state (non-utrace
  // kernel).
  pthread_barrier_wait (&running_thread_can_exit);
  while (1)    
    {
      char buffer [1024];
      int n = pread (fd, buffer, sizeof (buffer), 0);
      if (n <= 0) {
	// On FC-6 the thread completly disappears from /proc.
	if (errno == ESRCH) {
	  printf ("%d.%d pread returns %d (%s)\n",
		  getpid (), gettid(),
		  errno, strerror (errno));
	  exit (1);
	}
	perror ("pread");
	exit (1);
      }
      char* commEnd = strchr (buffer, ')');
      char state = buffer[(long)(commEnd - buffer + (void*)2)];
      printf ("%d.%d pread returns %d, state=%c\n",
	      getpid (), gettid(),
	      n, state);
      if (state == 'X') {
	printf ("%d.%d thread termination: tid=%d state=X",
		getpid (), gettid(),
		(int)thread_id);
	break;
      }
      usleep (1000 * 100); // 10th of a second, hopefully
    }
  close (fd);

  // Clean up.
  OK (pthread_join, (p, NULL));

  return 0;
}
