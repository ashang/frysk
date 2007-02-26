// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

package frysk.rt;

import frysk.dom.DOMFunction;
import frysk.dom.DOMSource;
import frysk.proc.Task;
import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.unwind.FrameCursor;
import java.io.File;

public class StackFrame
{

  private StackFrame inner;

  // Accessed by StackFactory.
  StackFrame outer;
  
  private FrameCursor cursor;

  private DOMFunction func;
  
  private DOMSource data;
  
  private Task task;

  private int lineNum;
  
  private int startLine;

  private int endLine;

  private int column;
  
  private String sourceFile = "";
  
  private DwflLine dwflLine;
  
  /**
   * Create a new StackFrame without knowing the inner frame ahead of time.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param task  The Task this StackFrame belongs to.
   */
  public StackFrame (FrameCursor current, Task task)
  {
    this(current, task, null);
  }

  /**
   * Create a new StackFrame, knowing the inner frame ahead of time. Should
   * be called to create all StackFrames except the innermost. Based on whether
   * or not the inner frame is null tells us that this frame is the innermost 
   * frame. With this as a condition, or having this frame a 'signal frame' via
   * an interrupt, determines whether or not the address sent to the Dwfl
   * object is decremented.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param task  The Task this StackFrame belongs to.
   * @param inner   This StackFrame's inner StackFrame.
   */
  public StackFrame (FrameCursor current, Task task, StackFrame inner)
  {
    this.task = task;
    this.cursor = current;
    this.inner = inner;
    long address = this.cursor.getAddress();

    if (address != 0) /* We were able to pull information from this cursor */
      {
        Dwfl dwfl = new Dwfl(task.getTid());
        DwflLine line = null;
        
        /* The innermost frame and frames which were interrupted during
         * execution use their PC to get the line in source. All other 
         * frames have their PC set to the line after the inner frame call
         * and must be decremented by one. */
        if (inner == null || this.cursor.isSignalFrame())
          line = dwfl.getSourceLine(address);
        else
          line = dwfl.getSourceLine(address - 1);
        
        if (line != null)
          {
            this.lineNum = line.getLineNum();
            this.startLine = this.lineNum;
            this.endLine = this.lineNum;
            this.sourceFile = line.getSourceFile();
            this.column = line.getColumn();
          }
        this.dwflLine = line;
      }
  }
  
  public StackFrame (Task task)
  {
      this.task = task;
  }
  
  /**
   * Sets the DOMFunction representing the function which is in turn
   * represented by this StackFrame.  This method also calls
   * {@link setData} using the function's source as an argument; if
   * the function does not have a source, then null is used.
   * 
   * @param f   The DOMFunction for this StackFrame.
   */
  public void setDOMFunction (DOMFunction f)
  {
    this.func = f;
  }
  
  /**
   * Sets the DOMSource, representing the source code of the
   * executable.
   * 
   * @param s   The DOMSource for the executable this StackFrame belongs to.
   */
  public void setDOMSource (DOMSource s)
  {
    this.data = s;
  }
  
  /**
   * Returns this StackFrame's function.  If no function was
   * previously set using {@link setFunction}, this will return null.
   * 
   * @return This StackFrame's function.
   */
  public DOMFunction getDOMFunction ()
  {
    return this.func;
  }
  
  /**
   * Returns this StackFrame's source code.  If no source was
   * previously set using {@link setData}, null is returned.
   * 
   * @return This StackFrame's source code.
   */
  public DOMSource getDOMSource ()
  {
    return this.data;
  }

  /**
   * Return the name of the source file associated with this stack
   * frame.  If the source file is not known, this will return null.
   */
  public String getSourceFile ()
  {
    if (sourceFile == null)
      return "";
    
    return sourceFile;
  }

  /**
   * Return the line number of the source code associated with this
   * stack frame.  Line numbers begin at 1.  If the source line number
   * is not known, this will return 0.
   */
  public int getLineNumber ()
  {
    return lineNum;
  }

  /**
   * Returns the column in the currently executing line in the source file.
   * 
   * @return The column in the currently executing source file line. 
   */
  public int getColumn ()
  {
    return column;
  }

  /**
   * Returns the program counter for this StackFrame.
   * 
   * @return The program counter for this StackFrame.
   */
  public long getAddress ()
  {
    return this.cursor.getAddress();
  }
  
  /**
   * Returns the pre-call PC for this non-interrupted StackFrame.
   * 
   * @return The pre-call program counter for this StackFrame.
   */
  public long getAdjustedAddress ()
  {
    if (this.inner != null && !this.cursor.isSignalFrame())
      return this.cursor.getAddress() - 1;
    else
      return this.cursor.getAddress();
  }

  /**
   * Returns the Task this StackFrame belongs to.
   * 
   * @return The Task this StackFrame belongs to.
   */
  public Task getTask ()
  {
    return task;
  }

