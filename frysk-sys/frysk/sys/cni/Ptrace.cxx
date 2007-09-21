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

#include <stdint.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include "linux.ptrace.h"
#include <errno.h>
#include <linux/unistd.h>
#include <sys/wait.h>
#include <alloca.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawData.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>

#include "frysk/sys/Ptrace.h"
#include "frysk/sys/Ptrace$RegisterSet.h"
#include "frysk/sys/Ptrace$AddressSpace.h"
#include "frysk/sys/Ptrace.h"
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"

static const char*
op_as_string (int op)
{
  switch (op)
    {
#define OP(NAME) case NAME: return #NAME
      OP(PTRACE_ATTACH);
      OP(PTRACE_DETACH);
      OP(PTRACE_SINGLESTEP);
      OP(PTRACE_CONT);
      OP(PTRACE_SYSCALL);
#if defined(__i386__)|| defined(__x86_64__)
      OP(PTRACE_GETREGS);
      OP(PTRACE_SETREGS);
      OP(PTRACE_GETFPREGS);
      OP(PTRACE_SETFPREGS);
#endif
#if defined(__i386__)
      OP(PTRACE_GETFPXREGS);
      OP(PTRACE_SETFPXREGS);
#endif
      OP(PTRACE_GETEVENTMSG);
      OP(PTRACE_SETOPTIONS);
      OP(PTRACE_PEEKDATA);
      OP(PTRACE_POKEDATA);
      OP(PTRACE_PEEKTEXT);
      OP(PTRACE_POKETEXT);
      OP(PTRACE_PEEKUSR);
      OP(PTRACE_POKEUSR);
    default: return "<unknown>";
#undef OP
    }
}

static long
request (int op, int pid, void* addr, long data)
{
  errno = 0;
  long result = ::ptrace ((enum __ptrace_request) op, pid, addr, data);
  if (errno != 0)
    throwErrno (errno, "ptrace", "op 0x%x (%s), pid %d, addr 0x%lx, data 0x%lx",
		op, op_as_string(op), pid, (long)addr, data);
  return result;
}

void
frysk::sys::Ptrace::attach (jint pid)
{
  request(PTRACE_ATTACH, pid, NULL, 0);
}

void
frysk::sys::Ptrace::detach (jint pid, jint sig)
{
  request(PTRACE_DETACH, pid, NULL, sig);
} 

void
frysk::sys::Ptrace::singleStep (jint pid, jint sig)
{
  request(PTRACE_SINGLESTEP, pid, NULL, sig);
} 

void
frysk::sys::Ptrace::cont (jint pid, jint sig)
{
  request(PTRACE_CONT, pid, NULL, sig);
}

void
frysk::sys::Ptrace::sysCall (jint pid, jint sig)
{
  request(PTRACE_SYSCALL, pid, NULL, sig);
}

jlong
frysk::sys::Ptrace::getEventMsg (jint pid)
{
  /* Note: PTRACE_GETEVENTMSG ends up calling the function
     kernel/ptrace.c: ptrace_request() and that uses put_user to store
     child->ptrace_message write sizeof(ptrace_message) bytes into the
     MESSAGE parameter.  include/linux/sched.h declares ptrace_message
     as a long.  */
  long msg;
  request(PTRACE_GETEVENTMSG, pid, NULL, (long) &msg);
  return msg;
}

void
frysk::sys::Ptrace::setOptions (jint pid, jlong options)
{
  request(PTRACE_SETOPTIONS, pid, 0, options);
}

jlong
frysk::sys::Ptrace::optionTraceClone ()
{
  return PTRACE_O_TRACECLONE;
}
jlong
frysk::sys::Ptrace::optionTraceFork ()
{
  return PTRACE_O_TRACEFORK;
}
jlong
frysk::sys::Ptrace::optionTraceExit ()
{
  return PTRACE_O_TRACEEXIT;
}
jlong
frysk::sys::Ptrace::optionTraceSysgood ()
{
  return PTRACE_O_TRACESYSGOOD;
}
jlong
frysk::sys::Ptrace::optionTraceExec ()
{
  return PTRACE_O_TRACEEXEC;
}

void
frysk::sys::Ptrace$RegisterSet::get (jint pid, jbyteArray data)
{
  if (data->length < ptLength)
    throwErrno (EIO, "ptrace");
  request (ptGet, pid, NULL, (long)elements (data));
}

void
frysk::sys::Ptrace$RegisterSet::set (jint pid, jbyteArray data)
{
  if (data->length < ptLength)
    throwErrno (EIO, "ptrace");
  request (ptSet, pid, NULL, (long)elements (data));
}

frysk::sys::Ptrace$RegisterSet*
frysk::sys::Ptrace$RegisterSet::regs ()
{
#if defined(__i386__)|| defined(__x86_64__)
  return new frysk::sys::Ptrace$RegisterSet (sizeof (user_regs_struct),
					     PTRACE_GETREGS, PTRACE_SETREGS);
#else
  return NULL;
#endif
}

