#include <pthread.h>
#include <stdlib.h>
#include <signal.h>

static
void infLoop (int sig)
{
  for (;;)
    ;
}

static
void *threadFunc (void *args)
{
  int *i = (int *)0;
  // We create a SIGSEGV to ensure accudog knows that the thread
  // has indeed started the infinite loop
  signal (SIGSEGV, &infLoop);
  *i = 0;
}

int
main (int argc, char **argv)
{
  pthread_t p[2]; 
  int i;

  for (i = 0; i < 2; ++i) {
    pthread_create (&p[i], NULL, &threadFunc, NULL);
  }

  for (i = 0; i < 2; ++i)
    pthread_join (p[i], NULL);

  exit (0);
}


