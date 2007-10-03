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

import frysk.proc.Task;
import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.unwind.FrameCursor;
import java.io.File;

public class StackFrame extends Frame
{

  private Frame inner;

  // Accessed by StackFactory.
  Frame outer;
  
  private FrameCursor cursor;
 
  private FrameIdentifier frameIdentifier;
  
  private Symbol symbol;
  
  private Line[] lines;

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
  public StackFrame (FrameCursor current, Task task, Frame inner)
  {
    this.task = task;
    this.cursor = current;
    this.inner = inner;
  }
  
  public StackFrame (Task task)
  {
      this.task = task;
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
   * Returns this StackFrame's inner frame.
   * 
   * @return This StackFrame's inner frame.
   */
  public Frame getInner ()
  {
    return inner;
  }

  /**
   * Returns this StackFrame's outer frame.
   * 
   * @return This StackFrame's outer frame.
   */
  public Frame getOuter ()
  {
    return outer;
  }
  
  
  public String getProcName()
  {
    Symbol symbol = getSymbol();
    return symbol.getDemangledName();
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
      Line[] lines = getLines ();
      for (int i = 0; i < lines.length; i++) {
	  Line line = lines[i];
	  builder.append (" from: ");
	  if (! isSourceWindow) {
	      builder.append(line.getFile ().getPath ());
	      builder.append ("#");
	      builder.append (line.getLine ());
	  }
	  else {
	      builder.append (line.getFile ().getName ());
	      builder.append (": line #");
	      builder.append (line.getLine ());
	  }
      }

      return builder.toString();
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
    if (this.frameIdentifier != null)
      // XXX: This is wrong, it needs to be the function's
      // address, and not the current instruction.
      this.frameIdentifier = new FrameIdentifier(cursor.getAddress(),
                                            cursor.getCfa());
    return this.frameIdentifier;
  }

    /**
     * Return this frame's symbol; UNKNOWN if there is no symbol.
     */
  public Symbol getSymbol ()
  {
    if (this.symbol == null)
      {
        String mangledName = cursor.getProcName();
        if (mangledName == null)
          this.symbol = Symbol.UNKNOWN;
        else
          {
            long address = getAddress() - cursor.getProcOffset();
            this.symbol = new Symbol(address, mangledName);
          }
      }
    return this.symbol;
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
            Dwfl dwfl = new Dwfl(this.task.getTid());
            // The innermost frame and frames which were
            // interrupted during execution use their PC to get
            // the line in source. All other frames have their PC
            // set to the line after the inner frame call and must
            // be decremented by one.
            DwflLine dwflLine = dwfl.getSourceLine(getAdjustedAddress());
            if (dwflLine != null)
              {
                File f = new File (dwflLine.getSourceFile());
                if (!f.isAbsolute())
                  {
                    /* The file refers to a path relative to the compilation
                     * directory; so prepend the path to that directory in
                     * front of it. */
                    File parent = new File(dwflLine.getCompilationDir());
                    f = new File (parent, dwflLine.getSourceFile());
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