frysk::sys::Ptrace$RegisterSet*
frysk::sys::Ptrace$RegisterSet::fpregs ()
{
#if defined(__i386__)|| defined(__x86_64__)
  return new frysk::sys::Ptrace$RegisterSet (sizeof(user_fpregs_struct),
					     PTRACE_GETFPREGS,
					     PTRACE_SETFPREGS);
#else
  return NULL;
#endif
}

frysk::sys::Ptrace$RegisterSet*
frysk::sys::Ptrace$RegisterSet::fpxregs ()
{
#if defined(__i386__)
  return new frysk::sys::Ptrace$RegisterSet (sizeof(user_fpxregs_struct),
					     PTRACE_GETFPXREGS,
					     PTRACE_SETFPXREGS);
#else
  return NULL;
#endif
}

union word {
  long l;
  uint8_t b[sizeof (long)];
};

jint
frysk::sys::Ptrace$AddressSpace::peek (jint pid, jlong addr)
{
  union word w;
  long paddr = addr & -sizeof(long);
  // fprintf (stderr, "peek 0x%lx paddr 0x%lx", (long)addr, paddr);
  w.l = request (ptPeek, pid, (void*)paddr, 0);
  // fprintf (stderr, " word 0x%lx", w.l);
  int index = addr & (sizeof(long) - 1);
  // fprintf (stderr, " index %d", index);
  uint8_t byte = w.b[index];
  // fprintf (stderr, " byte %d/0x%x\n", byte, byte);
  return byte;
}

jint
frysk::sys::Ptrace$AddressSpace::peek (jint pid, jlong addr, jint length,
				       jbyteArray bytes, jint offset)
{
   if (offset < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException (JvNewStringUTF("Offset < 0"));
   if (length < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException (JvNewStringUTF("length < 0"));
  if (offset +  length >  bytes->length)
    throw new java::lang::ArrayIndexOutOfBoundsException (JvNewStringUTF("offset + length > bytes->length"));
   
  // Somewhat more clueful implementation
  // fprintf (stderr, "starting from %#08x to offset %d, length %d\n",
  //          (unsigned int)addr, (int)offset, (int)length);
  for (jlong i = 0; i < length;) {
    union word w;
    long waddr = addr & -sizeof(long);
    long woff = (addr - waddr);
    long wlen = sizeof(long) - woff;
    long ilen = length - i;
    wlen = (wlen < ilen) ? wlen : ilen;

    w.l = request (ptPeek, pid, (void*)waddr, 0);
    //fprintf (stderr, "waddr %#08x contains %#08x\n",
    //         (unsigned int)waddr, (unsigned int)w.l);

    memcpy (offset + i +  elements(bytes), &w.b[woff], wlen);
    //fprintf (stderr, "i = %d, length = %d, wlen = %d\n",
    //       (int)i, (int)length, (int)wlen);
    i += wlen;
    addr += wlen;
  }
  return length;
}

void
frysk::sys::Ptrace$AddressSpace::poke (jint pid, jlong addr, jint data)
{
  // Implement read-modify-write
  union word w;
  // fprintf (stderr, "poke 0x%x", (int)(data & 0xff));
  long paddr = addr & -sizeof(long);
  // fprintf (stderr, " addr 0x%lx paddr 0x%lx", (long)addr, paddr);
  w.l = request (ptPeek, pid, (void*)paddr, 0);
  // fprintf (stderr, " word 0x%lx", w.l);
  int index = addr & (sizeof(long) - 1);
  // fprintf (stderr, " index %d", index);
  w.b[index] = data;
  // fprintf (stderr, " word 0x%lx\n", w.l);
  request (ptPoke, pid, (void*)(addr & -sizeof(long)), w.l);
}

jlong
frysk::sys::Ptrace$AddressSpace::length ()
{
  return -1UL;
}

frysk::sys::Ptrace$AddressSpace*
frysk::sys::Ptrace$AddressSpace::text ()
{
  return new frysk::sys::Ptrace$AddressSpace (JvNewStringUTF ("TEXT"),
					      PTRACE_PEEKTEXT,
					      PTRACE_POKETEXT);
}

frysk::sys::Ptrace$AddressSpace*
frysk::sys::Ptrace$AddressSpace::data ()
{
  return new frysk::sys::Ptrace$AddressSpace (JvNewStringUTF ("DATA"),
					      PTRACE_PEEKDATA,
					      PTRACE_POKEDATA);
}

frysk::sys::Ptrace$AddressSpace*
frysk::sys::Ptrace$AddressSpace::usr ()
{
  return new frysk::sys::Ptrace$AddressSpace (JvNewStringUTF ("USR"),
					      PTRACE_PEEKUSR,
					      PTRACE_POKEUSR);
}
