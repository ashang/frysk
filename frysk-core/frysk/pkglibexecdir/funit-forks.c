// gcc -std=gnu99 -O -o forker forker.c -lm

#define  _GNU_SOURCE
#include <sys/types.h>
#include <math.h>
#include <wait.h>
#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
//#define _POSIX_C_SOURCE 199309
#include <time.h>
#include <errno.h>

int active = 0;

static void
kidcatcher (int signum)
{
  int status;

  wait (&status);
  --active;
  // fprintf (stderr, "caught %d\n", active);
  if (SIG_ERR == signal(SIGCHLD, kidcatcher)) _exit (1);
}

int
main(int ac, char * av[])
{
  int limit = atoi (av[1]);
  int target = atoi (av[2]);
  double pfactor = 5.0e8;

  srand48 (time (0));
  
  if (SIG_ERR == signal(SIGCHLD, kidcatcher)) _exit (1);

  while (0 <= limit--)  {
    pid_t kid_pid;

    active++;
    //    fprintf (stderr, "forking %d\n", active);
    
    kid_pid = fork();

    switch (kid_pid) {
    case -1:		/* error */
      perror ("fork()");
      break;
    case 0:		/* child */
      //   fprintf (stderr, "child\n");
      //usleep (500000 + lrint (((2.0 * drand48()) - 1.0) * 4.9e5));
      _exit (0);
      break;
    default:		/* parent */
      if (active < target)      pfactor /= 1.1;
      else if (active > target) pfactor *= 1.1;
      {
	int run;
	struct timespec rs;
	struct timespec ts = {0, lrint (drand48() * pfactor)};
	//fprintf (stderr, "iteration %d active %d pfactor %g delay %g\r",
	//	 limit, active, ((double)pfactor)/1.0e9,
	//	 ((double)ts.tv_nsec)/1.0e9);
	run = 1;
	while (run) {
	  if (0 == nanosleep (&ts, &rs)) run = 0;
	  else {
	    if (EINTR == errno) ts = rs;
	    else _exit (1);
	  }
	}
      }
      break;
    }
  }
  return 0;
}
