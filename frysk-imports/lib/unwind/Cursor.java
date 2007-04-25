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

import gnu.gcj.RawDataManaged;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Cursor
{
  Logger logger = Logger.getLogger("frysk");
  RawDataManaged cursor = null; 
  Unwind unwinder;

  public Cursor(AddressSpace addressSpace, Accessors accessors)
  {
    this(addressSpace.unwinder.initRemote(addressSpace.addressSpace, accessors),
         addressSpace.unwinder); 
  }
  
  private Cursor(RawDataManaged cursor, Unwind unwinder)
  {
    logger.log(Level.FINE, "{0} Create Cursor\n", this);
    this.cursor = cursor;
    this.unwinder = unwinder;
  }
  
  public boolean isSignalFrame()
  {
    return (unwinder.isSignalFrame(cursor) == 1);
  }
  
  public int step()
  {
    return unwinder.step(cursor);
  }
  
  public ProcName getProcName(int maxNameSize)
  {
    return unwinder.getProcName(cursor, maxNameSize);
  }
  
  public ProcInfo getProcInfo ()
	{
		return unwinder.getProcInfo(cursor);
	}
  
  public Cursor unwind()
  {
    logger.log(Level.FINE, "{0}, unwind\n", this);
    Cursor newCursor = new Cursor(unwinder.copyCursor(cursor), unwinder);
    int step = newCursor.step();
    
    logger.log(Level.FINEST, "{0}, unwind, step returned: {1}\n", 
               new Object[] {this, new Integer(step)});
    
    if (step > 0)
      return newCursor;
       
    return null;
  }
 
  
}
