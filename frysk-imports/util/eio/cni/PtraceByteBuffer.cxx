// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

#include <sys/types.h>
#include <sys/ptrace.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <alloca.h>
#include <stdio.h>

#include <gcj/cni.h>

#include "util/eio/Buffer.h"
#include "util/eio/ByteBuffer.h"
#include "util/eio/PtraceByteBuffer.h"
#include "util/eio/PtraceByteBuffer$Area.h"

util::eio::PtraceByteBuffer$Area*
util::eio::PtraceByteBuffer$Area::textArea ()
{
#ifdef PT_READ_I
  return new PtraceByteBuffer$Area (PT_READ_I);
#else
  return NULL;
#endif
}
util::eio::PtraceByteBuffer$Area*
util::eio::PtraceByteBuffer$Area::dataArea ()
{
#ifdef PT_READ_D
  return new PtraceByteBuffer$Area (PT_READ_D);
#else
  return NULL;
#endif
}
util::eio::PtraceByteBuffer$Area*
util::eio::PtraceByteBuffer$Area::usrArea ()
{
#ifdef PT_READ_U
  return new PtraceByteBuffer$Area (PT_READ_U);
#else
  return NULL;
#endif
}

jint
util::eio::PtraceByteBuffer::peek (jlong addr)
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
util::eio::PtraceByteBuffer::peek (jlong addr, jbyteArray buf,
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
