// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
// Copyright 2007 Oracle Corporation.
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

#include "jni.hxx"

#include "jnixx/exceptions.hxx"
#include "jnixx/elements.hxx"
#include "jnixx/bounds.hxx"

using namespace java::lang;
using namespace frysk::sys;

void
FileDescriptor::close(jnixx::env env, jint fd) {
  // ::fprintf (stderr, "%d closing %d\n", getpid(), (int)fd);
  errno = 0;
  ::close (fd);
  if (errno != 0)
    errnoException(env, errno, "close", "fd %d", (int)fd);
}

void
FileDescriptor::write(jnixx::env env, jint fd, jint b) {
  char c = b;
  errno = 0;
  ::write(fd, &c, 1);
  int err = errno;
  // ::fprintf (stderr, "wrote <<%c>>\n", c);
  if (err != 0)
    errnoException(env, err, "write", "fd %d", (int)fd);
}

jint
FileDescriptor::write(jnixx::env env, jint fd,
		      jnixx::jbyteArray bytes,
		      jint off, jint len)
{
  verifyBounds(env, bytes, off, len);
  errno = 0;
  jbyteArrayElements b = jbyteArrayElements(env, bytes);
  int size = ::write(fd, b.elements() + off, len);
  int err = errno;
  // ::fprintf (stderr, "wrote <<%c>>\n", (char) b);
  if (err != 0)
    errnoException(env, err, "write", "fd %d", (int)fd);
  return size;
}

bool
FileDescriptor::ready(jnixx::env env, jint fd, jlong timeout)
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
    errnoException(env, err, "select", "fd %d", (int)fd);
  }
}

static jint
doRead(jnixx::env env, jint fd, void *bytes, jint len)
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
    errnoException(env, err, "read", "fd %d", (int)fd);
  }
}

jint
FileDescriptor::read(jnixx::env env, jint fd) {
  jbyte b = 0;
  errno = 0;
  int nr = doRead(env, fd, &b, 1);
  if (nr >= 0)
    return b & 0xff;
  else
    return nr;
}

jint
FileDescriptor::read(jnixx::env env, jint fd,
		     jnixx::jbyteArray bytes,
		     jint off, jint len) {
  verifyBounds(env, bytes, off, len);
  jbyteArrayElements b = jbyteArrayElements(env, bytes);
  jint ok = doRead(env, fd, b.elements() + off, len);
  b.release();
  return ok;
}

jint
FileDescriptor::rdonly(jnixx::env) {
  return O_RDONLY;
}

jint
FileDescriptor::wronly(jnixx::env) {
  return O_WRONLY;
}

jint
FileDescriptor::rdwr(jnixx::env) {
  return O_RDWR;
}

jint
FileDescriptor::creat(jnixx::env) {
  return O_CREAT;
}

jint
FileDescriptor::open(jnixx::env env, String file, jint flags, jint mode) {
  // ::fprintf ("opening <<%s>>\n", pathname);
  jstringUTFChars pathname = jstringUTFChars(env, file);
  int fd = ::open(pathname.elements(), flags, mode);
  if (fd < 0) {
    errnoException(env, errno, "open", "file %s", pathname.elements());
  }
  return fd;
}

void
FileDescriptor::dup(jnixx::env env, jint fd, jint old) {
  errno = 0;
  // ::fprintf (stderr, "%d dup (%d, %d)\n", getpid (), (int)old->fd, (int)fd);
  if (::dup2(old, fd) < 0) {
    errnoException(env, errno, "dup2");
  }
  // ::fprintf (stderr, "%d dup done\n", getpid ());
}

Size
FileDescriptor::getSize(jnixx::env env, jint fd) {
  struct winsize size;
  errno = 0;
  if (::ioctl(fd, TIOCGWINSZ, (char *)&size) < 0) {
    errnoException(env, errno, "ioctl");
  }
  return Size::New(env, size.ws_row, size.ws_col);
}

void
FileDescriptor::setSize(jnixx::env env, jint fd, Size jsize) {
  struct winsize size;

  errno = 0;
  ::memset(&size, 0, sizeof(size));
  size.ws_row = jsize.getRows(env);
  size.ws_col = jsize.getColumns(env);
  if (::ioctl(fd, TIOCSWINSZ, (char *)&size) < 0) {
    errnoException(env, errno, "ioctl");
  }
}

static jlong
seek(jnixx::env env, int fd, jlong off, int where) {
  errno = 0;
  jlong pos = ::lseek64 (fd, off, where);
  int err = errno;
  if (err != 0)
    errnoException(env, err, "lseek", "fd %d offset %lld",
		   (int)fd, (long long)off);
  return pos;
}

jlong
FileDescriptor::seekSet(jnixx::env env, jint fd, jlong off) {
  return ::seek(env, fd, off, SEEK_SET);
}

jlong
FileDescriptor::seekCurrent(jnixx::env env, jint fd, jlong off) {
  return ::seek(env, fd, off, SEEK_CUR);
}

jlong
FileDescriptor::seekEnd(jnixx::env env, jint fd, jlong off) {
  return ::seek(env, fd, off, SEEK_END);
}
