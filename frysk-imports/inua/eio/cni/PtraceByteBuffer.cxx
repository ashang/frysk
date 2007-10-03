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

#include <java/lang/RuntimeException.h>

#include "inua/eio/Buffer.h"
#include "inua/eio/ByteBuffer.h"
#include "inua/eio/PtraceByteBuffer.h"
#include "inua/eio/PtraceByteBuffer$Area.h"

static java::lang::RuntimeException *
newPerror (const char *syscall, int nr)
{
  const char *error = strerror (nr);
  const int len = strlen (error) + strlen (syscall) + strlen (": ") + 1;
  char *message = (char*) alloca (len);
  if (snprintf (message, len, "%s: %s", syscall, error) >= len)
    throw new java::lang::RuntimeException (JvNewStringLatin1 ("oops"));
  return new java::lang::RuntimeException (JvNewStringLatin1 (message));
}


inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::textArea ()
{
#if defined PT_READ_I && defined PT_WRITE_I
  return new PtraceByteBuffer$Area (PT_READ_I, PT_WRITE_I);
#else
  return NULL;
#endif
}

inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::dataArea ()
{
#if defined PT_READ_D && defined PT_WRITE_D
  return new PtraceByteBuffer$Area (PT_READ_D, PT_WRITE_D);
#else
  return NULL;
#endif
}

inua::eio::PtraceByteBuffer$Area*
inua::eio::PtraceByteBuffer$Area::usrArea ()
{
#if defined PT_READ_U && defined PT_WRITE_U
  return new PtraceByteBuffer$Area (PT_READ_U, PT_WRITE_U);
#else
  return NULL;
#endif
}

jint
inua::eio::PtraceByteBuffer::peek (jlong addr)
{
  const enum __ptrace_request pt_peek = (enum __ptrace_request) area->peek;
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
  if (errno != 0)
    throw newPerror ("ptrace", errno);

  return tmp.byte[addr & (sizeof (int) - 1)];
}

jlong
inua::eio::PtraceByteBuffer::peek (jlong addr, jbyteArray buf,
				   jlong off, jlong len)
{
  const enum __ptrace_request pt_peek = (enum __ptrace_request) area->peek;
  jbyte *bytes = elements (buf);
  union
  {
    int word;
    jbyte byte[sizeof (int)];
  }
  tmp;

  if (len == 0)
    return 0;

  // Word align the address.
  unsigned long paddr = addr & -sizeof (int);

  // Read an entire word.
  errno = 0;
  tmp.word = ::ptrace (pt_peek, pid, (char *) paddr, 0);
  if (errno != 0)
    throw newPerror ("ptrace", errno);

  /* Adjust the xfer size to ensure that it doesn't exceed the size of
     the single word being transfered.  */
  unsigned long pend = addr + len;
  if (pend > paddr + sizeof (int))
    pend = paddr + sizeof (int);

  for (unsigned long a = addr; a < pend; a++)
    bytes[a - addr] = tmp.byte[a - paddr];

  return pend - addr;
}

void
inua::eio::PtraceByteBuffer::poke (jlong addr, jint byte)
{
  const enum __ptrace_request pt_peek = (enum __ptrace_request) area->peek;
  const enum __ptrace_request pt_poke = (enum __ptrace_request) area->poke;
  union
  {
    int word;
    jbyte byte[sizeof (int)];
  }
  tmp;

  /* Word align the address, transfer one word.  */
  long paddr = addr & -sizeof (int);

  // Perform a read ...
  errno = 0;
  tmp.word = ::ptrace (pt_peek, pid, (char *) paddr, 0);
  if (errno != 0)
    throw newPerror ("ptrace", errno);

  // ... modify ...
  tmp.byte[addr & (sizeof (int) - 1)] = byte;

  // ... write.
  errno = 0;
  ::ptrace (pt_poke, pid, (char *) paddr, tmp.word);
  if (errno != 0)
    throw newPerror ("ptrace", errno);
}
