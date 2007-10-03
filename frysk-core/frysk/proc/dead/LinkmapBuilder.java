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

package frysk.proc.dead;

import inua.eio.ByteBuffer;

/**
 * Build a list of maps from the contents of the file linkmap
 * table at address specified
 */
public abstract class LinkmapBuilder
{

  /**
   * Create a LinkmapBuilder; can only extend.
   */
  protected LinkmapBuilder ()
  {
  }
  
  /**
   * Scan the maps file found in <tt>/proc/PID/auxv</tt> building up
   * a list of memory maps.  Return true if the scan was successful.
   */
  public final void construct (long addr, ByteBuffer buffer)
  {
    
    long linkStep = 0xff;
    long l_ld = 0;
    long l_addr = 0;
    long stringAddr = 0;
    String name = "";
    if (buffer != null)
      {
	buffer.position(addr);
	
	//buffer.position(addr);
	while (linkStep != 0)
	  {
	    l_addr = buffer.getUWord();
	    stringAddr = buffer.getUWord();
	    l_ld = buffer.getUWord();
	    linkStep = buffer.getUWord();
	    name = getString(stringAddr,buffer);

	    buildMap(l_addr,l_ld,stringAddr,name);
	    if (linkStep !=0)
	      buffer.position(linkStep);
	  }
      }
  }

 
  /**
   * Build an address map covering [addressLow,addressHigh) with
   * permissions {permR, permW, permX, permP }, device devMajor
   * devMinor, inode, and the pathname's offset/length within the
   * buf.
   *
   * !shared implies private, they are mutually exclusive.
   */
  
  abstract public void buildMap (long l_addr, long l_ld, long saddr, String name);

  private String getString(long address, ByteBuffer buffer)
  {
      byte[] sbuffer = new byte[255];
      for (int i=0; i<sbuffer.length; i++)
	  sbuffer[i] = 0; 
      int count = 0;
      byte in = -1;
      String constructedName = "";
      
      long currentPos = buffer.position();
      buffer.position(address);
      
      while (in != 0)
      {
	  // Read until end of buffer or null
	  try
	  {
	      in = buffer.getByte();
	  }
	  catch (RuntimeException e)
	  {
	      break;
	  }

	  if (in == 0)
	      break;
	  sbuffer[count] = in;
	  count++;
      }

      constructedName = new String(sbuffer);
      constructedName = constructedName.trim();
      
      buffer.position(currentPos);
      return constructedName;
  }
}
