// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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
#include <alloca.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <gcj/cni.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/ProcessIdentifier.h"
#include "frysk/sys/Daemon.h"
#include "frysk/sys/Redirect.h"
#include "frysk/sys/Execute.h"

jint
frysk::sys::Daemon::daemon (frysk::sys::Redirect* redirect,
			    frysk::sys::Execute* exec)
{
  volatile int pid = -1;

  // This is executed by the child with the parent blocked, the final
  // process id ends up in PID.
  errno = 0;
  register int v = vfork ();
  switch (v) {
  case -1:
    throwErrno (errno, "vfork");
  case 0:
    // vforked child
    // ::fprintf (stderr, "%d is vfork child\n", getpid ());
    pid = fork ();
    switch (pid) {
    case -1:
      // error handled by parent; look for pid<0.
      _exit (0);
    case 0:
      // ::fprintf (stderr, "%d child calls redirect\n", getpid ());
      redirect->reopen ();
      // ::fprintf (stderr, "%d child calls execute\n", getpid ());
      exec->execute ();
      _exit (0);
    default:
      _exit (0);
    }
  default:
    // Reach here after the vfork child - or middle player - exits.
    // Save the fork's error status.
    int fork_errno = errno;
    // Consume the middle players wait.
    errno = 0;
    pid_t wpid = ::waitpid (v, NULL, 0);
    int wait_errno = errno;
    // Did the fork succeed?  If not throw its status.
    if (pid < 0)
      throwErrno (fork_errno, "fork");
    // Did the wait succeed?  If not throw its status.
    if (wpid < 0)
      throwErrno (wait_errno, "waitpid", "process", v);
    // printf ("v %d pid %d\n", v, pid);
    redirect->close ();
    return pid;
  }
}
