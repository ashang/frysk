// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
import gnu.gcj.RawDataManaged;
import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.unwind.FrameCursor;

public class StackFrame
{

  private String methodName;

  private String sourceFile;

  private int lineNum;
  
  private int startLine;

  private int endLine;

  private int startOffsset;

  private int endOffset;

  private int column;

  private long address;

  protected StackFrame inner;

  protected StackFrame outer;

  private RawDataManaged unwind_data;
  
  private DOMFunction func;
  
  private DOMSource data;

  private Task myTask;
  
  private boolean isSignalFrame = false;
  
  /**
   * Create a new StackFrame without knowing the inner frame ahead of time.
   * 
   * @param current The FrameCursor representing the current frame.
   * @param myTask  The Task this StackFrame belongs to.
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
   * @param myTask  The Task this StackFrame belongs to.
   * @param inner   This StackFrame's inner StackFrame.
   */
  public StackFrame (FrameCursor current, Task myTask, StackFrame inner)
  {
    this.myTask = myTask;
    unwind_data = current.getNativeCursor();
    initialize();
    this.inner = inner;

    if (address != 0) /* We were able to pull information from this cursor */
      {
        Dwfl dwfl = new Dwfl(myTask.getTid());
        DwflLine line = null;
        
        /* The innermost frame and frames which were interrupted during
         * execution use their PC to get the line in source. All other 
         * frames have their PC set to the line after the inner frame call
         * and must be decremented by one. */
        if (inner == null || isSignalFrame)
          line = dwfl.getSourceLine(address);
        else
          line = dwfl.getSourceLine(address - 1);
        
        if (line != null)
          {
            this.lineNum = line.getLineNum();
            this.startLine = this.lineNum;
            this.endLine = this.lineNum;
            this.startOffsset = 0;
            this.endOffset = -1;
            this.sourceFile = line.getSourceFile();
            this.column = line.getColumn();
          }
      }
    else
      {
        this.sourceFile = "<Unknown file>";
      }
  }
  
  /**
   * Native function. Sets methodName and address via the cursor obtained
   * from unwind_data.
   */
  private native void initialize ();
  
  /**
   * Sets func to the incoming DOMFunction representing the function which
   * is in turn represented by this StackFrame. 
   * 
   * @param f   The DOMFunction for this StackFrame.
   */
  public void setFunction(DOMFunction f)
  {
    this.func = f;
    if (f != null)
        setData(f.getSource());
    else
        setData(null);
  }
  
  /**
   * Sets data to the incoming DOMSource, representing the source code of
   * the executable.
   * 
   * @param s   The DOMSource for the executable this StackFrame belongs to.
   */
  public void setData(DOMSource s)
  {
    this.data = s;
  }
  
  /**
   * Returns this StackFrame's function.
   * 
   * @return func   This StackFrame's function.
   */
  public DOMFunction getFunction()
  {
    return this.func;
  }
  
  /**
   * Returns this StackFrame's source code.
   * 
   * @return data   This StackFrame's source code.
   */
  public DOMSource getData()
  {
    return this.data;
  }

  /**
   * Returns this StackFrame's method name.
   * 
   * @return methodName  This StackFrame's method name.
   */
  public String getMethodName ()
  {
    return methodName;
  }

  /**
   * Returns the name of the source file represented by this StackFrame
   * 
   * @return sourceFile The name of the SourceFile represented by this 
   * StackFrame.
   */
  public String getSourceFile ()
  {
    return sourceFile;
  }

  /**
   * Returns the current line number of this StackFrame.
   * 
   * @return lineNum    The current line number of this Stackframe.
   */
  public int getLineNumber ()
  {
    return lineNum;
  }

  /**
   * Returns the column in the currently executing line in the source file.
   * 
   * @return column The column in the currently executing source file line. 
   */
  public int getColumn ()
  {
    return column;
  }

  /**
   * Returns the program counter for this StackFrame.
   * 
   * @return address    The program counter for this StackFrame.
   */
  public long getAddress ()
  {
    return address;
  }

  /**
   * Returns the Task this StackFrame belongs to.
   * 
   * @return myTask The Task this StackFrame belongs to.
   */
  public Task getMyTask ()
  {
    return myTask;
  }

  /**
   * Returns the RawDataManaged  which represents this StackFrame's cursor.
   * 
   * @return unwind_data    This StackFrame's cursor.
   */
  protected RawDataManaged getUnwindData ()
  {
    return unwind_data;
  }

  /**
   * Returns this StackFrame's inner frame.
   * 
   * @return inner  This StackFrame's inner frame.
   */
  public StackFrame getInner ()
  {
    return inner;
  }

  /**
   * Returns this StackFrame's outer frame.
   * 
   * @return outer  This StackFrame's outer frame.
   */
  public StackFrame getOuter ()
  {
    return outer;
  }
  
  public int getEndLine ()
  {
    return endLine;
  }
  
  public void setEndLine (int i )
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

  public int getStartOffsset ()
  {
    return startOffsset;
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
    this.isSignalFrame = sigFrame;
  }
  
  /**
   * Returns whether or not this frame is a signal frame.
   * 
   * @return isSignalFrame  Whether or not this frame is a signal frame.
   */
  public boolean getIsSignalFrame()
  {
    return this.isSignalFrame;
  }
}
