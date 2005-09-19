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
#include <sys/types.h>
#include <sys/ptrace.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <alloca.h>
#include <stdio.h>

#include <gcj/cni.h>

#include "inua/eio/Buffer.h"
#include "inua/eio/ByteBuffer.h"
#include "inua/eio/PtraceByteBuffer.h"
#include "inua/eio/PtraceByteBuffer$Area.h"

inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::textArea ()
{
#ifdef PT_READ_I
  return new PtraceByteBuffer$Area (PT_READ_I);
#else
  return NULL;
#endif
}
inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::dataArea ()
{
#ifdef PT_READ_D
  return new PtraceByteBuffer$Area (PT_READ_D);
#else
  return NULL;
#endif
}
inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::usrArea ()
{
#ifdef PT_READ_U
  return new PtraceByteBuffer$Area (PT_READ_U);
#else
  return NULL;
#endif
}

jint
inua::eio::PtraceByteBuffer::peek (jlong addr)
{
  enum __ptrace_request pt_peek = (enum __ptrace_request) area->area;
  union
  {
    int word;
    jbyte byte[sizeof (int)];
  }
  tmp;

  /* Word align the address, transfer one word.  */
  long paddr = addr & -sizeof (int);

  errno = 0;
  tmp.word = ::ptrace (pt_peek, pid, (char *) paddr, 0);
  if (errno != 0) {
    ::perror ("ptrace");
    ::exit (errno);
  }

  return tmp.byte[addr & (sizeof (int) - 1)];
}

jlong
inua::eio::PtraceByteBuffer::peek (jlong addr, jbyteArray buf,
				   jlong off, jlong len)
{
  enum __ptrace_request pt_peek = (enum __ptrace_request) area->area;
  jbyte *bytes = elements (buf);
  union
  {
    int word;
    jbyte byte[sizeof (int)];
  }
  tmp;

  if (len == 0)
    return 0;

  /* Word align the address, transfer one word.  */
  long paddr = addr & -sizeof (int);
  long xfer = sizeof (int);

  /* Adjust the xfer size according to the upper bound.  */
  if (xfer > (addr + len) - addr)
    xfer = (addr + len) - addr;

  errno = 0;
  tmp.word = ::ptrace (pt_peek, pid, (char *) paddr, 0);
  if (errno != 0) {
    ::perror ("ptrace");
    ::exit (errno);
  }

  for (int i = addr - paddr; i < xfer; i++)
    bytes[i - (addr - paddr)] = tmp.byte[i];

  return xfer - (addr - paddr);
}
