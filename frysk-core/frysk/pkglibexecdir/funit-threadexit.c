#define _GNU_SOURCE

#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <limits.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include "util.h"

void
usage ()
{
  printf ("\
Usage:\n\
Where:\n\
    <pid> <sig> <sec>  Manager process to signal, signal to use once\n\
                     a thread completes, and how long to wait to join\n\
                     the thread.\n\
");
  _exit (1);
}

static pthread_barrier_t barrier;
pthread_cond_t  condition_cond  = PTHREAD_COND_INITIALIZER;
pthread_mutex_t condition_mutex = PTHREAD_MUTEX_INITIALIZER;

pthread_t thread_id = 0;

void *
op_thread (void *np)
{
  thread_id = gettid ();

  // Make sure scan_thread gets the thread_id
  pthread_mutex_lock( &condition_mutex );
  pthread_cond_signal( &condition_cond );
  pthread_mutex_unlock( &condition_mutex );

  while (thread_id != 0) {
    pthread_mutex_lock( &condition_mutex );
    pthread_cond_wait( &condition_cond, &condition_mutex );
    pthread_mutex_unlock( &condition_mutex );
  }

  trace ("thread creation: PID=%d TTID=%d", (int)*((pthread_t*)np), (int)thread_id);
  return NULL;
}

/* Scan /proc for state of thread_id.  */

void *
scan_thread (void *thread_status)
{
  int fd = 0;
  char buf [1024];
  char buffer [1024];
  char state;
  struct stat statbuf;

  // Wait for the thread_id from op_thread
  while (thread_id == 0) {
    pthread_mutex_lock( &condition_mutex );
    pthread_cond_wait( &condition_cond, &condition_mutex );
    pthread_mutex_unlock( &condition_mutex );
  }

  *((int*)thread_status) = 0;

  sprintf ((char*)&buf, "/proc/%d/task/%d/stat", (int) getpid(), (int) thread_id);
  OK (stat,(buf, &statbuf));
  
  errno = 0;
  fd = open (buf, O_RDONLY);
  if (errno != 0) {
    perror ("opening");
    return 0;
  }

  pthread_mutex_lock( &condition_mutex );
  thread_id = 0;
  pthread_cond_signal( &condition_cond );
  pthread_mutex_unlock( &condition_mutex );

  while (1)    
    {
      lseek (fd, 0, SEEK_SET);
      if (errno != 0) {
	perror ("seeking");
	break;
      }
      read (fd, buffer, sizeof (buffer));
      if (errno != 0) {
	perror ("reading");
	break;
      }
      char* commEnd = strchr (buffer, ')');
      state = buffer[(long)(commEnd - buffer + (void*)2)];

      if (state == 'X') {
	trace ("thread termination: tid=%d state=X", (int)thread_id);
	*((int*)thread_status) = 1;
	break;
      }
    }

  close (fd);
  pthread_barrier_wait (&barrier);
  return 0;
}

int
main (int argc, char *argv[])
{
  int sig;
  int pid;
  int sec;
  pthread_t p;
  pthread_t q;
  pthread_t threads;
  pthread_attr_t pthread_attr;
  int thread_status;

  if (argc < 4)
    usage ();
  pid = atol (argv[1]);
  sig = atol (argv[2]);
  sec = atol (argv[3]);

  pthread_attr_init (&pthread_attr);
  pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);

  trace ("PID=%d SIG=%d SEC=%d", pid, sig, sec);

  tkill (pid, sig); // ack.

  threads = gettid();
  // Synchronization barrier; need both main and the new thread.
  OK (pthread_barrier_init, (&barrier, NULL, 2));
  if (pthread_create (&q, &pthread_attr, scan_thread, &thread_status)) {
    perror ("pthread_create: scan_thread");
    _exit (1);
  }

  if (pthread_create (&p, &pthread_attr, op_thread, &threads)) {
    perror ("pthread_create: op_thread");
    _exit (1);
  }
  pthread_barrier_wait (&barrier);
  OK (pthread_join, (q, NULL));

  if (thread_status)
    tkill (pid, sig); // ack.
  else
    abort ();

  printf ("%d\n", thread_status);
  sleep (sec);

  OK (pthread_join, (p, NULL));

  return 0;
}
