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
Usage: <pid> <sig> <timeout>\n\
Where:\n\
    <pid> <sig>    Notify <pid> with <sig> once thread has exited.\n\
    <timeout>      Terminate program after <timeout>\n\
Creates a thread that exits but isn't joined, putting it into the X state.\n\
");
  exit (1);
}

static pthread_barrier_t thread_running_barrier;
static pthread_barrier_t running_thread_can_exit;
volatile pthread_t thread_id = 0;

void *
op_thread (void *np)
{
  thread_id = gettid ();
  trace ("thread creation: TTID=%d", (int)thread_id);
  // Make sure scan_thread gets the thread_id
  pthread_barrier_wait (&thread_running_barrier);
  // And don't exit until the main thread is good and ready.
  pthread_barrier_wait (&running_thread_can_exit);
  return NULL;
}

int
main (int argc, char *argv[])
{
  int sig;
  int pid;
  int sec;
  pthread_t p;

  if (argc < 4)
    usage ();
  pid = atol (argv[1]);
  sig = atol (argv[2]);
  sec = atol (argv[3]);
  trace ("PID=%d SIG=%d TIMEOUT=%d", pid, sig, sec);

  alarm (sec);

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
    if (path == NULL)
      pfatal ("asprintf");
    fd = open (path, O_RDONLY);
    if (fd <= 0)
      pfatal ("open");
    trace ("opened %s fd %d", path, fd);
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
	  trace ("pread returns %d (%s)", errno, strerror (errno));
	  break;
	}
	pfatal ("pread");
      }
      char* commEnd = strchr (buffer, ')');
      char state = buffer[(long)(commEnd - buffer + (void*)2)];
      trace ("pread returns %d, state=%c", n, state);
      if (state == 'X') {
	trace ("thread termination: tid=%d state=X", (int)thread_id);
	break;
      }
      usleep (1000 * 100); // 10th of a second, hopefully
    }
  close (fd);

  // Tell the parent that the thread has reached 'X' state.
  tkill (pid, sig); // ack.
  
  // Stop until signaled in some way.
  sigset_t mask;
  sigprocmask (SIG_BLOCK, NULL, &mask);
  sigsuspend (&mask);

  // Clean up.
  OK (pthread_join, (p, NULL));

  return 0;
}
