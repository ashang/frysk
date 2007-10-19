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

import java.io.PrintWriter;

import frysk.isa.Register;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;
import frysk.value.Value;
import frysk.value.ScratchLocation;

public abstract class Frame {

    private final Task task;
    private final Frame inner;
    private Frame outer;
    private final int level;

    /**
     * Construct a frame.  For the inner most frame "inner" is NULL.
     *
     * XXX: pacakge-private if you need to extend this, use
     * FrameDecorator.
     */
    Frame(Frame inner, Task task) {
	this.inner = inner;
	this.task = task;
	this.level = (inner == null) ? 0 : inner.level() + 1;
	if (inner != null) {
	    inner.outer = this;
	}
    }

    /**
     * Returns the program counter for this StackFrame.
     * 
     * @return The program counter for this StackFrame.
     */
    public abstract long getAddress ();
  
    /**
     * Returns the pre-call PC for this non-interrupted StackFrame.
     * 
     * @return The pre-call program counter for this StackFrame.
     */
    public abstract long getAdjustedAddress ();

    /**
     * Returns the Task this StackFrame belongs to.
     * 
     * @return The Task this StackFrame belongs to.
     */
    public final Task getTask() {
	return task;
    }

    public final int level() {
	return level;
    }

    /**
     * Returns this StackFrame's inner frame.
     * 
     * @return This StackFrame's inner frame.
     */
    public final Frame getInner () {
	return inner;
    }

    /**
     * Returns this StackFrame's outer frame.
     * 
     * @return This StackFrame's outer frame.
     */
    public final Frame getOuter () {
	if (!unwound && outer == null) {
	    // Only try to unwind once.
	    unwound = true;
	    outer = unwind();
	}
	return outer;
    }
  
    /**
     * Has there already been an unwind attempt?
     */
    private boolean unwound = false;

    /**
     * Returns this Frame's outer frame; or null if there is no frame.
     */
    protected abstract Frame unwind();

    /**
     * Write a simple string representation of this stack frame.
     * @param printWriter
     */
    public void toPrint (PrintWriter writer) {
	// the frame number ...
	writer.print("#");
	writer.print(level());
	writer.print(" ");
	// the address (padded to task's word size) ...
	writer.write("0x");
	String addr = Long.toHexString(getAddress());
	int padding = 2 * getTask().getISA().wordSize() - addr.length();
	for (int i = 0; i < padding; ++i)
	    writer.write('0');
	writer.write(addr);
	// the symbol, if known append (), ...
	Symbol symbol = getSymbol();
	writer.write(" in ");
	writer.write(symbol.getDemangledName());
	if (symbol != SymbolFactory.UNKNOWN)
	    writer.write(" ()");
	// the library if known ...
	String library = getLibraryName();
	if (library != null) {
	    writer.print(" from ");
	    writer.print(library);
	}
    }

    /**
     * Write a simple string representation of this stack frame.
     * @param printWriter
     */
    public void toPrint (PrintWriter printWriter, boolean printSourceLibrary) {
	// Pad the address based on the task's word size.
	printWriter.write("0x");
	String addr = Long.toHexString(getAddress());
	int padding = 2 * getTask().getISA().wordSize() - addr.length();
	for (int i = 0; i < padding; ++i)
	    printWriter.write('0');
	printWriter.write(addr);
	
	// Print the symbol, if known append ().
	Symbol symbol = getSymbol();
	printWriter.write(" in ");
	printWriter.write(symbol.getDemangledName());
	if (symbol != SymbolFactory.UNKNOWN)
	    printWriter.write(" ()");
	
	if (printSourceLibrary) {
	    printWriter.print(" from " + this.getLibraryName());
	}
    }
  
    public String getLibraryName() {
	Dwfl dwfl = DwflCache.getDwfl(getTask());
	DwflModule dwflModule = dwfl.getModule(getAdjustedAddress()); 
	if (dwflModule != null) {
	    return dwflModule.getName();
	} else {
	    return "Unknown";
	}
    }
  

    /**
     * Extracts OFFSET:LENGTH bytes of REGISTER storing them from
     * START in BYTES (in the target's byte order).
     */
    public abstract void getRegister(Register register,
				     long offset, int length,
				     byte[] bytes, int start);
    private byte[] getRegisterBytes(Register register) {
	byte[] bytes = new byte[register.getType().getSize()];
	getRegister(register, 0, register.getType().getSize(), bytes, 0);
	return bytes;
    }
    public final Value getRegisterValue(Register register) {
	return new Value(register.getType(),
			 new ScratchLocation(getRegisterBytes(register)));
    }
    public final long getRegister(Register register) {
	return getRegisterValue(register).asLong();
    }
    /**
     * Stores BYTES from START into REGISTER at OFFSET:LENGTH (in the
     * target's byte order).
     */
    public abstract void setRegister(Register register,
				     long offset, int length,
				     byte[] bytes, int start);
  
    /**
     * Return this frame's FrameIdentifier.
     */
    public abstract FrameIdentifier getFrameIdentifier ();

    /**
     * Return this frame's symbol; UNKNOWN if there is no symbol.
     */
    public abstract Symbol getSymbol ();
  
}
