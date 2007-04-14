//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.proc.corefile;

import java.io.File;

import lib.elf.ElfException;

import frysk.Config;
import frysk.proc.TestLib;

public class TestCorefileByteBuffer
extends TestLib
{


  public void testCoreFileByteBufferPeek() throws ElfException
  {
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new File(Config.getPkgDataDir (), 
                                                                    "test-core"));

    assertEquals("Peek a byte at 0x00170000",0x7f,coreBuffer.peek(0x00170000L));
    assertEquals("Peek a byte at 0x00170001",0x45,coreBuffer.peek(0x00170001L));
    assertEquals("Peek a byte at 0x00170002",0x4c,coreBuffer.peek(0x00170002L));
    assertEquals("Peek a byte at 0x00170003",0x46,coreBuffer.peek(0x00170003L));
    assertEquals("Peek a byte at 0x00170004",0x01,coreBuffer.peek(0x00170004L));

  }
  
  public void testCoreFileByteBufferSequentialGet() throws ElfException
  {
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new File(Config.getPkgDataDir (), 
                                                                    "test-core"));

    coreBuffer.position(0x00170000L);
    assertEquals("Peek a byte at 0x00170000",0x7f,coreBuffer.get());
    assertEquals("Peek a byte at 0x00170001",0x45,coreBuffer.get());
    assertEquals("Peek a byte at 0x00170002",0x4c,coreBuffer.get());
    assertEquals("Peek a byte at 0x00170003",0x46,coreBuffer.get());
    assertEquals("Peek a byte at 0x00170004",0x01,coreBuffer.get());


    // Test reading at position 0. In this core file there is no 
    // segment at position 0 (in our case for this bytebuffer, the 
    // position within the buffer should equal the memory address access.)
    // This should always fail!

    coreBuffer.position(0);
    try 
    {
      assertEquals("Peek a byte at 0x00170000",0x7f,coreBuffer.get());
    }
    catch (RuntimeException e)
    {
      return;
    }
    fail(".get() read at position 0 should have failed but didn't!");
  }
  
  
  public void testCoreFileByteBufferPeekArray() throws ElfException
  {
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new File(Config.getPkgDataDir (), 
                                                                    "test-core"));
    byte byteArray[] = new byte[10];
    coreBuffer.get (0x00170000L, byteArray, 0,10);
    assertEquals("Check .get array index 0",0x7f,byteArray[0]);
    assertEquals("Check .get array index 1",0x45,byteArray[1]);
    assertEquals("Check .get array index 2",0x4c,byteArray[2]);
    assertEquals("Check .get array index 3",0x46,byteArray[3]);
    assertEquals("Check .get array index 4",0x01,byteArray[4]);
  }
  
  public void testCoreFileByteBufferPoke() throws ElfException 
  {
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new File(Config.getPkgDataDir (), 
                                                                    "test-core"));
    
    try
    {
      coreBuffer.poke(0x00170000L,10);
    }
    catch (RuntimeException e)
    {
      // Expected behaviour on a poke is always fail. If it does not fail
      // then something is going wrong. In this case expecting and receiving a fail
      // is the correct behaviour.
      return;
    }
    fail("Poke a byte at 0x00170000 should have failed with an exception, but did not");
  }
}
