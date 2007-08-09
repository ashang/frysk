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


package frysk.proc;

import frysk.sys.proc.MapsBuilder;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;

public class TestMapGet
    extends TestLib
{


  // Build maps twice, one with getMaps() and once 
  // manually using frysk.sys.proc.MapsBuilder. Test
  // the output and see if they are the same.
  public void testMapGet ()
  {

    Proc ackProc = giveMeAProc();
    final MemoryMap[] testMaps = ackProc.getMaps();


    class TestBuildMaps
      extends MapsBuilder
    {
      
      byte[] mapsLocal;
      int i = 0;
      
      public void buildBuffer (byte[] maps)
      {
	mapsLocal = maps;
	maps[maps.length - 1] = 0;
      }
      
      public void buildMap (long addressLow, long addressHigh,
			    boolean permRead, boolean permWrite,
			    boolean permExecute, boolean shared, long offset,
			    int devMajor, int devMinor, int inode,
			    int pathnameOffset, int pathnameLength)
      {
	
	byte[] filename = new byte[pathnameLength];
	
	System.arraycopy(mapsLocal, pathnameOffset, filename, 0,
			 pathnameLength);

	String name = new String(filename);

	assertEquals("Testing name map " + i, 
		     testMaps[i].name, name);
	assertEquals("Testing addressLow map " + i,
		     testMaps[i].addressLow, addressLow);
	assertEquals("Testing addressHigh map " + i,
		     testMaps[i].addressHigh, addressHigh);
	assertEquals("Testing permRead  map " + i,
		     testMaps[i].permRead, permRead);
	assertEquals("Testing permWrite  map " + i,
		     testMaps[i].permWrite, permWrite);
	assertEquals("Testing permExecute  map " + i,
		     testMaps[i].permExecute, permExecute);
	assertEquals("Testing shared  map " + i,
		     testMaps[i].shared,shared);
	assertEquals("Testing offset  map " + i,
		     testMaps[i].offset, offset);
	assertEquals("Testing devMajor map " + i,
		     testMaps[i].devMajor, devMajor);
	assertEquals("Testing devMinor  map " + i,
		     testMaps[i].devMinor, devMinor);
	assertEquals("Testing inode  map " + i,
		     testMaps[i].inode, inode);
	assertEquals("Testing pathnameOffset  map " + i,
		     testMaps[i].pathnameOffset, pathnameOffset);
	assertEquals("Testing pathnameLength  map " + i,
		     testMaps[i].pathnameLength, pathnameLength);	
	i++;
      }
    }


    TestBuildMaps liveMap = new TestBuildMaps();
    liveMap.construct(ackProc.getPid());
  }


  protected Proc giveMeAProc ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    assertNotNull(ackProc);
    Proc proc = ackProc.assertFindProcAndTasks();
    assertNotNull(proc);
    return proc;
  }

}
