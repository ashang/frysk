#include <stdio.h>
#include <error.h>
#include <errno.h>
#include <unistd.h>
#include <malloc.h>
#include <stdarg.h>


/*
 * for write:
 *
 *	orig_eax = syscall nr (4)
 *	eax = rc (-ENOSYS = -38)
 *	ebx = fd (1 for stdout)
 *	ecx = addr of string
 *	edx = length of string
 *
 * for read:
 *
 *	orig_eax = syscall nr (4)
 *	eax = rc (-ENOSYS = -38)
 *	ebx = fd (6 for opened file)
 *	ecx = addr of string
 *	edx = length of string (4096 for buffered read)
 *
 * for open
 *
 *	orig_eax = syscall nr (5)
 *	eax = rc (-ENOSYS = -38)
 *
 * for close
 *
 *	orig_eax = syscall nr (6)
 *	eax = rc (-ENOSYS = -38)
 *	ebx = fd (6 for opened file)
 *
 * for alloca
 *
 *	orig_eax = syscall nr (45 == brk)
 *	eax = rc (-ENOSYS = -38)
 *
 */

char arg1[] = {"one"};
char arg2[] = {"two"};
char arg3[] = {"three"};

char * val[] = {
  arg1,
  arg2,
  arg3,
  NULL
};

main()
{
#define BSIZE 256
  char * bffr = malloc (BSIZE);
  FILE * file = fopen ("./aux/testsyscall.c", "r");
  if (!file) error (1, errno, "fopen");

  fprintf (stderr, "bffr = %08x\n", file);
  
  vfprintf (stdout, "%s %s %s\n", (va_list)val);

  while (fgets (bffr, BSIZE, file)) fprintf (stdout, "%s", bffr);

  fclose (file);
  free (bffr);
  _exit (0);
}
