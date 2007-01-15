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
package lib.opcodes.tests;

import frysk.Config;
import inua.eio.ByteBuffer;

public class DummyByteBuffer
  extends ByteBuffer 
{

  private byte bytes[];
  
  public DummyByteBuffer()
  {
    super(0, 8);

    if (Config.getTargetCpuXXX ().indexOf("powerpc") != - 1)
      {
	bytes = new byte[]
	  {
	    0x4e, (byte)0x80, 0x04, 0x20, // bctr
	    0x44, 0x00, 0x00, 0x02,       // sc
	    0x4e, (byte)0x80, 0x00, 0x20, // blr
	    0x60, 0x00, 0x00, 0x00,       // nop
	    0x38, 0x00, 0x00, 0x02,       // li      r0,2
	    0x39, (byte)0x8c, 0x03, 0x68, // addi    r12,r12,872
	    0x38, 0x21, 0x00, 0x70,       // addi    r1,r1,112
	    (byte)0x7d, 0x69, 0x03, (byte)0xa6,        // mtctr   r11
	    (byte)0x78, (byte)0xa5, 0x26, (byte) 0xc6  // rldicr  r5,r5,36,27
	  };

      }
    else if (Config.getTargetCpuXXX ().indexOf("_64") != - 1)
      {
	bytes = new byte[]
	  {
	    0, 1, // add    %al,(%rcx) 
	    2, 3, // add    (%rbx),%al
	    4, 5, // add    $0x5,%al
	    6,    // (bad)
	    7     // (bad)
	  };
      }
    else
      {
	bytes = new byte[]
	  {
	    0, 1, // add    %al,(%ecx)
	    2, 3, // add    (%ebx),$al
	    4, 5, // add    $0x5,%al
	    6,    // push   %es
	    7     // pop    %es
	  };
      }
  }

  protected int peek(long caret) 
  {
    if(caret < bytes.length && caret >= 0)
      return bytes[(int) caret];	
    return -1;
  }

  protected void poke(long caret, int val) 
  {
    if(caret < bytes.length && caret >= 0)
      bytes[(int) caret] = (byte) val;
  }
}
