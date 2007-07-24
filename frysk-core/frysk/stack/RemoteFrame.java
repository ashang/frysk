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

import java.io.File;

import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.rt.Line;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;

import lib.dwfl.Dwfl;
import lib.dwfl.DwflLine;
import lib.unwind.Cursor;
import lib.unwind.ProcInfo;
import lib.unwind.ProcName;

class RemoteFrame extends Frame
{  
  
  private Symbol symbol;
  private Line[] lines;
  
  /* Identifies this frame by its CFA and frame start address */
  private FrameIdentifier frameIdentifier;
  
  RemoteFrame inner = null;
  RemoteFrame outer = null;
   
  /**
   * Creates a new RemoteFrame object. Represents a frame on the stack of a 
   * remote (non-local) process.
   * 
   * @param cursor The Cursor used to unwind this Frame 
   * @param task The Task whose stack this Frame belongs to
   */
  RemoteFrame (Cursor cursor, Task task)
  {
    this.cursor = cursor;
    this.task = task;
  }
  
  /**
   * Returns the Frame outer to this Frame on the stack. If that Frame object
   * has not yet been created, it is created using this Frame's Cursor and
   * unwinding outwards a single frame.
   */
  public Frame getOuter()
  {
   if (outer == null)
      {
	Cursor newCursor = this.cursor.unwind();
	if (newCursor != null) 
	  {
	    outer = new RemoteFrame(newCursor, task);
	    outer.inner = this;
	  }
      }
    return outer;
  }
  
  /**
   * Returns the Frame inner to this frame on the stack.
   */
  public Frame getInner()
  {
    return inner;
  }
  
  /**
   * Returns the ProcInfo object for this Frame.
   */
  public ProcInfo getProcInfo ()
  {
    return cursor.getProcInfo();
  }
  
  /**
   * Returns the current program counter of this Frame.
   */
  public long getAddress()
  {
    ProcInfo myInfo = cursor.getProcInfo();
    ProcName myName = cursor.getProcName(0);
    
    if (myInfo.getError() != 0 || myName.getError() != 0)
      	return 0;
    
   return myInfo.getStartIP() + myName.getOffset();
  }
  
  /**
   * Returns the adjusted address of this frame. If this Frame is an innermost
   * frame, the current program counter is returned as-is. Otherwise, it is 
   * decremented by one, to represent the frame address pointing to its inner
   * frame, rather than the inner frame's return address.
   */
  public long getAdjustedAddress()
  {
    if (this.inner != null && !this.cursor.isSignalFrame())
      return getAddress() - 1;
    else
      return getAddress();
  }
  
  /**
   * Returns the value stored at the given register number.
   */
  public long getReg(int regNum)
  {
    byte[] word = new byte[task.getIsa().getWordSize()];
    if (cursor.getRegister(regNum, word) < 0)
      return 0;
    return byteArrayToLong(word);
  }
  
  /**
   * Returns the value stored at the given register number.
   */
  public long getReg (long regNum)
  {
    return getReg((int) regNum);
  }
  
  /**
   * Returns the given byte array as a long.
   * 
   * @param word The byte array
   * @return val The converted long
   */
  public long byteArrayToLong(byte[] word)
  {
    long val = 0;
    for (int i = 0; i < word.length; i++)
      val = val << 8 | (word[i] & 0xff);
    return val;
  }
  
  /**
   * Returns the Canonical Frame Address of this Frame.
   */
  public long getCFA()
  {
    byte[] word = new byte[task.getIsa().getWordSize()];
    if (cursor.getSP(word) < 0)
      return 0;
    return byteArrayToLong(word);
  }
  
  /**
   * Return this frame's FrameIdentifier.
   */
  public FrameIdentifier getFrameIdentifier ()
  {
    if (this.frameIdentifier == null)
      {
        ProcInfo myInfo = getProcInfo();
        this.frameIdentifier = new FrameIdentifier(myInfo.getStartIP(),
                                          getCFA());
      }
    return this.frameIdentifier;
  }
  
  /**
   * Sets the value of the given register number with the word value.
   */
  public int setReg(int regNum, long word)
  {
    return cursor.setRegister(regNum, word);
  }
  
  /**
   * Sets the value of the given register number with the word value.
   */
  public long setReg (long regNum, long word)
  {
    return (long) setReg ((int) regNum, word);
  }
  
  /**
   * Returns whether or not this frame's execution was interrupted by
   * a signal.
   * @return true If this Frame is a signal frame.
   */
  public boolean isSignalFrame()
  {
    return cursor.isSignalFrame();
  }

   /**
   * Return this frame's symbol; UNKNOWN if there is no symbol.
   */
    public Symbol getSymbol() {
	if (symbol == null) {
	    symbol = SymbolFactory.getSymbol(task, getAdjustedAddress());
	}
	return symbol;
    }

  /**
   * Return this frame's list of lines as an array; returns an empty array if
   * there is no line number information available. The lack of line-number
   * information can be determined with the test: <<tt>>.getLines().length == 0</tt>.
   * XXX: When there are multiple lines, it isn't clear if there is a well
   * defined ordering of the information; for instance: outer-to-inner or
   * inner-to-outer.
   */
  public Line[] getLines ()
  {
    if (this.lines == null)
      {
	if (this.cursor != null)
	  {
	    Dwfl dwfl = DwflCache.getDwfl(task);
	    // The innermost frame and frames which were
	    // interrupted during execution use their PC to get
	    // the line in source. All other frames have their PC
	    // set to the line after the inner frame call and must
	    // be decremented by one.
	    DwflLine dwflLine = dwfl.getSourceLine(getAdjustedAddress());
	    if (dwflLine != null)
	      {
		File f = new File(dwflLine.getSourceFile());
		if (! f.isAbsolute())
		  {
		    /* The file refers to a path relative to the compilation
		     * directory; so prepend the path to that directory in
		     * front of it. */
		    File parent = new File(dwflLine.getCompilationDir());
		    f = new File(parent, dwflLine.getSourceFile());
		  }

		this.lines = new Line[] { new Line(f, dwflLine.getLineNum(),
						   dwflLine.getColumn(),
						   this.task.getProc()) };
	      }

	  }
	// If the fetch failed, mark it as unknown.
	if (this.lines == null)
	  this.lines = new Line[0];
      }

    return this.lines;
  }

}
