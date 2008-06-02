// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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
#include <errno.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"
#include "frysk/sys/jni/Fork.hxx"

using namespace java::lang;

class redirect_inout : public redirect {
private:
  int in_in;
  int in_out;
  int out_in;
  int out_out;
public:
  redirect_inout(int in_in, int in_out, int out_in, int out_out) {
    this->in_in = in_in;
    this->in_out = in_out;
    this->out_in = out_in;
    this->out_out = out_out;
  }
  // this.out > [ out.out | out.in > child > in.out | in.in ] > this.in
  void reopen() {
    // Rewire: out.in > child's in
    ::dup2(out_in, 0);
    // Rewire: in.out > child's out
    ::dup2(in_out, 1);
    // Close the pipes.
    ::close(in_in);
    ::close(in_out);
    ::close(out_in);
    ::close(out_out);
  }
};

static jint
spawn(jnixx::env env, enum tracing trace, String exe,
      jnixx::array<String> args,
      jint in_in, jint in_out,
      jint out_in, jint out_out) {
  redirect_inout inout = redirect_inout(in_in, in_out, out_in, out_out);
  exec_program program = exec_program(env, exe, args,
				      jnixx::array<String>(env, NULL));
  return ::spawn(env, trace, inout, program);
}

jint
frysk::sys::PipePair::child(jnixx::env env, String exe,
			    jnixx::array<String> args,
			    jint in_in, jint in_out,
			    jint out_in, jint out_out) {
  return ::spawn(env, CHILD, exe, args, in_in, in_out, out_in, out_out);
}

jint
frysk::sys::PipePair::daemon(jnixx::env env, String exe,
			     jnixx::array<String> args,
			     jint in_in, jint in_out,
			     jint out_in, jint out_out) {
  return ::spawn(env, DAEMON, exe, args, in_in, in_out, out_in, out_out);
}
