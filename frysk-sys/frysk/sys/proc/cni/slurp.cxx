// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <stdlib.h>

#include <gcj/cni.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/cni/slurp.hxx"


int
slurp (int pid, const char* name, char buf[], long sizeof_buf)
{
  // Get the file name.
  char file[FILENAME_MAX];
  if (::snprintf (file, sizeof file, "/proc/%d/%s", (int) pid, name)
      >= FILENAME_MAX)
    throwRuntimeException ("snprintf: buffer overflow");
  
  // Open the file file.
  errno = 0;
  int fd = ::open (file, O_RDONLY);
  if (errno != 0)
    return -1;

  // Read in the entire file file, NUL terminate the string.
  errno = 0;
  int len = ::read (fd, buf, sizeof_buf - 1);
  if (errno != 0) {
    ::close (fd);
    return -1;
  }

  // Close the file, no longer needs to be open.
  errno = 0;
  ::close (fd);
  if (errno != 0)
    return -1;

  // Null terminate the buffer.
  buf[len] = '\0';
  return len;
}

jbyteArray
slurp (int pid, const char* name)
{
  // This implementation unfortunatly double buffers the data.
  char buf[BUFSIZ];
  int buf_len = slurp (pid, name, buf, sizeof buf);
  if (buf_len < 0)
    return NULL;
  jbyteArray jbuf = JvNewByteArray (buf_len);
  memcpy (elements (jbuf), buf, buf_len);
  return jbuf;
}

// Scan a jint, throw an error if there's a problem.

jint
scanJint (const char **p, int base)
{
  char *pp;
  jint tmp = ::strtoul (*p, &pp, base);
  if (*p == pp)
    throwRuntimeException ("strtoul");
  *p = pp;
  return tmp;
}

jint
scanJint (const char **p)
{
  return scanJint (p, 0);
}

// Scan a jlong, throw an error if there's a problem.
jlong
scanJlong (const char **p, int base)
{
  char *pp;
  jlong tmp = ::strtoull (*p, &pp, base);
  if (*p == pp)
    throwRuntimeException ("strtoul");
  *p = pp;
  return tmp;
}

jlong
scanJlong (const char **p)
{
  return scanJlong (p, 0);
}
