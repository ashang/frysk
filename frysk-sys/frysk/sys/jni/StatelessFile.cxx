// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include "jni.hxx"

#include "jnixx/bounds.hxx"
#include "jnixx/exceptions.hxx"
#include "jnixx/elements.hxx"

using namespace java::lang;

jint
frysk::sys::StatelessFile::pread(jnixx::env env, jlong fileOffset,
				 jnixx::jbyteArray bytes,
				 jint start, jint length) {
  verifyBounds(env, bytes, start, length);
  
  jbyteArrayElements unixPath = jbyteArrayElements(env, GetUnixPath(env));
  int fd = ::open((const char *)unixPath.elements(), O_RDONLY);
  if (fd < 0)
    errnoException(env, errno, "open", "filename %s",
		   (const char *)unixPath.elements());
  unixPath.release();

  // XXX: 64-bit?
  jbyteArrayElements buffer = jbyteArrayElements(env, bytes);
  ssize_t rc = ::pread64 (fd, start + buffer.elements(), length, fileOffset);
  if (rc < 0) {
    int savedErrno = errno;
    ::close (fd);
    errnoException(env, savedErrno, "pread", "fd %d, count %d, offset %ld",
    		fd, (int) length, (long)fileOffset);
  }
  buffer.release();

  ::close (fd);
  return rc;
}

jint
frysk::sys::StatelessFile::pwrite(jnixx::env env, jlong fileOffset,
				  jnixx::jbyteArray bytes,
				  jint start, jint length) {
  verifyBounds (env, bytes, start, length);
  
  jbyteArrayElements unixPath = jbyteArrayElements(env, GetUnixPath(env));
  int fd = ::open((const char *)unixPath.elements(), O_WRONLY);
  if (fd < 0)
    errnoException(env, errno, "open", "filename %s",
		   (const char *)unixPath.elements());
  unixPath.release();

  // XXX: 64-bit?
  jbyteArrayElements buffer = jbyteArrayElements(env, bytes);
  ssize_t rc = ::pwrite64 (fd, start + buffer.elements(), length, fileOffset);
  if (rc < 0) {
    int savedErrno = errno;
    ::close (fd);
    errnoException(env, savedErrno, "pwrite", "fd %d, count %d, offset %ld",
		   fd, (int) length, (long)fileOffset);
  }
  buffer.release();
  
  ::close (fd);
  return rc;
}
