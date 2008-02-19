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
#include <stdio.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"

#include <gcj/cni.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/ptrace/Utrace.h"
//#include "frysk/sys/ptrace/cni/Utrace.hxx"

void
frysk::sys::ptrace::Utrace::attach(jint pid) {
  fprintf (stderr, "\n\n>>>>PTRACE_ATTACH via utrace\n\n");
}

void
frysk::sys::ptrace::Utrace::detach(jint pid, jint sig) {
  fprintf (stderr, "\n\n>>>>PTRACE_DETACH via utrace\n\n");
} 

void
frysk::sys::ptrace::Utrace::singleStep(jint pid, jint sig) {
  fprintf (stderr, "\n\n>>>>PTRACE_SINGLESTEP via utrace\n\n");
} 

void
frysk::sys::ptrace::Utrace::cont(jint pid, jint sig) {
  fprintf (stderr, "\n\n>>>>PTRACE_CONT via utrace\n\n");
}

void
frysk::sys::ptrace::Utrace::sysCall(jint pid, jint sig) {
  fprintf (stderr, "\n\n>>>>PTRACE_SYSCALL via utrace\n\n");
}

jlong
frysk::sys::ptrace::Utrace::getEventMsg(jint pid) {
  /* Note: PTRACE_GETEVENTMSG ends up calling the function
     kernel/ptrace.c: ptrace_ptraceOp() and that uses put_user to store
     child->ptrace_message write sizeof(ptrace_message) bytes into the
     MESSAGE parameter.  include/linux/sched.h declares ptrace_message
     as a long.  */
  fprintf (stderr, "\n\n>>>> PTRACE_GETEVENTMSG via utrace\n\n");
  return -1;
}

void
frysk::sys::ptrace::Utrace::setOptions(jint pid, jlong options) {
  fprintf (stderr, "\n\n>>>>PTRACE_SETOPTIONS via utrace\n");
}

