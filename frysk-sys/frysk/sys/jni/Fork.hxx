// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

enum tracing {
  CHILD,
  DAEMON,
  PTRACE,
  UTRACE,
};

/**
 * Class to redirect a child's stdio; the destructor should clean up
 * the parent resources.
 */
class redirect {
public:
  virtual void reopen() = 0;
  virtual ~redirect() {
    // where the close code goes.
  }
};

/**
 * Just close standard input; leave the rest as is.
 */
class redirect_nostdin : public redirect {
  void reopen() {
    ::close(0);
  }
};

/**
 * What to do once the child/daemon process fork as occured.
 */
class exec {
public:
  virtual void execute() = 0;
  virtual ~exec() {
    // where the close code goes.
  }
};

/**
 * exec the specified program.
 */
class exec_program : public exec {
  StringChars exe;
  const char* exeElements;
  StringArrayChars argv;
  char** argvElements;
  char** environ;
public:
  exec_program(jnixx::env env, java::lang::String exe,
	       jnixx::array<java::lang::String> args, jlong environ) {
    this->exe = StringChars(env, exe);
    this->argv = StringArrayChars(env, args);
    this->environ = (char**)(long)environ;
    // allocate the exec strings before the fork
    this->exeElements = this->exe.elements();
    this->argvElements = this->argv.elements();
  }
  void execute() {
    if (environ != NULL) {
      ::execve(exeElements, argvElements, environ);
      ::perror("execve");
    } else {
      ::execv(exeElements, argvElements);
      ::perror("execv");
    }
    ::_exit(errno);
  }
  ~exec_program() {
    exe.release();
    argv.release();
  }
};

/**
 * Spawn a child, return the PID, or throw an error.
 */
extern int spawn(jnixx::env env, tracing trace,
		 redirect& redirection, exec& execute);
