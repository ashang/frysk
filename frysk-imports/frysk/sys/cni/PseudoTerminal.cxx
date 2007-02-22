// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

#define _XOPEN_SOURCE
#include <sys/ioctl.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>

#include <gcj/cni.h>

#include "frysk/sys/PseudoTerminal.h"
#include "frysk/sys/PseudoTerminal$RedirectStdio.h"
#include "frysk/sys/cni/Errno.hxx"

jint
frysk::sys::PseudoTerminal::open (jboolean controllingTerminal)
{
  int master;

  int flags = O_RDWR | (controllingTerminal ? O_NOCTTY : 0);
  master = ::posix_openpt (flags);
  if (master < 0) {
    int err = errno;
    throwErrno (err, "posix_openpt");
  }

  if (::grantpt (master) < 0) {
    int err = errno;
    ::close (master);
    throwErrno (err, "grantpt", "fd", master);
  }

  if (::unlockpt (master) < 0) {
    int err = errno;
    ::close (master);
    throwErrno (err, "unlockpt", "fd", master);
  }

  return master;
}

jstring
frysk::sys::PseudoTerminal::getName ()
{
  char* pts_name = ::ptsname (fd);
  if (pts_name == NULL)
    throwErrno (errno, "ptsname");
  return JvNewStringUTF (pts_name);
}

void
frysk::sys::PseudoTerminal$RedirectStdio::reopen ()
{
  // Detach from the existing controlling terminal.
  int fd = ::open ("/dev/tty", O_RDWR|O_NOCTTY);
  if (fd < 0) {
    ::perror ("open (old controlling terminal)");
  }
  if (fd >= 0) {
    if (::ioctl (fd, TIOCNOTTY, NULL) < 0)
      ::perror ("ioctl (/dev/tty, TIOCNOTTY)");
    ::close (fd);
  }

  // Verify that the detach worked, this open should fail.
  fd = ::open ("/dev/tty", O_RDWR|O_NOCTTY);
  if (fd >= 0) {
    ::perror ("open (re-open old controlling terminal)");
    ::exit (1);
  }
  
  // Make this process the session leader.
  if (::setsid () < 0) {
    ::perror ("setsid");
  }

  if (::getpgrp () != ::getpid ()) {
    perror ("grp and pid differ");
    exit (1);
  }

//   // Move this to the session's process group.
//   if (::setpgid (0, getpid ()) < 0) {
//     perror ("setpgrp");
//   }

  // Open the new pty.
  char *pty = ALLOCA_STRING (name);
  int tty = open (pty, O_RDWR|O_NOCTTY);
  if (tty < 0) {
    perror ("open.pty");
    exit (1);
  }

  // Make the pty's tty the new controlling terminal.
  if (::ioctl (tty, TIOCSCTTY, NULL) < 0) {
    ::perror ("ioctl.TIOSCTTY");
    exit (1);
  }

  if (::dup2 (tty, STDIN_FILENO) < 0) {
    perror ("dup2.STDIN");
    exit (1);
  }
  if (::dup2 (tty, STDOUT_FILENO) < 0) {
    perror ("dup2.STDOUT");
    exit (1);
  }
  if (::dup2 (tty, STDERR_FILENO) < 0) {
    perror ("dup2.STDERR");
    exit (1);
  }
}
