// gcc -std=gnu99 -O2 -o forker forker.c -lm

#define  _GNU_SOURCE
#include <getopt.h>
#include <sys/types.h>
#include <math.h>
#include <wait.h>
#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#undef _POSIX_C_SOURCE
#define _POSIX_C_SOURCE 199309
#include <time.h>
#include <sys/time.h>
#include <errno.h>

/* response time in microseconds */
#define DEFAULT_CHILD_RESP 5.0e5
double child_resp = DEFAULT_CHILD_RESP;

double zero_d;
 int active = 0;
 FILE * ofile = NULL;

static struct option long_options[] = {
  {"log",            required_argument, 0, (int)'l'},
  {"child-response", required_argument, 0, (int)'c'},
  {0, 0, 0, 0}
};

static void
kidcatcher (int signum)
{
  int status;

  wait (&status);
  --active;
  // fprintf (stderr, "caught %d\n", active);
  if (SIG_ERR == signal(SIGCHLD, kidcatcher)) _exit (1);
}

static void
usage (char * an)
{
  fprintf (stdout, "%s [options] <count> <target>\n", an);
  fprintf (stdout, "<count> is the number of iterations, zero or\n");
  fprintf (stdout, "\tnegative for infinite.\n");
  fprintf (stdout, "<target> is the number of processes to maintain.\n");
  fprintf (stdout, "options:\n");
  fprintf (stdout, "\t -l <filenmae>\n");
  fprintf (stdout, "\t --log=<filenmae>\n");
  fprintf (stdout, "\t\trecord history in file <filename>.\n");
  fprintf (stdout, "\t -c delay\n");
  fprintf (stdout, "\t --child-response=delay\n");
  fprintf (stdout, "\t\taverage longevity of child procs in seconds.  (Default 0.5)\n");
}

static inline double
timeval_to_double (struct timeval * tv)
{
  return ((double)(tv->tv_sec)) + (((double)(tv->tv_usec))/1.0e6);
}

int
main(int ac, char * av[])
{
  {
    int c;
    int option_index = 0;

    while (-1 != (c = getopt_long (ac, av, "c:hl:",
				   long_options, &option_index))) {
      switch(c) {
      case (int)'?':
      case (int)':':
      case (int)'h':
      case 0:
	usage (av[0]);
	_exit (0);
	break;
      case (int)'c':
	child_resp = 1.0e6 * atof (optarg);
	if (1.0e5 > child_resp) {
	  fprintf (stderr,
		   "Invalid child response time.  Must be greater than .1sec\n");
	  _exit (6);
	}
	break;
      case (int)'l':
	if (NULL == (ofile = fopen (optarg, "w"))) {
	  perror ("opening log file");
	  _exit (2);
	}
	else {
	  struct timeval tv;
	  
	  gettimeofday (&tv, NULL);
	  zero_d =  timeval_to_double (&tv);
	}
	break;
      }
    }
  }

  if ((ac - optind) < 2) {
    usage (av[0]);
    _exit (3);
  }

  {
    int limit = atoi (av[optind]);
    int forever = (0 >= limit) ? 1 : 0;
    int target = atoi (av[optind + 1]);
    double pfactor = 5.0e8;

    srand48 (time (0));
  
    if (SIG_ERR == signal(SIGCHLD, kidcatcher)) _exit (4);

    while (forever || (0 <= limit--))  {
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
	usleep (child_resp + lrint (((2.0 * drand48()) - 1.0)
				   * (.9 * child_resp)));
	_exit (0);
	break;
      default:		/* parent */
	if (active < target)      pfactor /= 1.1;
	else if (active > target) pfactor *= 1.1;
	{
	  int run;
	  struct timespec rs;
	  struct timespec ts = {0, lrint (drand48() * pfactor)};
	  fprintf (stderr, "iteration %d active %d pfactor %g delay %g\r",
		   limit, active, ((double)pfactor)/1.0e9,
		   ((double)ts.tv_nsec)/1.0e9);
	  if (ofile) {
	    struct timeval tv;

	    gettimeofday (&tv, NULL);
	    fprintf (ofile, "%g %3d\n",
		     timeval_to_double (&tv) - zero_d,
		     active);
	  }
	  run = 1;
	  while (run) {
	    if (0 == nanosleep (&ts, &rs)) run = 0;
	    else {
	      if (EINTR == errno) ts = rs;
	      else _exit (5);
	    }
	  }
	}
	break;
      }
    }

    if (ofile) fclose (ofile);
  }
  _exit(0);
}
