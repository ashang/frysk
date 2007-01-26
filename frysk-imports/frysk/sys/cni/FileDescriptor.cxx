// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

#include <errno.h>
#include <unistd.h>
#include <sys/select.h>
#include <stdio.h>

#include <gcj/cni.h>

#include <java/io/IOException.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/FileDescriptor.h"

void
frysk::sys::FileDescriptor::close ()
{
  // ::printf ("closing %d\n", (int)fd);
  errno = 0;
  ::close (fd);
  if (errno != 0)
    throwErrno (errno, "close", "fd", fd);
  fd = -1;
}

void
frysk::sys::FileDescriptor::write (jint b)
{
  char c = b;
  errno = 0;
  ::write (fd, &c, 1);
  int err = errno;
  // ::printf ("wrote <<%c>>\n", c);
  if (err != 0)
    throwErrno (err, "write", "fd", fd);
}

void
frysk::sys::FileDescriptor::write (jbyteArray bytes, jint off, jint len)
{
  errno = 0;
  ::write (fd, elements(bytes) + off, len);
  int err = errno;
  // ::printf ("wrote <<%c>>\n", (char) b);
  if (err != 0)
    throwErrno (err, "write", "fd", fd);
}

jboolean
frysk::sys::FileDescriptor::ready ()
{
  fd_set readfds;
  FD_ZERO (&readfds);
  FD_SET (fd, &readfds);

  struct timeval timeout = { 0, 0 };

  errno = 0;
  int count = ::select (fd + 1, &readfds, NULL, NULL, &timeout);
  int err = errno;
  // ::printf ("ready count %d\n", count);
  switch (count) {
  case 1:
    return true;
  case 0:
    return false;
  default:
    throwErrno (err, "select", "fd", fd);
  }
}

jint
frysk::sys::FileDescriptor::read (void)
{
  jbyte b;
  errno = 0;
  int nr = ::read (fd, &b, 1);
  int err = errno;
  switch (nr) {
  case 0:
    return -1; // EOF
  case 1:
    return b & 0xff;
  default:
    throwErrno (err, "read", "fd", fd);
  }
}

jint
frysk::sys::FileDescriptor::read (jbyteArray bytes, jint off, jint len)
{
  errno = 0;
  int nr = ::read (fd, elements(bytes) + off, len);
  int err = errno;
  if (nr == 0)
    return -1; // EOF
  else if (nr > 0)
    return nr;
  else
    throwErrno (err, "read", "fd", fd);
}

JArray<frysk::sys::FileDescriptor*>*
frysk::sys::FileDescriptor::pipe ()
{
  int gc_count = 0;
  const int nfds = 2;
  int filedes[nfds];
  while (::pipe (filedes) < 0) {
    int err = errno;
    // ::printf ("err = %d %s\n", err, strerror (err));
    switch (err) {
    case EMFILE:
      tryGarbageCollect (gc_count, err, "pipe");
      continue;
    default:
      throwErrno (err, "pipe");
    }
  }
  // printf ("pipe [%d, %d]\n", filedes[0], filedes[1]);
  JArray<frysk::sys::FileDescriptor*>* fds
    = ( JArray<frysk::sys::FileDescriptor*>*)
    JvNewObjectArray (nfds, &frysk::sys::FileDescriptor::class$, NULL);
  for (int i = 0; i < nfds; i++) {
    elements(fds)[i] = new frysk::sys::FileDescriptor (filedes[i]);
  }
  return fds;
}
