// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
#include <string.h>
#include <ctype.h>
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

char * proc_file[NR_CHILDREN + 1];

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
      while (1) {
	usleep (250000);
      }
#if 0
      int timeout = 5;
      do 
	timeout -= sleep (timeout);
      while (timeout > 0);
      exit (1);
#endif
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

typedef struct {
  char * tag;
  char * val;
} stt_s;

static stt_s *
read_status (int idx)
{
  FILE * st;
  stt_s * stt;
  int stt_max;
  int stt_nxt;
#define STT_INCR 40

  stt = NULL;
  stt_max = 0;
  stt_nxt = 0;
  
  if (NULL == (st = fopen (proc_file [idx], "r"))) return NULL;
  else {
#define IBFFR_LEN 256
    char * ibffr = alloca (IBFFR_LEN);
    while (NULL != fgets (ibffr, IBFFR_LEN, st)) {
      if (strstr (ibffr, "Uid") || strstr (ibffr, "Gid")) continue;
      char * cptr = strchr (ibffr, ':');
      if (cptr) {
	if (stt_max <= stt_nxt) {
	  stt_max += STT_INCR;
	  stt = realloc (stt, stt_max * sizeof(stt_s));
	}
	*cptr = 0;
	stt[stt_nxt].tag = strdup (ibffr);
	ibffr = cptr + 1;
	cptr = strchr (ibffr, '\n');
	*cptr = 0;
	{
	  int k;
	  char * b = ibffr;
	  int l = strlen (ibffr);
	  for (k = 0; k < l; k++) if (!isspace ((int)ibffr[k])) break;
	  b = &ibffr[k];
	  l -= k;
	  for (k = l-1; k >= 0; k--) if (!isspace ((int)b[k])) break;
	  b[k+1] = 0;
	  stt[stt_nxt++].val = strdup (b);
	}
      }
    }
    if (stt_max <= stt_nxt) {
      stt_max += STT_INCR;
      stt = realloc (stt, stt_max * sizeof(stt_s));
    }
    stt[stt_nxt].tag = NULL;
    stt[stt_nxt++].val = NULL;
    fclose (st);
  }

  return stt;
}

static void
show_status (stt_s * sttv[])
{
  if (sttv) {
    int i;
    int j;
    int max_tl = -1;
    int max_vl[NR_CHILDREN + 1];
    int max_idx;

    for (i = 0; i < NR_CHILDREN + 1; i++) max_vl[i] = -1;

    for (i = 0; sttv[0][i].tag; i++) {
      int tl = strlen (sttv[0][i].tag);
      if (max_tl < tl) max_tl = tl;
    }
    max_idx = i;

    for (i = 0; i < NR_CHILDREN + 1; i++) max_vl[i] = -1;
    
    for (i = 0; i < max_idx; i++) {
      for (j = 0; j < NR_CHILDREN + 1; j++) {
	if ( sttv[j] && sttv[j][i].val) {
	  int vl = strlen (sttv[j][i].val);
	  if (max_vl[j] < vl) max_vl[j] = vl;
	}
      }
    }

    fprintf (stderr, "%*s | ", 2+max_tl, " ");
    for (j = 0; j < NR_CHILDREN + 1; j++) 
      fprintf (stderr, "%*s | ", 2+max_vl[j],
	       (0 == j) ? "Parent" : "Child");
    fprintf (stderr, "\n");

    for(i = 0; i < max_idx; i++) {
      fprintf (stderr, "%*s | ", 2+max_tl, sttv[0][i].tag);
      for (j = 0; j < NR_CHILDREN + 1; j++) 
	fprintf (stderr, "%*s | ", 2+max_vl[j],
		 (sttv[j] && sttv[j][i].val) ? sttv[j][i].val : " ");
      fprintf (stderr, "\n");
    }
  }
}

static void 
dump_status()
{
  int i;
  stt_s * sttv[NR_CHILDREN + 1];
  
  for (i = 0; i < NR_CHILDREN + 1; i++)
    sttv[i] = read_status (i);
  show_status(sttv);
}

