// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

#include <stdio.h>
#include <stdarg.h>
#include <sys/types.h>
#include <stdlib.h>
#include <linux.syscall.h>

_syscall0(pid_t, gettid);
_syscall2(int, tkill, pid_t, tid, int, sig);


// Like perror() but also abort the program.

static void pfatal (const char *syscall) __attribute__ ((noreturn));
static void
pfatal (const char *syscall)
{
  perror (syscall);
  abort ();
}

// Print a fatal error (and abort).

static void fatal  (const char *fmt, ...) __attribute__ ((noreturn))  __attribute__ ((format (printf, 1, 2)));
static void
fatal (const char *fmt, ...)
{
  char *buf;
  va_list ap;
  va_start (ap, fmt);
  if (vasprintf (&buf, fmt, ap) < 0)
    pfatal ("vasprintf");
  fprintf (stderr, "%d.%d: %s\n", getpid (), gettid (), buf);
  abort ();
}

// Print a trace message.

static void trace (const char *fmt, ...) __attribute__ ((format (printf, 1, 2))) __attribute__ ((unused));
static void
trace (const char *fmt, ...)
{
  char *buf;
  va_list ap;
  va_start (ap, fmt);
  if (vasprintf (&buf, fmt, ap) < 0)
    pfatal ("vasprintf");
  va_end (ap);
  printf ("%d.%d: %s\n", getpid (), gettid (), buf);
  free (buf);
}

// Wrapper to check that a function's return status is zero; use as
// OK(func,(arg list)).

static void
ok (const char *call, int status)
{
  if (status != 0)
    pfatal (call);
}

#define OK(FUNC,ARGS) ok (#FUNC, FUNC ARGS)
