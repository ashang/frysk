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

package frysk.stack;

import java.util.logging.Level;
import frysk.isa.Register;
import java.util.logging.Logger;
import lib.unwind.Cursor;
import lib.unwind.ProcInfo;
import lib.unwind.ProcName;
import frysk.isa.ISA;
import frysk.proc.Task;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;
import frysk.isa.RegisterMap;

class LibunwindFrame extends Frame
{  
    private static Logger logger = Logger.getLogger("frysk");
    private Symbol symbol;
  
    /* Identifies this frame by its CFA and frame start address */
    private FrameIdentifier frameIdentifier;
  
    LibunwindFrame outer = null;
   
    private final Cursor cursor;
    private final RegisterMap registerMap;
    private final ISA isa;

    /**
     * Creates a new LibunwindFrame object. Represents a frame on the stack of a 
     * remote (non-local) process.
     * 
     * @param cursor The Cursor used to unwind this Frame 
     * @param task The Task whose stack this Frame belongs to
     */
    LibunwindFrame (Cursor cursor, Task task) {
	super(task);
	this.cursor = cursor;
	this.isa = task.getISA();
	this.registerMap = LibunwindRegisterMapFactory.getRegisterMap(isa);
    }

    LibunwindFrame(Cursor cursor, Frame inner) {
	super(inner);
	this.cursor = cursor;
	this.isa = inner.getTask().getISA();
	this.registerMap = LibunwindRegisterMapFactory.getRegisterMap(isa);
    }
  
    private LibunwindFrame getLibunwindOuter() {
	if (outer == null) {
	    Cursor newCursor = this.cursor.unwind();
	    if (newCursor != null) {
		outer = new LibunwindFrame(newCursor, this);
	    }
	}
	return outer;
    }

    /**
     * Returns the Frame outer to this Frame on the stack. If that Frame object
     * has not yet been created, it is created using this Frame's Cursor and
     * unwinding outwards a single frame.
     */
    public Frame getOuter() {
	return getLibunwindOuter();
    }
  
    /**
     * Returns the ProcInfo object for this Frame.
     */
    public ProcInfo getProcInfo () {
	return cursor.getProcInfo();
    }
  
    /**
     * Returns the current program counter of this Frame.
     */
    public long getAddress() {
	ProcInfo myInfo = cursor.getProcInfo();
	ProcName myName = cursor.getProcName(0);
    
	if (myInfo.getError() != 0 || myName.getError() != 0)
	    return 0;
    
	return myInfo.getStartIP() + myName.getOffset();
    }
  
    /**
     * Returns the adjusted address of this frame. If this Frame is an
     * innermost frame, the current program counter is returned
     * as-is. Otherwise, it is decremented by one, to represent the
     * frame address pointing to its inner frame, rather than the
     * inner frame's return address.
     */
    public long getAdjustedAddress() {
	if (getInner() != null && !this.cursor.isSignalFrame())
	    return getAddress() - 1;
	else
	    return getAddress();
    }
  
    public void getRegister(Register register, long offset, int length,
			    byte[] bytes, int start) {
	Number number = registerMap.getRegisterNumber(register);
	logger.log(Level.FINE, "{0}: getRegister register: {1} ({2})\n",
		   new Object[] { this, register, number });
	cursor.getRegister(number, offset, length, bytes, start);
    }
  
    public void setRegister(Register register, long offset, int length,
			    byte[] bytes, int start) {
	Number number = registerMap.getRegisterNumber(register);
	logger.log(Level.FINE, "{0}: getRegister register: {1} ({2})\n",
		   new Object[] { this, register, number });
	cursor.setRegister(registerMap.getRegisterNumber(register),
			   offset, length, bytes, start);
    }
  
    /**
     * Returns the given byte array as a long.
     * 
     * @param word The byte array
     * @return val The converted long
     */
    public long byteArrayToLong(byte[] word) {
	long val = 0;
	for (int i = 0; i < word.length; i++)
	    val = val << 8 | (word[i] & 0xff);
	return val;
    }
  
    /**
     * Return this frame's FrameIdentifier.
     */
    public FrameIdentifier getFrameIdentifier () {
	if (frameIdentifier == null) {
	    ProcInfo myInfo = getProcInfo();
	    long cfa = 0;
	    LibunwindFrame outer = getLibunwindOuter();
	    if (outer != null)
		// The previous frame's SP makes for a good CFA for
		// this frame.  It's a value that needs to be constant
		// through out the life-time of this frame, and hence
		// this frame's SP (which changes) is no good.
		cfa = outer.cursor.getSP();
	    frameIdentifier = new FrameIdentifier(myInfo.getStartIP(), cfa);
	}
	return this.frameIdentifier;
    }
  
    /**
     * Returns whether or not this frame's execution was interrupted by
     * a signal.
     * @return true If this Frame is a signal frame.
     */
    public boolean isSignalFrame() {
	return cursor.isSignalFrame();
    }

    /**
     * Return this frame's symbol; UNKNOWN if there is no symbol.
     */
    public Symbol getSymbol() {
	if (symbol == null) {
	    symbol = SymbolFactory.getSymbol(getTask(), getAdjustedAddress());
	}
	return symbol;
    }
    
}
