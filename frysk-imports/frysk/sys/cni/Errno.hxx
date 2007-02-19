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

// <<prefix>>: <<strerror(err)>>
extern void throwErrno (int err, const char *prefix)
  __attribute__ ((noreturn));
// <<prefix>>: <<strerror(err)>> (<<suffix>>)
extern void throwErrno (int err, const char *prefix, const char *suffix)
  __attribute__ ((noreturn));
// <<prefix>>: <<strerror(err)>> (<<suffix>> <<val>>)
extern void throwErrno (int err, const char *prefix, const char *suffix,
			int val)
  __attribute__ ((noreturn));
// <<message>>
extern void throwRuntimeException (const char *message)
  __attribute__ ((noreturn));
// <<message>> (<<suffix>> <<val>>)
extern void throwRuntimeException (const char *message, const char *suffix,
				   int val)
  __attribute__ ((noreturn));

/**
 * Like asprintf, only it returns a java string.
 */
extern jstring vajprintf (const char *fmt, ...)
  __attribute__ ((format (printf, 1, 2)));


/**
 * Attempt a garbage collect, if count is up, throw errno anyway.
 */
extern int tryGarbageCollect (int &count);
extern void tryGarbageCollect (int &count, int err, const char *prefix);
extern void tryGarbageCollect (int &count, int err, const char *prefix,
			       const char *suffix, int val);

/**
 * Convert ARGV, a String[], into a C char* array allocated on the
 * stack.
 */
extern size_t sizeof_argv (jstringArray argv);
extern char** fill_argv (void* p, jstringArray argv);
#define ALLOCA_ARGV(ARGV) (fill_argv (alloca (sizeof_argv (ARGV)), (ARGV)))
/**
 * Convert S, a String, into a C char* alocated on the stack.
 */
extern size_t sizeof_string (jstring s);
extern char* fill_string (void* p, jstring s);
#define ALLOCA_STRING(S) (fill_string (alloca (sizeof_string (S)), (S)))
