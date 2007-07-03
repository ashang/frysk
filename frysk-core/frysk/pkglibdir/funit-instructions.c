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

#include <stdio.h>
#include <stdlib.h>

int
main (int argc, char *argv[], char *envp[])
{
  __label__ nop1_instruction;
  __label__ nop2_instruction;
  __label__ nop3_instruction;

  __label__ jump1_instruction;
  __label__ jump2_instruction;
  __label__ jump3_instruction;

  __label__ done;

  // Tell the tester the addresses of the assembly instruction labels
  // in order that they should appear.
  printf("%p\n", &&nop1_instruction);
  printf("%p\n", &&nop2_instruction);
  printf("%p\n", &&jump1_instruction);
  printf("%p\n", &&nop3_instruction);
  printf("%p\n", &&jump3_instruction);
  printf("%p\n", &&jump2_instruction);
  printf("%p\n", &&nop4_instruction);
  printf("%p\n", &&done);
  printf("%p\n", (void *) 0);
  fflush(stdout);

  // Wait till we are OK to go!
  char c = getchar();
  if (c == EOF)
    exit (-1);

// This works for both x86 and x86_64
#if defined __i386__  || defined __x86_64__
 nop1_instruction:
  asm volatile ("nop1_instruction: nop");
  
 nop2_instruction:
  asm volatile ("nop2_instruction: nop");

 jump1_instruction:
  asm volatile ("jump1_instruction: jmp nop3_instruction");

 jump2_instruction:
  asm volatile ("jump2_instruction: jmp nop4_instruction");

 nop3_instruction:
  asm volatile ("nop3_instruction: nop");

 jump3_instruction:
  asm volatile ("jump3_instruction: jmp jump2_instruction");

 nop4_instruction:
  asm volatile ("nop4_instruction: nop");
#else
#error Not ported for this arch.
#endif
 done:
  return 0;
}