  /**
   * Returns this StackFrame's inner frame.
   * 
   * @return This StackFrame's inner frame.
   */
  public StackFrame getInner ()
  {
    return inner;
  }

  /**
   * Returns this StackFrame's outer frame.
   * 
   * @return This StackFrame's outer frame.
   */
  public StackFrame getOuter ()
  {
    return outer;
  }
  
  /**
   * Return a simple string representation of this stack frame.
   * The returned string is suitable for display to the user.
   */
  public String toPrint (boolean isSourceWindow)
  {
      // XXX: There is always an inner cursor.
      if (this.cursor == null)
	  return "Empty stack trace";
    
      // Pad the address based on the task's word size.
      StringBuffer builder = new StringBuffer ("0x");
      String addr = Long.toHexString (getAddress());
      int padding = 2 * task.getIsa().getWordSize() - addr.length();
      for (int i = 0; i < padding; ++i)
	  builder.append('0');
      builder.append(addr);

      // Print the symbol, if known append ().
      Symbol symbol = getSymbol ();
      builder.append(" in ");
      builder.append (symbol.getDemangledName ());
      if (symbol != Symbol.UNKNOWN)
	  builder.append (" ()");

      // If there's line number information append that.
      if (this.dwflLine != null) {
	  builder.append (" from: ");
	  if (! isSourceWindow) {
	      builder.append(this.sourceFile + "#" + this.lineNum);
	  }
	  else {
	      // XXX: This should be a File, not string.
	      String[] fileName = this.sourceFile.split("/");
	      builder.append(fileName[fileName.length - 1] + ": line #" + this.lineNum);
	  }
      }

      return builder.toString();
  }
  
  public int getEndLine ()
  {
    return endLine;
  }
  
  public void setEndLine (int i)
  {
    this.endLine = i;
  }

  public int getStartLine ()
  {
    return startLine;
  }
  
  public void setStartLine (int i)
  {
    this.startLine = i;
  }

  /**
   * Return the DwflLine Object for this StackFrame. If this is null, it is
   * an excellent indication that this frame has no debuginfo.
   * 
   * @return    dwflLine    The DwflLine Object for this StackFrame
   */
  public DwflLine getDwflLine ()
  {
    return this.dwflLine;
  }
  
  public long getReg(long reg)
  {
    // ??? Use something akin to Register interface?
    return this.cursor.get_reg(reg);
  }
  
  /**
   * Returns the Cannonical Frame Address of this StackFrame. Used in
   * conjunction with methodName to provide a unique identifier.
   * 
   * @return cfa    The cannonical frame address of this StackFrame
   */
  public long getCFA()
  {
    return this.cursor.getCfa();
  }
  
  public long setReg(long reg, long val)
  {
    // ??? Use something akin to Register interface?
    return this.cursor.set_reg(reg, val);
  }
  
    /**
     * Return this frame's FrameIdentifier.
     */
    public FrameIdentifier getFrameIdentifier ()
    {
	if (frameIdentifier != null)
	    // XXX: This is wrong, it needs to be the function's
	    // address, and not the current instruction.
	    frameIdentifier = new FrameIdentifier(cursor.getAddress(),
						  cursor.getCfa());
	return this.frameIdentifier;
    }
    private FrameIdentifier frameIdentifier;

    /**
     * Return this frame's symbol; UNKNOWN if there is no symbol.
     */
    public Symbol getSymbol ()
    {
	if (symbol == null) {
	    String mangledName = cursor.getProcName ();
	    if (mangledName == null)
		symbol = Symbol.UNKNOWN;
	    else {
		long address = (cursor.getAddress ()
				- cursor.getProcOffset ());
		symbol = new Symbol (address, mangledName);
	    }
	}
	return symbol;
    }
    private Symbol symbol;

    /**
     * Return this frame's list of lines as an array; returns an empty
     * array if there is no line number information available.
     */
    public Line[] getLines ()
    {
	if (lines == null) {
	    if (cursor != null) {
		Dwfl dwfl = new Dwfl(task.getTid());
		// The innermost frame and frames which were
		// interrupted during execution use their PC to get
		// the line in source. All other frames have their PC
		// set to the line after the inner frame call and must
		// be decremented by one.
		DwflLine dwflLine = dwfl.getSourceLine (getAdjustedAddress ());
		if (dwflLine != null) {
		    lines = new Line[] {
			new Line (new File (dwflLine.getSourceFile ()),
				  dwflLine.getLineNum (),
				  dwflLine.getColumn ())
		    };
		}

	    }
	    // If the fetch failed, mark it as unknown.
	    if (lines == null)
		lines = new Line[0];
	}
	return lines;
    }
    private Line[] lines;
}
