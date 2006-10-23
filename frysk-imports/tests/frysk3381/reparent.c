/*	
 * frysk-imports/tests/frysk3381
 */

/*
 * This test starts off with a fork, the child of which forks again.  The
 * first child then terminates, forcing the second child to be reparented
 * to init.
 *
 * Before the first fork, the original parent has opened a pair of pipes.
 * After the first fork, the first child, just before it exits, writes the
 * pid of the second child to the first parent.  The first parent ptrace-
 * attaches to that pid, its grandchiild's pid.  (There are probably more
 * efficient ways to report back a pid to a grandparent, but I couldn't
 * think of any.)
 *
 * This code emulates the kill -KILL, kill -CONT, detach -KILL, sequence used
 * by frysk tearDown, at the end of which the grandchild proc has been
 * successfully detached and killed, and has reported its exit to its nominal
 * parent, init, (evidenced by the grandchild not becoming a zombie), but never
 * reports its exit to frysk.
 *
 * I think this demonstrates a bug in ptrace-over-utrace.
 */

#define _GNU_SOURCE
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <alloca.h>
#include <signal.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ptrace.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <linux/ptrace.h>

#define NR_CHILDREN 3
volatile pid_t dpids[NR_CHILDREN];
volatile pid_t parent_pid;
volatile pid_t pid;
volatile pid_t pid2;

char * proc_fn[NR_CHILDREN];
char * proc_ppid[NR_CHILDREN];
char * proc_file[NR_CHILDREN];

int pipes[] = {-1, -1};


void
timeout (int sig)
{
  fprintf (stderr, "test timeout, aborting\n");
  kill (parent_pid, SIGKILL);
  exit (1);
}

static void
make_grandkid()
{
  pid2 = fork ();
  switch (pid2) {
  case -1:
    perror ("fork2");
    exit (1);
  case 0: // child
    {
      int timeout = 5;
      do 
	timeout -= sleep (timeout);
      while (timeout > 0);
      exit (1);
    }
    exit (0);
    break;
  default: // Parent
    // send child's (i.e., parent's grandchild) pid to parent 
    close (pipes[0]);
    write(pipes[1], (void *)(&pid2), sizeof(pid_t));
    exit(0);
    break;
  }
}
  
int
main (int ac, char * av[])
{
  int i;

  for (i = 0; i < NR_CHILDREN; i++) {
    pipe (pipes);
    pid = fork ();
    switch (pid) {
    case -1:
      perror ("fork");
      exit (1);
    case 0: // child
      make_grandkid();
      break;
    default: // Parent
      {
	pid_t qpid = -88;
      
	waitpid (pid, NULL, 0);
    
	close (pipes[1]);

	{
	  int rc;
	  int ct;
	  int run;
	
	  ct = 0;
	  run = 1;	
	  while (run && (ct < sizeof(pid_t))) {
	    rc = read (pipes[0], ct + &qpid, sizeof(qpid)-ct);
	    if (rc <= 0) run = 0;
	    else ct += rc;
	  }
	  if (ct < sizeof(pid_t)) {
	    exit (1);
	  }
	  dpids[i] = qpid;
	}
      }
      break;
    }
  }

  parent_pid = getpid();
  signal (SIGALRM, timeout);
  alarm (1);

  for (i = 0; i < NR_CHILDREN; i++) {
    asprintf (&proc_file[i], "/proc/%d", (int)dpids[i]);
    asprintf (&proc_fn[i], "cat /proc/%d/status | grep State:", (int)dpids[i]);
    asprintf (&proc_ppid[i], "cat /proc/%d/status | grep PPid:", (int)dpids[i]);
    fprintf (stderr, "[%d] Initial:\t\t\t\t", dpids[i]);
    system (proc_fn[i]);
    fprintf (stderr, "[%d]         \t\t\t\t", dpids[i]);
    system (proc_ppid[i]);
  }
  
  fprintf (stderr, "\nAttach to grandchild and wait for the stop\n");
  for (i = 0; i < NR_CHILDREN; i++) {
    if (ptrace (PTRACE_ATTACH, dpids[i], NULL, NULL) < 0) {
      perror ("ptrace -- for attach");
      exit (1);
    }
    if (waitpid (dpids[i], NULL,  __WALL) < 0) {
      perror ("waitpid -- for attach");
      exit (1);
    }
    
    fprintf (stderr, "[%d] After attach:\t\t\t\t", dpids[i]);
    system (proc_fn[i]);
  }

  fprintf (stderr, "\nsending SIGKILL\n");
  for (i = 0; i < NR_CHILDREN; i++) {
    kill (dpids[i], SIGKILL);
    fprintf (stderr, "[%d] After SIGKILL:\t\t\t\t", dpids[i]);
    system (proc_fn[i]);
  }

  fprintf (stderr, "\nsending SIGCONT\n");
  for (i = 0; i < NR_CHILDREN; i++) {
    kill (dpids[i], SIGCONT);
    fprintf (stderr, "[%d] After SIGCONT:\t\t\t\t", dpids[i]);
    system (proc_fn[i]);
  }
  
  fprintf (stderr, "\ndetaching with SIGCKILL\n");
  for (i = 0; i < NR_CHILDREN; i++) {
    if (ptrace (PTRACE_DETACH, dpids[i], NULL, (void *)SIGKILL) < 0) {
      perror ("ptrace -- for detach");
      exit (1);
    }
    fprintf (stderr, "[%d] After detach:\t\t\t\t", dpids[i]);
    if (0 == access (proc_file[i], R_OK))
      system (proc_fn[i]);
    else
      fprintf (stderr, "No longer exists\n");
  }
  
  fprintf (stderr, "\nwaiting for completion\n");
  {
    int kids_remaining = NR_CHILDREN;
    while (kids_remaining > 0) {
      int status;
      pid_t npid = waitpid (-1, &status, 0);
      for (i = 0; i < NR_CHILDREN; i++) {
	if ((npid == dpids[i]) && (WIFEXITED(status) || WIFSIGNALED(status))) {
	  dpids[i] = -1;
	  kids_remaining--;
	}
      }
    }
  }
  exit (0);
  
}
