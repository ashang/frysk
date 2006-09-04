// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

/* linux/unistd.h declares __NR_*.  Moreover it might define _syscall2 and
 * such, so it is safer to make sure it gets included first.  Might refer
 * to errno variable, so better also include that. */
#include <errno.h>
#include <linux/unistd.h>

/* Declares syscall() */
#include <unistd.h>

#ifndef _syscall0
#define _syscall0(type,name)						\
type name(void)								\
{									\
        return syscall(__NR_##name);					\
}
#endif

#ifndef _syscall1
#define _syscall1(type,name,type1,arg1)					\
type name(type1 arg1)							\
{									\
        return syscall(__NR_##name, arg1);				\
}
#endif

#ifndef _syscall2
#define _syscall2(type,name,type1,arg1,type2,arg2)			\
type name(type1 arg1,type2 arg2)					\
{									\
        return syscall(__NR_##name, arg1, arg2);			\
}
#endif

#ifndef _syscall3
#define _syscall3(type,name,type1,arg1,type2,arg2,type3,arg3)		\
type name(type1 arg1,type2 arg2, type3 arg3)				\
{									\
  return syscall(__NR_##name, arg1, arg2, arg3);			\
}
#endif