static void
decode_status (int status)
{
  if (0)
    ;
  else if (WIFEXITED (status))                // (s & 0x7f) == 0
    fprintf (stderr, "WIFEXITED\n");
  else if (WIFSTOPPED (status)) {     // (s & 0xff) == 0x7f
    fprintf (stderr, "WIFSTOPPED with signal %d\n", WSTOPSIG(status));
#if 0
    switch (WSTOPEVENT (status)) {
    case PTRACE_EVENT_CLONE:
      fprintf (stderr, "\tPTRACE_EVENT_CLONE\n");
      break;
    case PTRACE_EVENT_FORK:
      fprintf (stderr, "\tPTRACE_EVENT_FORK\n");
      break;
    case PTRACE_EVENT_EXIT:
      fprintf (stderr, "\tPTRACE_EVENT_EXIT\n");
      break;
    case PTRACE_EVENT_EXEC:
      fprintf (stderr, "\tPTRACE_EVENT_EXEC\n");
      break;
    case 0:
      fprintf (stderr, "\t0\n");
      break;
    default:
      fprintf (stderr, "\tUnknown\n");
    }
#endif
  }
  else if (WIFSIGNALED (status))      // (((s & 0x7f) + 1) >> 1) > 0
    fprintf (stderr, "WIFSIGNALED with signal %d\n", WTERMSIG(status));
  else if (WIFCONTINUED (status))             // (s == 0xffff) == 0
    fprintf (stderr, "WIFEXITED\n");
  else
    fprintf (stderr, "Unknown\n");
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
  asprintf (&proc_file[0], "/proc/%d/status", (int)parent_pid);

  for (i = 0; i < NR_CHILDREN; i++)
    asprintf (&proc_file[i+1], "/proc/%d/status", (int)dpids[i]);

  signal (SIGALRM, timeout);
  alarm (1);

  fprintf (stderr, "INITIAL STATE\n");
  dump_status();
  
  for (i = 0; i < NR_CHILDREN; i++) {
    int status;
    
    fprintf (stderr, "\n\nAttach to grandchild %d and wait.\n", (int)dpids[i]);
    if (ptrace (PTRACE_ATTACH, dpids[i], NULL, NULL) < 0) {
      perror ("ptrace -- for attach");
      exit (1);
    }
    if (waitpid (dpids[i], &status,  __WALL) < 0)
      perror ("waitpid -- for attach");
    else {
      fprintf (stderr, "wait status = %#08x  ", status);
      decode_status (status);
    }
  }

  fprintf (stderr, "\nAFTER ATTACH\n");
  dump_status();

  fprintf (stderr, "\n\nSTARTING tearDown EMULATION.\n");

  for (i = 0; i < NR_CHILDREN; i++) {
    fprintf (stderr, "\nsending SIGKILL to %d\n", (int)dpids[i]);
    if (kill (dpids[i], SIGKILL)) 
      perror ("kill SIGKILL");
  }

  usleep (250000);

  fprintf (stderr, "\nAFTER kill SIGKILL\n");
  dump_status();

  for (i = 0; i < NR_CHILDREN; i++) {
    fprintf (stderr, "\nsending SIGCONT to %d\n", (int)dpids[i]);
    if (kill (dpids[i], SIGCONT))
      perror ("kill SIGCONT");
  }

  usleep (250000);

  fprintf (stderr, "\nAFTER kill SIGCONT\n");
  dump_status();
  
  for (i = 0; i < NR_CHILDREN; i++) {
    fprintf (stderr, "\ndetaching with SIGCKILL on %d\n", (int)dpids[i]);
    if (ptrace (PTRACE_DETACH, dpids[i], NULL, (void *)SIGKILL) < 0)
      perror ("ptrace PTRACE_DETACH(KILL)");
  }

  usleep (250000);

  fprintf (stderr, "\nAFTER PTRACE_DETACH/SIGKILL\n");
  dump_status();
  
  fprintf (stderr, "\nwaiting for completion\n");
  {
    int kids_remaining = NR_CHILDREN;
    while (kids_remaining > 0) {
      int status;
      pid_t npid = waitpid (-1, &status, 0);

      if (npid == -1) {
	perror ("waitpid(-1, ...)");
	if (errno == ECHILD) {
	  break;
	}
	else
	  // Should only fail with ECHILD - no children.
	  exit (1);
      }
      else {
	fprintf (stderr, "process %d, waitpid returned %#08x  ",
		 (int)npid, status);
	decode_status (status);
	for (i = 0; i < NR_CHILDREN; i++) {
	  if ((npid == dpids[i]) &&
	      (WIFEXITED(status) || WIFSIGNALED(status))) {
	    dpids[i] = -1;
	    kids_remaining--;
	  }
	}
      }
    }
  }

  fprintf (stderr, "\nAFTER waitpid(-1, ...)\n");
  dump_status();
  exit (0);
  
}
