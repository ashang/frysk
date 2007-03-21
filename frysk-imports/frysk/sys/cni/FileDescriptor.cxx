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
#include <stdio.h>
#include <string.h>
#include <sys/poll.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <alloca.h>
#include <fcntl.h>

#include <gcj/cni.h>

#include <java/io/IOException.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/FileDescriptor.h"
#include "frysk/sys/Size.h"

void
frysk::sys::FileDescriptor::close ()
{
  // ::fprintf (stderr, "%d closing %d\n", getpid(), (int)fd);
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
  // ::fprintf (stderr, "wrote <<%c>>\n", c);
  if (err != 0)
    throwErrno (err, "write", "fd", fd);
}

void
frysk::sys::FileDescriptor::write (jbyteArray bytes, jint off, jint len)
{
  errno = 0;
  ::write (fd, elements(bytes) + off, len);
  int err = errno;
  // ::fprintf (stderr, "wrote <<%c>>\n", (char) b);
  if (err != 0)
    throwErrno (err, "write", "fd", fd);
}

jboolean
frysk::sys::FileDescriptor::ready (jlong timeout)
{
  // ::fprintf (stderr, "%d %d ready %ld?\n", getpid (), (int)fd, (long) timeout);
  struct pollfd pollfd = { fd, POLLIN, 0 };
  int count = ::poll (&pollfd, 1, timeout);
  int err = errno;
  // ::fprintf (stderr, "%d ready count %d\n", getpid (), count);
  switch (count) {
  case 1:
    return (pollfd.revents & (POLLIN | POLLHUP)) != 0;
  case 0:
    return false;
  default:
    throwErrno (err, "select", "fd", fd);
  }
}

static jint
doRead (jint fd, void *bytes, jint len)
{
  errno = 0;
  int nr = ::read (fd, bytes, len);
  int err = errno;
  // ::fprintf (stderr, "nr %d errno %d (%s)\n", nr, err, strerror (err));
  switch (nr) {
  case 0:
    return -1; // EOF
  default:
    return nr;
  case -1:
    // Convert a hangup into EOF.
    if (err == EIO) {
      struct pollfd pollfd = { fd, 0, 0 };
      if (::poll (&pollfd, 1, 0) > 0
	  && (pollfd.revents & POLLHUP))
	return -1;
    }
    throwErrno (err, "read", "fd", fd);
  }
}

jint
frysk::sys::FileDescriptor::read (void)
{
  jbyte b = 0;
  errno = 0;
  int nr = doRead (fd, &b, 1);
  if (nr >= 0)
    return b & 0xff;
  else
    return nr;
}

jint
frysk::sys::FileDescriptor::read (jbyteArray bytes, jint off, jint len)
{
  return doRead (fd, elements(bytes) + off, len);
}

jint
frysk::sys::FileDescriptor::open (jstring file, jint f)
{
  const char* pathname = ALLOCA_STRING (file);
  // ::fprintf ("opening <<%s>>\n", pathname);
  int flags = 0;
  if (f & frysk::sys::FileDescriptor::RDONLY)
    flags |= O_RDONLY;
  if (f & frysk::sys::FileDescriptor::WRONLY)
    flags |= O_WRONLY;
  if (f & frysk::sys::FileDescriptor::RDWR)
    flags |= O_RDWR;
  int gc_count;
  while (1) {
    errno = 0;
    int fd = ::open (pathname, flags);
    int err = errno;
    if (fd >= 0)
      return fd;
    switch (err) {
    case EMFILE:
      tryGarbageCollect (gc_count, err, "open");
      continue;
    default:
      throwErrno (errno, "open");
    }
  }
}

void
frysk::sys::FileDescriptor::dup (frysk::sys::FileDescriptor *old)
{
  errno = 0;
  int gc_count = 0;
  // ::fprintf (stderr, "%d dup (%d, %d)\n", getpid (), (int)old->fd, (int)fd);
  while (::dup2 (old->fd, fd) < 0) {
    int err = errno;
    // ::fprintf (stderr, "err = %d %s\n", err, strerror (err));
    switch (err) {
    case EMFILE:
      tryGarbageCollect (gc_count, err, "dup2");
      continue;
    default:
      throwErrno (err, "dup2");
    }
  }
  // ::fprintf (stderr, "%d dup done\n", getpid ());
}

frysk::sys::Size *
frysk::sys::FileDescriptor::getSize()
{
    struct winsize size;

    errno = 0;
    if (::ioctl(fd, TIOCGWINSZ, (char *)&size) < 0)
    {
	throwErrno(errno, "ioctl");
    }
    return new frysk::sys::Size(size.ws_row, size.ws_col);
}

void
frysk::sys::FileDescriptor::setSize(frysk::sys::Size *jsize)
{
    struct winsize size;

    errno = 0;
    ::memset(&size, 0, sizeof(size));
    size.ws_row = jsize->getRows();
    size.ws_col = jsize->getColumns();
    if (::ioctl(fd, TIOCSWINSZ, (char *)&size) < 0)
    {
	throwErrno(errno, "ioctl");
    }
}
