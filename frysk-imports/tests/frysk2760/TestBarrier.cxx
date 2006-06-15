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

#include <pthread.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"
#include <errno.h>
#include <sys/wait.h>
#include <alloca.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

int errno;

#include <gcj/cni.h>

#include "frysk2760/TestBarrier.h"

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_barrier_t barrier;
pthread_t timer_thread;
pid_t thread_pid;

jint
frysk2760::TestBarrier::doFork () {
	if ((pid = fork()) == 0) {
		printf("Waiting on barrier\n");
		pthread_barrier_wait(&barrier);
	}
	printf("My PID is: %d\n", getpid());
	return pid;
}

void
frysk2760::TestBarrier::timerThread () {

  errno = 0;
  ::ptrace ((enum __ptrace_request) PTRACE_ATTACH, \
			     pid, NULL, 0);
			     
	if (errno != 0)
    	::perror("ptrace - timer");
    	
  printf("Limit reached - deadlock occured! Exiting!\n");
  kill(pid, 15);
  exit(1);
}

void
frysk2760::TestBarrier::initBarrier ()
{
  pthread_barrier_init(&barrier, NULL, 4);
}

void
frysk2760::TestBarrier::doWork ()
{
  pthread_mutex_lock(&mutex);
  pthread_mutex_unlock(&mutex);
  int pid;
  if ((pid = fork()) < 0) {
  	perror("Error: could not fork child process");
	exit(EXIT_FAILURE);
  } else if (pid == 0) {
  	
  	errno = 0;
  	::ptrace ((enum __ptrace_request) PTRACE_TRACEME, \
			     pid, NULL, 0);
			     
	if (errno != 0)
    	::perror("ptrace");
			     
	::execv ("/bin/true", NULL);
	::perror ("execvp");
  }
  else {
  long i;
  for (i = 0; i < 1000000; i+=2)
  	i--;
  printf("Waiting on barrier\n");
  pthread_barrier_wait(&barrier);
  printf("Through barrier, killing timer\n");
  kill(thread_pid, SIGTERM);
  }
}
