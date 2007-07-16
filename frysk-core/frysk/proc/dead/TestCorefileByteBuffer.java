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

package frysk.proc.dead;

import java.io.File;
import lib.elf.Elf;
import lib.elf.ElfData;
import lib.elf.ElfCommand;

import lib.elf.ElfException;

import frysk.Config;
import frysk.proc.TestLib;
import inua.eio.ByteBuffer;

public class TestCorefileByteBuffer
extends TestLib
{


  public void testCorefileByteBufferSlice() throws ElfException
  {

    ElfData rawData;
    final long sliceBottom = 0x411bb000L;
    final long sliceTop = 0x411bbfffL;
    final long elfOffset = 0x28000;
    final long elfLen = 0x1000;

    ByteBuffer coreBuffer = new CorefileByteBuffer(new 
						   File(Config.
							getPkgDataDir(), 
							"test-core-x86"));
    
    // Slice buffer
    ByteBuffer coreSlice = coreBuffer.slice(sliceBottom, sliceTop);
    
    assertNotNull("Corefile slice was null", coreSlice);

    // Independently get the elf core data as a raw image
    Elf segment = new Elf(Config.getPkgDataDir()+"/test-core-x86",  ElfCommand.ELF_C_READ);
    rawData = segment.getRawData(elfOffset,elfLen);

    //    coreSlice.position(sliceBottom);
    for(int i=0; i<elfLen; i++)
      assertEquals("Offset at 0x"+Long.toHexString(elfOffset+i)
		   +" does not match rawData at location " 
		   + Long.toHexString(i),
		   rawData.internal_buffer[i],
		   coreSlice.get());

    segment.close();
  }

  public void testCoreFileByteBufferPeek() throws ElfException
  {

    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));

    // Test beginning segment
    assertEquals("Peek a byte at 0x0062a000",0x7f,coreBuffer.peek(0x0062a000L));
    assertEquals("Peek a byte at 0x0062a001",0x45,coreBuffer.peek(0x0062a001L));
    assertEquals("Peek a byte at 0x0062a002",0x4c,coreBuffer.peek(0x0062a002L));
    assertEquals("Peek a byte at 0x0062a003",0x46,coreBuffer.peek(0x0062a003L));
    assertEquals("Peek a byte at 0x0062a004",0x01,coreBuffer.peek(0x0062a004L));

    // Test a middleish segment.

    assertEquals("Peek a byte at 0x41490000",(byte)0xca, coreBuffer.peek(0x41490000L));
    assertEquals("Peek a byte at 0x41490001",(byte)0xed,coreBuffer.peek(0x41490001L));


    // Test the end of the last segment
    assertEquals("Peek a byte at 0xBfcfffff",0x0,coreBuffer.peek(0xbfcfffffL));

  }

  public void testCoreFileByteBufferMapOverrun () throws ElfException
  {
    
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));

    // Attempt to peek over a segment boundary, but within the 
    // high and low marks of the bytebuffer


    try 
      {
	coreBuffer.peek(0xbfd0000L);
	fail("peek() in an over boundary should thrown an exception");
      }
    catch (RuntimeException e)
      {
	assertTrue(true);
      }
  }  

  public void testCoreFileByteBufferMapUnderrun () throws ElfException
  {

    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));
    // Attempt to peek under a segment boundary, but within the 
    // high and low marks of the bytebuffer

    try 
      {
	coreBuffer.peek(0);
	fail("peek() in an under boundary should thrown an exception");
      }
    catch (RuntimeException e)
      {
	assertTrue(true);
      }
  }

  
  public void testCoreFileByteBufferSequentialGet() throws ElfException
  {
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));

    coreBuffer.position(0x0062a000L);
    assertEquals("Peek a byte at 0x0062a000",0x7f,coreBuffer.get());
    assertEquals("Peek a byte at 0x0062a001",0x45,coreBuffer.get());
    assertEquals("Peek a byte at 0x0062a002",0x4c,coreBuffer.get());
    assertEquals("Peek a byte at 0x0062a003",0x46,coreBuffer.get());
    assertEquals("Peek a byte at 0x0062a004",0x01,coreBuffer.get());


    // Test reading at position 0. In this core file there is no 
    // segment at position 0 (in our case for this bytebuffer, the 
    // position within the buffer should equal the memory address access.)
    // This should always fail!

    coreBuffer.position(0);
    try 
    {
      coreBuffer.get();
      fail(".get() read at position 0 should have raise an exception but didn't!");

    }
    catch (RuntimeException e)
    {
      assertTrue(true);
    }
  }
  
  
  public void testCoreFileByteBufferPeekArray() throws ElfException
  {
    
    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));
    byte byteArray[] = new byte[10];

    coreBuffer.get(0x0062a000L, byteArray, 0, 10);
    assertEquals("Peek a byte at 0x0062a000",0x7f,byteArray[0]);
    assertEquals("Peek a byte at 0x0062a001",0x45,byteArray[1]);
    assertEquals("Peek a byte at 0x0062a002",0x4c,byteArray[2]);
    assertEquals("Peek a byte at 0x0062a003",0x46,byteArray[3]);
    assertEquals("Peek a byte at 0x0062a004",0x01,byteArray[4]);

  }
  
  public void testCoreFileByteBufferPoke() throws ElfException 
  {

    CorefileByteBuffer coreBuffer = new CorefileByteBuffer(new 
							   File(Config.
								getPkgDataDir(), 
								"test-core-x86"));

    try
    {
      coreBuffer.poke(0x0062a000L, 10);
      fail("Poke a byte at 0x0062a000 should have raised an exception, but did not");
    }
    catch (RuntimeException e)
    {
      // Expected behaviour on a poke is always fail. If it does not fail
      // then something is going wrong. In this case expecting and receiving a fail
      // is the correct behaviour.
      assertTrue(true);
    }

  }
}
