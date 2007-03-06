// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package lib.unwind;


import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.junit.TestCase;
import frysk.sys.TestLib;

public class TestUnwind
    extends TestCase
{
  Logger logger = Logger.getLogger("frysk");
  
  public void testCreateAddress()
  {  
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    
    assertNotNull("AddressSpace should not be null", addr.addressSpace);
  }
  
  public void testCreateCursor()
  {
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    
     Cursor cursor = new Cursor(addr, new Accessors(){

      //@Override
      public int accessFPReg (int regnum, byte[] fpvalp, boolean write)
      {
        return 0;
      }

//    @Override 
      public int accessMem (long addr, byte[] valp, boolean write)
      {
        return 0;
      }

      //@Override
      public int accessReg (int regnum, byte[] valp, boolean write)
      {
        return 0;
      }

      //@Override
      public ProcInfo findProcInfo (long ip, boolean needUnwindInfo)
      {
        return null;
      }

      //@Override
      public int getDynInfoListAddr (byte[] dilap)
      {
        return 0;
      }

      //@Override
      public ProcName getProcName (long addr, int maxNameSize)
      {
        return null;
      }

      //@Override
      public void putUnwindInfo (ProcInfo procInfo)
      {
      }

      //@Override
      public int resume (Cursor cursor)
      {
        return 0;
      }

     });
    
    assertNotNull("Cursor should not be null", cursor.cursor);
  }
  
  public void testPtraceAccessorsProc0()
  {   
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    PtraceAccessors ptraceAccessors = new PtraceAccessors(0, ByteOrder.DEFAULT);
    
    new Cursor(addr, ptraceAccessors);
  }
  
  public void testPtraceAccessorsProcMax()
  {
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    PtraceAccessors ptraceAccessors = new PtraceAccessors(Integer.MAX_VALUE, ByteOrder.DEFAULT);
    
    new Cursor(addr, ptraceAccessors);
  }
  
  public void testPtraceAccessors()
  {
    //Start a bash process.
    final int pid = TestLib.forkIt();
    
    PtraceAccessors.attachXXX(pid);
    
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    
    PtraceAccessors ptraceAccessors = new PtraceAccessors(pid, ByteOrder.DEFAULT);
    
    Cursor cursor = new Cursor(addr, ptraceAccessors);     
    
    int temp = 1;
    while (temp > 0)
      {
        assertNotNull("Cursor should not be null", cursor.cursor);
        logger.log(Level.FINE, "testPtraceAccessors returned: {0}\n", 
                   cursor.getProcName(1000).name);
        temp = cursor.step();
      }
    
    assertEquals("Cursor step return value should be 0", 0, temp);
    
    PtraceAccessors.detachXXX(pid);
  }
  
  public void testPtraceAccessorsSmallMaxName()
  {
    //Start a process.
   final int pid = TestLib.forkIt();
    
     PtraceAccessors.attachXXX(pid);
    
    AddressSpace addr = new AddressSpace(ByteOrder.DEFAULT);
    
    PtraceAccessors ptraceAccessors = new PtraceAccessors(pid, ByteOrder.DEFAULT);
    
    Cursor cursor = new Cursor(addr, ptraceAccessors);     
    
    int temp = 1;
    while (temp > 0)
      {
        assertNotNull("Cursor should not be null", cursor.cursor);
        logger.log(Level.FINE, "testPtraceAccessorsSmallMaxName returned: {0}\n", cursor.getProcName(10).name);
        temp = cursor.step();
      }
    
    assertEquals("Cursor step return value should be 0", 0, temp);
    
    PtraceAccessors.detachXXX(pid);
  }
}
