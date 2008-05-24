// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import frysk.rsl.Log;
import frysk.rsl.LogFactory;

public class Cursor {
    private static final Log fine = LogFactory.fine(Cursor.class);
    private static final Log finest = LogFactory.finest(Cursor.class);

    private final long unwCursor; 
    private final Unwind unwinder;
    private final AddressSpace addressSpace;
    private int step;

    Cursor(AddressSpace addressSpace, long unwCursor, Unwind unwinder) {
	this.addressSpace = addressSpace;
	this.unwCursor = unwCursor;
	this.unwinder = unwinder;
	this.step = 1;
    }
    private Cursor(Cursor orig) {
	this(orig.addressSpace, orig.unwinder.copyCursor(orig.unwCursor),
	     orig.unwinder);
    }
    protected void finalize() {
	unwinder.destroyCursor(unwCursor);
    }

    public boolean isSignalFrame() {
	return (unwinder.isSignalFrame(unwCursor) == 1);
    }
  
    public void getRegister(Number regNum, long offset, int length,
			    byte[] bytes, int start) {
	unwinder.getRegister(unwCursor, regNum, offset, length, bytes, start);
    }

    public void setRegister(Number regNum, long offset, int length,
			    byte[] bytes, int start) {
	unwinder.setRegister(unwCursor, regNum, offset, length, bytes, start);
    }

    public long getIP() {
	return unwinder.getIP(unwCursor);
    }
  
    public long getSP() {
	return unwinder.getSP(unwCursor);
    }
  
    public long getCFA() {
	return unwinder.getCFA(unwCursor);
    }
  
    public int step() {
	return unwinder.step(unwCursor);
    }
  
    public ProcInfo getProcInfo () {
	return new ProcInfo(unwinder, unwinder.getProcInfo(unwCursor));
    }
  
    public Cursor unwind() {
	fine.log(this, "unwind");

	//XXX: Don't unwind if no more, or unknown frames.
	if (step == 0 || getIP() == 0)
	    return null;
    
	Cursor newCursor = new Cursor(this);
	step = newCursor.step();
    
	finest.log(this, "unwind, step returned: ",  step);
    
	if (step > 0)
	    return newCursor;
       
	return null;
    }  
}
