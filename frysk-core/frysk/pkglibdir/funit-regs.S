// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

// Simple test program for frysk-asm.h header.  Set the registers to
// known values and then, from frysk examine them.

#include "frysk-asm.h"

	FUNCTION_BEGIN(main,0)
	MAIN_PROLOGUE(0)

#if defined __i386__
	mov $0x7eb03efc, %eax
	mov $0x35a322a0, %ebx
	mov $0x7f198cab, %ecx
	mov $0x35b374c3, %edx
	mov $0x1bc5daed, %esi
	mov $0x457319f3, %edi
	mov $0xcbfed73c, %ebp
	// eip
	// %eflags
	mov $0x93d4a6ed, %esp
	jmp 0 // SIGILL
#else
	LOAD_IMMED_WORD(REG0, 0)
	STORE(REG0, REG0)
#endif

	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)