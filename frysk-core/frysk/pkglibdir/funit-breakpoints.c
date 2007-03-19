// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <setjmp.h>

// Counters for how many times the breakpoint functions have been called.
// Used as sanity check to tell the tester the functions have actually ran.
static int bp1;
static int bp2;

static pid_t pid;

// Counters to check whether real and received Trap signals match
// and don't interfere with the breakpoints inserted by frysk.
static int send_trap;
static int received_trap;

static int send_hup;
static int received_hup;

static sigjmp_buf env;

static void
signal_handler(int sig)
{
  if (sig == SIGHUP)
    {
      received_hup++;
    }
  else if (sig == SIGTRAP)
    {
      received_trap++;
      // Trap handler is triggered by inline invalide instruction in dummy()
      // longjmp around it.
      siglongjmp(env, SIGTRAP);
    }
  else
    {
      fprintf (stderr, "Wrong signal recieved %d\n", sig);
      exit (-1);
    }
}

// Tries to trick frysk by sending sighup signals and by having its
// own trap instruction. This doesn't always work. We have a workaround
// here for the fact that ptrace uses SIGTRAP itself to signal stepping
// see bug #3997 for a discussion about this.
static void
dummy()
{
  // Sending ourselves an HUP signal.
  kill (pid, SIGHUP);
  send_hup++;

  // Generating a trap event ourselves, simulating "bad code".  Setup
  // a sigsetjump so we can handle it and return from this function
  // safely when the signal handler uses longjmp. On some
  // architectures the PC isn't incremented on invallid/trapping
  // instructions. So this makes sure we skip it when we return.
  if (sigsetjmp (env, 1) == 0)
    {
      send_trap++;
#if defined(__i386__) || defined(__x86_64__)
      asm("int3");
#elif defined(__powerpc64__) || defined(__powerpc__)
      asm(".long 0x7d821008");
#else
      #error unsuported architecture
#endif
      abort(); // Should never be reached.
    }
  else
    {
      // Returned from signal handler through longjmp.
      return;
    }
}

static void
first_breakpoint_function ()
{
  bp1++;
  dummy();
}

static void
second_breakpoint_function ()
{
  bp2++;
  dummy();
}

int
main (int argc, char *argv[], char *envp[])
{
  pid = getpid();
  received_trap = 0;
  send_trap = 0;
  received_hup = 0;
  send_hup = 0;

  // To work around a design issue in ptrace we need to define the
  // SIGTRAP handler as reentrant. See bug #3997 for a discussion.
  // FIXME - Needed till a non-ptrace mechanism (utrace) is available
  // that doesn't reuse SIGTRAP to signal attached process about events.
  struct sigaction action;
  action.sa_handler = signal_handler;
  sigemptyset (&action.sa_mask);
  action.sa_flags = SA_NODEFER;
  sigaction (SIGTRAP, &action, NULL);

  // The sighup handler doesn't need any special flags.
  signal (SIGHUP, &signal_handler);

  // The number of runs the tester wants us to do.
  // Zero when we should terminate.
  // Minus one when something went wrong.
  int runs;

  // Function call counters.
  bp1 = 0;
  bp2 = 0;

  // Tell the tester the addresses of the functions to put breakpoints on.
  // There's great difference to get the addresses of one function between 
  // PPC64 and other platform(such as X86/X86_64). What we get through the
  // the form "&function_name" is the address of function descriptor but 
  // not the true entry address of the function on PPC64.
#ifndef __powerpc64__
  printf("%p\n", &first_breakpoint_function);
  printf("%p\n", &second_breakpoint_function);
#else
  printf("%p\n", (void *)(*(long *)&first_breakpoint_function));
  printf("%p\n", (void *) (*(long *)&second_breakpoint_function));
#endif

  fflush(stdout);

  // Go round and round.
  while (1)
    {
      // Wait till we are OK to go!
      runs = getchar();
      if (runs == 0)
	break;
      if (runs < 0) 
        {
	  fprintf(stderr, "Couldn't read runs\n");
	  break;
        }

      while(runs--)
	{
	  first_breakpoint_function ();
	  second_breakpoint_function ();
	}

      printf("%d\n", bp1);
      printf("%d\n", bp2);
      fflush(stdout);
    }

  // Have our own trap signals and trap instructions triggered?
  if (send_trap != received_trap)
    {
      fprintf (stderr, "TRAP send: %d, recv: %d\n", send_trap, received_trap);
      exit (-1);
    }
  if (send_hup != received_hup)
    {
      fprintf (stderr, "HUP send: %d, recv: %d\n", send_hup, received_hup);
      exit (-1);
    }

  // Finally re-exec ourselves to show breakpoints are gone.
  // When called with an argument then we are execing ourselves just to
  // do a little testrun (all breakpoints should be cleared now).
  if (argc > 1)
    return runs;
  else
    {
      char *new_argv[3];
      new_argv[0] = argv[0];
      new_argv[1] = "restart";
      new_argv[2] = NULL;
      execve (argv[0], new_argv, envp);
    }

  // Should never be reached.
  perror ("unreachable");
  return -1;
}
