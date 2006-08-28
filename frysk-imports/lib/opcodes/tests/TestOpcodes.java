// This file is part of the program FRYSK.
//
// Copyright 2005, 2006 Red Hat Inc.
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


package lib.opcodes.tests;

import inua.eio.ByteBuffer;

import java.util.LinkedList;

import frysk.junit.TestCase;
import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;
import lib.opcodes.OpcodesException;
import frysk.imports.Build;

public class TestOpcodes
    extends TestCase
{
  /*
   * Note: this test is expected to fail on anything but i386 for the time
   * being. TODO: come up with a way of doing the correct assertEquals for other
   * archs
   */
  public void testDisassembler ()
  {
      if (brokenPpcXXX(2712))
          return;

    ByteBuffer buffer = new DummyByteBuffer();
    final int numInstructions = 16;

    Disassembler disAsm = new Disassembler(buffer);
    LinkedList list = null;
    try
      {
        list = disAsm.disassembleInstructions(0, numInstructions);
      }
    catch (OpcodesException e)
      {
        e.printStackTrace(System.err);
        fail("Exception thrown during disassembly");
      }

    assertNotNull(list);
    assertEquals(list.size(), numInstructions);

    String[] addrs = { "0", "2", "4", "6", "7", "8", "a", "c", "e", "f", "10",
                      "12", "14", "16", "17", "18" };

    String[] insts;

    boolean is64 = Build.BUILD_ARCH.indexOf("_64") != - 1;

    if (! is64)
      {
        insts = new String[] { "DWORD PTR [ecx]", "BYTE PTR [ebx]", "0x5",
                              "es", "es", "al", "BYTE PTR [ebx]", "0x5", "es",
                              "es", "al", "BYTE PTR [ebx]", "0x5", "es", "es",
                              "al" };

      }
    else
      {
        insts = new String[] { "DWORD PTR [rcx]", "BYTE PTR [rbx]", "0x5",
                              "(bad)  ", "(bad)  ", "al", "BYTE PTR [rbx]",
                              "0x5", "(bad)  ", "(bad)  ", "al" };
      }

    assertNotNull(insts);

    int instCount = is64 ? 11 : 16;

    for (int i = 0; i < instCount; i++)
      {
        Instruction inst = (Instruction) list.get(i);
        assertNotNull(inst);

        assertEquals(addrs[i], Long.toHexString(inst.address));
        assertEquals(insts[i], inst.instruction);
      }
  }
}
