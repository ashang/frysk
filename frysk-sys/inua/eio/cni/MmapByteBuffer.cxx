// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <alloca.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>

#include <gcj/cni.h>
#include "inua/eio/Buffer.h"
#include "inua/eio/ByteBuffer.h"
#include "inua/eio/MmapByteBuffer.h"
#include "inua/eio/MmapByteBuffer$Mmap.h"

jlong
inua::eio::MmapByteBuffer$Mmap::mmap (jstring file, jlong length)
{
  int pathlength = JvGetStringUTFLength (file);
  char *pathname = (char *) alloca (pathlength + 1);
  JvGetStringUTFRegion (file, 0, file->length(), pathname);
  pathname[pathlength] = '\0';

  errno = 0;
  int fd = open (pathname, O_RDONLY);
  if (errno != 0) {
    ::perror ("open");
    ::exit (errno);
  }

  errno = 0;
  void* buffer = ::mmap (NULL, length, PROT_READ | PROT_WRITE, MAP_PRIVATE,
			 fd, 0);
  if (errno != 0) {
    ::perror ("mmap");
    ::exit (errno);
  }

  return (jlong) (long) buffer;
}

jint
inua::eio::MmapByteBuffer::peek (jlong caret)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  return p[caret];
}

void
inua::eio::MmapByteBuffer::poke (jlong caret, jint value)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  p[caret] = value;
}

jlong
inua::eio::MmapByteBuffer::peek (jlong caret, jbyteArray bytes, jlong off,
			     jlong len)
{
  u_int8_t *p = (u_int8_t*) (long) this->map->byteData;
  memcpy (elements (bytes) + off, p + caret, len);
  return len;
}
