#define _GNU_SOURCE

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
fork_exec_thread (void *data)
{
  char **argv = data;
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
	// Parent
	errno = 0;
	int status;
	pid_t wait_res = waitpid (c_pid, &status, __WALL); 
	if (wait_res != c_pid)
	  {
	    perror("waitpid didn't return the child pid");
	    exit(1);
	  }
	// Now exec ourselves with the pid as a parameter.
	char *args[3];
	args[0] = argv[0];
	asprintf (&args[1], "%d", c_pid);
	args[2] = NULL;
	printf ("execing %s %s\n", args[0], args[1]);
	execv (args[0], args);
	perror ("execing with ptraced child\n");
	exit (1);
      }
    }
}

  
int 
main (int argc, char **argv) 
{
  switch (argc)
    {
    default:
      printf ("whats with the %d arguments\n", argc);
      exit (1);

    case 1: // First time run
      {
	pthread_t thread;
	if (pthread_create(&thread, NULL, fork_exec_thread, argv) != 0)
	  {
	    perror ("attempting to create fork_exec_thread");
	    exit (1);
	  }
	void *result;
	pthread_join (thread, &result);
	printf ("outch thread was joined\n");
	exit (1);
      }

    case 2:// The exec of this process
      {
	c_pid = atol (argv[1]);
	printf ("after exec, pid is %d\n", c_pid);
	errno = 0;
	ptrace(PTRACE_PEEKUSER, c_pid, 0, 0);
	if (errno != 0)
	  {
	    perror("Ptrace after exec");
	    exit(1);
	  }
      }
    }
  tkill(c_pid, SIGKILL);
  return 0;
}
