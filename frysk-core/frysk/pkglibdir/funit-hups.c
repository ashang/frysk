// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

/* Until the timeout, keep signalling thy self.  Check that no signals
   are lost. */

#define _GNU_SOURCE

#include <signal.h>
#include <ctype.h>
#include <pthread.h>
#include <limits.h>
#include <stdlib.h>

#include "funit-util.h"



static void
usage ()
{
  printf ("\
Usage: funit-hups [OPTION]\n\
Send SIGHUPS to self, repeatedly.\n\
Where valid options are:\n\
    -t TIMEOUT       Set an ALARM to TIMEOUT seconds; after which exit\n\
                     Default is to not exit\n\
");
  exit (1);
}



static volatile int count;

static void
handler(int sig) {
  if ((count & 1) == 0) {
    fprintf(stdout, "even count %d in signal handler\n", count);
    kill(getpid(), SIGTERM);
  }
  count++;
}

int
main (int argc, char *argv[], char *envp[])
{
  if (argc <= 1) {
    usage();
    exit(1);
  }

  int opt;
  int timeout = 0;

  while ((opt = getopt(argc, argv, "+t:h")) != -1) {
    switch (opt) {
    case 't': // timeout
      timeout = atoi (optarg);
      break;
    case 'h':
    case '?':
      usage();
      exit(1);
    }
  }

  alarm(timeout);
  signal(SIGHUP, handler);

  while (1) {
    if (count & 1) {
      fprintf(stdout, "odd count %d in main\n", count);
      kill(getpid(), SIGTERM);
    }
    count++;
    kill(getpid(), SIGHUP);
  }
}
