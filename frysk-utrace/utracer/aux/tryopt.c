#define _GNU_SOURCE
#include <getopt.h>
#include <stdio.h>
#include <unistd.h>

main (int ac, char * av[])
{
  int run = 1;

  fprintf (stderr, "pass 1\n");
  while (1 == run) {
    int val = getopt_long (ac, av, "a:m:w:l:r", NULL, NULL);
    switch (val) {
    case 'a':
      fprintf (stderr, "got an a: %s\n", optarg);
      break;
    case 'm':
      fprintf (stderr, "got an m: %s\n", optarg);
      break;
    case 'r':
      run = 0;
      break;
    }
  }

  fprintf (stderr, "pass 2\n");
  while (1 == run) {
    int val = getopt_long (ac, av, "a:m:w:l:r", NULL, NULL);
    switch (val) {
    case 'a':
      fprintf (stderr, "got an a: %s\n", optarg);
      break;
    case 'm':
      fprintf (stderr, "got an m: %s\n", optarg);
      break;
    case 'r':
      _exit (0);
      break;
    }
  }
}

