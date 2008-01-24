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
import frysk.isa.registers.Register;
import java.util.logging.Logger;
import lib.unwind.Cursor;
import frysk.isa.ISA;
import frysk.proc.Task;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;
import frysk.isa.registers.RegisterMap;

class LibunwindFrame extends Frame
{  
    private static Logger logger = Logger.getLogger("frysk");
    private Symbol symbol;
  
    /* Identifies this frame by its CFA and frame start address */
    private FrameIdentifier frameIdentifier;
  
    private final Cursor cursor;
    private final RegisterMap registerMap;
    private final ISA isa;

    /**
     * Creates a new LibunwindFrame object. Represents a frame on the
     * stack of a remote (non-local) process.
     */
    LibunwindFrame(Cursor cursor, Frame inner, Task task) {
	super(inner, task);
	this.cursor = cursor;
	this.isa = task.getISA();
	this.registerMap = LibunwindRegisterMapFactory.getRegisterMap(isa);
    }
  
    protected Frame unwind() {
	Cursor newCursor = this.cursor.unwind();
	if (newCursor == null)
	    return null;
	return new LibunwindFrame(newCursor, this, getTask());
    }

    /**
     * Returns the current program counter of this Frame.
     */
    public long getAddress() {
      return cursor.getIP();
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
	if (registerMap.containsKey(register)) {
	    Number number = registerMap.getRegisterNumber(register);
	    logger.log(Level.FINE, "{0}: getRegister register: {1} ({2})\n",
		       new Object[] { this, register, number });
	    cursor.getRegister(number, offset, length, bytes, start);
	} else {
	    getTask().access(register, (int)offset, length, bytes, start,
			     false);
	}
    }
  
    public void setRegister(Register register, long offset, int length,
			    byte[] bytes, int start) {
	if (registerMap.containsKey(register)) {
	    Number number = registerMap.getRegisterNumber(register);
	    logger.log(Level.FINE, "{0}: getRegister register: {1} ({2})\n",
		       new Object[] { this, register, number });
	    cursor.setRegister(registerMap.getRegisterNumber(register),
			       offset, length, bytes, start);
	} else {
	    getTask().access(register, (int)offset, length, bytes, start,
			     true);
	}
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
     *
     * The frame identifier is the combination of the current symbols
     * (function) start address and the more outer frame's inner most
     * address.
     */
    public FrameIdentifier getFrameIdentifier () {
	if (frameIdentifier == null) {
	  long functionAddress = getSymbol().getAddress();
	  // Note, cursor.getCFA is wrong here; libunwind returns the
	  // CFA used as part of computing the location of registers
	  // in the current cursor and not the "CFA" of this frame;
	  // effectively this frame's stack-pointer (in fact often
	  // getCFA() == getSP()).
	  long cfa = 0;
	  Frame outer = getOuter();
	  if (outer != null)
	      // Need an address that is constant through out the
	      // lifetime of the frame (in particular when the stack
	      // grows).  Use the outer-to-this frame's inner most
	      // stack address a.k.a. the stack-pointer..
	      cfa = ((LibunwindFrame)outer).cursor.getSP();
	  frameIdentifier = new FrameIdentifier(functionAddress, cfa);
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
