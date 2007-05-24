// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

#include <gcj/cni.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>

#include "frysk/sys/StatelessFile.h"
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

jlong
frysk::sys::StatelessFile::pread(jlong fileOffset,
				 jbyteArray bytes,
				 jlong start,
				 jlong length)
{
  if (fileOffset < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (length < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start + length > bytes->length)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  
  int fd = ::open ((const char *)elements (unixPath), O_RDONLY);

  // the fanicy throwErrno() has been at least temporarily removed because 
  // jasprintf choked on it with "java.lang.RuntimeException: JvNewStringUTF
  // failed in vajprintf".  maybe some java hacker can figure out why.

  // if (-1 == fd) throwErrno (errno, "open", "filename %s",
  //			    (const char *)elements (unixPath));
  if (-1 == fd) throwErrno (errno, "open", "/proc/<pid>/mem");

  ssize_t rc = ::pread (fd, start + elements(bytes), length, fileOffset);
  if (-1 == rc) {
    ::close (fd);
    throwErrno (errno, "pread", "fd %d, count %d, offset %d",
    		fd,
    		(int)length,
    		(int)fileOffset);
  }
  
  ::close (fd);
  return rc;
}

jlong
frysk::sys::StatelessFile::pwrite(jlong fileOffset,
				 jbyteArray bytes,
				 jlong start,
				 jlong length)
{
  if (fileOffset < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (length < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  if (start + length > bytes->length)
    throw new java::lang::ArrayIndexOutOfBoundsException ();
  
  int fd = ::open ((const char *)elements (unixPath), O_WRONLY);

  // the fanicy throwErrno() has been at least temporarily removed because 
  // jasprintf choked on it with "java.lang.RuntimeException: JvNewStringUTF
  // failed in vajprintf".  maybe some java hacker can figure out why.

  // if (-1 == fd) throwErrno (errno, "open", "filename %s",
  //			    (const char *)elements (unixPath));
  if (-1 == fd) throwErrno (errno, "open", "/proc/<pid>/mem");

  ssize_t rc = ::pwrite (fd, start + elements(bytes), length, fileOffset);
  if (-1 == rc) {
    ::close (fd);
    throwErrno (errno, "pwrite", "fd %d, count %d, offset %d",
		fd,
		(int)length,
		(int)fileOffset);
  }
  
  ::close (fd);
  return rc;
}
