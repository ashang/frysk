// This file is part of the program FRYSK.
//
// Copyright 2006 Red Hat Inc.
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

#include "frysk/proc/ForkTestLib.h"
#include "frysk/proc/ForkTestLib$ForkedInputStream.h"
#include "frysk/proc/ForkTestLib$ForkedOutputStream.h"
#include "frysk/proc/ForkTestLib$ForkedProcess.h"

#include <java/io/IOException.h>

frysk::proc::ForkTestLib$ForkedProcess*
frysk::proc::ForkTestLib::fork (jstringArray args)
{
  // Convert args into argv, argc.
  int argc = JvGetArrayLength (args);
  char **argv = (char **) alloca ((argc + 1) * sizeof (void*));
  for (int i = 0; i < argc; i++) {
    jstring arg = elements (args)[i];
    int len = JvGetStringUTFLength (arg);
    argv[i] = (char *) alloca (len + 1);
    JvGetStringUTFRegion (arg, 0, arg->length (), argv[i]);
    argv[i][len] = '\0';
  }
  argv[argc] = 0;

  // Create input/output pipes.
  int pfds[2][2];
  errno = 0;
  if (pipe (pfds[0]) == -1)
    throwErrno (errno, JvNewStringLatin1("pipe"));
  errno = 0;
  if (pipe (pfds[1]) == -1)
    throwErrno (errno, JvNewStringLatin1("pipe"));

  // Fork/exec
  errno = 0;
  pid_t pid = ::fork ();
  switch (pid) {
  case -1:
    // Fork failed.
    throwErrno (errno, JvNewStringLatin1("fork"));
  case 0:
    // Child
    dup2 (pfds[0][0], 0);
    close (pfds[0][1]);
    dup2 (pfds[1][1], 1);
    close (pfds[1][0]);
    ::execvp (argv[0], argv);
    // This should not happen.
    ::perror ("execvp");
    ::_exit (errno);
  default:
    frysk::proc::ForkTestLib$ForkedInputStream *in;
    in = new frysk::proc::ForkTestLib$ForkedInputStream (pfds[1][0]);
    close (pfds[1][1]);
    frysk::proc::ForkTestLib$ForkedOutputStream *out;
    out = new frysk::proc::ForkTestLib$ForkedOutputStream (pfds[0][1]);
    close (pfds[0][0]);
    return new frysk::proc::ForkTestLib$ForkedProcess (pid, in, out);
  }
}

void
frysk::proc::ForkTestLib$ForkedOutputStream::write (jint i)
{
  jbyte b;
  int w;

  b = (jbyte) i;
  errno = 0;
  w = ::write (fd, &b, 1);
  if (w == -1)
    throw new java::io::IOException (JvNewStringLatin1 (strerror (errno)));
}

jint
frysk::proc::ForkTestLib$ForkedInputStream::read (void)
{
  jbyte b;
  int r;
  errno = 0;
  r = ::read (fd, &b, 1);
  if (r == 0)
    return -1;
  if (r == -1)
    throw new java::io::IOException (JvNewStringLatin1 (strerror (errno)));

  return b & 0xff;
}

jint
frysk::proc::ForkTestLib$ForkedInputStream::read (jbyteArray buf, jint off,
						  jint len)
{
  jbyte *bs = elements (buf) + off;
  int r;
  errno = 0;
  r = ::read (fd, bs, len);
  if (r == 0)
    return -1;
  if (r == -1)
    throw new java::io::IOException (JvNewStringLatin1 (strerror (errno)));

  return r;
}
