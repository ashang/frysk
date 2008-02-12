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

#include <errno.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"

#include <gcj/cni.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/ptrace/Ptrace.h"
#include "frysk/sys/ptrace/cni/Ptrace.hxx"

static const char*
op_as_string(int op) {
  switch(op) {
#define OP(NAME) case NAME: return #NAME
    OP(PTRACE_ATTACH);
    OP(PTRACE_DETACH);
    OP(PTRACE_SINGLESTEP);
    OP(PTRACE_CONT);
    OP(PTRACE_SYSCALL);
#if defined(__i386__) || defined(__x86_64__)
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

long
ptraceOp(int op, int pid, void* addr, long data) {
  errno = 0;
  long result = ::ptrace ((enum __ptrace_request) op, pid, addr, data);
  if (errno != 0)
    throwErrno(errno, "ptrace", "op 0x%x (%s), pid %d, addr 0x%lx, data 0x%lx",
	       op, op_as_string(op), pid, (long)addr, data);
  return result;
}

void
frysk::sys::ptrace::Ptrace::attach(jint pid) {
  ptraceOp(PTRACE_ATTACH, pid, NULL, 0);
}

void
frysk::sys::ptrace::Ptrace::detach(jint pid, jint sig) {
  ptraceOp(PTRACE_DETACH, pid, NULL, sig);
} 

void
frysk::sys::ptrace::Ptrace::singleStep(jint pid, jint sig) {
  ptraceOp(PTRACE_SINGLESTEP, pid, NULL, sig);
} 

void
frysk::sys::ptrace::Ptrace::cont(jint pid, jint sig) {
  ptraceOp(PTRACE_CONT, pid, NULL, sig);
}

void
frysk::sys::ptrace::Ptrace::sysCall(jint pid, jint sig) {
  ptraceOp(PTRACE_SYSCALL, pid, NULL, sig);
}

jlong
frysk::sys::ptrace::Ptrace::getEventMsg(jint pid) {
  /* Note: PTRACE_GETEVENTMSG ends up calling the function
     kernel/ptrace.c: ptrace_ptraceOp() and that uses put_user to store
     child->ptrace_message write sizeof(ptrace_message) bytes into the
     MESSAGE parameter.  include/linux/sched.h declares ptrace_message
     as a long.  */
  long msg;
  ptraceOp(PTRACE_GETEVENTMSG, pid, NULL, (long) &msg);
  return msg;
}

void
frysk::sys::ptrace::Ptrace::setOptions(jint pid, jlong options) {
  ptraceOp(PTRACE_SETOPTIONS, pid, 0, options);
}

jlong
frysk::sys::ptrace::Ptrace::optionTraceClone() {
  return PTRACE_O_TRACECLONE;
}
jlong
frysk::sys::ptrace::Ptrace::optionTraceFork() {
  return PTRACE_O_TRACEFORK;
}
jlong
frysk::sys::ptrace::Ptrace::optionTraceExit() {
  return PTRACE_O_TRACEEXIT;
}
jlong
frysk::sys::ptrace::Ptrace::optionTraceSysgood() {
  return PTRACE_O_TRACESYSGOOD;
}
jlong
frysk::sys::ptrace::Ptrace::optionTraceExec() {
  return PTRACE_O_TRACEEXEC;
}
