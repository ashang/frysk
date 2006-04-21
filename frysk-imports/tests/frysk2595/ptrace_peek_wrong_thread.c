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

void *test_thread(void* param)
{
	errno = 0;
	long val = ptrace(PTRACE_PEEKUSER, c_pid, 0, 0);
	
	if (errno != 0)
	{
		perror("Ptrace error in thread for PTRACE_PEEKUSER");
		exit(1);
	}
		
	printf("Got word: %lx\n", val);
	
	return NULL;
}

int 
main (int argc, char **argv) 
{
	int status;
	c_pid = fork();
	
	if(c_pid == -1)
	{
		perror("Error: could not fork child process");
		exit(1);
	}
	
	if (c_pid == 0)
	{
		// Child
		long ptrace_res = ptrace(PTRACE_TRACEME, 0, NULL, NULL);
		
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
	else
	{
		// Parent
		errno = 0;
		pid_t wait_res = waitpid(c_pid, &status, __WALL); 

		if (wait_res != c_pid)
		{
			perror("waitpid didn't return the child pid");
			exit(1);
		}
		
		pthread_t thread;
		pthread_create(&thread, NULL, test_thread, NULL);
		void* dummy;
		pthread_join(thread, &dummy);
		
		tkill(c_pid, SIGKILL);
	}
	
	return 0;
}
