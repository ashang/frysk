// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

#define DEBUG 0

#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"
#include <string.h>

#include "jni.hxx"
#include "jnixx/bounds.hxx"
#include "jnixx/elements.hxx"

#include "frysk/sys/ptrace/jni/Ptrace.hxx"

using namespace java::lang;

union word {
  long l;
  uint8_t b[sizeof (long)];
};

jint
frysk::sys::ptrace::AddressSpace::peek(::jnixx::env env, jint pid, jlong addr) {
  union word w;
  long paddr = addr & -sizeof(long);
  if (DEBUG)
    fprintf(stderr, "peek 0x%lx paddr 0x%lx", (long)addr, paddr);
  w.l = ptraceOp(env, GetPtPeek(env), pid, (void*)paddr, 0);
  if (DEBUG)
    fprintf(stderr, " word 0x%lx", w.l);
  int index = addr & (sizeof(long) - 1);
  if (DEBUG)
    fprintf(stderr, " index %d", index);
  uint8_t byte = w.b[index];
  if (DEBUG)
    fprintf(stderr, " byte %d/0x%x\n", byte, byte);
  return byte;
}

void
frysk::sys::ptrace::AddressSpace::poke(::jnixx::env env, jint pid, jlong addr, jint data) {
  // Implement read-modify-write
  union word w;
  if (DEBUG)
    fprintf(stderr, "poke 0x%x", (int)(data & 0xff));
  long paddr = addr & -sizeof(long);
  if (DEBUG)
    fprintf(stderr, " addr 0x%lx paddr 0x%lx", (long)addr, paddr);
  w.l = ptraceOp(env, GetPtPeek(env), pid, (void*)paddr, 0);
  if (DEBUG)
    fprintf(stderr, " word 0x%lx", w.l);
  int index = addr & (sizeof(long) - 1);
  if (DEBUG)
    fprintf (stderr, " index %d", index);
  w.b[index] = data;
  if (DEBUG)
    fprintf(stderr, " word 0x%lx\n", w.l);
  ptraceOp(env, GetPtPoke(env), pid, (void*)(addr & -sizeof(long)), w.l);
}

void
frysk::sys::ptrace::AddressSpace::transfer(::jnixx::env env,
					   jint op, jint pid, jlong addr,
					   ::jnixx::jbyteArray byteArray,
					   jint offset, jint length) {
  const int ptPeek = GetPtPeek(env);
  const int ptPoke = GetPtPoke(env);
  verifyBounds(env, byteArray, offset, length);
  // Somewhat more clueful implementation
  for (jlong i = 0; i < length;) {
    if (DEBUG)
      fprintf(stderr,
	      "transfer pid %d addr 0x%lx length %d offset %d op %d (%s) ...",
	      (int)pid, (long)addr, (int)length, (int)offset,
	      (int)op, ptraceOpToString(op));
    union word w;
    unsigned long waddr = addr & -sizeof(long);
    unsigned long woff = (addr - waddr);
    unsigned long remaining = length - i;
    unsigned long wend;
    if (remaining > sizeof(long) - woff)
      wend = sizeof(long);
    else
      wend = woff + remaining;
    long wlen = wend - woff;

    if (DEBUG)
      fprintf(stderr,
	      " i %ld waddr 0x%lx woff %lu wend %lu remaining %lu wlen %lu ...",
	      (long)i, waddr, woff, wend, remaining, wlen);

    // Either a peek; or a partial write requiring read/modify/write.
    if (op == ptPeek || woff != 0 || wend != sizeof(long)) {
      w.l = ptraceOp(env, ptPeek, pid, (void*)waddr, 0);
      if (DEBUG)
	fprintf(stderr, " peek 0x%lx ...", w.l);
    }

    // extract or modify
    jbyteArrayElements bytes = jbyteArrayElements(env, byteArray);
    if (op == ptPeek)
      memcpy(offset + i + bytes.elements(), &w.b[woff], wlen);
    else {
      memcpy(&w.b[woff], offset + i + bytes.elements(), wlen);
      if (DEBUG)
	fprintf(stderr, " poke 0x%lx ...", w.l);
      w.l = ptraceOp(env, ptPoke, pid, (void*)waddr, w.l);
    }
    bytes.release();

    i += wlen;
    addr += wlen;

    if (DEBUG)
      fprintf(stderr, "\n");
  }
}

frysk::sys::ptrace::AddressSpace
frysk::sys::ptrace::AddressSpace::text(::jnixx::env env) {
  return frysk::sys::ptrace::AddressSpace::New(env, -1UL,
					       String::NewStringUTF(env, "TEXT"),
					       PTRACE_PEEKTEXT,
					       PTRACE_POKETEXT);
}

frysk::sys::ptrace::AddressSpace
frysk::sys::ptrace::AddressSpace::data(::jnixx::env env) {
  return frysk::sys::ptrace::AddressSpace::New(env, -1UL,
					       String::NewStringUTF(env, "DATA"),
					       PTRACE_PEEKDATA,
					       PTRACE_POKEDATA);
}

frysk::sys::ptrace::AddressSpace
frysk::sys::ptrace::AddressSpace::usr(::jnixx::env env) {
  return frysk::sys::ptrace::AddressSpace::New(env, -1UL,
					       String::NewStringUTF(env, "USR"),
					       PTRACE_PEEKUSR,
					       PTRACE_POKEUSR);
}
