// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
// Copyright 2007 Oracle Corporation.
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

#include <stdarg.h>

// <<prefix>>: <<strerror(err)>>
extern void throwErrno (int err, const char *prefix)
  __attribute__ ((noreturn));
// <<prefix>>: <<strerror(err)>> (<<suffix>> ...)
extern void throwErrno (int err, const char *prefix, const char *suffix, ...)
  __attribute__ ((noreturn)) __attribute__((format (printf, 3, 4)));

// <<prefix>>: <<strerror(err)>> (<<suffix>> ...)
extern void throwUserException(const char *format, ...)
  __attribute__ ((noreturn)) __attribute__((format (printf, 1, 2)));

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
extern jstring ajprintf (const char *fmt, ...)
  __attribute__ ((format (printf, 1, 2)));
  
extern jstring vajprintf (const char *fmt, va_list ap);


extern int tryOpen (const char *file, int flags);
extern int tryOpen (const char *file, int flags, int mode);

/**
 * Convert ARGV, a String[], into a C char* array allocated on the
 * stack.
 */
extern size_t sizeof_argv (jstringArray argv);
extern char** fill_argv (void* p, jstringArray argv);
#define ALLOCA_ARGV(ARGV) (fill_argv (alloca (sizeof_argv (ARGV)), (ARGV)))
#define MALLOC_ARGV(ARGV) (fill_argv (JvMalloc (sizeof_argv (ARGV)), (ARGV)))
/**
 * Convert S, a String, into a C char* alocated on the stack.
 */
extern size_t sizeof_string (jstring s);
extern char* fill_string (void* p, jstring s);
#define ALLOCA_STRING(S) (fill_string (alloca (sizeof_string (S)), (S)))
#define MALLOC_STRING(S) (fill_string (JvMalloc (sizeof_string (S)), (S)))

/**
 * Throw an ArrayIndexOutOfBounds exception if START and LENGTH do not
 * fall within the byte array.
 */
extern void verifyBounds (jbyteArray bytes, jint start, jint length);
