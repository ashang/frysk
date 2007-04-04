// gcc -g -o stepper -lpthread stepper.c

#include <stdio.h>
#include <sys/types.h>
#include <signal.h>
#include <unistd.h>
#include <errno.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/time.h>
#include <assert.h>
/* For `UNW_TARGET_*'.  */
#include <libunwind.h>

volatile int lock;
volatile pid_t pid;
volatile int sig;

void shazaam () {
	volatile __attribute__((__unused__)) char a;
	volatile __attribute__((__unused__)) char b;
	volatile __attribute__((__unused__)) char c;
	volatile __attribute__((__unused__)) char d;
}

void jump ()
{
	int z = 1;
	int y = 2;
	int x = 3;
	volatile __attribute__((__unused__)) int w = (((((x + y + z) * 20) / 10) - 0) + 1);
	return;
}

void *signal_parent (void* args)
{
  pthread_exit (NULL);
}

void foo ()
{
  int a = 0;
  int b = 0;
  int c = 0;
  long d = 0;
  lock = 0;
  while (1)
    {
      jump ();
      a++;
      b++;
      c++;
      d = a;
      if (a + b == 2)
		{
	 	 if (b + d == 2)
	  	  {
	 	     a = 0;
	 	     b = 0;
		     c = 0;
		     d = 0;
	 		 if (d == 0)
				d = 1;
	    	}
		}
		signal(SIGUSR1,SIG_IGN);
		raise(SIGUSR1);
		jump ();
    }
}

#if defined (UNW_TARGET_X86_64) || defined (UNW_TARGET_X86)
asm (".cfi_startproc\n.globl lockup\n.type lockup,@function\nlockup: jmp lockup\n.cfi_endproc");
extern void lockup (void);
#elif defined (UNW_TARGET_PPC64)
asm (".cfi_startproc\n.globl lockup\n.type lockup,@function\nlockup: b   lockup\n.cfi_endproc");
extern void lockup (void);
#else
#warning "FIXME: Test may not work."
static void lockup (void)
{
  for (;;);
}
#endif

void prefoo ()
{
int err;

	signal (SIGALRM, (void (*)(int signo)) foo);
	err = alarm (1);
	assert (err == 0);
	lockup ();
}

int main (int argc, char ** argv)
{
  
  lock = 1;

  pthread_t thread;
  pthread_create (&thread, NULL, signal_parent, NULL);

  /* Start the tracing by: ./test-ptrace -t ...  */
  signal(SIGUSR1,SIG_IGN);
  raise(SIGUSR1);

  prefoo ();

  return 0;
}
