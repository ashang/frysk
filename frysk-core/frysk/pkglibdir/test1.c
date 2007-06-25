#include <sys/types.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#include <math.h>

volatile int running = 1;
volatile int result;
volatile double doubleResult;

static void
signal_handler(int sig)
{
  running = 0;
}

int anotherFunction(int a, int b)
{
  result = a + b;
  return result;
}

int funcWithoutParams(void)
{
  return 0;
}

static inline int add(int i, int j)
{
  result = i + j;
  return result;
}

double doubleFun(double a, double b)
{
  doubleResult = sin(a) + cos(b);
  return doubleResult;
}

int main (int argc, char **argv)
{
  int i = 0;

  if (argc > 2)
    {
      pid_t target_pid;
      int signal;
      
      errno = 0;
      target_pid = (pid_t)strtoul(argv[1], (char **)0, 10);
      if (errno)
	{
	  perror("Invalid pid");
	  exit(1);
	}
      signal = (int)strtoul(argv[2], (char **)0, 10);
      if (errno)
	{
	  perror("Invalid signal");
	  exit(1);
	}
      kill(target_pid, signal);
    }
  signal(SIGALRM, &signal_handler);
  while (running)
    {
      if (i >= 0)
	{
	  i--;
	  add(i, 2 * i);
	  anotherFunction(i, i);
	}
      else
	{
	  i++;
	  add(2 * i, i);
	  doubleFun((double)(2 * i), (double)i);
	}
    }
  return 0;
}

