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
import lib.dw.DwflModule;
import lib.unwind.FrameCursor;

public class StackFrame
{

  protected StackFrame inner;

  protected StackFrame outer;
  
  protected FrameCursor myCursor;

  private DOMFunction func;
  
  private DOMSource data;
  
  private Task task;

  private int lineNum;
  
  private int startLine;

  private int endLine;

  private int startOffset;

  private int endOffset;

  private int column;
  
  private String sourceFile = "";
  
  private DwflLine dwflLine;
  
  /**
   * Create a new StackFrame without knowing the inner frame ahead of time.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param task  The Task this StackFrame belongs to.
   */
  public StackFrame (FrameCursor current, Task myTask)
  {
    this(current, myTask, null);
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
  public StackFrame (FrameCursor current, Task myTask, StackFrame inner)
  {
    this.task = myTask;
    this.myCursor = current;
    this.inner = inner;
    long address = this.myCursor.getAddress();

    if (address != 0) /* We were able to pull information from this cursor */
      {
        Dwfl dwfl = new Dwfl(myTask.getTid());
        DwflLine line = null;
        
        /* The innermost frame and frames which were interrupted during
         * execution use their PC to get the line in source. All other 
         * frames have their PC set to the line after the inner frame call
         * and must be decremented by one. */
        if (inner == null || this.myCursor.isSignalFrame())
          line = dwfl.getSourceLine(address);
        else
          line = dwfl.getSourceLine(address - 1);
        
        if (line != null)
          {
            this.lineNum = line.getLineNum();
            this.startLine = this.lineNum;
            this.endLine = this.lineNum;
            this.startOffset = 0;
            this.endOffset = -1;
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
  public void setDOMFunction(DOMFunction f)
  {
    this.func = f;
  }
  
  /**
   * Sets the DOMSource, representing the source code of the
   * executable.
   * 
   * @param s   The DOMSource for the executable this StackFrame belongs to.
   */
  public void setDOMSource(DOMSource s)
  {
    this.data = s;
  }
  
  /**
   * Returns this StackFrame's function.  If no function was
   * previously set using {@link setFunction}, this will return null.
   * 
   * @return This StackFrame's function.
   */
  public DOMFunction getDOMFunction()
  {
    return this.func;
  }
  
  /**
   * Returns this StackFrame's source code.  If no source was
   * previously set using {@link setData}, null is returned.
   * 
   * @return This StackFrame's source code.
   */
  public DOMSource getData()
  {
    return this.data;
  }

  /**
   * Return the name of the function associated with this stack frame.
   * This will return null if the function's name is not known.
   */
  public String getMethodName ()
  {
    if (this.myCursor == null || this.myCursor.getMethodName() == null)
      return null;
    
    return this.myCursor.getMethodName();
  }
  
  /**
   * Return the name of the function associated with this stack frame.
   * This will return null if the function's name is not known.
   *
   * @return the name of the function associated with this stack frame.
   * Or null if not known. 
   */
  public String getSymbolName()
  {
    Dwfl dwfl = new Dwfl(task.getTid());
    DwflModule dm = dwfl.getModule(getAddress());
    if (dm != null)
      return dm.getAddressName(getAddress());
    else
      return "";
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
    return this.myCursor.getAddress();
  }
  
  /**
   * Returns the pre-call PC for this non-interrupted StackFrame.
   * 
   * @return The pre-call program counter for this StackFrame.
   */
  public long getAdjustedAddress ()
  {
    if (this.inner != null && !this.myCursor.isSignalFrame())
      return this.myCursor.getAddress() - 1;
    else
      return this.myCursor.getAddress();
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
    if (this.myCursor == null)
      return "Empty stack trace";
    
    StringBuffer builder = new StringBuffer("0x");
    
    String addr = Long.toHexString(getAddress());
    
    // Pad the address based on the task's word size.
    int padding = 2 * task.getIsa().getWordSize() - addr.length();
    for (int i = 0; i < padding; ++i)
    builder.append('0');
    
    builder.append(addr);
    
   String funcString = getSymbolName();
   
   //XXX: This has to go, above uses libdwfl, this uses libunwind.
   funcString = getMethodName();
    
    if (this.dwflLine != null)
      {
        if (funcString == null)
          funcString = "[unknown] from: ";
        else
          funcString = funcString + " () from: ";
        
        if (! isSourceWindow)
          {
            builder.append(" in "
                  + funcString
                  + this.sourceFile + "#" + this.lineNum);
          }
        else
          {
            String[] fileName = this.sourceFile.split("/");
            builder.append(" in "
                  + funcString
                  + fileName[fileName.length - 1] + ": line #" + this.lineNum);
          }
      }
    else
      {
        if (funcString == null)
          funcString = "[unknown]";
        else
          funcString = funcString + " ()";
        builder.append(" in "
              + funcString);
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

  public int getEndOffset ()
  {
    return endOffset;
  }
  
  public int getStartLine ()
  {
    return startLine;
  }
  
  public void setStartLine (int i)
  {
    this.startLine = i;
  }

  public int getStartOffset ()
  {
    return startOffset;
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
  
  /**
   * Sets whether or not this frame is a signal frame - meaning it has been
   * sent an interrupt, and we should not decrement its address when trying
   * to get its line number from the Dwfl object.
   * 
   * @param sigFrame    Whether or not this frame is a 'signal frame.'
   */
  public void setIsSignalFrame(boolean sigFrame)
  {
    this.myCursor.setIsSignalFrame(sigFrame);
  }
  
  /**
   * Returns whether or not this frame is a signal frame.
   * 
   * @return Whether or not this frame is a signal frame.
   */
  public boolean getIsSignalFrame()
  {
    return this.myCursor.isSignalFrame();
  }
  
  public long getReg(long reg)
  {
    // ??? Use something akin to Register interface?
    return this.myCursor.get_reg(reg);
  }
  
  /**
   * Returns the Cannonical Frame Address of this StackFrame. Used in
   * conjunction with methodName to provide a unique identifier.
   * 
   * @return cfa    The cannonical frame address of this StackFrame
   */
  public long getCFA()
  {
    return this.myCursor.getCfa();
  }
  
  public long setReg(long reg, long val)
  {
    // ??? Use something akin to Register interface?
    return this.myCursor.set_reg(reg, val);
  }
}

