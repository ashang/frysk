#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <errno.h>
#include <linux/unistd.h>
#include <pthread.h>

#define __REENTRANT

_syscall2(int, tkill, int, tid, int, sig);

pid_t c_pid;

void *
thread_forks (void* param)
{
  int status;
  c_pid = fork();
  
  switch (c_pid)
    {
    case -1:
      {
	perror("Error: could not fork child process");
	exit(1);
      }
    case 0:
      {
	// Child
	long ptrace_res = ptrace (PTRACE_TRACEME, 0, NULL, NULL);
	if (ptrace_res == -1)
	  {
	    perror("Error: could not start trace");
	    exit(1);
	  }
	char* const args[] = {"/bin/true", NULL};
	execv(args[0], args);
	perror("Exec returned. Whoops!");
	exit(1);
      }
    default:
      {
	printf ("waiting for child\n");
	// Parent
	errno = 0;
	pid_t wait_res = waitpid (c_pid, &status, __WALL); 
	if (wait_res < 0)
	  {
	    perror ("wating for child from thread");
	    exit (1);
	  }
	// Status?
	printf ("ptracing child from thread\n");
	errno = 0;
	ptrace (PTRACE_PEEKUSER, c_pid, 0, 0);
	if (errno != 0)
	  {
	    perror ("attempting to ptrace peek from thread");
	    exit (1);
	  }
      }
    }
  return NULL;
}

int 
main (int argc, char **argv) 
{
  pthread_t thread;
  if (pthread_create(&thread, NULL, thread_forks, NULL) != 0)
    {
      perror ("pthread_create");
      exit (1);
    }
  void* dummy;
  if (pthread_join(thread, &dummy) != 0)
    {
      perror ("pthread_join");
      exit (1);
    }
  errno = 0;
  ptrace (PTRACE_PEEKUSER, c_pid, 0, 0);
  if (errno != 0)
    {
      perror ("attempting to ptrace from main after thread exit");
      exit (1);
    }
  tkill(c_pid, SIGKILL);
  return 0;
}
